package reuiot2015.smartwatch.sensors_msband;

import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.sensors.BandUVEvent;
import com.microsoft.band.sensors.BandUVEventListener;

import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Microsoft Band UV sensor.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class UVSensor extends MSBandSensor {
    // This is necessary for connecting to MS Band smartwatch.
    private final BandClient client;

    // Listens for sensor data and updates the general sensor with the data.
    private final BandUVEventListener listener = new BandUVEventListener() {
        @Override
        public void onBandUVChanged(BandUVEvent bandUVEvent) {
            update(bandUVEvent.getUVIndexLevel().toString());
        }
    };

    /**
     * Constructs an instance of an UVSensor.
     *
     * @param client A connected MS Band Client.
     */
    public UVSensor(BandClient client) {
        super(
                "ms_ultraviolet",
                new String[] {"level"},
                new Types[] {Types.String},
                null
        );
        this.client = client;
    }

    @Override
    public void register() {
        try { client.getSensorManager().registerUVEventListener(this.listener);
        } catch (BandException e) {  Log.e("UVSensor", e.getMessage()); }
    }

    @Override
    public void unregister() {
        try { client.getSensorManager().unregisterUVEventListener(this.listener);
        } catch (BandIOException e) {  Log.e("UVSensor", e.getMessage()); }
    }
}