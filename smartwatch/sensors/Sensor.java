package reuiot2015.smartwatch.sensors;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/** A wrapper for interacting with various sensor APIs.
 *
 * Works with float values. For sensors collecting qualitative data, use
 * a mapping to ordinal values and pass that data in instead. Use windowSize only
 * if it makes sense. When value is collected, you can then cast it back to whatever
 * qualitative or ordinal value is required.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public abstract class Sensor {

    /** Chooses how the sensor data is averaged. */
    public enum AverageWeighting {
        EQUAL, LINEAR;

        public float[] generateWeights(int size) {
            float[] weights = new float[size];
            switch (this) {
                case EQUAL:
                    Arrays.fill(weights, 1.0f / size);
                    break;
                case LINEAR:
                    float div = size * (size + 1.0f) / 2.0f;
                    for (int i = 0; i < size; ++i) {
                        weights[i] = (float)(size - i) / div;
                        Log.d("AverageWeighting", weights[i] + "");
                    }
                    break;
            }
            return weights;
        }
    }

    // This describes the sensor.
    private final SensorMetaData meta;

    // These are for the windowSize.
    private int windowSize; // Over how many samples do you want to average?
    private AverageWeighting weighting; // The windowSize mode.
    private float[] weights; // The weighting for the windowSize.

    // This stores the data, one SlidingWindowQueue for each dimension.
    private final ArrayList<SlidingWindowQueue<Object>> data;

    /** Constructs an instance of Sensor.
     *
     * @param mainLabel The main label of the sensor.
     * @param dimensionLabels The labels for each dimension.
     * @param dimensionTypes The types of data in each dimension.
     */
    public Sensor(String mainLabel, String[] dimensionLabels, Types[] dimensionTypes) {
        this.meta = new SensorMetaData(mainLabel, dimensionLabels, dimensionTypes, dimensionTypes.length);
        this.data = new ArrayList<>();
        setAveraging(1, null);
    }

    /** Grabs the meta data of the sensor.
     *
     * @return The sensor meta data.
     */
    public final SensorMetaData getMetaData() { return this.meta; }

    /** Updates the memory with new data.
     *
     * @param data The new data to store, one value for each dimension.
     */
    protected void update(Object... data) {
        synchronized (this.data) {
            if (this.data.size() != data.length) { Log.e("Sensor", "Invalid data lengths in update()."); return; }
            for (int i = 0; i < data.length; ++i) this.data.get(i).add(data[i]);
        }
    }

    /** Collect the most recent data for each dimension; averaged if option is set.
     *
     * @return The averaged sensor data, or last values if window size is just 1.
     */
    public Object[] collect() {
        Object[] result = new Object[data.size()];

        // If window size is 1, don't need to do any averaging.
        if (windowSize == 1) for (int i = 0; i < result.length; ++i) result[i] = data.get(i).peek();

        // Otherwise, let's do averaging.
        else {
            synchronized (this.data) {
                Types[] types = this.meta.getDimensionTypes(); // Get the dimension types.
                for (int i = 0; i < data.size(); ++i) { // For each dimension of the sensor.
                    SlidingWindowQueue<Object> window = data.get(i); // Get the window of data.
                    boolean keep = true; Object value;
                    switch (types[i]) { // Then, depending on the dimension type, calculate average.
                        case Float:
                            float f_result = 0;
                            for (int j = 0; j < window.size(); ++j) {
                                if ((value = window.peek()) == null) { keep = false; break; }
                                f_result += (Float) value * weights[j];
                            }
                            result[i] = (keep) ? f_result : null;
                            break;

                        case Double:
                            double d_result = 0;
                            for (int j = 0; j < window.size(); ++j) {
                                if ((value = window.peek()) == null) { keep = false; break; }
                                d_result += (Double) value * weights[j];
                            }
                            result[i] = (keep) ? d_result : null;
                            break;

                        case Integer:
                            float i_result = 0;
                            for (int j = 0; j < window.size(); ++j) {
                                if ((value = window.peek()) == null) { keep = false; break; }
                                i_result += (Integer) value * weights[j];
                            }
                            result[i] = (keep) ? (int) i_result : null;
                            break;

                        default: // Just use the last value.
                            result[i] = window.peek();
                    }
                }
            }
        }
        return result;
    }

    /** Allows smoothing over a number of samples; destroys the current sensor data in memory.
     *
     * @param windowSize The number of samples to average over.
     * @param weighting The type of weighting to use.
     * */
    public void setAveraging(int windowSize, AverageWeighting weighting) {
        this.windowSize = (windowSize <= 1) ? 1 : windowSize;
        this.weighting = (weighting != null) ? weighting : AverageWeighting.EQUAL;
        this.weights = this.weighting.generateWeights(this.windowSize);
        this.data.clear(); for (int i = 0; i < this.meta.getDimension(); ++i)
            this.data.add(new SlidingWindowQueue<Object>(windowSize));
    }

    class SlidingWindowQueue<T> {
        private Object[] queue;
        private int head; // Always positioned at the first item.
        private int tail; // Always positioned one after the last item.
        private int size;

        private final Object cursorLock = new Object();

        /** Constructs an instance of a SlidingWindowQueue.
         *
         * @param windowSize The number of items to store at any given time.
         */
        public SlidingWindowQueue(int windowSize) {
            this.queue = new Object[windowSize];
            this.head = this.tail = 0;
            this.size = 0;
        }

        /** Adds an item to the tail of the queue.
         *
         * @param item The item to add.
         */
        public synchronized void add(T item) {
            queue[tail] = item; // Place item.
            tail = ++tail % queue.length;
            if (size == queue.length) head = tail;
            else ++size;

            if (size > queue.length) Log.e("SlidingWindowQueue", "Size is greater than expected in add()!");
        }

        /** Retrieves and removes the first item from the queue.
         *
         * @return The item at the head of the queue.
         */
        public synchronized T poll() {
            if (size == 0) return null;
            T result = null; try { result = (T) queue[head]; // Save the item at the head.
            } catch (ClassCastException e) { Log.e("SlidingWindowQueue", "Problem casting item in poll()."); }
            queue[head] = null; // Nullify spot in queue.
            head = ++head % queue.length; // Shift head up.
            --size; // Update size.
            return result; // Return saved item.
        }

        /** Retrieves but does not remove the first item from the queue.
         *
         * @return The item at the head of the queue.
         */
        public synchronized T peek() {
            if (size == 0) return null;
            T result = null; try { result = (T) queue[head];
            } catch (ClassCastException e) { Log.e("SlidingWindowQueue", "Problem casting item in peek()."); }
            return result;
        }

        /** Retrieves but does not remove the ith item in the queue.
         *
         * @param i The zero-indexed position of the item.
         * @return The item at the specified position, or null if does not exist.
         */
        public synchronized T getAt(int i) {
            T result = null; try { result = (T) queue[(head + i) % queue.length]; }
            catch (ClassCastException e) { Log.e("SlidingWindowQueue", "Problem casting item in getAt(i)."); }
            return result;
        }

        /** Grabs the overall size of the queue (window).
         *
         * @return Window size, or queue length.
         */
        public int size() {
            return queue.length;
        }
    }

    /** Encapsulates meta data of the sensor, makes it easy to pass around. */
    public static class SensorMetaData {
        private final String mainLabel;
        private final String[] dimensionLabels;
        private final Types[] dimensionTypes;
        private final int dimension;

        /** Constructs a new SensorMetaData instance.
         *
         * @param mainLabel  The main label of the sensor (e.g. "gyroscope").
         * @param dimensionLabels The labels of each dimension (e.g., "x", or "velocity_x").
         * @param dimension The dimensionality of the sensor.
         */
        public SensorMetaData(String mainLabel, String[] dimensionLabels, Types[] dimensionTypes, int dimension) {
            this.mainLabel = mainLabel;
            this.dimensionLabels = dimensionLabels;
            this.dimensionTypes = dimensionTypes;
            this.dimension = dimension;
        }

        /** Grabs the sensor's main label (e.g. "accelerometer", "gyroscope").
         *
         * @return The main label of the sensor.
         */
        public String getMainLabel() { return this.mainLabel; }

        /** Grabs the label of a sensor dimension (e.g. "x", "velocity_x").
         *
         * @param index The index of the dimension (starts with 0).
         * @return The label of the dimension, or null if doesn't exist.
         */
        public String getDimensionLabel(int index) {
            return (this.dimensionLabels != null && (index >= 0 && index < this.dimensionLabels.length))
                    ? this.dimensionLabels[index] : null;
        }

        /** Get the types of each dimension.
         *
         * @return The type of data for each dimension.
         */
        public Types[] getDimensionTypes() { return this.dimensionTypes; }

        /** Checks the dimensionality of the sensor.
         *
         * @return The dimensionality of the sensor.
         */
        public int getDimension() { return this.dimension; }
    }
}
