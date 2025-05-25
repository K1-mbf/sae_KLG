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

// HandlerMeasurement gère les requêtes HTTP liées aux mesures (measurement)
public class HandlerMeasurement implements Handler<RoutingContext> {
    EntityManager db; // Gestionnaire d'entités pour accéder à la base de données

    // Constructeur qui prend un EntityManager en paramètre
    public HandlerMeasurement(EntityManager db) {
        this.db = db;
    }

    // Méthode principale appelée lors de la réception d'une requête HTTP
    @Override
    public void handle(RoutingContext event) {

        // Affichage de diverses informations sur la requête reçue (pour le debug)
        System.out.println("Route called: " + event.currentRoute().getName());
        System.out.println("Request HTTP method: " + event.request().method());
        System.out.println("We received these query parameters: " + event.queryParam("from"));
        System.out.println("We have these path params: " + event.pathParams());
        System.out.println("Value for the path param 'name': " + event.pathParam("name"));
        System.out.println("We received this body: " + event.body().asString());

        // Si la route correspond à "/measurement/:id/values" et que l'id est un nombre
        if (event.currentRoute().getName().matches("\\/measurement\\/:id\\/values") && event.pathParam("id").matches("[0-9]+") ) {
            
            String id = event.pathParam("id"); // Récupère l'id du path paramètre
            // Récupère les paramètres de requête "from" et "to" ou utilise des valeurs par défaut
            String from = event.queryParam("from").isEmpty() ?  "0" : event.queryParam("from").get(0);
            String to = event.queryParam("to").isEmpty()  ?  "2147483646" : event.queryParam("to").get(0);
            
            JsonObject res = new JsonObject();
            // Récupère l'id du capteur associé à la mesure
            res.put("sensor_id", db.createNativeQuery("SELECT sensor FROM measurement WHERE id = " + id).getSingleResult());
            res.put("measurement_id", Integer.parseInt(id));

            // Récupère la liste des valeurs de la mesure entre les timestamps "from" et "to"
            List<Object[]>  values = (List<Object[]>) db.createNativeQuery(
                    "SELECT timestamp, value FROM datapoint WHERE measurement = " + id + 
                    " AND timestamp BETWEEN " + from + " AND " + to + 
                    " ORDER BY timestamp"
                ).getResultList();

            JsonArray valuesArray = new JsonArray();
            // Pour chaque valeur récupérée, crée un objet JSON et l'ajoute au tableau
            for (Object[] row : values) {
                JsonObject valueObj = new JsonObject();
                valueObj.put("timestamp", row[0]);
                valueObj.put("value", row[1]);
                valuesArray.add(valueObj);
            }
            res.put("values", valuesArray);
            
            // Retourne la réponse au format JSON
            event.json(res);
            
        // Si la route correspond à "/measurement/:id" et que l'id est un nombre
        } else if (event.currentRoute().getName().matches("\\/measurement\\/(.*)") && event.pathParam("id").matches("[0-9]+")) {
            // Récupère les informations de la mesure (nom, unité, capteur)
            Object[] sql = (Object[]) db.createNativeQuery("SELECT name, unit, sensor FROM measurement WHERE id = " +event.pathParam("id")).getSingleResult();
            
            JsonObject res = new JsonObject();

            res.put("name", sql[0]);
            res.put("unit", sql[1]);
            res.put("sensor", sql[2]);
            
            // Retourne la réponse au format JSON
            event.json(res);

        } else  {
            // Si la route ne correspond à aucun des cas précédents, retourne une erreur 404
            event.end("error 404 request not found");
        }
        
    }

}