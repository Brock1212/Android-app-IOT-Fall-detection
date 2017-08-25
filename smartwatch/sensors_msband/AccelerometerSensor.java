package reuiot2015.smartwatch.sensors_msband;

import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;

import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Microsoft Band accelerometer sensor.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class AccelerometerSensor extends MSBandSensor {
    // This is necessary for connecting to MS Band smartwatch.
    private final BandClient client;

    // Listens for sensor data and updates the general sensor with the data.
    private final BandAccelerometerEventListener listener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(BandAccelerometerEvent evt) {
            update(evt.getAccelerationX(), evt.getAccelerationY(), evt.getAccelerationZ());
        }
    };

    /**
     * Constructs an instance of an AccelerometerSensor.
     *
     * @param client A connected MS Band Client.
     * @param sampleRate The sampling rate for the sensor.
     */
    public AccelerometerSensor(BandClient client, SampleRate sampleRate) {
        super(
                "ms_accelerometer",
                new String[] {"x", "y", "z"},
                new Types[] {Types.Float, Types.Float, Types.Float},
                sampleRate
        );
        this.client = client;
    }

    @Override
    public void register() {
        try { client.getSensorManager().registerAccelerometerEventListener(this.listener, getSampleRate());
        } catch (BandException  e) {  Log.e("AccelerometerSensor", e.getMessage()); }
    }

    @Override
    public void unregister() {
        try { client.getSensorManager().unregisterAccelerometerEventListener(this.listener);
        } catch (BandIOException e) {  Log.e("AccelerometerSensor", e.getMessage()); }
    }
}
