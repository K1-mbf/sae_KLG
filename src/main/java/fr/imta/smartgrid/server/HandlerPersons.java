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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

// Classe qui gère les requêtes HTTP pour les personnes
public class HandlerPersons implements Handler<RoutingContext> {
    EntityManager db; // Gestionnaire d'entités pour accéder à la base de données

    // Constructeur qui prend un EntityManager en paramètre
    public HandlerPersons(EntityManager db) {
        this.db = db;
    }

    // Méthode principale appelée à chaque requête HTTP
    @Override
    public void handle(RoutingContext event) {
        // Affichage d'informations de debug sur la requête reçue
        System.out.println("Route called: " + event.currentRoute().getName());
        System.out.println("Request HTTP method: " + event.request().method());
        System.out.println("We received these query parameters: " + event.queryParams());
        System.out.println("We have these path params: " + event.pathParams());
        System.out.println("Value for the path param 'name': " + event.pathParam("name"));
        System.out.println("We received this body: " + event.body().asString());

        String method = event.request().method().toString();

        // Gestion des requêtes GET
        if (method.equals("GET")) {
            // Si la route correspond à /person/:id et que l'id est un nombre
            if (event.currentRoute().getName().matches("/person/:id") && event.pathParam("id").matches("[0-9]+")) {
                // Récupération des informations de la personne depuis la base
                Object[] sql = (Object[]) db.createNativeQuery(
                    "SELECT firstname, lastname, grid FROM person WHERE id = " + event.pathParam("id")
                ).getSingleResult();

                // Récupération des capteurs associés à la personne
                List<Integer> sql2 = (List<Integer>) db.createNativeQuery(
                    "SELECT DISTINCT Sensor.id FROM Sensor " +
                    "JOIN Person_Sensor ON Sensor.id = Person_Sensor.Sensor_id " +
                    "JOIN Person ON Person.id = Person_Sensor.person_id"
                ).getResultList();

                // Construction de la réponse JSON
                JsonObject res = new JsonObject();
                res.put("id", event.pathParam("id"));
                res.put("first_name", sql[0]);
                res.put("last_name", sql[1]);
                res.put("grid", sql[2]);
                res.put("owned_sensors", sql2);

                // Envoi de la réponse
                event.json(res);

            // Si la route correspond à /persons, on retourne la liste des IDs
            } else if (event.currentRoute().getName().matches("/persons")) {
                List<Integer> persons = (List<Integer>) db.createNativeQuery("SELECT id FROM person").getResultList();
                event.json(persons);
            } else {
                // Route non trouvée
                event.end("error 404: Not found");
            }

        // Gestion des requêtes POST (mise à jour d'une personne)
        } else if (method.equals("POST")) {
            JsonObject json = event.body().asJsonObject();
            String id = event.pathParam("id");

            if (id != null) {
                Person p = db.find(Person.class, Integer.parseInt(id));

                // Mise à jour des champs si présents dans le JSON
                if (json.containsKey("first_name")) {
                    p.setFirstName(json.getString("first_name"));
                }
                if (json.containsKey("last_name")) {
                    p.setLastName(json.getString("last_name"));
                }
                if (json.containsKey("grid") && json.getInteger("grid") != null) {
                    Grid grid = db.find(Grid.class, json.getInteger("grid"));
                    if (grid != null) {
                        p.setGrid(grid);
                    }
                }
                if (json.containsKey("owned_sensors")) {
                    p.getSensors().clear();
                    for (Integer s : (List<Integer>) json.getJsonArray("owned_sensors").getList()) {
                        Sensor sensor = db.find(Sensor.class, s);
                        if (sensor != null) {
                            p.addSensor(sensor);
                        }
                    }
                }

                // Sauvegarde des modifications en base
                db.getTransaction().begin();
                db.merge(p);
                db.getTransaction().commit();

                event.response().setStatusCode(200).end("Person updated successfully");
            }

        // Gestion des requêtes DELETE (suppression d'une personne)
        } else if (method.equals("DELETE")) {
            try {
                System.out.println("DELETE request received for person with ID: " + event.pathParam("id"));

                String idParam = event.pathParam("id");
                if (idParam == null) {
                    event.response().setStatusCode(400).end("Missing id");
                    return;
                }

                int id;
                try {
                    id = Integer.parseInt(idParam);
                } catch (NumberFormatException e) {
                    event.response().setStatusCode(400).end("Invalid id format");
                    return;
                }

                Person person = db.find(Person.class, id);
                if (person == null) {
                    event.response().setStatusCode(404).end("Person not found");
                    return;
                }

                // Suppression de la personne en base
                db.getTransaction().begin();
                db.remove(person);
                db.getTransaction().commit();

                event.response().setStatusCode(200).end("Person deleted successfully");
            } catch (Exception e) {
                e.printStackTrace();
                if (db.getTransaction().isActive()) db.getTransaction().rollback();
                event.response().setStatusCode(500).end("Internal error during deletion");
            }

        // Gestion des requêtes PUT (création d'une nouvelle personne)
        } else if (method.equals("PUT")) {
            System.out.println("\n\n\nPUT request received for person\n\n\n");

            JsonObject json;
            try {
                json = event.body().asJsonObject();
            } catch (Exception e) {
                event.response().setStatusCode(500).end("Invalid JSON format");
                return;
            }

            // Vérification des champs obligatoires
            if (json == null ||
                !json.containsKey("first_name") ||
                !json.containsKey("last_name") ||
                !json.containsKey("grid")) {
                event.response().setStatusCode(500).end("Missing required fields: first_name, last_name, or grid");
                return;
            }

            try {
                Person p = new Person();
                p.setFirstName(json.getString("first_name"));
                p.setLastName(json.getString("last_name"));

                // Association à un grid existant
                Grid grid = db.find(Grid.class, json.getInteger("grid"));
                if (grid == null) {
                    event.response().setStatusCode(500).end("Invalid grid ID");
                    return;
                }
                p.setGrid(grid);

                // Ajout des capteurs si présents
                if (json.containsKey("owned_sensors")) {
                    for (Integer s : (List<Integer>) json.getJsonArray("owned_sensors").getList()) {
                        Sensor sensor = db.find(Sensor.class, s);
                        if (sensor != null) {
                            p.addSensor(sensor);
                        }
                    }
                }

                // Sauvegarde de la nouvelle personne en base
                db.getTransaction().begin();
                db.persist(p);
                db.getTransaction().commit();

                JsonObject res = new JsonObject().put("id", p.getId());
                event.response().setStatusCode(200).end(res.encode());

            } catch (Exception e) {
                e.printStackTrace();
                if (db.getTransaction().isActive()) db.getTransaction().rollback();
                event.response().setStatusCode(500).end("Error inserting person: " + e.getMessage());
            }
        }
    }
}
