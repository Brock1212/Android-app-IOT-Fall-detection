package reuiot2015.smartwatch.sensors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Provides interface to sensors from a particular platform. */
public abstract class Linker {
    private List<Sensor> sensors = Collections.synchronizedList(new ArrayList<Sensor>());

    /** Connects the linker to the sensor platform.
    *
    * @return True if connected, false if failed to connect.
    */
    public abstract boolean connect();

    /** Disconnects the linker and sensors from the sensor platform. */
    public abstract void disconnect();

    /** Subscribes the sensors with the platform for data monitoring. */
    public abstract void subscribe();

    /** Unsubscribes the sensors from the platform to stop data monitoring. */
    public abstract void unsubscribe();

    /** Checks if the linker is connected to the hardware platform. */
    public abstract boolean isConnected();

    /** Adds a sensor to the internal list of sensors. */
    protected final void addSensor(Sensor s) {
        if (s != null && !this.sensors.contains(s)) this.sensors.add(s);
    }

    /** Removes a sensor from the internal list of sensors. */
    protected final void removeSensor(Sensor s) {
        if (s != null) this.sensors.remove(s);
    }

    /** Unsubscribes and clears the sensor list. */
    protected final void clearSensors() {
        unsubscribe();
        this.sensors.clear();
    }

    /** Grabs the list of sensors.
     *
     * @return The list of sensors.
     */
    public final List<Sensor> getSensors() {
        return sensors;
    }
}
