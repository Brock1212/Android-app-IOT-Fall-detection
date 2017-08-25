package reuiot2015.smartwatch.sensors_msband;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;

import reuiot2015.smartwatch.sensors.Linker;
import reuiot2015.smartwatch.sensors.Sensor;

/** Links the sensors of the first paired Microsoft Band smart watch found with the "sensor" framework. */
public class MSBandLinker extends Linker {
    private final Context context; // The Android application context.
    private BandClient client; // The client used to connect the sensors.
    private Activity activity; // Used for heart rate consent.

    private boolean hasHeartRateConsent = false;

    private HeartRateConsentListener heartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean b) {
            hasHeartRateConsent = b;
        }
    };

    /** Constructs a MSBandLinker object instance.
     *
     * @param context The Android application context.
     */
    public MSBandLinker(Context context) {
        this.context = context;
    }

    @Override
    public boolean connect() {
        try {
            if (getConnectedClient()) {
                addMSBandSensors();
                return true;
            } else
                return false;
        } catch (InterruptedException e) {
            Log.d("MSBandLinker", e.getMessage());
        } catch (BandException e) {
            String exceptionMessage;
            switch (e.getErrorType()) {
                case UNSUPPORTED_SDK_VERSION_ERROR:
                    exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                    break;
                case SERVICE_ERROR:
                    exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                    break;
                default:
                    exceptionMessage = "Unknown error occurred: " + e.getMessage();
                    break;
            }
            Log.d("MSBandLinker", exceptionMessage);
        }
        return false;
    }
    private boolean getConnectedClient() throws BandException, InterruptedException {
        if (this.client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                Log.d("MSBandLinker", "Band isn't paired with your phone.");
                return false;
            }
            this.client = BandClientManager.getInstance().create(this.context, devices[0]);
        } else if (ConnectionState.CONNECTED == this.client.getConnectionState()) {
            return true;
        }
        Log.d("MSBandLinker", "Band is connecting...");
        return ConnectionState.CONNECTED == this.client.connect().await();
    }

    @Override
    public void disconnect() {
        if (this.client != null) {
            clearSensors();
            this.client.disconnect();
        }
    }

    @Override
    public void subscribe() {
        for (Sensor s : getSensors()) ((MSBandSensor)s).register();
    }

    @Override
    public void unsubscribe() {
        for (Sensor s : getSensors()) ((MSBandSensor)s).unregister();
    }

    @Override
    public boolean isConnected() {
        return this.client != null && this.client.isConnected();
    }

    /** Set the Activity to use for heart rate consent.
     *
     * @param activity The heart rate consent activity.
     */
    public void setConsentActivity(Activity activity) {
        this.activity = activity;
    }

    /** Establishes heart rate consent so you can use the heart rate sensor. */
    private boolean establishHeartRateConsent() {
        if (activity != null) {
            if (this.client.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED)
                this.client.getSensorManager().requestHeartRateConsent(activity, this.heartRateConsentListener);
            else hasHeartRateConsent = true;
        } return hasHeartRateConsent;
    }

    /** Wraps all the MS Band sensors and adds them to the list. */
    private void addMSBandSensors() {
        clearSensors();

        MSBandSensor sensor = new AccelerometerSensor(this.client, SampleRate.MS128);
        sensor.setAveraging(3, Sensor.AverageWeighting.LINEAR);
        addSensor(sensor);

        /*sensor = new GyroscopeSensor(this.client, SampleRate.MS128);
        sensor.setAveraging(3, Sensor.AverageWeighting.LINEAR);
        addSensor(sensor);

        addSensor(new DistanceSensor(this.client));
        if (establishHeartRateConsent()) addSensor(new HeartRateSensor(this.client));
        addSensor(new PedometerSensor(this.client));
        addSensor(new SkinTemperatureSensor(this.client));
        addSensor(new UVSensor(this.client));
        addSensor(new ContactSensor(this.client));
        addSensor(new CaloriesSensor(this.client));*/
    }
}