package fr.imta.smartgrid.server;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.persistence.config.TargetServer;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

import static org.eclipse.persistence.config.PersistenceUnitProperties.*;
import io.vertx.ext.web.handler.BodyHandler;

public class VertxServer {
    private Vertx vertx;
    private EntityManager db; // database object

    public VertxServer() {
        this.vertx = Vertx.vertx();

        // setup database connexion
        Map<String, String> properties = new HashMap<>();

        properties.put(LOGGING_LEVEL, "FINE");
        properties.put(CONNECTION_POOL_MIN, "1");

        properties.put(TARGET_SERVER, TargetServer.None);

        var emf = Persistence.createEntityManagerFactory("smart-grid", properties);
        db = emf.createEntityManager();
    }

    public void start() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());


        router.get("/hello").handler(new ExampleHandler(this.db));
        router.get("/grids").handler(new HandlerGrid(this.db));
        router.get("/grid/:id").handler(new HandlerGrid(this.db));
        router.get("/grid/:id/production").handler(new HandlerGrid(this.db)); 
        router.get("/grid/:id/consumption").handler(new HandlerGrid(this.db));

        router.get("/persons").handler(new HandlerPersons(this.db));
        router.get("/person/:id").handler(new HandlerPersons(this.db));
        router.post("/person/:id").handler(new HandlerPersons(this.db));
        router.delete("/delete/person/:id").handler(new HandlerPersons(this.db));
        router.put("/person").handler(new HandlerPersons(this.db));


        router.post("/sensor/:id").handler(new SensorHandler(this.db));
        router.get("/sensor/:id").handler(new SensorHandler(this.db));
        router.get("/sensors/:kind").handler(new SensorHandler(this.db));
        

        router.get("/producers").handler(new HandlerProducer(this.db));
        router.get("/consumers").handler(new HandlerConsumers(this.db));

        router.get("/measurement/:id/values").handler(new HandlerMeasurement(this.db));
        router.get("/measurement/:id").handler(new HandlerMeasurement(this.db));


        router.post("/ingress/windturbine").handler(new WindTurbineHandler(this.db));

        
        
        
        /*router.delete("/person/:id").handler(new HandlerPersons(this.db));
        router.put("/person/:id").handler(new HandlerPersons(this.db));
        router.post("/person/:id").handler(new HandlerPersons(this.db)); */
        
        // start the server
        vertx.createHttpServer().requestHandler(router).listen(8080);
    }

    public static void main(String[] args) {
        new VertxServer().start();
    }
}
