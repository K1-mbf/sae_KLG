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

/**
 * HandlerConsumers est un handler Vert.x qui gère les requêtes pour obtenir la liste des consommateurs.
 */
public class HandlerConsumers implements Handler<RoutingContext> {
    EntityManager db; // Gestionnaire d'entités pour accéder à la base de données

    /**
     * Constructeur prenant un EntityManager en paramètre.
     * @param db EntityManager pour la base de données
     */
    public HandlerConsumers(EntityManager db) {
        this.db = db;
    }

    /**
     * Méthode principale appelée lors de la réception d'une requête HTTP.
     * Récupère tous les consommateurs et retourne leurs informations au format JSON.
     * @param event Le contexte de routage Vert.x
     */
    @Override
    public void handle(RoutingContext event) {
        // Récupère la liste des IDs des consommateurs depuis la base de données
        List<Integer> consumer = (List<Integer>) db.createNativeQuery("SELECT id FROM consumer").getResultList();
        List<JsonObject> Prod = new ArrayList<>();
        // Pour chaque consommateur, récupère ses informations détaillées
        for (Integer c : consumer){ 
            JsonObject temp = getJsonById(c.toString());
            Prod.add(temp);
        }
        // Retourne la liste des consommateurs au format JSON
        event.json(Prod);
    }

    /**
     * Récupère les informations détaillées d'un consommateur à partir de son ID.
     * @param id L'identifiant du consommateur
     * @return Un objet JsonObject contenant les informations du consommateur
     */
    private JsonObject getJsonById(String id){
        // Récupère les informations principales du consommateur et du capteur associé
        Object[] sql = (Object[]) db.createNativeQuery(
            "SELECT s.id,s.name,s.description,s.dtype,s.grid,c.max_power FROM Sensor as s, consumer as c WHERE s.id = " +id + " AND s.id = c.id"
        ).getSingleResult();

        // Récupère la liste des mesures disponibles pour ce capteur
        List<Integer> sql2 = (List<Integer>) db.createNativeQuery(
            "SELECT m.id FROM measurement AS m WHERE m.sensor = "+id
        ).getResultList();

        // Récupère la liste des propriétaires (Person) de ce capteur
        List<Integer> sql3 = (List<Integer>) db.createNativeQuery(
            "SELECT DISTINCT Person.id FROM Person JOIN Person_Sensor ON Person.id = Person_Sensor.person_id JOIN Sensor ON Sensor.id = Person_Sensor.Sensor_id WHERE Sensor.id = "+id
        ).getResultList();

        JsonObject res = new JsonObject();

        // Ajoute les informations de base au résultat JSON
        res.put("id", sql[0]);
        res.put("name", sql[1]);
        res.put("description",sql[2]);
        res.put("kind", sql[3]);
        res.put("grid",sql[4]);
        res.put("available_measurements",sql2);
        res.put("owners",sql3);
        res.put("max_power",sql[5]);

        // Ajoute des informations spécifiques selon le type de consommateur
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