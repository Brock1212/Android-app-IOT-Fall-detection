package reuiot2015.smartwatch.sensors_msband;

import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;

import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Microsoft Band pedometer sensor.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class PedometerSensor extends MSBandSensor {
    // This is necessary for connecting to MS Band smartwatch.
    private final BandClient client;

    // Listens for sensor data and updates the general sensor with the data.
    private final BandPedometerEventListener listener = new BandPedometerEventListener() {
        @Override
        public void onBandPedometerChanged(BandPedometerEvent e) {
            update(e.getTotalSteps());
        }
    };

    /**
     * Constructs an instance of a PedometerSensor.
     *
     * @param client A connected MS Band Client.
     */
    public PedometerSensor(BandClient client) {
        super(
                "ms_pedometer",
                new String[] {"steps"},
                new Types[] {Types.Long},
                null
        );
        this.client = client;
    }

    @Override
    public void register() {
        try { client.getSensorManager().registerPedometerEventListener(this.listener);
        } catch (BandException  e) {  Log.e("PedometerSensor", e.getMessage()); }
    }

    @Override
    public void unregister() {
        try { client.getSensorManager().unregisterPedometerEventListener(this.listener);
        } catch (BandIOException e) {  Log.e("PedometerSensor", e.getMessage()); }
    }
}
