package reuiot2015.smartwatch.sensors_msband;

import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;

import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Microsoft Band calories sensor.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class HeartRateSensor extends MSBandSensor {
    // This is necessary for connecting to MS Band smartwatch.
    private final BandClient client;

    // Listens for sensor data and updates the general sensor with the data.
    private final BandHeartRateEventListener listener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(BandHeartRateEvent e) {
            update(e.getHeartRate(), e.getQuality().toString());
        }
    };

    /**
     * Constructs an instance of a HeartRateSensor.
     *
     * @param client A connected MS Band Client.
     */
    public HeartRateSensor(BandClient client) {
        super(
                "ms_heart_rate",
                new String[] {"bpm", "quality"},
                new Types[] {Types.Integer, Types.String},
                null
        );
        this.client = client;
    }

    @Override
    public void register() {
        try { client.getSensorManager().registerHeartRateEventListener(this.listener);
        } catch (BandException  e) {  Log.e("HeartRateSensor", e.getMessage()); }
    }

    @Override
    public void unregister() {
        try { client.getSensorManager().unregisterHeartRateEventListener(this.listener);
        } catch (BandIOException e) {  Log.e("HeartRateSensor", e.getMessage()); }
    }
}
