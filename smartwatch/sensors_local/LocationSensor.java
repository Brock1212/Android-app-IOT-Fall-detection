package reuiot2015.smartwatch.sensors_local;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import reuiot2015.smartwatch.sensors.Sensor;
import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to the Android Location Services.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class LocationSensor extends Sensor {
    private LocationManager manager;
    private Context context;

    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            update(
                    location.getAccuracy(),
                    location.getAltitude(),
                    location.getBearing(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getProvider(),
                    location.getSpeed(),
                    location.getTime()
            );
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onProviderDisabled(String provider) { }
    };

    /** Constructs an instance of an LocationSensor. */
    public LocationSensor(Context context) {
        super(
                "location",
                new String[] {"accuracy", "altitude", "bearing", "latitude", "longitude", "provider", "speed", "time"},
                new Types[] {Types.Float, Types.Double, Types.Float, Types.Double, Types.Double, Types.String, Types.Float, Types.Long}
        );
        this.manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.context = context;
    }

    /** Subscribes the sensor for collection. */
    public void subscribe() {
        this.manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this.listener, this.context.getMainLooper());
        this.manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this.listener, this.context.getMainLooper());
    }

    /** Unsubscribes the sensor from collection. */
    public void unsubscribe() {
        this.manager.removeUpdates(this.listener);
    }
}
