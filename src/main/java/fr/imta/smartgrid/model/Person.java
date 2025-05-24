package fr.imta.smartgrid.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "person")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String firstName;
    private String lastName;

    @ManyToOne
    @JoinColumn(name = "grid")
    private Grid grid;

    @ManyToMany
    @JoinTable(
            name = "person_sensor", joinColumns = @JoinColumn(name = "person_id"), inverseJoinColumns = @JoinColumn(name = "sensor_id"))
    private List<Sensor> sensors = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Grid getGrid() {
        return grid;
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<Sensor> sensors) {
        this.sensors = sensors;
    }
    
    public void addSensor(Sensor sensor) {
    if (sensor == null) return;

    // Avoid duplicates using a simple check
    for (Sensor s : this.sensors) {
        if (s.getId() == sensor.getId()) {
            return; // Owner already exists, no need to add
        }
    }

    this.sensors.add(sensor);
    // If needed, also add this person to the person's Person (if bidirectional)
    sensor.getOwners().add(this);
    }

    
}
