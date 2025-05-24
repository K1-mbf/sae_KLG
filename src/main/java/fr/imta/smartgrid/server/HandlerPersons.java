package fr.imta.smartgrid.server;

import java.util.List;


import fr.imta.smartgrid.model.Consumer;
import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Grid;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Producer;
import fr.imta.smartgrid.model.Sensor;
import fr.imta.smartgrid.model.SolarPanel;
import fr.imta.smartgrid.model.WindTurbine;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
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

        if (event.request().method().toString() == "GET"){
            if (event.currentRoute().getName().matches("/person/:id") && event.pathParam("id").matches("[0-9]+")){
            Object[] sql = (Object[]) db.createNativeQuery("SELECT firstname, lastname, grid FROM person WHERE id = " +event.pathParam("id")).getSingleResult();
            List<Integer> sql2 = (List<Integer>) db.createNativeQuery("SELECT DISTINCT Sensor.id FROM Sensor JOIN Person_Sensor ON Sensor.id = Person_Sensor.Sensor_id JOIN Person ON Person.id = Person_Sensor.person_id").getResultList();
            
            JsonObject res = new JsonObject();

            res.put("id", event.pathParam("id"));
            res.put("first_name", sql[0]);
            res.put("last_name", sql[1]);
            res.put("grid", sql[2]);
            res.put("owned_sensors",sql2);
            
            event.json(res);

        }else if (event.currentRoute().getName().matches("/persons")) {
            List<Integer> persons = (List<Integer>) db.createNativeQuery("SELECT id FROM person").getResultList();
            event.json(persons);
        }else {
            event.end("error 404: Not found");
        }
        
        }else if (event.request().method().toString() == "POST"){

            
            JsonObject json = event.body().asJsonObject();
            String id = event.pathParam("id");
            
            if (event.pathParam("id")!=null){
                Person p = (Person) db.find(Person.class, Integer.parseInt(id));

                if (json.containsKey("first_name")){
                    p.setFirstName(json.getString("first_name"));
                }
                if (json.containsKey("last_name")){
                    p.setLastName(json.getString("last_name"));
                }
                if (json.containsKey("grid") && json.getInteger("grid") != null) {
                    Grid grid = db.find(Grid.class, json.getInteger("grid"));
                    if (grid != null) {
                        p.setGrid(grid);
                    }
                }
                if (json.containsKey("owned_sensors")) {
                    p.getSensors().clear(); // Optional: clear if replacing
                    for (Integer s : (List<Integer>) json.getJsonArray("owned_sensors").getList()) {
                        Sensor sensor = db.find(Sensor.class, s);
                        if (sensor != null) {
                            p.addSensor(sensor);
                        }
                    }
                }
                

                // when you want to make change to the DB you need to start a transaction
                db.getTransaction().begin();
                // then you can register your new or modified objects to be saved
                db.merge(p);
                // finally you can commit the change
                db.getTransaction().commit();

                
                
                event.response().setStatusCode(200).end("Person updated successfully");
            }
        }else if (event.request().method().toString() == "DELETE"){
            System.out.println("DELETE request received for person with ID: " + event.pathParam("id"));
            String id = event.pathParam("id");
            Person p = (Person) db.find(Person.class, Integer.parseInt(id));
            if (p != null) {
                try{
                    db.getTransaction().begin();
                    db.remove(p);
                    db.getTransaction().commit();
                    event.response().setStatusCode(200).end("Person deleted successfully");
                } catch (Exception e) {
                    //db.getTransaction().rollback();
                    event.response().setStatusCode(500).end("Error deleting person: " + e.getMessage());
                }
            } else {
                event.response().setStatusCode(404).end("Person not found");
            }
        }else if (event.request().method().toString() == "PUT") {


            System.out.println("PUT request received for person with ID: " + event.pathParam("id"));

            JsonObject json = event.body().asJsonObject();

            
            Person p = new Person();

            if (json.containsKey("first_name")) {
                p.setFirstName(event.pathParam("first_name"));
            }
            if (json.containsKey("last_name")) {
                p.setLastName(event.pathParam("last_name"));
            }
            if (json.containsKey("grid")) {
                Grid grid = db.find(Grid.class, json.getInteger("grid"));
                if (grid != null) {
                    p.setGrid(grid);
                }
            }
            if (json.containsKey("owned_sensors")) {
                p.getSensors().clear(); // Optional: clear if replacing
                for (Integer s : (List<Integer>) json.getJsonArray("owned_sensors").getList()) {
                    Sensor sensor = db.find(Sensor.class, s);
                    if (sensor != null) {
                        p.addSensor(sensor);
                    }
                }
            }
            if (p != null) {
                try{
                    db.getTransaction().begin();
                    db.persist(p);
                    db.getTransaction().commit();
                    event.response().setStatusCode(200).end("Person updated successfully");

                    JsonObject res = new JsonObject();
                    res.put("id", p.getId());
                    event.json(res);
                } catch (Exception e) {
                    //db.getTransaction().rollback();
                    event.response().setStatusCode(500).end("Error updating person: " + e.getMessage());
                }
            } else {
                event.response().setStatusCode(404).end("Person not found");
            }
        }

    }

}