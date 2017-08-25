package reuiot2015.smartwatch.sensors_msband;

import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.sensors.BandContactEvent;
import com.microsoft.band.sensors.BandContactEventListener;

import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Microsoft Band contact sensor.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class ContactSensor extends MSBandSensor {
    // This is necessary for connecting to MS Band smartwatch.
    private final BandClient client;

    // Listens for sensor data and updates the general sensor with the data.
    private final BandContactEventListener listener = new BandContactEventListener() {
        @Override
        public void onBandContactChanged(BandContactEvent e) {
            update(e.getContactState().toString());
        }
    };

    /**
     * Constructs an instance of a ContactSensor.
     *
     * @param client A connected MS Band Client.
     */
    public ContactSensor(BandClient client) {
        super(
                "ms_contact",
                new String[] {"state"},
                new Types[] {Types.String},
                null
        );
        this.client = client;
    }

    @Override
    public void register() {
        try { client.getSensorManager().registerContactEventListener(this.listener);
        } catch (BandException  e) {  Log.e("ContactSensor", e.getMessage()); }
    }

    @Override
    public void unregister() {
        try { client.getSensorManager().unregisterContactEventListener(this.listener);
        } catch (BandIOException e) {  Log.e("ContactSensor", e.getMessage()); }
    }
}
