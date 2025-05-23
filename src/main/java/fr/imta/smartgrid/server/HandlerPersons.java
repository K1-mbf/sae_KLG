package fr.imta.smartgrid.server;

import java.util.List;

import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

public class HandlerPersons implements Handler<RoutingContext> {
    EntityManager db;

    public HandlerPersons(EntityManager db) {
        this.db = db;
    }
    @Override
    public void handle(RoutingContext event) {

        System.out.println("Route called: " + event.currentRoute().getName());
        System.out.println("Request HTTP method: " + event.request().method());
        System.out.println("We received these query parameters: " + event.queryParams());
        System.out.println("We have these path params: " + event.pathParams());
        System.out.println("Value for the path param 'name': " + event.pathParam("name"));
        System.out.println("We received this body: " + event.body().asString());

        
        
        if (event.currentRoute().getName().matches("/person/:id") && event.pathParam("id").matches("[0-9]+")){
            Object[] sql = (Object[]) db.createNativeQuery("SELECT firstname, lastname, grid FROM person WHERE id = " +event.pathParam("id")).getSingleResult();
            List<Integer> sql2 = (List<Integer>) db.createNativeQuery("SELECT DISTINCT Sensor.id FROM Sensor JOIN Person_Sensor ON Sensor.id = Person_Sensor.Sensor_id JOIN Person ON Person.id = Person_Sensor.person_id").getResultList();
            
            JsonObject res = new JsonObject();

            res.put("firstName", sql[0]);
            res.put("lastName", sql[1]);
            res.put("grid", sql[2]);
            res.put("owned_sensors",sql2);
            
            event.json(res);

            
        }

        if (event.currentRoute().getName().matches("/delete/person/:id") && event.pathParam("id").matches("[0-9]+")){
            Person persone = (Person) db.find(Person.class, Integer.parseInt(event.pathParam("id")));
            db.remove(persone);
        }
         else {
            List<Integer> persons = (List<Integer>) db.createNativeQuery("SELECT id FROM person").getResultList();
            event.json(persons);
        }
    }

}