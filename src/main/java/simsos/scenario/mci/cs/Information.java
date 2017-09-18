package simsos.scenario.mci.cs;

import simsos.scenario.mci.Location;

import java.util.HashMap;

/**
 *
 * Created by Youlim Jung on 06/09/2017.
 *
 */
// Currently not used for Fire department and PTS center
// Store original values to reset
public class Information {
    private String name;
    private int id;
    private Location location;
    private HashMap<String, Integer> properties;

    public Information() {
        properties = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public HashMap<String, Integer> getProperties() {
        return properties;
    }

    public void setProperties(String name, int value) {
        this.properties.put(name, value);
    }
}
