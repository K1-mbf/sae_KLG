package fr.imta.smartgrid.server;

import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import fr.imta.smartgrid.model.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * Handler pour traiter les données reçues d'une éolienne via des requêtes HTTP.
 * Ce handler attend un corps JSON contenant l'ID de l'éolienne, un timestamp, et un objet data
 * avec les valeurs "speed" (vitesse) et "power" (puissance).
 * Il valide l'entrée, persiste les mesures de vitesse et puissance,
 * recalcule l'énergie totale produite et stocke cette valeur comme une nouvelle mesure.
 */
public class WindTurbineHandler implements Handler<RoutingContext>{

    // EntityManager pour accéder à la base de données
    private final EntityManager db;

    // Constructeur prenant l'EntityManager en paramètre
    public WindTurbineHandler(EntityManager db) {
        this.db = db;
    }

    /**
     * Méthode principale appelée lors de la réception d'une requête HTTP.
     * Elle traite le corps JSON, valide les champs, persiste les données et gère les erreurs.
     */
    public void handle(RoutingContext event) {
        try {
            // Récupère le corps de la requête sous forme de JsonObject
            JsonObject body = event.body().asJsonObject();
            if (body == null) {
                event.response().setStatusCode(500).end("Invalid JSON");
                return;
            }

            // Extraction des champs principaux
            Integer turbineId = body.getInteger("windturbine");
            Long timestamp = body.getLong("timestamp");
            JsonObject data = body.getJsonObject("data");

            // Vérifie la présence des champs requis
            if (turbineId == null || timestamp == null || data == null) {
                event.response().setStatusCode(500).end("Missing fields");
                return;
            }

            // Extraction des valeurs de vitesse et puissance
            Double speed = data.getDouble("speed");
            Double power = data.getDouble("power");

            // Vérifie la présence des valeurs de vitesse et puissance
            if (speed == null || power == null) {
                event.response().setStatusCode(400).end("Missing speed or power");
                return;
            }

            // Recherche de l'éolienne en base de données
            WindTurbine turbine = db.find(WindTurbine.class, turbineId);
            if (turbine == null) {
                event.response().setStatusCode(404).end("Turbine not found");
                return;
            }

            // L'éolienne est aussi un capteur (Sensor)
            Sensor sensor = turbine;

            // Recherche des mesures "speed" et "power" associées au capteur
            Measurement speedMeasurement = findMeasurement(sensor, "speed");
            Measurement powerMeasurement = findMeasurement(sensor, "power");

            // Vérifie la présence des mesures
            if (speedMeasurement == null || powerMeasurement == null) {
                event.response().setStatusCode(500).end("Measurements not found");
                return;
            }

            // Début de la transaction pour persister les nouveaux DataPoint
            db.getTransaction().begin();

            // Création et persistance du DataPoint pour la vitesse
            DataPoint dpSpeed = new DataPoint();
            dpSpeed.setMeasurement(speedMeasurement);
            dpSpeed.setTimestamp(timestamp);
            dpSpeed.setValue(speed);
            db.persist(dpSpeed);

            // Création et persistance du DataPoint pour la puissance
            DataPoint dpPower = new DataPoint();
            dpPower.setMeasurement(powerMeasurement);
            dpPower.setTimestamp(timestamp);
            dpPower.setValue(power);
            db.persist(dpPower);

            // Commit de la transaction
            db.getTransaction().commit();

            // Recalcul de l’énergie totale produite (en Wh)
            double energyWh = computeTotalEnergy(powerMeasurement);

            // Recherche ou création de la mesure "TotalEnergyProduced"
            Measurement energyMeasurement = findMeasurement(sensor, "\ttotal_energy_produced");
            if (energyMeasurement == null) {
                energyMeasurement = new Measurement();
                energyMeasurement.setName("\ttotal_energy_produced");
                energyMeasurement.setUnit("Wh");
                energyMeasurement.setSensor(sensor);
                db.getTransaction().begin();
                db.persist(energyMeasurement);
                db.getTransaction().commit();
            }

            // Création et persistance du DataPoint pour l’énergie produite
            DataPoint dpEnergy = new DataPoint();
            dpEnergy.setMeasurement(energyMeasurement);
            dpEnergy.setTimestamp(timestamp);
            dpEnergy.setValue(energyWh);

            db.getTransaction().begin();
            db.persist(dpEnergy);
            db.getTransaction().commit();

            // Réponse JSON de succès
            JsonObject response = new JsonObject().put("status", "success");
            event.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(200)
                    .end(response.encode());

        } catch (Exception e) {
            // Gestion des erreurs : rollback si nécessaire et réponse 500
            e.printStackTrace();
            if (db.getTransaction().isActive()) db.getTransaction().rollback();
            event.response().setStatusCode(500).end("Internal error");
        }
    }

    /**
     * Recherche une mesure par nom pour un capteur donné.
     * @param sensor Le capteur
     * @param name Le nom de la mesure
     * @return La mesure trouvée ou null
     */
    private Measurement findMeasurement(Sensor sensor, String name) {
        return sensor.getMeasurements().stream()
                .filter(m -> name.equalsIgnoreCase(m.getName()))
                .findFirst().orElse(null);
    }

    /**
     * Calcule l'énergie totale produite à partir des DataPoint de puissance.
     * @param powerMeasurement La mesure de puissance
     * @return L'énergie totale en Wh
     */
    private double computeTotalEnergy(Measurement powerMeasurement) {
        List<DataPoint> points = powerMeasurement.getDatapoints();
        // Pour chaque DataPoint, on suppose que la puissance est constante pendant 60 secondes
        double totalJoules = points.stream()
                .mapToDouble(dp -> dp.getValue() * 60.0)
                .sum();
        return totalJoules / 3600.0; // Conversion Joules -> Wh
    }

}
