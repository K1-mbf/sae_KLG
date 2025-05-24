package fr.imta.smartgrid.server;

import java.util.List;

import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class HandlerMeasurement implements Handler<RoutingContext> {
    EntityManager db;

    public HandlerMeasurement(EntityManager db) {
        this.db = db;
    }
    @Override
    public void handle(RoutingContext event) {

        System.out.println("Route called: " + event.currentRoute().getName());
        System.out.println("Request HTTP method: " + event.request().method());
        System.out.println("We received these query parameters: " + event.queryParam("from"));
        System.out.println("We have these path params: " + event.pathParams());
        System.out.println("Value for the path param 'name': " + event.pathParam("name"));
        System.out.println("We received this body: " + event.body().asString());

        
        
        if (event.currentRoute().getName().matches("\\/measurement\\/:id\\/values") && event.pathParam("id").matches("[0-9]+") ) {
            
            String id = event.pathParam("id");
            String from = event.queryParam("from").isEmpty() ?  "0" : event.queryParam("from").get(0);
            String to = event.queryParam("to").isEmpty()  ?  "2147483646" : event.queryParam("to").get(0);
            
            
            JsonObject res = new JsonObject();
            res.put("sensor_id", db.createNativeQuery("SELECT sensor FROM measurement WHERE id = " + id).getSingleResult());
            res.put("measurement_id", Integer.parseInt(id));


            List<Object[]>  values = (List<Object[]>) db.createNativeQuery(
                    "SELECT timestamp, value FROM datapoint WHERE measurement = " + id + 
                    " AND timestamp BETWEEN " + from + " AND " + to + 
                    " ORDER BY timestamp"
                ).getResultList();


                JsonArray valuesArray = new JsonArray();
            for (Object[] row : values) {
                JsonObject valueObj = new JsonObject();
                valueObj.put("timestamp", row[0]);
                valueObj.put("value", row[1]);
                valuesArray.add(valueObj);
            }
            res.put("values", valuesArray);
            
            event.json(res);
            
         
        } else if (event.currentRoute().getName().matches("\\/measurement\\/(.*)") && event.pathParam("id").matches("[0-9]+")) {
            Object[] sql = (Object[]) db.createNativeQuery("SELECT name, unit, sensor FROM measurement WHERE id = " +event.pathParam("id")).getSingleResult();
            
            JsonObject res = new JsonObject();

            res.put("name", sql[0]);
            res.put("unit", sql[1]);
            res.put("sensor", sql[2]);
            
            event.json(res);

        } else  {
            //List<Integer> measurement = (List<Integer>) db.createNativeQuery("SELECT id FROM measurement").getResultList();
            event.end("error 404 request not found");
            //event.json(measurement);
        }
        
    }

}