package reuiot2015.smartwatch;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;


/** This fragment is used to calculate EBAC-related values.
 *
 * @author Mario A. Gutierrez
 */
public class EBACCalculationFragment extends DialogFragment {
    public final static float BODY_WATER_CONSTANT = 0.806f;
    public final static float BODY_WATER_MALE_CONSTANT = 0.58f;
    public final static float BODY_WATER_FEMALE_CONSTANT = 0.49f;
    public final static float METABOLISM_RATE_MALE = 0.018f;
    public final static float METABOLISM_RATE_FEMALE = 0.017f;
    public final static float LBS_TO_KG = 0.453592f; // This many kg for every pound.

    Button calculate;
    EditText ebac, sd, wt, dp;
    RadioButton male, female;

    public EBACCalculationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_ebaccalculation, container, false);

        calculate = (Button) v.findViewById(R.id.button_calculate);
        ebac = (EditText) v.findViewById(R.id.edit_text_ebac);
        sd = (EditText) v.findViewById(R.id.edit_text_sd);
        wt = (EditText) v.findViewById(R.id.edit_text_weight);
        dp = (EditText) v.findViewById(R.id.edit_text_period);
        male = (RadioButton) v.findViewById(R.id.radio_button_male);
        female = (RadioButton) v.findViewById(R.id.radio_button_female);

        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    boolean[] filled = new boolean[4]; // Which fields are filled?
                    filled[0] = (ebac.getText().length() != 0);
                    filled[1] = (sd.getText().length() != 0);
                    filled[2] = (wt.getText().length() != 0);
                    filled[3] = (dp.getText().length() != 0);

                    // Count fields.
                    int checksum = 0; for (boolean b : filled) if (b) ++checksum;

                    if (checksum != 3) { Toast.makeText(getActivity(), "Must fill all but one field.", Toast.LENGTH_SHORT).show(); return; }

                    float febac = (filled[0]) ? Float.parseFloat(ebac.getText().toString()) : 0.0f;
                    float fsd = (filled[1]) ? Float.parseFloat(sd.getText().toString()) : 0.0f;
                    float fwt = (filled[2]) ? Float.parseFloat(wt.getText().toString()) : 0.0f;
                    float fdp = (filled[3]) ? Float.parseFloat(dp.getText().toString()) : 0.0f;

                    float bw = (male.isChecked()) ? BODY_WATER_MALE_CONSTANT : BODY_WATER_FEMALE_CONSTANT;
                    float mr = (male.isChecked()) ? METABOLISM_RATE_MALE : METABOLISM_RATE_FEMALE;

                    if (!filled[0]) { // Solve for EBAC.
                        float result = ((BODY_WATER_CONSTANT * (fsd * 1.2f)) / (bw * (fwt * LBS_TO_KG))) - (mr * fdp);
                        ebac.setText(String.format("%.5f", result));
                    } else if (!filled[1]) { // Solve for SD.
                        float result = ((febac + (mr * fdp)) * (bw * (fwt * LBS_TO_KG))) / (BODY_WATER_CONSTANT * 1.2f);
                        sd.setText(String.format("%.2f", result));
                    } else if (!filled[2]) { // Solve for WT.
                        float result = (BODY_WATER_CONSTANT * (fsd * 1.2f)) / ((febac + mr * fdp) * bw * LBS_TO_KG);
                        wt.setText(String.format("%.1f", result));
                    } else if (!filled[3]) { // Solve for DP.
                        float result = (((BODY_WATER_CONSTANT * (fsd * 1.2f)) / (bw * (fwt * LBS_TO_KG))) - febac) / mr;
                        dp.setText(String.format("%.2f", result));
                    }
                } catch (NumberFormatException e) {
                    /** Nothing to do really. */
                }
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
}
