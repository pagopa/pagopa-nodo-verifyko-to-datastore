package it.gov.pagopa.nodoverifykotodatastore;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.nodoverifykotodatastore.exception.AppException;
import it.gov.pagopa.nodoverifykotodatastore.util.Constants;
import it.gov.pagopa.nodoverifykotodatastore.util.ObjectMapperUtils;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Azure Functions with Azure Event Hub trigger.
 * This function will be invoked when an Event Hub trigger occurs
 */
public class NodoVerifyKOEventToDataStore {

	private static final Integer MAX_RETRY_COUNT = 5;

	@FunctionName("EventHubNodoVerifyKOEventToDSProcessor")
	@ExponentialBackoffRetry(maxRetryCount = 5, maximumInterval = "00:15:00", minimumInterval = "00:00:10")
    public void processNodoVerifyKOEvent (
            @EventHubTrigger(
                    name = "NodoVerifyKOEvent",
                    eventHubName = "", // blank because the value is included in the connection string
                    connection = "EVENTHUB_CONN_STRING",
                    cardinality = Cardinality.MANY)
    		List<String> events,
    		@BindingName(value = "PropertiesArray") Map<String, Object>[] properties,
			@CosmosDBOutput(
					name = "NodoVerifyKOEventToDataStore",
					databaseName = "nodo_verifyko",
					containerName = "events",
					createIfNotExists = false,
					connection = "COSMOS_CONN_STRING")
			@NonNull OutputBinding<List<Object>> documentdb,
            final ExecutionContext context) {

		String errorCause = null;
		boolean isPersistenceOk = true;
		int retryIndex = context.getRetryContext() == null ? -1 : context.getRetryContext().getRetrycount();

		Logger logger = context.getLogger();
		logger.log(Level.FINE, () -> String.format("Persisting [%d] events...", events.size()));

		if (retryIndex == MAX_RETRY_COUNT) {
			logger.log(Level.WARNING, () -> String.format("[ALERT][LAST RETRY][VerifyKOToDS] Performing last retry for event ingestion: InvocationId [%s], Events: %s", context.getInvocationId(), events));
		}

        try {
        	if (events.size() == properties.length) {
				List<Object> eventsToPersist = new ArrayList<>();

				for (int index = 0; index < properties.length; index++) {
					final Map<String, Object> event = ObjectMapperUtils.readValue(events.get(index), Map.class);

					// update event with the required parameters and other needed fields
					properties[index].forEach((property, value) -> event.put(replaceDashWithUppercase(property), value));

					Map<String, Object> faultBeanMap = (Map) event.getOrDefault(Constants.FAULTBEAN_EVENT_FIELD, new HashMap<>());
					String faultBeanTimestamp = (String) faultBeanMap.getOrDefault(Constants.TIMESTAMP_EVENT_FIELD, "ERROR");

					// sometimes faultBeanTimestamp has less than 6 digits regarding microseconds
					faultBeanTimestamp = fixDateTime(faultBeanTimestamp);

					if (faultBeanTimestamp.contains("ERROR")) {
						throw new IllegalStateException("Missing " + Constants.FAULTBEAN_EVENT_FIELD + " or " + Constants.FAULTBEAN_TIMESTAMP_EVENT_FIELD);
					}

					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
					LocalDateTime dateTime = LocalDateTime.parse(faultBeanTimestamp, formatter);

					long timestamp = dateTime.toEpochSecond(ZoneOffset.UTC);
					faultBeanMap.put(Constants.TIMESTAMP_EVENT_FIELD, timestamp);
					faultBeanMap.put(Constants.DATE_TIME_EVENT_FIELD, faultBeanTimestamp);

					String insertedDateValue = dateTime.getYear() + "-" + dateTime.getMonthValue() + "-" + dateTime.getDayOfMonth();
					event.put(Constants.PARTITION_KEY_EVENT_FIELD, generatePartitionKey(event, insertedDateValue));

					eventsToPersist.add(event);
				}

				logger.log(Level.INFO, () -> String.format("Performing event ingestion: InvocationId [%s], Retry Attempt [%d], Events: %s", context.getInvocationId(), retryIndex, extractTraceForEventsToPersist(eventsToPersist)));

				// save all events in the retrieved batch in the storage
				persistEventBatch(logger, documentdb, eventsToPersist);
            } else {
				isPersistenceOk = false;
				errorCause = String.format("[ALERT][VerifyKOToDS] AppException - Error processing events, lengths do not match: [events: %d - properties: %d]", events.size(), properties.length);
            }
        } catch (IllegalArgumentException e) {
			isPersistenceOk = false;
			errorCause = "[ALERT][VerifyKOToDS] AppException - Illegal argument exception on cosmos nodo-verify-ko-events msg ingestion at " + LocalDateTime.now() + " : " + e;
		} catch (IllegalStateException e) {
			isPersistenceOk = false;
			errorCause = "[ALERT][VerifyKOToDS] AppException - Missing argument exception on nodo-verify-ko-events msg ingestion at " + LocalDateTime.now() + " : " + e;
		} catch (Exception e) {
			isPersistenceOk = false;
			errorCause = "[ALERT][VerifyKOToDS] AppException - Generic exception on cosmos nodo-verify-ko-events msg ingestion at " + LocalDateTime.now() + " : " + e.getMessage();
        }

		if (!isPersistenceOk) {
			String finalErrorCause = errorCause;
			logger.log(Level.SEVERE, () -> finalErrorCause);
			throw new AppException(errorCause);
		}
    }

	@SuppressWarnings({"unchecked"})
	private static String extractTraceForEventsToPersist(List<Object> eventsToPersist) {
        return Arrays.toString(eventsToPersist.stream()
                .map(event -> {
                    Map<String, Object> eventMap = (Map<String, Object>) event;
					String rowKey = getEventField(eventMap, "id", String.class, "null");
					String partitionKey = getEventField(eventMap, Constants.PARTITION_KEY_EVENT_FIELD, String.class, "null");
					Long eventTimestamp = getEventField(eventMap, "faultBean.timestamp", Long.class, -1L);
                    return String.format("{PartitionKey: %s, RowKey: %s, EventTimestamp: %d}", partitionKey, rowKey, eventTimestamp);
                })
				.toArray());
	}

	private String fixDateTime(String faultBeanTimestamp) {
		int dotIndex = faultBeanTimestamp.indexOf('.');
		if (dotIndex != -1) {
			int fractionLength = faultBeanTimestamp.length() - dotIndex - 1;
			faultBeanTimestamp = fractionLength < 6 ? String.format("%s%s", faultBeanTimestamp, "0".repeat(6 - fractionLength)) : faultBeanTimestamp;
		}
		else {
			faultBeanTimestamp = String.format("%s.000000", faultBeanTimestamp);
		}
		return faultBeanTimestamp;
	}

	private String replaceDashWithUppercase(String input) {
		if(!input.contains("-")){
			return input;
		}
		Matcher matcher = Constants.REPLACE_DASH_PATTERN.matcher(input);
		StringBuilder builder = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(builder, matcher.group(1).toUpperCase());
		}
		matcher.appendTail(builder);
		return builder.toString();
	}

	private void persistEventBatch(Logger logger, OutputBinding<List<Object>> documentdb, List<Object> eventsToPersistCosmos) {
		documentdb.setValue(eventsToPersistCosmos);
		logger.log(Level.FINE, () -> "Done processing events");
	}

	private String generatePartitionKey(Map<String, Object> event, String insertedDateValue) {
		return insertedDateValue.replace(":", "").replace(".", "").replace("T", "").replace("-", "") +
				"-" +
				getEventField(event, Constants.CREDITOR_ID_EVENT_FIELD, String.class, Constants.NA) +
				"-" +
				getEventField(event, Constants.PSP_ID_EVENT_FIELD, String.class, Constants.NA);
	}

	@SuppressWarnings({"rawtypes"})
	private static <T> T getEventField(Map<String, Object> event, String name, Class<T> clazz, T defaultValue) {
		T field = null;
		List<String> splitPath = List.of(name.split("\\."));
		Map eventSubset = event;
		Iterator<String> it = splitPath.listIterator();
		while(it.hasNext()) {
			Object retrievedEventField = eventSubset.get(it.next());
			if (!it.hasNext()) {
				field = clazz.cast(retrievedEventField);
			} else {
				eventSubset = (Map) retrievedEventField;
				if (eventSubset == null) {
					throw new IllegalArgumentException("The field [" + name + "] does not exists in the passed event.");
				}
			}
		}
		return field == null ? defaultValue : field;
	}
}
