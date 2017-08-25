package reuiot2015.smartwatch.sensors;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Collects samples from a list of Sensor objects.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu) */
public class Collector {
    // Used for timestamp interpolation, both must always add up to 1.0.
    private final static double TIME_SMP_LOC_F = 0.8;
    private final static double TIME_SMP_LOC_I = 0.2;

    private final ArrayList<Sensor> sensors = new ArrayList<>();
    private final Set<SampleListener> listeners = Collections.synchronizedSet(new HashSet<SampleListener>());

    private long sleepTime; // Sleep time (ms/collection) is calculated from sample rate (collection/sec).

    private Sensor.SensorMetaData[] meta;
    private Object[] data;

    private Thread collector;

    /** This thread collects data from the GeneralSensors at a static rate. */
    class CollectorThread extends Thread {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                long timestamp = System.currentTimeMillis(); // Get initial timestamp.

                // Collect data from sensors into arrays.
                int idx = 0; for (int i = 0; i < sensors.size(); ++i)
                    for (Object o : sensors.get(i).collect()) data[idx++] = o;

                // Determine weighted average timestamp using initial and final timestamps.
                timestamp = (long)(TIME_SMP_LOC_I*timestamp + TIME_SMP_LOC_F*System.currentTimeMillis());

                // Send Sample to all registered listeners.
                for (SampleListener l : listeners) l.onSampleReceived(data.clone(), meta,timestamp);

                // Wait for the specified amount of time (to satisfy collection frequency).
                try { Thread.sleep(sleepTime);
                } catch (InterruptedException e) { /** Nothing to do here. */ }
            }
        }
    }

    /** Constructs a new Collector instance.
     *
     * @param sampleRate The sample rate in Hertz (collections/sec).
     */
    public Collector(float sampleRate) {
        this.sleepTime = (long)(1000.0f / sampleRate); // Milliseconds per collection.
    }

    /** Begins the collection of data. */
    public void begin() {
        if (this.collector == null) {
            this.collector = new CollectorThread();
            this.collector.start();
        }
    }

    /** Stops the collection of data. */
    public void stop() {
        if (this.collector != null) {
            this.collector.interrupt(); // Interrupt the thread.
            try { this.collector.join(); // Wait for the thread to finish.
            } catch (InterruptedException e) { /** Nothing to do here. */ }
            this.collector = null; // Clear reference to thread.
        }
    }

    /** Re-constructs the member arrays to appropriate size for sensor list. */
    private void refreshArrays() {
        // Read the meta data from each sensor (also calculate total dimensionality).
        this.meta = new Sensor.SensorMetaData[this.sensors.size()];
        int dim = 0; for (int i = 0; i < this.meta.length; ++i) {
            this.meta[i] = this.sensors.get(i).getMetaData();
            dim += this.meta[i].getDimension();
        }
        this.data = new Object[dim];
        Log.d("Collector", "Collection size is " + dim + ".");
    }

    /** Adds a new sensor to collect data from (if not already in list).
     *
     * @param sensor The sensor to add.
     */
    public void addSensor(Sensor sensor) {
        if(sensor != null && !this.sensors.contains(sensor)) this.sensors.add(sensor);
        refreshArrays();
    }

    /** Removes a sensor from the internal list of sensors (if in list).
     *
     * @param sensor The sensor to remove.
     */
    public void removeSensor(Sensor sensor) {
        this.sensors.remove(sensor);
        refreshArrays();
    }

    /** Add all the sensors from a linker to the collector. */
    public void addSensors(Linker linker) {
        for (Sensor s : linker.getSensors())
            if (!this.sensors.contains(s)) this.sensors.add(s);
        refreshArrays();
    }

    public void clearSensors() {
        this.sensors.clear();
        refreshArrays();
    }

    /** Adds a new SampleListener to the collector.
     *
     * @param sampleListener The SampleListener to add.
     */
    public void addListener(SampleListener sampleListener) {
        this.listeners.add(sampleListener);
    }

    /** Removes a SampleListener from the collector.
     *
     * @param sampleListener The SampleListener to remove.
     */
    public void removeListener(SampleListener sampleListener) {
        this.listeners.remove(sampleListener);
    }

    /** Grabs a copy of the sensor meta data.
     *
     * @return The sensor meta data.
     */
    public Sensor.SensorMetaData[] getMeta() {
        return this.meta.clone();
    }

    /** An interface for receiving Samples from the Collector. */
    public interface SampleListener {
        /** Called when a sample is received (constructed).
         *
         * @param sample The collected sample.
         * @param metaData The sensor meta data needed to parse the data.
         * @param timestamp The time the collection was made.
         */
        void onSampleReceived(Object[] sample, Sensor.SensorMetaData[] metaData, long timestamp);
    }
}