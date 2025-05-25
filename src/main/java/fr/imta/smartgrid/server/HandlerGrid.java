package fr.imta.smartgrid.server;

import java.util.List;

import fr.imta.smartgrid.model.EVCharger;
import fr.imta.smartgrid.model.Sensor;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

// Classe HandlerGrid qui implémente l'interface Handler pour gérer les requêtes HTTP
public class HandlerGrid implements Handler<RoutingContext> {
    EntityManager db; // Gestionnaire d'entités pour accéder à la base de donnée

    // Constructeur prenant un EntityManager en paramètre
    public HandlerGrid(EntityManager db) {
        this.db = db;
    }

    @Override
    public void handle(RoutingContext event) {

         // Affichage de diverses informations sur la requête reçue (pour le debug)
        System.out.println("Route called: " + event.currentRoute().getName());
        System.out.println("Request HTTP method: " + event.request().method());
        System.out.println("We received these query parameters: " + event.queryParam("from"));
        System.out.println("We have these path params: " + event.pathParams());
        System.out.println("Value for the path param 'name': " + event.pathParam("name"));
        System.out.println("We received this body: " + event.body().asString());



        // Si la route correspond à la production d'une grille et que l'id est un nombre
        if (event.currentRoute().getName().matches("/grid/:id/production") && event.pathParam("id").matches("[0-9]+")){
            // Requête SQL pour obtenir la somme des valeurs de production pour la grille donnée
            Double prod = (Double) db.createNativeQuery(
                "SELECT SUM(d.value) " +
                "FROM Grid g " +
                "JOIN Sensor s ON s.grid = g.id " +
                "JOIN Producer p ON p.id = s.id " +
                "JOIN Measurement m ON m.sensor = s.id " +
                "JOIN Datapoint d ON d.measurement = m.id " +
                "WHERE g.id = "+ event.pathParam("id")
            ).getSingleResult();

            // Retourne le résultat en JSON
            event.json(prod);
        }

        // Si la route correspond à la consommation d'une grille et que l'id est un nombre
        else if (event.currentRoute().getName().matches("/grid/:id/consumption") && event.pathParam("id").matches("[0-9]+")) {
            // Requête SQL pour obtenir la somme des valeurs de consommation pour la grille donnée
            Double cons = (Double) db.createNativeQuery(
                "SELECT SUM(d.value) " +
                "FROM Grid g " +
                "JOIN Sensor s ON s.grid = g.id " +
                "JOIN Consumer c ON c.id = s.id " +
                "JOIN Measurement m ON m.sensor = s.id " +
                "JOIN Datapoint d ON d.measurement = m.id " +
                "WHERE g.id = " + event.pathParam("id")
            ).getSingleResult();

            // Retourne le résultat en JSON
            event.json(cons);
        
        }
        // Si aucun id n'est fourni, retourne la liste des ids de toutes les grilles
        else if (event.currentRoute().getName().matches("/grids(.*)") && event.pathParam("id") == null) {
            List<Integer> grids = (List<Integer>) db.createNativeQuery("SELECT id FROM grid").getResultList();
            event.json(grids);
        }
        // Si la route correspond à une grille spécifique et que l'id est un nombre
        else if (event.currentRoute().getName().matches("/grid/:id") && event.pathParam("id").matches("[0-9]+")) {
            // Récupère le nom et la description de la grille
            Object[] sql = (Object[]) db.createNativeQuery("SELECT name,description FROM grid WHERE id = " + event.pathParam("id")).getSingleResult();
            // Récupère la liste des capteurs associés à la grille
            List<Integer> sensors = (List<Integer>) db.createNativeQuery("SELECT sensor.id FROM sensor WHERE sensor.grid = "+event.pathParam("id") ).getResultList();
            // Récupère la liste des utilisateurs associés à la grille
            List<Integer> users = (List<Integer>) db.createNativeQuery("SELECT person.id FROM person WHERE person.grid = "+event.pathParam("id") ).getResultList();

            JsonObject res = new JsonObject();

            // Ajoute la description, le nom, les capteurs et les utilisateurs au résultat
            res.put("description", sql[1]);
            res.put("name", sql[0]);
            res.put("sensors", sensors);
            res.put("users",users);
            
            // Retourne le résultat en JSON
            event.json(res);

        }
        // Si l'ID donné ne correspond à rien, retourne une erreur 404
        else {
            event.end("error 404: Not found");
        }
        
    }
}