package reuiot2015.smartwatch.sensors_msband;

import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;

import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Microsoft Band distance sensor.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class DistanceSensor extends MSBandSensor {
    // This is necessary for connecting to MS Band smartwatch.
    private final BandClient client;

    // Listens for sensor data and updates the general sensor with the data.
    private final BandDistanceEventListener listener = new BandDistanceEventListener() {
        @Override
        public void onBandDistanceChanged(BandDistanceEvent e) {
            update(e.getPace(), e.getSpeed(), e.getTotalDistance(), e.getMotionType().toString());
        }
    };

    /**
     * Constructs an instance of an DistanceSensor.
     *
     * @param client A connected MS Band Client.
     */
    public DistanceSensor(BandClient client) {
        super(
                "ms_distance",
                new String[] {"pace", "speed", "total_distance", "motion_type"},
                new Types[] {Types.Float, Types.Float, Types.Long, Types.String},
                null
        );
        this.client = client;
    }

    @Override
    public void register() {
        try { client.getSensorManager().registerDistanceEventListener(this.listener);
        } catch (BandException  e) {  Log.e("DistanceSensor", e.getMessage()); }
    }

    @Override
    public void unregister() {
        try { client.getSensorManager().unregisterDistanceEventListener(this.listener);
        } catch (BandIOException e) {  Log.e("DistanceSensor", e.getMessage()); }
    }
}
