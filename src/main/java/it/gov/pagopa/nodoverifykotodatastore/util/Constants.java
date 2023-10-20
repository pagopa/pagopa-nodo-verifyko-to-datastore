package it.gov.pagopa.nodoverifykotodatastore.util;

import java.util.regex.Pattern;

public class Constants {

    private Constants() {}

    public static final String POM_PROPERTIES_PATH = "/META-INF/maven/it.gov.pagopa/nodoverifykotodatastore/pom.properties";
    public static final Pattern REPLACE_DASH_PATTERN = Pattern.compile("-([a-zA-Z])");
    public static final String NA = "NA";
    public static final String UNIQUE_ID_EVENT_FIELD = "uniqueId";
    public static final String ID_EVENT_FIELD = "id";
    public static final String ID_DOMINIO_EVENT_FIELD = "idDominio";
    public static final String PSP_EVENT_FIELD = "psp";
    public static final String INSERTED_TIMESTAMP_EVENT_FIELD = "insertedTimestamp";
    public static final String INSERTED_DATE_EVENT_FIELD = "insertedDate";
    public static final String PARTITION_KEY_EVENT_FIELD = "PartitionKey";
    public static final String PAYLOAD_EVENT_FIELD = "payload";
}
