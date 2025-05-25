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

/**
 * Handler pour gérer les requêtes HTTP liées aux capteurs (Sensor).
 */
public class SensorHandler implements Handler<RoutingContext> {
    EntityManager db; // Gestionnaire d'entités pour accéder à la base de données

    /**
     * Constructeur prenant un EntityManager.
     * @param db EntityManager pour la base de données
     */
    public SensorHandler(EntityManager db) {
        this.db = db;
    }

    /**
     * Méthode principale appelée à chaque requête HTTP sur la route associée.
     * @param event Contexte de routage Vert.x
     */
    @Override
    public void handle(RoutingContext event) {

        // Affichage de debug des informations de la requête
        System.out.println("Route called: " + event.currentRoute().getName());
        System.out.println("Request HTTP method: " + event.request().method());
        System.out.println("We received these query parameters: " + event.queryParams());
        System.out.println("We have these path params: " + event.pathParams());
        System.out.println("Value for the path param 'name': " + event.pathParam("name"));
        System.out.println("We received this body: " + event.body().asString());

        // Traitement des requêtes GET
        if (event.request().method().toString() == "GET"){
            // Si la route correspond à /sensor/{id} et que l'id est un nombre
            if (event.currentRoute().getName().matches("\\/sensor\\/(.*)") && event.pathParam("id").matches("[0-9]+")) {
                JsonObject res = getJsonById(event.pathParam("id"));
                event.json(res);

            // Si la route correspond à /sensors/{kind} et que le kind est un type connu
            }else if (event.currentRoute().getName().matches("\\/sensors\\/(.*)") && event.pathParam("kind").matches("EVCharger|WindTurbine|SolarPanel")){ 
                // Récupère la liste des capteurs du type demandé
                List<Integer> sensors_match = (List<Integer>) db.createNativeQuery("SELECT s.id FROM sensor as s WHERE s.dtype='" + event.pathParam("kind")+ "'").getResultList();
                event.json(sensors_match);
            }
            else{
                // Route non trouvée
                event.end("error 404: ID Not found");
            }
        }
        // Traitement des requêtes POST (mise à jour d'un capteur)
        else if (event.request().method().toString() == "POST"){

            // Récupère le corps JSON de la requête
            JsonObject json = event.body().asJsonObject();
            String id = event.pathParam("id");
            
            if (event.pathParam("id")!=null){
                // Recherche du capteur à modifier
                Sensor s = (Sensor) db.find(Sensor.class, Integer.parseInt(id));

                // Mise à jour des champs génériques
                if (json.containsKey("name")) {
                    s.setName(json.getString("name"));
                }
                if (json.containsKey("description")) {
                    s.setDescription(json.getString("description"));
                }
                if (json.containsKey("owners")) {
                    s.getOwners().clear(); // On vide la liste si on remplace
                    for (Integer p : (List<Integer>) json.getJsonArray("owners").getList()) {
                        s.addOwner(db.find(Person.class, p));
                    }
                }

                // Mise à jour des champs spécifiques selon le type
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

                // Début de la transaction pour sauvegarder les modifications
                db.getTransaction().begin();
                db.merge(s); // Enregistre les modifications
                db.getTransaction().commit(); // Commit la transaction

                // Réponse de succès
                event.response().setStatusCode(200).end("Sensor updated successfully");
            }
            else{
                // Route non trouvée
                event.end("error 404: ID Not found");
            }
        }
    }

    /**
     * Récupère un capteur et ses informations au format JSON à partir de son id.
     * @param id identifiant du capteur
     * @return objet JsonObject contenant les informations du capteur
     */
    private JsonObject getJsonById(String id){
        // Récupère les informations principales du capteur
        Object[] sql = (Object[]) db.createNativeQuery("SELECT s.id,s.name,s.description,s.dtype,s.grid FROM Sensor as s WHERE s.id = " +id).getSingleResult();
        // Récupère les identifiants des mesures associées
        List<Integer> sql2 = (List<Integer>) db.createNativeQuery("SELECT m.id FROM measurement AS m WHERE m.sensor = "+id ).getResultList();
        // Récupère les identifiants des propriétaires
        List<Integer> sql3 = (List<Integer>) db.createNativeQuery("SELECT DISTINCT Person.id FROM Person JOIN Person_Sensor ON Person.id = Person_Sensor.person_id JOIN Sensor ON Sensor.id = Person_Sensor.Sensor_id WHERE Sensor.id = "+id).getResultList();

        JsonObject res = new JsonObject();

        // Remplit le JSON avec les informations de base
        res.put("id", sql[0]);
        res.put("name", sql[1]);
        res.put("description",sql[2]);
        res.put("kind", sql[3]);
        res.put("grid",sql[4]);
        res.put("available_measurements",sql2);
        res.put("owners",sql3);

        // Ajoute les champs spécifiques selon le type de capteur
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
