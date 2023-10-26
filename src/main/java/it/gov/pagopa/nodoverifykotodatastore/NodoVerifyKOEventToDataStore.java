package it.gov.pagopa.nodoverifykotodatastore;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.nodoverifykotodatastore.util.Constants;
import it.gov.pagopa.nodoverifykotodatastore.util.ObjectMapperUtils;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Azure Functions with Azure Event Hub trigger.
 * This function will be invoked when an Event Hub trigger occurs
 */
public class NodoVerifyKOEventToDataStore {

	@FunctionName("EventHubNodoVerifyKOEventToDSProcessor")
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


		Logger logger = context.getLogger();
		logger.log(Level.INFO, () -> String.format("Persisting [%d] events...", events.size()));

        try {
        	if (events.size() == properties.length) {
				List<Object> eventsToPersist = new ArrayList<>();

				for (int index = 0; index < properties.length; index++) {
					final Map<String, Object> event = ObjectMapperUtils.readValue(events.get(index), Map.class);

					// update event with the required parameters and other needed fields
					properties[index].forEach((property, value) -> event.put(replaceDashWithUppercase(property), value));

					String insertedTimestampValue = getEventField(event, Constants.INSERTED_TIMESTAMP_EVENT_FIELD, String.class, Constants.NA);
					String insertedDateValue = Constants.NA.equals(insertedTimestampValue) ? Constants.NA : insertedTimestampValue.substring(0, 10);
					event.put(Constants.PARTITION_KEY_EVENT_FIELD, generatePartitionKey(event, insertedDateValue));

					eventsToPersist.add(event);
				}

				// save all events in the retrieved batch in the storage
				persistEventBatch(logger, documentdb, eventsToPersist);
            } else {
				logger.log(Level.SEVERE, () -> String.format("[ALERT][VerifyKOToDS] AppException - Error processing events, lengths do not match: [events: %d - properties: %d]", events.size(), properties.length));
            }
        } catch (NullPointerException e) {
            logger.log(Level.SEVERE, () -> "[ALERT][VerifyKOToDS] AppException - NullPointerException exception on cosmos nodo-verify-ko-events msg ingestion at " + LocalDateTime.now() + " : " + e.getMessage());
        } catch (Exception e) {
			logger.log(Level.SEVERE, () -> "[ALERT][VerifyKOToDS] AppException - Generic exception on cosmos nodo-verify-ko-events msg ingestion at " + LocalDateTime.now() + " : " + e.getMessage());
        }
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
		logger.info("Done processing events");
	}

	private String generatePartitionKey(Map<String, Object> event, String insertedDateValue) {
		return insertedDateValue.replace(":", "").replace(".", "").replace("T", "").replace("-", "") +
				"-" +
				getEventField(event, Constants.CREDITOR_ID_EVENT_FIELD, String.class, Constants.NA) +
				"-" +
				getEventField(event, Constants.PSP_ID_EVENT_FIELD, String.class, Constants.NA);
	}

	private <T> T getEventField(Map<String, Object> event, String name, Class<T> clazz, T defaultValue) {
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
			}
		}
		return field == null ? defaultValue : field;
	}
}
