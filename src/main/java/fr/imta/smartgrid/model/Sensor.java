package fr.imta.smartgrid.model;

import java.util.ArrayList;
import java.util.List;



import io.vertx.core.json.JsonObject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sensor")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Sensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private String description;

    @ManyToOne
    @JoinColumn(name = "grid")
    private Grid grid;

    @ManyToMany(mappedBy = "sensors")
    private List<Person> owners = new ArrayList<>();

    @OneToMany(mappedBy = "sensor")
    private List<Measurement> measurements = new ArrayList<>();

    public JsonObject toJSON() {
        JsonObject res = new JsonObject();

        res.put("id", id);
        res.put("name", name);
        res.put("description", description);
        res.put("grid", grid.getId());
        res.put("dtype", this.getClass().getSimpleName());
        res.put("available_measurements", measurements);
        res.put("owners", owners);
        res.put("kind", this.getClass().getSimpleName());
        if (this instanceof EVCharger) {
            EVCharger charger = (EVCharger) this;
            res.put("type", charger.getType());
            res.put("maxAmp", charger.getMaxAmp());
            res.put("voltage", charger.getVoltage());
        } else if (this instanceof WindTurbine) {
            WindTurbine turbine = (WindTurbine) this;
            res.put("height", turbine.getHeight());
            res.put("blade_length", turbine.getBladeLength());
        } else if (this instanceof SolarPanel) {
            SolarPanel panel = (SolarPanel) this;
            res.put("efficiency", panel.getEfficiency());
        }

        return res;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Grid getGrid() {
        return grid;
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
    }

    public List<Person> getOwners() {
        return owners;
    }

    public void setOwners(List<Person> owners) {
        this.owners = owners;
    }

    public void addOwner(Person owner) {
    if (owner == null) return;

    // Avoid duplicates using a simple check
    for (Person p : this.owners) {
        if (p.getId() == owner.getId()) {
            return; // Owner already exists, no need to add
        }
    }

    this.owners.add(owner);
    // If needed, also add this sensor to the owner's sensors (if bidirectional)
    //owner.getSensors().add(this);
    }

    

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    
}
