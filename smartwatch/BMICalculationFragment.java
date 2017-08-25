package reuiot2015.smartwatch;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;


/** This fragment is used to obtain BMI-related values and calculate BMI.
 *
 * @author Mario A. Gutierrez
 */
public class BMICalculationFragment extends DialogFragment {

    BMICalculationListener listener;

    Button calculate;
    EditText heightA, heightB, weight;


    public BMICalculationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (BMICalculationListener) activity;
        } catch (ClassCastException e) {
            Log.e("BMICalculationFragment", "Launching activity must be ready to accept bmi result by implementing listener.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bmicalculation, container, false);

        calculate = (Button) v.findViewById(R.id.button_calculate);
        heightA = (EditText) v.findViewById(R.id.edit_text_height_a);
        heightB = (EditText) v.findViewById(R.id.edit_text_height_b);
        weight = (EditText) v.findViewById(R.id.edit_text_weight);

        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener == null) return;
                try {
                    float ha = Integer.parseInt(heightA.getText().toString());
                    float hb = Integer.parseInt(heightB.getText().toString());
                    float wt = Float.parseFloat(weight.getText().toString());

                    float hs = 12 * ha + hb; // Turn feet and inches into inches.
                    listener.onBMICalculated(wt * 703.0f / (hs * hs));
                } catch (NumberFormatException e) {
                    /** Nothing to do really. */
                }
                getDialog().dismiss();
            }
        });

        return v;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    public interface BMICalculationListener {
        void onBMICalculated(float result);
    }

}
