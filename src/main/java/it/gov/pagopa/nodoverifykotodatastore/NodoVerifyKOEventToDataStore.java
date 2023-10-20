package it.gov.pagopa.nodoverifykotodatastore;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.*;
import it.gov.pagopa.nodoverifykotodatastore.util.Constants;
import it.gov.pagopa.nodoverifykotodatastore.util.ObjectMapperUtils;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
		logger.log(Level.INFO, "Persisting {0} events...", events.size());

        try {
        	if (events.size() == properties.length) {
				List<Object> eventsToPersist = new ArrayList<>();

				for (int index = 0; index < properties.length; index++) {
					final Map<String, Object> event = ObjectMapperUtils.readValue(events.get(index), Map.class);

					// update event with the required parameters and other needed fields
					properties[index].forEach((property, value) -> event.put(replaceDashWithUppercase(property), value));
					event.put(Constants.ID_EVENT_FIELD, event.get(Constants.UNIQUE_ID_EVENT_FIELD));
					String insertedDateValue = event.get(Constants.INSERTED_TIMESTAMP_EVENT_FIELD) != null ? ((String)event.get(Constants.INSERTED_TIMESTAMP_EVENT_FIELD)).substring(0, 10) : Constants.NA;
					event.put(Constants.INSERTED_DATE_EVENT_FIELD, insertedDateValue);
					event.put(Constants.PARTITION_KEY_EVENT_FIELD, generatePartitionKey(event, insertedDateValue));
					event.put(Constants.PAYLOAD_EVENT_FIELD, null);

					eventsToPersist.add(event);
				}

				// save all events in the retrieved batch in the storage
				persistEventBatch(logger, documentdb, eventsToPersist);
            } else {
				logger.log(Level.SEVERE, "Error processing events, lengths do not match [{0}, {1}]", new Object[]{events.size(), properties.length});
            }
        } catch (NullPointerException e) {
            logger.severe("NullPointerException exception on cosmos nodo-verify-ko-events msg ingestion at "+ LocalDateTime.now()+ " : " + e.getMessage());
        } catch (Throwable e) {
            logger.severe("Generic exception on cosmos nodo-verify-ko-events msg ingestion at "+ LocalDateTime.now()+ " : " + e.getMessage());
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
		try {
			documentdb.setValue(eventsToPersistCosmos);
			logger.info("Done processing events");
		} catch (Throwable t){
			logger.log(Level.SEVERE, "Could not save {0} events on CosmosDB, error: [{1}]", new Object[]{eventsToPersistCosmos.size(), t});
		}
	}

	private String generatePartitionKey(Map<String, Object> event, String insertedDateValue) {
		return new StringBuilder().append(insertedDateValue)
				.append("-")
				.append(event.get(Constants.ID_DOMINIO_EVENT_FIELD) != null ? event.get(Constants.ID_DOMINIO_EVENT_FIELD).toString() : Constants.NA)
				.append("-")
				.append(event.get(Constants.PSP_EVENT_FIELD) != null ? event.get(Constants.PSP_EVENT_FIELD).toString() : Constants.NA)
				.toString();
	}
}
