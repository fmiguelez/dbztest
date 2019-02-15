## Test project to reproduce [DBZ-1144](https://issues.jboss.org/browse/DBZ-1144)

Test project to reproduce crash using Debezium 0.8.3.Final (and also 0.9.1.Final) when working with PostGIS geometries.

### Environment

Docker compose running locally is required. We installed Ubuntu-18.04.1-0 on local Virtual Box VM (tested on Windows) but any other solution to get docker-compose running locally will do. 

VM Network configured using NAT. Port mapping:
```console
  5434 -> 5434 
```

### DB Setup

Start DB:
```console
$ sudo mkdir -p /var/lib/postgres/dbztest/data
$ export PGPASSWORD=dbztest
$ docker-compose -f docker-compose-dbztest.yml up
```

On another terminal:
```console
$ export PGPASSWORD=dbztest
$ docker-compose -f docker-compose-dbztest.yml exec postgresql bash -c 'psql -U postgres dbztest' 
```

Paste content of ddl.sql into psql terminal:
```console
dbztest=# ALTER USER postgres SET search_path to public;
dbztest=# CREATE EXTENSION IF NOT EXISTS postgis;
dbztest=# DROP TABLE IF EXISTS public.test_geom;
dbztest=# CREATE TABLE public.test_geom(id bigint NOT NULL, CONSTRAINT test_geom_pk PRIMARY KEY (id));
dbztest=# SELECT AddGeometryColumn ('public','test_geom','geom',4326,'GEOMETRY',2);
```

### Crash

Start application.

Paste content of data.sql into psql terminal:
```console
dbztest=# INSERT INTO public.test_geom (id, geom) VALUES (1, ST_GeomFromEWKT('SRID=4326;POLYGON ((-73.97283794446733 41.093070008400616, -73.97261212112936 41.10756167742005, -73.94387270343 41.10730179477687, -73.94410484096726 41.09281025753938, -73.97283794446733 41.093070008400616))'));
```

See DB crash:
```console
postgresql_1  | postgres: wal sender process postgres 10.0.2.2(15479) idle: protobuf-c/protobuf-c.c:643: protobuf_c_message_get_packed_size: Assertion `((message)->descriptor)->magic == PROTOBUF_C__MESSAGE_DESCRIPTOR_MAGIC' failed.
postgresql_1  | LOG:  server process (PID 178) was terminated by signal 6: Aborted
postgresql_1  | LOG:  terminating any other active server processes
postgresql_1  | WARNING:  terminating connection because of crash of another server process
postgresql_1  | DETAIL:  The postmaster has commanded this server process to roll back the current transaction and exit, because another server process exited abnormally and possibly corrupted shared memory.
postgresql_1  | HINT:  In a moment you should be able to reconnect to the database and repeat your command.
postgresql_1  | WARNING:  terminating connection because of crash of another server process
postgresql_1  | DETAIL:  The postmaster has commanded this server process to roll back the current transaction and exit, because another server process exited abnormally and possibly corrupted shared memory.
postgresql_1  | HINT:  In a moment you should be able to reconnect to the database and repeat your command.
postgresql_1  | WARNING:  terminating connection because of crash of another server process
postgresql_1  | DETAIL:  The postmaster has commanded this server process to roll back the current transaction and exit, because another server process exited abnormally and possibly corrupted shared memory.
postgresql_1  | HINT:  In a moment you should be able to reconnect to the database and repeat your command.
postgresql_1  | WARNING:  terminating connection because of crash of another server process
postgresql_1  | DETAIL:  The postmaster has commanded this server process to roll back the current transaction and exit, because another server process exited abnormally and possibly corrupted shared memory.
postgresql_1  | HINT:  In a moment you should be able to reconnect to the database and repeat your command.
postgresql_1  | LOG:  all server processes terminated; reinitializing
postgresql_1  | LOG:  database system was interrupted; last known up at 2019-02-15 12:06:49 GMT
postgresql_1  | LOG:  database system was not properly shut down; automatic recovery in progress
postgresql_1  | LOG:  redo starts at 0/1FBF618
postgresql_1  | LOG:  invalid record length at 0/1FC9038: wanted 24, got 0
postgresql_1  | LOG:  redo done at 0/1FC9000
postgresql_1  | LOG:  last completed transaction was at log time 2019-02-15 12:09:01.890756+00
postgresql_1  | LOG:  MultiXact member wraparound protections are now enabled
postgresql_1  | LOG:  database system is ready to accept connections
postgresql_1  | LOG:  autovacuum launcher started
```

And also Application (EOF received due to DB crash):
```console
115771 [debezium-postgresconnector-localhost:5434/dbztest-records-stream-producer] WARN  io.debezium.connector.postgresql.RecordsStreamProducer  - Closing replication stream due to db connection IO exception...
115826 [pool-2-thread-1] INFO  org.apache.kafka.connect.storage.FileOffsetBackingStore  - Stopped FileOffsetBackingStore
115828 [pool-2-thread-1] ERROR io.debezium.embedded.EmbeddedEngine  - Error while trying to run connector class 'io.debezium.connector.postgresql.PostgresConnector'
org.apache.kafka.connect.errors.ConnectException: An exception ocurred in the change event producer. This connector will be stopped.
  at io.debezium.connector.base.ChangeEventQueue.throwProducerFailureIfPresent(ChangeEventQueue.java:168)
  at io.debezium.connector.base.ChangeEventQueue.poll(ChangeEventQueue.java:149)
  at io.debezium.connector.postgresql.PostgresConnectorTask.poll(PostgresConnectorTask.java:146)
  at io.debezium.embedded.EmbeddedEngine.run(EmbeddedEngine.java:698)
  at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
  at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
  at java.lang.Thread.run(Thread.java:745)
Caused by: org.postgresql.util.PSQLException: Database connection failed when reading from copy
  at org.postgresql.core.v3.QueryExecutorImpl.readFromCopy(QueryExecutorImpl.java:964)
  at org.postgresql.core.v3.CopyDualImpl.readFromCopy(CopyDualImpl.java:41)
  at org.postgresql.core.v3.replication.V3PGReplicationStream.receiveNextData(V3PGReplicationStream.java:145)
  at org.postgresql.core.v3.replication.V3PGReplicationStream.readInternal(V3PGReplicationStream.java:114)
  at org.postgresql.core.v3.replication.V3PGReplicationStream.read(V3PGReplicationStream.java:60)
  at io.debezium.connector.postgresql.connection.PostgresReplicationConnection$1.read(PostgresReplicationConnection.java:198)
  at io.debezium.connector.postgresql.RecordsStreamProducer.streamChanges(RecordsStreamProducer.java:128)
  at io.debezium.connector.postgresql.RecordsStreamProducer.lambda$start$1(RecordsStreamProducer.java:114)
  at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
  at java.util.concurrent.FutureTask.run(FutureTask.java:266)
  ... 3 more
Caused by: java.io.EOFException
  at org.postgresql.core.PGStream.receiveChar(PGStream.java:284)
  at org.postgresql.core.v3.QueryExecutorImpl.processCopyResults(QueryExecutorImpl.java:1006)
  at org.postgresql.core.v3.QueryExecutorImpl.readFromCopy(QueryExecutorImpl.java:962)
  ... 12 more
```
