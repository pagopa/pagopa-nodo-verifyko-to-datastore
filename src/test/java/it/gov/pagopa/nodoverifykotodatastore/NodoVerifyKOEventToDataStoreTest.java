package it.gov.pagopa.nodoverifykotodatastore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.OutputBinding;
import it.gov.pagopa.nodoverifykotodatastore.util.LogHandler;
import it.gov.pagopa.nodoverifykotodatastore.util.TestUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NodoVerifyKOEventToDataStoreTest {

    @Spy
    NodoVerifyKOEventToDataStore function;

    @Mock
    ExecutionContext context;


    @SuppressWarnings("unchecked")
    @Test
    @SneakyThrows
    void runOk_withoutAdditionalProperties() {
        // mocking objects
        Logger logger = Logger.getLogger("NodoVerifyKOEventToDataStore-test-logger");
        when(context.getLogger()).thenReturn(logger);
        OutputBinding<List<Object>> document = (OutputBinding<List<Object>>) mock(OutputBinding.class);

        // generating input
        String eventInStringForm = TestUtil.readStringFromFile("events/event_ok_1.json");
        List<String> events = new ArrayList<>();
        events.add(eventInStringForm);
        Map<String, Object>[] properties = new HashMap[1];
        properties[0] = new HashMap<>();

        // generating expected output
        Map<String, Object> expectedEvent = new ObjectMapper().readValue(eventInStringForm, Map.class);
        expectedEvent.put("PartitionKey", "20230101-77777777777-88888888888");
        List<Object> expectedEventsToPersist = List.of(expectedEvent);

        // execute logic
        function.processNodoVerifyKOEvent(events, properties, document, context);

        // test assertion
        verify(document).setValue(expectedEventsToPersist);
    }


    @SuppressWarnings("unchecked")
    @Test
    @SneakyThrows
    void runOk_multipleEvents() {
        // mocking objects
        Logger logger = Logger.getLogger("NodoVerifyKOEventToDataStore-test-logger");
        when(context.getLogger()).thenReturn(logger);
        OutputBinding<List<Object>> document = (OutputBinding<List<Object>>) mock(OutputBinding.class);

        // generating input
        String eventInStringForm1 = TestUtil.readStringFromFile("events/event_ok_1.json");
        List<String> events = new ArrayList<>();
        events.add(eventInStringForm1);
        String eventInStringForm2 = TestUtil.readStringFromFile("events/event_ok_2.json");
        events.add(eventInStringForm2);
        Map<String, Object>[] properties = new HashMap[2];
        properties[0] = new HashMap<>();
        properties[0].put("prop1_without_dash", true);
        properties[0].put("prop1-with-dash", "1");
        properties[1] = new HashMap<>();
        properties[1].put("prop1_without_dash", false);
        properties[1].put("prop1-with-dash", "2");

        // generating expected output
        Map<String, Object> expectedEvent1 = new ObjectMapper().readValue(eventInStringForm1, Map.class);
        expectedEvent1.put("PartitionKey", "20230101-77777777777-88888888888");
        expectedEvent1.put("prop1_without_dash", true);
        expectedEvent1.put("prop1WithDash", "1");
        Map<String, Object> expectedEvent2 = new ObjectMapper().readValue(eventInStringForm2, Map.class);
        expectedEvent2.put("PartitionKey", "20230102-77777777777-88888888888");
        expectedEvent2.put("prop1_without_dash", false);
        expectedEvent2.put("prop1WithDash", "2");
        List<Object> expectedEventsToPersist = List.of(expectedEvent1, expectedEvent2);

        // execute logic
        function.processNodoVerifyKOEvent(events, properties, document, context);

        // test assertion
        verify(document).setValue(expectedEventsToPersist);
    }

    @SuppressWarnings("unchecked")
    @Test
    @SneakyThrows
    void runKo_invalidNumberOfProperties() {
        // mocking objects
        Logger logger = Logger.getLogger("NodoVerifyKOEventToDataStore-test-logger");
        LogHandler logHandler = new LogHandler();
        logger.addHandler(logHandler);
        when(context.getLogger()).thenReturn(logger);
        OutputBinding<List<Object>> document = (OutputBinding<List<Object>>) mock(OutputBinding.class);

        // generating input
        String eventInStringForm = TestUtil.readStringFromFile("events/event_ok_1.json");
        List<String> events = new ArrayList<>();
        events.add(eventInStringForm);
        Map<String, Object>[] properties = new HashMap[2];
        properties[0] = new HashMap<>();
        properties[0].put("prop1_without_dash", true);
        properties[0].put("prop1-with-dash", "1");
        properties[1] = new HashMap<>();
        properties[1].put("prop1_without_dash", false);
        properties[1].put("prop1-with-dash", "2");

        // execute logic
        function.processNodoVerifyKOEvent(events, properties, document, context);

        // test assertion
        assertTrue(logHandler.getLogs().contains("Error processing events, lengths do not match: [events: 1 - properties: 2]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @SneakyThrows
    void runKo_missingFaultBeanTimestamp() {
        // mocking objects
        Logger logger = Logger.getLogger("NodoVerifyKOEventToDataStore-test-logger");
        LogHandler logHandler = new LogHandler();
        logger.addHandler(logHandler);
        when(context.getLogger()).thenReturn(logger);
        OutputBinding<List<Object>> document = (OutputBinding<List<Object>>) mock(OutputBinding.class);

        // generating input
        String eventInStringForm = TestUtil.readStringFromFile("events/event_ko_1.json");
        List<String> events = new ArrayList<>();
        events.add(eventInStringForm);
        Map<String, Object>[] properties = new HashMap[1];
        properties[0] = new HashMap<>();
        properties[0].put("prop1_without_dash", true);
        properties[0].put("prop1-with-dash", "1");

        // execute logic
        function.processNodoVerifyKOEvent(events, properties, document, context);

        // test assertion
        assertTrue(logHandler.getLogs().contains("java.lang.IllegalArgumentException: The field [faultBean.timestamp] does not exists in the passed event."));
    }

    @SuppressWarnings("unchecked")
    @Test
    @SneakyThrows
    void runKo_() {
        // mocking objects
        Logger logger = Logger.getLogger("NodoVerifyKOEventToDataStore-test-logger");
        LogHandler logHandler = new LogHandler();
        logger.addHandler(logHandler);
        when(context.getLogger()).thenReturn(logger);
        OutputBinding<List<Object>> document = (OutputBinding<List<Object>>) mock(OutputBinding.class);
        doThrow(NullPointerException.class).when(document).setValue(anyList());

        // generating input
        String eventInStringForm = TestUtil.readStringFromFile("events/event_ok_1.json");
        List<String> events = new ArrayList<>();
        events.add(eventInStringForm);
        Map<String, Object>[] properties = new HashMap[1];
        properties[0] = new HashMap<>();
        properties[0].put("prop1_without_dash", true);
        properties[0].put("prop1-with-dash", "1");

        // execute logic
        function.processNodoVerifyKOEvent(events, properties, document, context);

        // test assertion
        assertTrue(logHandler.getLogs().contains("[ALERT][VerifyKOToDS] AppException - Generic exception on cosmos nodo-verify-ko-events msg ingestion"));
    }

}
