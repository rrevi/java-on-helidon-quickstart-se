
package io.helidon.examples.quickstart.se;

import io.helidon.logging.common.LogConfig;
import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.health.DbClientHealthCheck;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.observe.ObserveFeature;
import io.helidon.webserver.observe.health.HealthObserver;

/**
 * The application main class.
 */
public class Main {

    /**
     * Cannot be instantiated.
     */
    private Main() {
    }

    /**
     * Application main entry point.
     * 
     * @param args command line arguments.
     */
    public static void main(String[] args) {

        // load logging configuration
        LogConfig.configureRuntime();

        // initialize global config from default configuration
        Config config = Config.create();
        Config.global(config);

        //
        // Database connection related configuration
        //
        DbClient dbClient = DbClient.create(config.get("db"));
        Contexts.globalContext().register(dbClient);

        HealthObserver healthObserver = HealthObserver.builder()
                .useSystemServices(false)
                .details(true)
                .addCheck(DbClientHealthCheck.create(dbClient, config.get("db.health-check")))
                .build();

        ObserveFeature observe = ObserveFeature.builder()
                .config(config.get("server.features.observe"))
                .addObserver(healthObserver)
                .build();

        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .addFeature(observe)
                .routing(Main::routing)
                .build()
                .start();

        System.out.println("WEB server is up! http://localhost:" + server.port() + "/simple-greet");

    }

    /**
     * Updates HTTP Routing.
     */
    static void routing(HttpRouting.Builder routing) {
        routing
                .register("/greet", new GreetService())
                .register("/library", new LibraryService())
                .get("/simple-greet", (req, res) -> res.send("Hello World!"));
    }
}