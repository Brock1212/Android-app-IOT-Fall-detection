package reuiot2015.smartwatch;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import reuiot2015.smartwatch.sensors.Collector;
import reuiot2015.smartwatch.sensors.Sensor;
import reuiot2015.smartwatch.sensors.Types;
import reuiot2015.smartwatch.sensors_persistence.SmartWatchValues;


public class MainActivity extends Activity implements Collector.SampleListener, CollectionStoppedFragment.OnCollectionStartButtonClick, CollectionStartedFragment.OnCollectionStopButtonClick, CollectionStartedFragment.OnDrinkingButtonLinked, ProfileSelectFragment.ProfileSelectionListener {
    private ListView sensorList;
    private TextView console;
    private TextView timestamp;
    private EditText bac;
    private Button bacButton;

    private final static DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("EEE,  MM.dd.yy,  HH:mm:ss:SSS  zzz");
    private final static DateFormat FILENAME_SUFFIX = new SimpleDateFormat("_yy-MM-dd_HH-mm");

    private ServiceBACPosterThread bacPoster; // Post the bac to the service when ready.

    private Sensor.SensorMetaData[] sensorMetaData;
    private Object[] sample;
    private final Object sampleLock = new Object();

    private final String[] consoleMessages = new String[5];
    private int consoleIndex = 0;

    private SensorService.SubjectInformation subjectInformation;

    private SensorService.ConnectionStatusListener connectionStatusListener = new SensorService.ConnectionStatusListener() {
        @Override
        public void onCollectionStarting() {
            updateConsole("Collection is starting...");
        }

        @Override
        public void onCollectionStarted() {
            updateConsole("Collection has started.");

            // Swap collection controls when collection has begun.
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_collection_controls, new CollectionStartedFragment())
                    .commit();
        }

        @Override
        public void onCollectionDisrupted() {
            updateConsole("Collection was disrupted.");
        }

        @Override
        public void onCollectionStopped() {
            updateConsole("Collection has stopped.");

            sensorList.post(new Runnable() {
                @Override
                public void run() {
                    sensorList.setAdapter(null);
                }
            });

            // Swap collection controls when collection has halted.
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_collection_controls, new CollectionStoppedFragment())
                    .commit();
        }
    };

    private SensorService service;
    private boolean bound = false;

    // Monitors the connection to the service.
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            SensorService.LocalBinder serviceBinder = (SensorService.LocalBinder) binder;
            service = serviceBinder.getService();
            bound = true;
            updateConsole("Bound to SensorService.");

            if (subjectInformation != null) service.setSubjectInformation(subjectInformation);
            else subjectInformation = service.getSubjectInformation();

            service.setConsentActivity(MainActivity.this);

            // Monitor the collected samples.
            service.addCollectorSampleListener(MainActivity.this);

            // Monitor the connection status of the service.
            service.addConnectionStatusListener(connectionStatusListener);

            // Make sure collection controls properly reflect status.
            if (service.isCollecting()) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container_collection_controls, new CollectionStartedFragment())
                        .commit();
            } else {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container_collection_controls, new CollectionStoppedFragment())
                        .commit();
            }

            // Set the BAC field if BAC is known.
            if (bacPoster == null || !bacPoster.isAlive())
                bac.setText(String.format("%.2f", service.getCurrentBac()));
            else
                bac.setText(String.format("%.2f", bacPoster.getBac()));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            updateConsole("Unexpectedly lost connection with SensorService.");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ProfileSettingsActivity.REQUEST_PROFILE) {
            if (resultCode == RESULT_OK) {
                // Handle profile information.
                Bundle extras = data.getExtras();

                this.subjectInformation = new SensorService.SubjectInformation.Builder()
                        .setAge( (int) extras.get(ProfileSettingsActivity.EXTRA_AGE) )
                        .setBloodType((String) extras.get(ProfileSettingsActivity.EXTRA_BLOOD_TYPE))
                        .setBmi((float) extras.get(ProfileSettingsActivity.EXTRA_BMI))
                        .setGender((String) extras.get(ProfileSettingsActivity.EXTRA_GENDER))
                        .setUUID((String) extras.get(ProfileSettingsActivity.EXTRA_UUID))
                        .build();

                updateConsole(String.format("%d Y.O.,  %.1f BMI,  %s,  %s",
                        this.subjectInformation.age,
                        this.subjectInformation.bmi,
                        this.subjectInformation.bloodType,
                        this.subjectInformation.gender
                        )
                );
                updateConsole(this.subjectInformation.uuid);

                if (bound && service != null) {
                    service.setSubjectInformation(this.subjectInformation);
                    updateConsole("Service profile information updated.");
                } else {
                    updateConsole("Service profile information will be updated on bind.");
                }

                String filename = subjectInformation.uuid.substring(0, 8) + ".txt";
                subjectInformation.exportToJSON(filename);
                updateConsole("Saved to: " + filename);

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link up the Views.
        this.sensorList = (ListView) findViewById(R.id.sensor_list);
        this.console = (TextView) findViewById(R.id.console);
        this.timestamp = (TextView) findViewById(R.id.timestamp);
        this.bac = (EditText) findViewById(R.id.edit_text_bac);
        this.bacButton = (Button) findViewById(R.id.button_bac);

        this.bacButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                EBACCalculationFragment dialog = new EBACCalculationFragment();
                dialog.show(getFragmentManager(), "EBACCalculationFragment");
                return true;
            }
        });

        // Initialize console strings.
        for (int i = 0; i < this.consoleMessages.length; ++i) this.consoleMessages[i] = "";

        // Set initial controls.
        getFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container_collection_controls, new CollectionStoppedFragment())
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to the SensorService.
        Intent intent = new Intent(this, SensorService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the SensorService.
        unbindService(serviceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;

        } else if (id == R.id.action_profile_settings) {
            Intent intent = new Intent(this, ProfileSettingsActivity.class);
            if (subjectInformation != null) {
                intent.putExtra(ProfileSettingsActivity.EXTRA_AGE, subjectInformation.age);
                intent.putExtra(ProfileSettingsActivity.EXTRA_BLOOD_TYPE, subjectInformation.bloodType);
                intent.putExtra(ProfileSettingsActivity.EXTRA_BMI, subjectInformation.bmi);
                intent.putExtra(ProfileSettingsActivity.EXTRA_GENDER, subjectInformation.gender);
                intent.putExtra(ProfileSettingsActivity.EXTRA_UUID, subjectInformation.uuid);
            }
            startActivityForResult(intent, ProfileSettingsActivity.REQUEST_PROFILE);

        }  else if(id == R.id.action_save_data) {
            if (bound && service != null) {
                SensorService.SubjectInformation subInfo = service.getSubjectInformation();

                if (subInfo == null)
                    updateConsole("Must set subject information first!");
                else {
                    File publicDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), SmartWatchValues.ALBUM_NAME + "/data");
                    File current = new File(publicDirectory, "default.csv");
                    File future = new File(publicDirectory, subInfo.uuid.substring(0, 8) + FILENAME_SUFFIX.format(new Date(System.currentTimeMillis()))  +".csv");

                    if (current.renameTo(future)) updateConsole("Saved to " + future.getName());
                    else updateConsole("Failed to save new file.");
                }

            } else updateConsole("Cannot get information from service!");

        } else if (id == R.id.action_load_profile) {

            File profiles = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), SmartWatchValues.ALBUM_NAME + "/profiles");
            File[] files = null;
            if (profiles.isDirectory()) files = profiles.listFiles();
            if (files == null) Toast.makeText(this, "No profiles.", Toast.LENGTH_SHORT).show();
            else {
                String[] names = new String[files.length];
                for (int i = 0; i < names.length; ++i) names[i] = files[i].getName();
                ProfileSelectFragment dialog = new ProfileSelectFragment();
                dialog.setProfiles(names);
                dialog.show(getFragmentManager(), "ProfileSelectFragment");
            }

        } else if (id == R.id.action_exit) {
            stopService(new Intent(this, SensorService.class));
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (service != null) {
            service.removeConnectionStatusListener(connectionStatusListener);
            service.removeCollectorSampleListener(this);
        }

        super.onDestroy();
    }

    /** Updates the console with a message.
     *
     * @param message The message to post.
     */
    private void updateConsole(String message) {
        synchronized (this.consoleMessages) {
            this.consoleIndex = ++this.consoleIndex % this.consoleMessages.length;
            this.consoleMessages[this.consoleIndex] = (message != null) ? message + "\n" : "\n";
            final StringBuilder sb = new StringBuilder();
            for (int i = this.consoleMessages.length - 1; i >= 0; --i)
                sb.append(this.consoleMessages[(this.consoleMessages.length + this.consoleIndex - i) % this.consoleMessages.length]);
            final String s = sb.toString();
            this.console.post(new Runnable() {
                @Override
                public void run() {
                    console.setText(s);
                }
            });
        }
    }

    @Override
    public void onCollectionStartButtonClick(Button button) {
        if (!bound) updateConsole("Not bound to service.");
        else {
            if (service != null && !service.isCollecting()) {
                if (service.getSubjectInformation() == null) {
                    updateConsole("Must set profile information first!");
                } else {
                    service.startCollection();
                }
            } else
                updateConsole("Service interface is missing!");
        }
    }

    @Override
    public void onCollectionStopButtonClick(Button button) {
        if (!bound) updateConsole("Not bound to service.");
        else {
            if (service != null && service.isCollecting()) {
                service.stopCollection();
            } else
                updateConsole("Service interface is missing!");
        }
    }

    @Override
    public void onDrinkingButtonLinked(final Button button) {
        // Wait for service to bind and set button.
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (service == null) {
                    try { Thread.sleep(1000);
                    } catch (InterruptedException e) { /** Nothing to do here. */ }
                }
                if (service != null && button != null) {
                    service.setIsDrinkingButton(button);
                    updateConsole("Drinking button (VS) linked with service.");
                }
            }
        }).start();
    }

    /** Executed when the BAC button is clicked, sends BAC info to service. */
    public void onBacButtonClick(View v) {
        try {
            float bacFloat = (this.bac != null) ? Float.parseFloat(this.bac.getText().toString()) : 0.0f;
            if (bacFloat < 0.0f || bacFloat > 0.5f) updateConsole("BAC value is not in valid range.");
            else {
                if (this.bacPoster != null && this.bacPoster.isAlive()) {
                    this.bacPoster.cancelPost(); // Cancel previous posting thread if any and wait for it to join.
                    try { this.bacPoster.join(); } catch (InterruptedException e) { /** Nothing to do here. */ }
                }
                this.bacPoster = new ServiceBACPosterThread(bacFloat);
                this.bacPoster.start(); // Start a new posting thread with the new value.
            }
        } catch (NumberFormatException e) {
            updateConsole("BAC not properly formatted.");
        } finally {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    @Override
    public void onSampleReceived(Object[] sample, Sensor.SensorMetaData[] metaData, final long timestamp) {
        synchronized (sampleLock) {
            this.sensorMetaData = metaData;
            this.sample = sample;
        }

        this.timestamp.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.timestamp.setText("ACT:   " + TIMESTAMP_FORMAT.format(new Date(timestamp)));
            }
        });

        // Update the sensor list with this data.
        sensorList.post(new Runnable() {
            @Override
            public void run() {
                ListAdapter adapter; if ((adapter = sensorList.getAdapter()) == null) {
                    Object[] sizing = new Object[MainActivity.this.sample.length + MainActivity.this.sensorMetaData.length];
                    adapter = new SensorListAdapter(MainActivity.this, sizing);
                    sensorList.setAdapter(adapter);
                }
                ((SensorListAdapter)adapter).notifyDataSetChanged();
            }
        });
    }

    @Override
    public void profileSelected(String name) {
        this.subjectInformation = SensorService.SubjectInformation.importFromJSON(name);
        if (bound && service != null) {
            service.setSubjectInformation(this.subjectInformation);
            updateConsole(name + " loaded. Service updated.");
        } else {
            updateConsole(name + " loaded. Will update service on bind.");
        }

    }

    /** This adapter is used to populate the ListView of the MainActivity. */
    class SensorListAdapter extends ArrayAdapter<Object> {
        private ArrayList<View> views;
        private HashMap<Integer, Integer> dataMap;
        private HashMap<Integer, Integer> metaMap;
        private HashMap<Integer, Integer> dimsMap;

        /** Constructs a new adapter.
         *
         * @param context The application context.
         * @param data The sensor data (used only for list size here).
         */
        public SensorListAdapter(Context context, Object[] data) {
            super(context, -1, data);

            views = new ArrayList<>(); // List of views to show in list.
            dataMap = new HashMap<>(); // Maps position to data index.
            metaMap = new HashMap<>(); // Maps position to meta index.
            dimsMap = new HashMap<>(); // Maps position to dimension index.

            int position = 0, idx = 0;
            for (int m = 0; m < sensorMetaData.length; ++m) {
                dataMap.put(position, null); // Position does not point to data.
                metaMap.put(position, m); // Position points to sensor meta data index m.
                ++position;
                views.add(null);
                for (int d = 0; d < sensorMetaData[m].getDimension(); ++d) {
                    dataMap.put(position, idx); // The position points to data index idx.
                    metaMap.put(position, m); // The position still points to meta data index m.
                    dimsMap.put(position, d);
                    ++position;
                    ++idx;
                    views.add(null);
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            Sensor.SensorMetaData smd = sensorMetaData[metaMap.get(position)];
            boolean isDivider = dataMap.get(position) == null;

            if (views.get(position) == null) {
                if (isDivider) {
                    v = LayoutInflater.from(getContext()).inflate(R.layout.sensor_divider_view, parent, false);
                    views.set(position, v);
                    TextView header = (TextView) v.findViewById(R.id.sensor_list_divider);
                    header.setText(smd.getMainLabel());
                } else {
                    v = LayoutInflater.from(getContext()).inflate(R.layout.sensor_item_view, parent, false);
                    views.set(position, v);
                    TextView sensor = (TextView) v.findViewById(R.id.attribute_label);
                    sensor.setText(smd.getDimensionLabel(dimsMap.get(position)));
                }
            }

            if (!isDivider) {
                TextView data = (TextView) views.get(position).findViewById(R.id.attribute_data);

                int d = dataMap.get(position);
                Types type = smd.getDimensionTypes()[dimsMap.get(position)];

                if (sample != null && d < sample.length && d >= 0){
                    if (sample[d] == null) data.setText("---");
                    data.setText(type.asString(sample[d]));
                } else {
                    data.setText("");
                }
            }

            return views.get(position);
        }
    }

    private class ServiceBACPosterThread extends Thread {
        private boolean running = true;
        private final float bac;

        public ServiceBACPosterThread(float bac) {
            this.bac = bac;
        }

        @Override
        public void run() {
            // Wait for service to become bound.
            while (!interrupted() && running && !bound)
                try { Thread.sleep(1000); }
                catch (InterruptedException e ) { /** Nothing to do here */ }

            // If service was bound and thread is still supposed to be running, set the BAC on the service.
            if (running && bound && service != null) {
                service.setCurrentBac(bac);
                updateConsole(String.format("Service updated with BAC %.2f.", bac));
            } else {
                updateConsole("Previous service BAC update request canceled.");
            }
        }

        /** Cancels the posting of the BAC to the service. */
        public void cancelPost() {
            this.running = false;
            interrupt();
        }

        /** Grabs the BAC value that is pending post.
         *
         * @return The BAC value set to be posted.
         */
        public float getBac() { return this.bac; }
    }
}
