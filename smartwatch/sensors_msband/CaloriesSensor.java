package reuiot2015.smartwatch.sensors_msband;

import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;

import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Microsoft Band calories sensor.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class CaloriesSensor extends MSBandSensor {
    // This is necessary for connecting to MS Band smartwatch.
    private final BandClient client;

    // Listens for sensor data and updates the general sensor with the data.
    private final BandCaloriesEventListener listener = new BandCaloriesEventListener() {
        @Override
        public void onBandCaloriesChanged(BandCaloriesEvent e) {
            update(e.getCalories());
        }
    };

    /**
     * Constructs an instance of a CaloriesSensor.
     *
     * @param client A connected MS Band Client.
     */
    public CaloriesSensor(BandClient client) {
        super(
                "ms_calories",
                new String[] {"burned"},
                new Types[] {Types.Long},
                null
        );
        this.client = client;
    }

    @Override
    public void register() {
        try { client.getSensorManager().registerCaloriesEventListener(this.listener);
        } catch (BandException  e) {  Log.e("CaloriesSensor", e.getMessage()); }
    }

    @Override
    public void unregister() {
        try { client.getSensorManager().unregisterCaloriesEventListener(this.listener);
        } catch (BandIOException e) {  Log.e("CaloriesSensor", e.getMessage()); }
    }
}
