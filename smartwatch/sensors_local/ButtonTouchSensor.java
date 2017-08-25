package reuiot2015.smartwatch.sensors_local;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import reuiot2015.smartwatch.sensors.Sensor;
import reuiot2015.smartwatch.sensors.Types;

/** A "sensors" interface to Android Button (onClick).
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 * */
public class ButtonTouchSensor extends Sensor {
    private Button button;
    private String label;
    private Button.OnTouchListener onTouchListener = new Button.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                update(label, "ACTION_DOWN", System.currentTimeMillis());
            else if (event.getAction() == MotionEvent.ACTION_UP)
                update(label, "ACTION_UP", System.currentTimeMillis());
            return false; // Don't consume the event, only monitoring.
        }
    };

    /** Constructs an instance of an LocationSensor. */
    public ButtonTouchSensor(String label) {
        super(
                "button_touch",
                new String[]{"label", "event", "timestamp"},
                new Types[]{Types.String, Types.String, Types.Long}
        );
        this.label = label;
    }

    /** Unregisters the old Button if any and assigns a new Button.
     *
     * @param button The Button to listen to.
     */
    public void setButton(Button button) {
        unsubscribe(); this.button = button;
    }

    /** Subscribes the sensor for collection */
    public void subscribe() {
        if (this.button != null) {
            this.button.setOnTouchListener(this.onTouchListener);
        }
    }

    /** Unsubscribes the sensor from collection.*/
    public void unsubscribe() {
        if (this.button != null) {
            this.button.setOnTouchListener(null);
        }
    }
}
