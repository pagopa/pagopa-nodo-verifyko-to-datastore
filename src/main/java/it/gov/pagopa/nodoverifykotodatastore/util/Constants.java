package it.gov.pagopa.nodoverifykotodatastore.util;

import java.util.regex.Pattern;

public class Constants {

    private Constants() {}

    public static final String POM_PROPERTIES_PATH = "/META-INF/maven/it.gov.pagopa/nodoverifykotodatastore/pom.properties";
    public static final Pattern REPLACE_DASH_PATTERN = Pattern.compile("-([a-zA-Z])");
    public static final String NA = "NA";
    public static final String PARTITION_KEY_EVENT_FIELD = "PartitionKey";
    public static final String INSERTED_TIMESTAMP_EVENT_FIELD = "faultBean.timestamp";
    public static final String CREDITOR_ID_EVENT_FIELD = "creditor.idPA";
    public static final String PSP_ID_EVENT_FIELD = "psp.idPsp";
}
