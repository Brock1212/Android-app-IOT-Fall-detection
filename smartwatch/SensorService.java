package reuiot2015.smartwatch;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import reuiot2015.smartwatch.sensors.Sensor;
import reuiot2015.smartwatch.sensors.Types;
import reuiot2015.smartwatch.sensors_local.ButtonTouchSensor;
import reuiot2015.smartwatch.sensors_local.LocationSensor;
import reuiot2015.smartwatch.sensors_msband.MSBandLinker;
import reuiot2015.smartwatch.sensors.Collector;
import reuiot2015.smartwatch.sensors_persistence.CSVSampleWriter;
import reuiot2015.smartwatch.sensors_persistence.JSONSampleWriter;
import reuiot2015.smartwatch.sensors_persistence.SampleAccumulator;
import reuiot2015.smartwatch.sensors_persistence.SmartWatchValues;

/** This service maintains connections with sensors and does the data collection.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 */
public class SensorService extends Service {
    private final static int ONGOING_NOTIFICATION_ID = 0x1;

    // Specific to Service methodology.
    private final IBinder binder = new LocalBinder();
    private final static boolean allowRebind = true; // Allow activities to unbind and re-bind.

    // Used for monitoring of the connection status.
    private final Set<ConnectionStatusListener> connectionStatusListeners = Collections.synchronizedSet(new HashSet<ConnectionStatusListener>());

    // Subject Information
    private SubjectInformation subjectInformation;
    private float currentBac = 0.0f;

    // These are specific to the sensors and application.
    private final MSBandLinker linker = new MSBandLinker(SensorService.this);
    private final Collector collector = new Collector(4f);
    private LocationSensor locationSensor;
    private ButtonTouchSensor isDrinkingSensor;

    // This is used to maintain connection with the sensors.
    private SensorCollectorThread thread = null;

    private final Object collectionMonitor = new Object();

    @Override
    public void onCreate() {
        // Set up a notification to keep the service in the foreground.
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Smart Watch")
                .setContentText("The sensor service is running.");

        // This is the intent to launch the main view.
        Intent notificationIntent = new Intent(this, MainActivity.class);

        // Make it a PendingIntent to grant higher permission to launch the Activity.
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        builder.setContentIntent(pendingIntent);

        // Set the service in the foreground with the created notification.
        startForeground(ONGOING_NOTIFICATION_ID, builder.build());

        //**this.locationSensor = new LocationSensor(SensorService.this);
        //**this.isDrinkingSensor = new ButtonTouchSensor("is_drinking");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Keep running until explicitly stopped (restart if killed).
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder; // Used as an interface to the service.
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return allowRebind;
    }

    @Override
    public void onDestroy() {
        stopCollection();
        linker.unsubscribe();
        linker.disconnect();
    }

    /** Checks if the collection thread is running.
     *
     * @return True if the collection thread is running, false otherwise.
     */
    public boolean isCollecting() {
        return this.thread != null && this.thread.isCollecting();
    }

    /** Starts collection if not running. */
    public void startCollection() {
        synchronized (collectionMonitor) { // Make sure someone doesn't try to stop collection.
            if (this.thread == null) {
                this.thread = new SensorCollectorThread();
                this.thread.start();
            }
        }
    }

    /** Stops collection if running. */
    public void stopCollection() {
        synchronized (collectionMonitor) { // Make sure someone doesn't try to start collection.
            if (this.thread != null) {
                this.thread.stopCollection();
                this.thread = null;
            }
        }
    }

    /** Registers a ConnectionStatusListener for monitoring connection status changes.
     *
     * @param listener The listener to add.
     */
    public void addConnectionStatusListener(ConnectionStatusListener listener) {
        this.connectionStatusListeners.add(listener);
    }

    /** Unregisters a ConnectionStatusListener.
     *
     * @param listener The listener to remove.
     */
    public void removeConnectionStatusListener(ConnectionStatusListener listener) {
        this.connectionStatusListeners.remove(listener);
    }

    /** Registers a CollectorSampleListener for monitoring collected samples.
     *
     * @param listener The listener to add.
     */
    public void addCollectorSampleListener(Collector.SampleListener listener) {
        this.collector.addListener(listener);
    }

    /** Unregisters a ConnectionStatusListener.
     *
     * @param listener The listener to remove.
     */
    public void removeCollectorSampleListener(Collector.SampleListener listener) {
        this.collector.removeListener(listener);
    }

    public void setIsDrinkingButton(Button button) {
        //***this.isDrinkingSensor.setButton(button);
        //***if (this.thread != null && this.thread.isCollecting()) this.isDrinkingSensor.subscribe();
    }

    /** Sets the subject information to use.
     *
     * @param information The subject information object.
     */
    public void setSubjectInformation(SubjectInformation information) {
        this.subjectInformation = information;
    }

    /** Sets the current BAC of the subject.
     *
     * @param bac The current BAC of the subject.
     */
    public void setCurrentBac(float bac) { this.currentBac = bac; }

    /** Grabs the current BAC of the subject.
     *
     * @return The current BAC of the subject.
     */
    public float getCurrentBac() { return this.currentBac; }

    /** Generates a list of names for each column in a sample of data.
     *
     * @param additional Additional dimensions to include.
     * @return The generated sample header.
     */
    public String[] generateSampleHeader(String... additional) {
        Sensor.SensorMetaData[] meta = this.collector.getMeta();
        int headerSize = additional.length; for (Sensor.SensorMetaData m : meta) headerSize += m.getDimension();
        String[] result = new String[headerSize];

        // Set the header names for the sensors.
        int i = 0; for (Sensor.SensorMetaData m : meta) for (int j = 0; j < m.getDimension(); ++j)
            result[i++] = m.getMainLabel() + "_" + m.getDimensionLabel(j);

        // Set the additional header names.
        for (String s : additional) result[i++] = s;

        return result;
    }

    /** Generates a sample given data from a collected sample.
     *
     * @param data The sensor sample data.
     * @param meta The respective sensor meta data.
     * @param additional Additional values to use.
     *
     * @return The generated sample string.
     */
    public String[] generateSampleString(Object[] data, Sensor.SensorMetaData[] meta, String... additional) {
        String[] result = new String[data.length + additional.length];

        // Set the sensor values (convert to string by appropriate type).
        int i = 0; for (Sensor.SensorMetaData m : meta)
            for (Types t :  m.getDimensionTypes()) { result[i] = t.asString(data[i]); ++i; }

        // Set the additional values.
        for (String s : additional) result[i++] = s;

        return result;
    }

    /** Grabs the SubjectInformation object in use.
     *
     * @return The current SubjectInformation object.
     */
    public SubjectInformation getSubjectInformation() {
        return this.subjectInformation;
    }

    /** Used to establish heart rate consent with the MS Band.
     *
     * @param activity The activity to use.
     */
    public void setConsentActivity(Activity activity) {
        this.linker.setConsentActivity(activity);
    }

    public class LocalBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }

    /** This thread creates and attempts to maintain connection with MS Band smartwatch. */
    private class SensorCollectorThread extends Thread {
        boolean collecting = true;

        @Override
        public void run() {
            // Notify that collection is starting.
            for (ConnectionStatusListener l : connectionStatusListeners) l.onCollectionStarting();

            // Begin the main loop.
            while (!Thread.interrupted() && this.collecting) {
                // Try to connect to smartwatch until connected.
                while (!linker.connect()) {
                    try { Thread.sleep(1000);
                    } catch (InterruptedException e) { /** Nothing to do here. */ }
                }

                // Add the sensors to the data collector.
                collector.addSensors(linker);
                //***collector.addSensor(locationSensor);
                collector.addSensor(isDrinkingSensor);

                // Subscribe the sensors for monitoring.
                // TODO: subscription should be in collector.
                linker.subscribe();
                //***locationSensor.subscribe();
                //***isDrinkingSensor.subscribe();

                // Begin collection from sensors.
                collector.begin();

                // Notify that collection was started.
                for (ConnectionStatusListener l : connectionStatusListeners) l.onCollectionStarted();

                String[] header = generateSampleHeader("timestamp", "bac_observed");

                // Construct a CSVSampleWriter to use for saving the samples to .csv file.
                final CSVSampleWriter csvSampleWriter = new CSVSampleWriter(
                        header, "default.csv"
                );

                // Setup the sample accumulator.
                SampleAccumulator.StorageConfig storageConfig = new SampleAccumulator.StorageConfig(
                        SampleAccumulator.StorageConfig.TriggerType.NUM_SAMPLES, 25
                );
                final SampleAccumulator sampleAccumulator = new SampleAccumulator(storageConfig);
                sampleAccumulator.addSampleAccumulationListener(csvSampleWriter);
                sampleAccumulator.start();

                // Set a Collector.SampleListener to send new samples to the SampleAccumulator.
                Collector.SampleListener sampleListener = new Collector.SampleListener() {
                    @Override
                    public void onSampleReceived(Object[] sample, Sensor.SensorMetaData[] metaData, long timestamp) {
                        sampleAccumulator.enqueueSample(generateSampleString(sample, metaData, timestamp+"", getCurrentBac()+""));
                    }
                };
                collector.addListener(sampleListener);

                // Observe the linker connection.
                try {
                    while (linker.isConnected()) Thread.sleep(2000);
                    // If the while loop is exited, the connection was disrupted; notify status listeners.
                    for (ConnectionStatusListener l : connectionStatusListeners) l.onCollectionDisrupted();
                } catch (InterruptedException e) { /** Nothing to do here. */ }

                sampleAccumulator.stopStorage(); // Kill the saving of the samples.
                csvSampleWriter.release();

                collector.removeListener(sampleListener);
                collector.clearSensors(); // Clear the sensors from the collector.
                locationSensor.unsubscribe(); // TODO: should be part of collector.
                isDrinkingSensor.unsubscribe();
            }

            // Notify that collection has stopped.
            for (ConnectionStatusListener l : connectionStatusListeners) l.onCollectionStopped();
        }

        /** Checks if the thread is currently trying to collect data.
         *
         * @return True if collection thread is running, false otherwise.
         */
        public boolean isCollecting() {
            return this.collecting;
        }

        /** Stops the collection thread. */
        public void stopCollection() {
            this.collecting = false;
            interrupt();
        }
    }

    /** Used to monitor changes in the status of the collection thread. */
    public interface ConnectionStatusListener {
        /** Called when the collection thread is starting. */
        void onCollectionStarting();

        /** Called when the collection has begun. */
        void onCollectionStarted();

        /** Called if the collection is disrupted, because of a dropped connection or something. */
        void onCollectionDisrupted();

        /** Called when the collection has stopped. */
        void onCollectionStopped();
    }

    /** An immutable encapsulation of a subject's profile information. */
    public static class SubjectInformation {
        public final int age;
        public final float bmi;
        public final String bloodType;
        public final String gender;
        public final String uuid;

        private SubjectInformation(Builder builder) {
            this.age = builder.age;
            this.bmi = builder.bmi;
            this.bloodType = builder.bloodType;
            this.gender = builder.gender;
            this.uuid = builder.uuid;
        }

        public void exportToJSON(String filename) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File publicDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), SmartWatchValues.ALBUM_NAME + "/profiles");
                Log.d("SubjectInformation", "Save path is: " + publicDirectory.getAbsolutePath());

                if (publicDirectory.mkdirs()) Log.d("SubjectInformation", "Created file structure.");
                else Log.d("SubjectInformation", "Using existing file structure, or failed to create.");

                FileWriter writer = null;
                try {
                    writer = new FileWriter(new File(publicDirectory, filename));
                    JSONObject object = new JSONObject()
                            .put("age", age)
                            .put("bmi", bmi)
                            .put("blood_type", bloodType)
                            .put("gender", gender)
                            .put("uuid", uuid);
                    writer.write(object.toString(2));
                } catch (IOException e) {
                    /** Nothing to do here. */
                } catch (JSONException e) {
                    Log.e("SubjectInformation", "Error writing to JSON object. " + e.getMessage());
                } finally {
                    if (writer != null) {
                        try { writer.close();
                        } catch (IOException e) { /** Nothing to do here. */}
                    }
                }
            } else {
                Log.d("SubjectInformation", "External media is not mounted.");
            }
        }

        public static SubjectInformation importFromJSON(String filename) {
            Builder builder = new Builder();
            try {
                JsonReader reader = new JsonReader(new FileReader(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), SmartWatchValues.ALBUM_NAME + "/profiles/" + filename)));
                reader.beginObject();
                reader.nextName();
                builder.setAge(reader.nextInt());
                reader.nextName();
                builder.setBmi((float) reader.nextDouble());
                reader.nextName();
                builder.setBloodType(reader.nextString());
                reader.nextName();
                builder.setGender(reader.nextString());
                reader.nextName();
                builder.setUUID(reader.nextString());
                reader.close();
                return builder.build();
            } catch (IOException e) {
                /** Nothing to do here. */
            }
            return null;
        }

        @Override
        public String toString() {
            return String.format("%d %.2f %s %s %s", age, bmi, bloodType, gender, uuid.substring(0, 8));
        }

        public static class Builder {
            private int age;
            private float bmi;
            private String bloodType;
            private String gender;
            private String uuid;

            /** Sets the age.
             *
             * @param age The age of the subject.
             *
             * @return This builder.
             */
            public Builder setAge(int age) { this.age = age; return this; }

            /** Sets the Body Mass Index (BMI).
             *
             * @param bmi The BMI of the subject.
             *
             * @return This builder.
             */
            public Builder setBmi(float bmi) { this.bmi = bmi; return this; }

            /** Sets the blood type.
             *
             * @param bloodType The blood type of the subject.
             *
             * @return This builder.
             */
            public Builder setBloodType(String bloodType) { this.bloodType = bloodType; return this; }

            /** Sets the gender.
             *
             * @param gender The gender of the subject.
             *
             * @return This builder.
             */
            public Builder setGender(String gender) { this.gender = gender; return this; }

            public Builder setUUID(String uuid) { this.uuid = uuid; return this; }

            /** Constructs a new SubjectInformation object from the given information.
             *
             * @return The new SubjectInformation object.
             */
            public SubjectInformation build() {
                return new SubjectInformation(this);
            }
        }
    }
}