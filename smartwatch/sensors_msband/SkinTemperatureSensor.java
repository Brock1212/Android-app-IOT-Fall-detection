package reuiot2015.smartwatch.sensors_msband;

import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;

import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Microsoft Band skin temperature sensor.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class SkinTemperatureSensor extends MSBandSensor {
    // This is necessary for connecting to MS Band smartwatch.
    private final BandClient client;

    // Listens for sensor data and updates the general sensor with the data.
    private final BandSkinTemperatureEventListener listener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent bandSkinTemperatureEvent) {
            update(bandSkinTemperatureEvent.getTemperature());
        }
    };

    /**
     * Constructs an instance of an SkinTemperatureSensor.
     *
     * @param client A connected MS Band Client.
     */
    public SkinTemperatureSensor(BandClient client) {
        super(
                "ms_skin_temperature",
                new String[] {"celsius"},
                new Types[] {Types.Float},
                null
        );
        this.client = client;
    }

    @Override
    public void register() {
        try { client.getSensorManager().registerSkinTemperatureEventListener(this.listener);
        } catch (BandException  e) {  Log.e("SkinTemperature", e.getMessage()); }
    }

    @Override
    public void unregister() {
        try { client.getSensorManager().unregisterSkinTemperatureEventListener(this.listener);
        } catch (BandIOException e) {  Log.e("SkinTemperature", e.getMessage()); }
    }
}
