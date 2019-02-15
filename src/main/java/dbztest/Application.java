package dbztest;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.debezium.config.Configuration;
import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.embedded.EmbeddedEngine;

@SpringBootApplication
public class Application implements Consumer<SourceRecord> {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Value("${source.database.url}")
    private String url;

    @Value("${source.database.user}")
    private String databaseUser;

    @Value("${source.database.password}")
    private String databasePassword;

    @Value("${debezium.embedded.slotName}")
    private String slotName;

    @Value("${debezium.embedded.tableWhitelist}")
    private String tableWhitelist;

    @Value("${debezium.embedded.offsetFile}")
    private String offsetFile;

    private EmbeddedEngine engine;

    @PostConstruct
    public void start() {
        String cleanURI = url.substring(5);
        URI uri = URI.create(cleanURI);
        String databaseHostname = uri.getHost();
        int databasePort = uri.getPort();
        String databaseDbname = uri.getPath().substring(1);

        // Define the configuration for the embedded and PostgreSQL connector ...
        // @formatter:off
        Configuration config = Configuration.create()
                .with("connector.class", PostgresConnector.class.getName())
                .with("slot.name", slotName)
                .with("name", "slotName")
                .with("database.hostname", databaseHostname)
                .with("database.port", databasePort)
                .with("database.user", databaseUser)
                .with("database.password", databasePassword)
                .with("database.dbname", databaseDbname)
                .with("table.whitelist", tableWhitelist)
                .with("time.precision.mode", "connect")
                .with("offset.storage", FileOffsetBackingStore.class.getName())
                .with("offset.storage.file.filename", offsetFile)
                .build();
        // @formatter:on

        // Create the engine with this configuration ...
        // @formatter:off
        engine = EmbeddedEngine.create()
                .using(config)
                .notifying(this)
                .build();
        // @formatter:on

        // Run the engine asynchronously ...
        Executor executor = Executors.newFixedThreadPool(1);
        executor.execute(engine);
    }

    @PreDestroy
    public void stop() {
        engine.stop();
        File f = new File(offsetFile);
        if (f.exists()) {
            f.delete();
        }
    }

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);

        logger.info("user.dir is " + System.getProperty("user.dir"));
        logger.info("home.dir is " + System.getProperty("home.dir"));

        logger.info("args are " + Arrays.toString(args));

        // http://patorjk.com/software/taag/#p=display&f=Old%20Banner&t=DBZTEST
        String banner = "\n";

        banner += "######  ######  ####### ####### #######  #####  ####### \n";
        banner += "#     # #     #      #     #    #       #     #    #  \n";
        banner += "#     # #     #     #      #    #       #          #    \n";
        banner += "#     # ######     #       #    #####    #####     #    \n";
        banner += "#     # #     #   #        #    #             #    #    \n";
        banner += "#     # #     #  #         #    #       #     #    #    \n";
        banner += "######  ######  #######    #    #######  #####     #    \n";

        logger.info(banner);
    }

    @Override
    public void accept(SourceRecord t) {
        logger.info(" >> {}", t);
    }
}
