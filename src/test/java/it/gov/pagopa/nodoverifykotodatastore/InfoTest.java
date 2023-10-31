package it.gov.pagopa.nodoverifykotodatastore;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.logging.Logger;

import it.gov.pagopa.nodoverifykotodatastore.util.AppInfo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InfoTest {

    @Mock
    ExecutionContext context;

    @Spy
    Info infoFunction;

    @Test
    void runHttpTriggerOK() {
        // test precondition
        Logger logger = Logger.getLogger("NodoVerifyKOEventToDataStore-test-logger");
        when(context.getLogger()).thenReturn(logger);

        // test precondition
        final HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class);
        @SuppressWarnings("unchecked")
        HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);

        HttpResponseMessage responseMock = mock(HttpResponseMessage.class);
        doReturn(HttpStatus.OK).when(responseMock).getStatus();
        doReturn(builder).when(builder).body(any());
        doReturn(responseMock).when(builder).build();
        doReturn(builder).when(request).createResponseBuilder(any(HttpStatus.class));
        doReturn(builder).when(builder).header(anyString(), anyString());

        // test execution
        HttpResponseMessage response = infoFunction.run(request, context);

        // test assertion
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @SneakyThrows
    @Test
    void getInfoOk() {

        // Mocking service creation
        Logger logger = Logger.getLogger("NodoVerifyKOEventToDataStore-test-logger");
        String path = "/META-INF/maven/it.gov.pagopa/nodoverifykotodatastore/pom.properties";

        // Execute function
        AppInfo response = infoFunction.getInfo(logger, path);

        // Checking assertions
        assertNotNull(response.getName());
        assertNotNull(response.getVersion());
        assertNotNull(response.getEnvironment());
    }

    @SneakyThrows
    @Test
    void getInfoKo() {

        // Mocking service creation
        Logger logger = Logger.getLogger("NodoVerifyKOEventToDataStore-test-logger");

        // Execute function
        AppInfo response = infoFunction.getInfo(logger, null);

        // Checking assertions
        assertNull(response.getName());
        assertNull(response.getVersion());
        assertNotNull(response.getEnvironment());
    }

}