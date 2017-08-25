package reuiot2015.smartwatch.sensors_msband;

import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.SampleRate;

import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Microsoft Band gyroscope sensor.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class GyroscopeSensor extends MSBandSensor {
    // This is necessary for connecting to MS Band smartwatch.
    private final BandClient client;

    // Listens for sensor data and updates the general sensor with the data.
    private final BandGyroscopeEventListener listener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(BandGyroscopeEvent e) {
            update(e.getAccelerationX(), e.getAccelerationY(), e.getAccelerationZ(), e.getAngularVelocityX(), e.getAngularVelocityY(), e.getAngularVelocityZ());
        }
    };

    /**
     * Constructs an instance of an GyroscopeSensor.
     *
     * @param client A connected MS Band Client.
     * @param sampleRate The sampling rate for the sensor.
     */
    public GyroscopeSensor(BandClient client, SampleRate sampleRate) {
        super(
                "ms_gyroscope",
                new String[] {"acceleration_x", "acceleration_y", "acceleration_z", "velocity_x", "velocity_y", "velocity_z"},
                new Types[] {Types.Float, Types.Float, Types.Float, Types.Float, Types.Float, Types.Float},
                sampleRate
        );
        this.client = client;
    }

    @Override
    public void register() {
        try { client.getSensorManager().registerGyroscopeEventListener(this.listener, getSampleRate());
        } catch (BandException  e) {  Log.e("GyroscopeSensor", e.getMessage()); }
    }

    @Override
    public void unregister() {
        try { client.getSensorManager().unregisterGyroscopeEventListener(this.listener);
        } catch (BandIOException e) {  Log.e("GyroscopeSensor", e.getMessage()); }
    }
}
