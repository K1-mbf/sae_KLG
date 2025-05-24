package fr.imta.smartgrid.server;

import java.util.List;

import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class HandlerGrid implements Handler<RoutingContext> {
    EntityManager db;

    public HandlerGrid(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext event) {

        
       
        
        if (event.currentRoute().getName().matches("/grid/:id/production") && event.pathParam("id").matches("[0-9]+")){
            Double prod = (Double) db.createNativeQuery(
                "SELECT SUM(d.value) " +
                "FROM Grid g " +
                "JOIN Sensor s ON s.grid = g.id " +
                "JOIN Producer p ON p.id = s.id " +
                "JOIN Measurement m ON m.sensor = s.id " +
                "JOIN Datapoint d ON d.measurement = m.id " +
                "WHERE g.id = "+ event.pathParam("id")
            ).getSingleResult();


            event.json(prod);
        }

        else if (event.currentRoute().getName().matches("/grid/:id/consumption") && event.pathParam("id").matches("[0-9]+")) {
            Double cons = (Double) db.createNativeQuery(
                "SELECT SUM(d.value) " +
                "FROM Grid g " +
                "JOIN Sensor s ON s.grid = g.id " +
                "JOIN Consumer c ON c.id = s.id " +
                "JOIN Measurement m ON m.sensor = s.id " +
                "JOIN Datapoint d ON d.measurement = m.id " +
                "WHERE g.id = " + event.pathParam("id")
            ).getSingleResult();

            event.json(cons);
        
        }
        else if (event.pathParam("id") == null){
            List<Integer> grids = (List<Integer>) db.createNativeQuery("SELECT id FROM grid").getResultList();
            event.json(grids);
        }
        else {
            Object[] sql = (Object[]) db.createNativeQuery("SELECT name,description FROM grid WHERE id = " + event.pathParam("id")).getSingleResult();
            List<Integer> sensors = (List<Integer>) db.createNativeQuery("SELECT sensor.id FROM sensor WHERE sensor.grid = "+event.pathParam("id") ).getResultList();
            List<Integer> users = (List<Integer>) db.createNativeQuery("SELECT person.id FROM person WHERE person.grid = "+event.pathParam("id") ).getResultList();

            JsonObject res = new JsonObject();

            res.put("description", sql[1]);
            res.put("name", sql[0]);
            res.put("sensors", sensors);
            res.put("users",users);
            
            event.json(res);

        }
        
    }
}