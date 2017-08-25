package reuiot2015.smartwatch.sensors_persistence;

import android.util.Log;

import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Accumulates samples and sent to listeners in batches based on a trigger value and type.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 */
public class SampleAccumulator extends Thread {
    private final ConcurrentLinkedDeque<String[]> sampleQueue;
    private final StorageConfig config;

    private final Object saveLock = new Object();

    private final HashSet<SampleAccumulationListener> sampleAccumulationListeners = new HashSet<>();

    private boolean running = true;
    private int sampleSize = -1;

    /** Constructs a new SampleAccumulator instance.
     *
     * @param config The base configuration for the writer.
     */
    public SampleAccumulator(StorageConfig config) {
        this.sampleQueue = new ConcurrentLinkedDeque<>();
        this.config = config;
    }

    /** Adds a new SampleAccumulationListener to this instance.
     *
     * @param listener The listener to add.
     */
    public void addSampleAccumulationListener(SampleAccumulationListener listener) {
        this.sampleAccumulationListeners.add(listener);
    }

    /** Removes a new SampleAccumulationListener to from instance.
     *
     * @param listener The listener to remove.
     */
    public void removeSampleAccumulationListener(SampleAccumulationListener listener) {
        this.sampleAccumulationListeners.remove(listener);
    }

    /** Grabs the set sample size in use by this SampleAccumulator instance.
     *
     * @return The set sample size, or -1 if it hasn't been set yet.
     */
    public int getSampleSize() {
        return this.sampleSize;
    }

    @Override
    public void run() {
        Log.d("SampleAccumulator", "Save thread starting...");

        while (!Thread.interrupted() && running) {
            Log.d("SampleAccumulator", "Waiting for trigger...");

            // Wait for the trigger value, depends on trigger type.
            switch (this.config.triggerType) {
                case NUM_SAMPLES:
                    while (this.sampleQueue.size() < this.config.triggerValue) try { Thread.sleep(1000); } catch (InterruptedException e) { /** Nothing to do here. */ }
                    break;
                case NUM_MINUTES:
                    long totalWait = this.config.triggerValue * 60000; // 60,000 ms in a minute.
                    long start = System.currentTimeMillis();
                    long elapsed; while ((elapsed = (System.currentTimeMillis() - start)) < totalWait)
                        try {  wait(totalWait - elapsed); }
                        catch (InterruptedException e) { /** Nothing to do here. */ }
                    break;
            }

            Log.d("SampleAccumulator", "Separating out samples. Initial queue size: " + this.sampleQueue.size());

            // Separate the samples.
            String[][] samples = null;
            switch (this.config.triggerType) {
                case NUM_SAMPLES:
                    samples = new String[this.config.triggerValue][this.sampleSize]; // [num_samples][sample_size], [rows][cols].
                    for (int i = 0; i < this.config.triggerValue; ++i) samples[i] = this.sampleQueue.poll();
                    break;
                case NUM_MINUTES:
                    samples = new String[this.sampleQueue.size()][this.sampleSize];
                    for (int i = 0; i < samples.length; ++i) samples[i] = this.sampleQueue.poll();
                    break;
            }

            Log.d("SampleAccumulator", samples.length + " samples selected.");
            Log.d("SampleAccumulator", "Resulting queue size: " + this.sampleQueue.size());

            // Save samples.
            synchronized (this.saveLock) {
                for (SampleAccumulationListener l : sampleAccumulationListeners)
                l.receiveAccumulatedSamples(samples);
            }
        }

        Log.d("SampleAccumulator", "Save thread finished.");
    }

    /** Stops sample storage. */
    public void stopStorage() {
        running = false;
        interrupt();
    }

    /** Adds a sample to be stored, the first sample sent determines the sample size.
     *
     * @param sample The sample, as a list of string values, to save.
     * @return True if sample was successfully queued, false otherwise.
     */
    public boolean enqueueSample(String[] sample) {
        if (this.sampleSize == -1) this.sampleSize = sample.length;
        return this.sampleQueue.offer(sample.clone());
    }

    /** Used by other classes to receive a set of accumulated samples. */
    public interface SampleAccumulationListener {
        /** Saves list of samples as precisely specified by the extending class. Implementing classes
         * must not modify the received sample array as it is mutable. Also, lock on the received
         * samples object to prevent concurrency problems.
         *
         * @param accumulatedSamples The samples, as lists of String values, to write.
         * @return True if samples were successfully written, false otherwise.
         */
        boolean receiveAccumulatedSamples(String[][] accumulatedSamples);
    }

    /** Encapsulates configuration options for samples written. */
    public static class StorageConfig {

        /** Enumerates the possible interpretations of the trigger value. */
        public enum TriggerType {
            NUM_MINUTES, NUM_SAMPLES
        }

        private final TriggerType triggerType;
        private final int triggerValue;

        /** Constructs a StorageConfig object for use by a SampleAccumulator.
         *
         * @param triggerType How the trigger value should be interpreted.
         * @param triggerValue The actual trigger value.
         */
        public StorageConfig(TriggerType triggerType, int triggerValue) {
            this.triggerType = triggerType;
            this.triggerValue = triggerValue;
        }

        /** Grabs the trigger type.
         *
         * @return The set trigger type.
         */
        public TriggerType getTriggerType() {
            return this.triggerType;
        }

        /** Grabs the trigger value.
         *
         * @return The set trigger value.
         */
        public int getTriggerValue() {
            return this.triggerValue;
        }
    }
}
