# pagoPA Functions nodo-verifyko-to-datastore

Java nodo-verifyko-to-datastore Azure Function.
The function aims to dump verify KO event sent via Azure Event Hub to a CosmosDB, with a TTL of 120 days, and to an Azure Table Storage with a TTL of 10 years.

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-nodo-verifyko-to-datastore&metric=alert_status)](https://sonarcloud.io/dashboard?id=pagopa_pagopa-nodo-verifyko-to-datastore)


---

## Run locally with Docker
`docker build -t pagopa-functions-nodo-verifyko-to-datastore .`

`docker run -it -rm -p 8999:80 pagopa-functions-nodo-verifyko-to-datastore`

### Test
`curl http://localhost:8999/example`

## Run locally with Maven

`mvn clean package`

`mvn azure-functions:run`

### Test
`curl http://localhost:7071/example`

---
