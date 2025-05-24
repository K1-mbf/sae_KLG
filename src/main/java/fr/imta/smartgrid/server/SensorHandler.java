
package fr.imta.smartgrid.server;

import java.util.List;

import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Sensor;
import fr.imta.smartgrid.model.SolarPanel;
import fr.imta.smartgrid.model.WindTurbine;
import fr.imta.smartgrid.model.Producer;
import fr.imta.smartgrid.model.Consumer;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;


public class SensorHandler implements Handler<RoutingContext> {
    EntityManager db;

    public SensorHandler(EntityManager db) {
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
            if (event.currentRoute().getName().matches("\\/sensor\\/(.*)") && event.pathParam("id").matches("[0-9]+")) {
                JsonObject res = getJsonById(event.pathParam("id"));
                event.json(res);

            }else if (event.currentRoute().getName().matches("\\/sensors\\/(.*)") && event.pathParam("kind").matches("EVCharger|WindTurbine|SolarPanel")){ 
                
                List<Integer> sensors_match = (List<Integer>) db.createNativeQuery("SELECT s.id FROM sensor as s WHERE s.dtype='" + event.pathParam("kind")+ "'").getResultList();
                event.json(sensors_match);
            }
            else{
                event.end("error 404: Not found");
            }
        }else if (event.request().method().toString() == "POST"){

            //System.out.println("\n\n\n C'est un post wooooooo \n\n\n");
            JsonObject json = event.body().asJsonObject();
            String id = event.pathParam("id");
            
            if (event.pathParam("id")!=null){
                Sensor s = (Sensor) db.find(Sensor.class, Integer.parseInt(id));

                if (json.containsKey("name")) {
                    s.setName(json.getString("name"));
                }
                if (json.containsKey("description")) {
                    s.setDescription(json.getString("description"));
                }
                if (json.containsKey("owners")) {
                    s.getOwners().clear(); // Optional: clear if replacing
                    for (Integer p : (List<Integer>) json.getJsonArray("owners").getList()) {
                        s.addOwner(db.find(Person.class, p));
                    }
                }

                if (s instanceof Producer) {
                    Producer p = (Producer) s;
                    if (json.containsKey("power_source")) {
                        p.setPowerSource(json.getString("power_source"));
                    }
                } else if (s instanceof Consumer) {
                    Consumer c = (Consumer) s;
                    if (json.containsKey("max_power")) {
                        c.setMaxPower(json.getDouble("max_power"));
                    }
                }

                if (s instanceof EVCharger) {
                    EVCharger charger = (EVCharger) s;
                    if (json.containsKey("type")) {
                        charger.setType(json.getString("type"));
                    }
                    if (json.containsKey("voltage")) {
                        charger.setVoltage(json.getInteger("voltage"));
                    }
                    if (json.containsKey("maxAmp")) {
                        charger.setMaxAmp(json.getInteger("maxAmp"));
                    }
                } else if (s instanceof WindTurbine) {
                    WindTurbine turbine = (WindTurbine) s;
                    if (json.containsKey("height")) {
                        turbine.setHeight(json.getDouble("height"));
                    }
                    if (json.containsKey("blade_length")) {
                        turbine.setBladeLength(json.getDouble("blade_length"));
                    }
                } else if (s instanceof SolarPanel) {
                    SolarPanel panel = (SolarPanel) s;
                    if (json.containsKey("efficiency")) {
                        panel.setEfficiency(json.getInteger("efficiency"));
                    }
                }

                // when you want to make change to the DB you need to start a transaction
                db.getTransaction().begin();
                // then you can register your new or modified objects to be saved
                db.merge(s);
                // finally you can commit the change
                db.getTransaction().commit();

                
                
                event.response().setStatusCode(200).end("Sensor updated successfully");
            }
        }
    }

    // get Json by id
    private JsonObject getJsonById(String id){
        
        Object[] sql = (Object[]) db.createNativeQuery("SELECT s.id,s.name,s.description,s.dtype,s.grid FROM Sensor as s WHERE s.id = " +id).getSingleResult();
        List<Integer> sql2 = (List<Integer>) db.createNativeQuery("SELECT m.id FROM measurement AS m WHERE m.sensor = "+id ).getResultList();
        List<Integer> sql3 = (List<Integer>) db.createNativeQuery("SELECT DISTINCT Person.id FROM Person JOIN Person_Sensor ON Person.id = Person_Sensor.person_id JOIN Sensor ON Sensor.id = Person_Sensor.Sensor_id WHERE Sensor.id = "+id).getResultList();

        JsonObject res = new JsonObject();

        res.put("id", sql[0]);
        res.put("name", sql[1]);
        res.put("description",sql[2]);
        res.put("kind", sql[3]);
        res.put("grid",sql[4]);
        res.put("available_measurements",sql2);
        res.put("owners",sql3);
        

        
        
        if (sql[3].equals("EVCharger")){
            EVCharger charger = (EVCharger) db.find(EVCharger.class, Integer.parseInt(id));
            res.put("max_power", db.createNativeQuery("SELECT c.max_power FROM consumer as c WHERE c.id = " +id ).getSingleResult());            
            res.put("type", charger.getType());
            res.put("maxAmp", charger.getMaxAmp());
            res.put("voltage", charger.getVoltage());
        }
        else if (sql[3].equals("WindTurbine")){
            WindTurbine turbine = (WindTurbine) db.find(WindTurbine.class, Integer.parseInt(id));
            res.put("power_source",db.createNativeQuery("SELECT p.power_source FROM Producer as p WHERE p.id = " +id ).getSingleResult());
            res.put("height", turbine.getHeight());
            res.put("blade_length", turbine.getBladeLength());
        }
        else if (sql[3].equals("SolarPanel")){
            SolarPanel panel = (SolarPanel) db.find(SolarPanel.class, Integer.parseInt(id));
            res.put("power_source", db.createNativeQuery("SELECT p.power_source FROM Producer as p WHERE p.id = " +id ).getSingleResult());
            res.put("efficiency", panel.getEfficiency());
        }   

        return res;            
    }

}

