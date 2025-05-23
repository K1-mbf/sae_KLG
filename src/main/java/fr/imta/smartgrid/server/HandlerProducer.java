
package fr.imta.smartgrid.server;

import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Person;
import fr.imta.smartgrid.model.Sensor;
import fr.imta.smartgrid.model.SolarPanel;
import fr.imta.smartgrid.model.WindTurbine;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;
import java.util.*;


public class HandlerProducer implements Handler<RoutingContext> {
    EntityManager db;

    public HandlerProducer(EntityManager db) {
        this.db = db;
    }
    @Override
    public void handle(RoutingContext event) {

        List<Integer> producer = (List<Integer>) db.createNativeQuery("SELECT id FROM producer").getResultList();
        List<JsonObject> Prod = new ArrayList<>();
        for (Integer p : producer){ 
            JsonObject temp = getJsonById(p.toString());
            Prod.add(temp);
        }

        event.json(Prod);
    }

    private JsonObject getJsonById(String id){
        
        Object[] sql = (Object[]) db.createNativeQuery("SELECT s.id,s.name,s.description,s.dtype,s.grid,p.power_source FROM Sensor as s, Producer as p WHERE s.id = " +id + " AND s.id = p.id").getSingleResult();
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
        res.put("power_source",sql[5]);

        
        
        if (sql[3].equals("EVCharger")){
            EVCharger charger = (EVCharger) db.find(EVCharger.class, Integer.parseInt(id));
            res.put("type", charger.getType());
            res.put("maxAmp", charger.getMaxAmp());
            res.put("voltage", charger.getVoltage());
        }
        else if (sql[3].equals("WindTurbine")){
            WindTurbine turbine = (WindTurbine) db.find(WindTurbine.class, Integer.parseInt(id));
            res.put("height", turbine.getHeight());
            res.put("blade_length", turbine.getBladeLength());
        }
        else if (sql[3].equals("SolarPanel")){
            SolarPanel panel = (SolarPanel) db.find(SolarPanel.class, Integer.parseInt(id));
            res.put("efficiency", panel.getEfficiency());
        }   

        return res;
            
    }
}