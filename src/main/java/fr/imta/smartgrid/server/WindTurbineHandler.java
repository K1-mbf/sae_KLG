
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
package fr.imta.smartgrid.handler;

import fr.imta.smartgrid.model.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.persistence.EntityManager;

import java.util.List;

public class WindTurbineHandler {

    private final EntityManager db;

    public WindTurbineHandler(EntityManager db) {
        this.db = db;
    }

    public void handle(RoutingContext event) {
        try {
            JsonObject body = event.body().asJsonObject();
            if (body == null) {
                event.response().setStatusCode(500).end("Invalid JSON");
                return;
            }

            Integer turbineId = body.getInteger("windturbine");
            Long timestamp = body.getLong("timestamp");
            JsonObject data = body.getJsonObject("data");

            if (turbineId == null || timestamp == null || data == null) {
                event.response().setStatusCode(500).end("Missing fields");
                return;
            }

             }

            Double speed = data.getDouble("speed");
            Double power = data.getDouble("power");

            if (speed == null || power == null) {
                event.response().setStatusCode(400).end("Missing speed or power");
                return;
            }
            WindTurbine turbine = db.find(WindTurbine.class, turbineId);
            if (turbine == null) {
                event.response().setStatusCode(404).end("Turbine not found");
                return;
            }

            Sensor sensor = turbine;

            Measurement speedMeasurement = findMeasurement(sensor, "speed");
            Measurement powerMeasurement = findMeasurement(sensor, "power");

            if (speedMeasurement == null || powerMeasurement == null) {
                event.response().setStatusCode(500).end("Measurements not found");
                return;
            }

            db.getTransaction().begin();

            DataPoint dpSpeed = new DataPoint();
            dpSpeed.setMeasurement(speedMeasurement);
            dpSpeed.setTimestamp(timestamp);
            dpSpeed.setValue(speed);
            db.persist(dpSpeed);

            DataPoint dpPower = new DataPoint();
            dpPower.setMeasurement(powerMeasurement);
            dpPower.setTimestamp(timestamp);
            dpPower.setValue(power);
            db.persist(dpPower);

            db.getTransaction().commit();

            // Recalcul de l’énergie produite
            double energyWh = computeTotalEnergy(powerMeasurement); // Wh

            db.getTransaction().begin();
            turbine.setTotalEnergyProduced(energyWh);
            db.merge(turbine);
            db.getTransaction().commit();

            JsonObject response = new JsonObject().put("status", "success");
            event.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(200)
                    .end(response.encode());

        } catch (Exception e) {
            e.printStackTrace();
            if (db.getTransaction().isActive()) db.getTransaction().rollback();
            event.response().setStatusCode(500).end("Internal error");
        }
    }

    private Measurement findMeasurement(Sensor sensor, String name) {
        return sensor.getMeasurements().stream()
                .filter(m -> name.equalsIgnoreCase(m.getName()))
                .findFirst().orElse(null);
    }

    private double computeTotalEnergy(Measurement powerMeasurement) {
        List<DataPoint> points = powerMeasurement.getDatapoints();
        double totalJoules = points.stream()
                .mapToDouble(dp -> dp.getValue() * 60.0)
                .sum();
        return totalJoules / 3600.0; // convert to Wh
    }
    JsonObject response = new JsonObject().put("status", "success");
event.response()
    .putHeader("Content-Type", "application/json")
    .setStatusCode(200)
    .end(response.encode());

}
