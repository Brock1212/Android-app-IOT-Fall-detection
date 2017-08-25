package reuiot2015.smartwatch;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import reuiot2015.smartwatch.sensors.Collector;
import reuiot2015.smartwatch.sensors.Sensor;
import reuiot2015.smartwatch.sensors.Types;


public class ProfileSettingsActivity extends Activity implements BMICalculationFragment.BMICalculationListener{
    public static final int REQUEST_PROFILE = 0x1;

    // Define keys for profile data extras.
    public static final String EXTRA_AGE = "reuiot.smartwatch.ProfileSettingsActivity.EXTRA_AGE";
    public static final String EXTRA_BMI = "reuiot.smartwatch.ProfileSettingsActivity.EXTRA_BMI";
    public static final String EXTRA_BLOOD_TYPE = "reuiot.smartwatch.ProfileSettingsActivity.EXTRA_BLOOD_TYPE";
    public static final String EXTRA_GENDER = "reuiot.smartwatch.ProfileSettingsActivity.EXTRA_GENDER";
    public static final String EXTRA_UUID = "reuiot.smartwatch.ProfileSettingsActivity.EXTRA_UUID";

    private static final String[] BLOOD_TYPES = {"O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-", "UNK"};
    private final static UUID DEFAULT_UUID = UUID.fromString("1c0d4e24-0062-40fc-9dca-4cb8ac11257d");

    private EditText age, bmi, uuid;
    private Spinner bloodType;
    private RadioButton male, female;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        // Link up the views.
        age = (EditText) findViewById(R.id.edit_text_age);
        bmi = (EditText) findViewById(R.id.edit_text_bmi);
        uuid = (EditText) findViewById(R.id.edit_text_uuid);
        bloodType = (Spinner) findViewById(R.id.spinner_blood_type);
        male = (RadioButton) findViewById(R.id.radio_button_male);
        female = (RadioButton) findViewById(R.id.radio_button_female);

        ArrayAdapter<CharSequence> bloodTypeAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, BLOOD_TYPES);
        bloodTypeAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        bloodType.setAdapter(bloodTypeAdapter);

        // Set onLongClick listener to launch secret BMI calculator.
        bmi.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                DialogFragment dialog = new BMICalculationFragment();
                dialog.show(getFragmentManager(), "BMICalculationFragment");
                return true;
            }
        });

        // Fill out the profile information from previous settings.
        Bundle extras = getIntent().getExtras(); if (extras != null) {
            int age = (int)extras.get(EXTRA_AGE);
            if (age != 0) this.age.setText(age + "");
            float bmi = (float)extras.get(EXTRA_BMI);
            if (bmi != 0.0) this.bmi.setText(bmi + "");
            String bloodType = (String)extras.get(EXTRA_BLOOD_TYPE);
            for (int i = 0; i < BLOOD_TYPES.length; ++i) if (bloodType.equalsIgnoreCase(BLOOD_TYPES[i]))
                { this.bloodType.setSelection(i); break; }
            String gender = (String)extras.get(EXTRA_GENDER);
            if (gender.equalsIgnoreCase("MALE")) male.setChecked(true);
            else if (gender.equalsIgnoreCase("FEMALE")) female.setChecked(true);
            String uuid = (String) extras.get(EXTRA_UUID);
            if (uuid != null) this.uuid.setText(uuid);
        } else {
            uuid.setText(DEFAULT_UUID.toString());
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void onSaveProfile(View v) {
        Intent data = new Intent(); // Store data in an Intent.
        data.putExtra(EXTRA_AGE, (age.length() != 0) ? Integer.parseInt(age.getText().toString()) : 0);
        data.putExtra(EXTRA_BLOOD_TYPE, (CharSequence) bloodType.getSelectedItem());
        data.putExtra(EXTRA_BMI, (bmi.length() != 0) ? Float.parseFloat(bmi.getText().toString()) : 0.0f);
        data.putExtra(EXTRA_GENDER, (male.isChecked()) ? "MALE" : (female.isChecked()) ? "FEMALE" : "UNSPECIFIED");
        data.putExtra(EXTRA_UUID, uuid.getText().toString());
        setResult(RESULT_OK, data);

        // Hide soft keyboard.
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(v.getWindowToken(), 0);

        finish(); // Kill the activity.
    }

    /** Generates a new UUID for the profile.
     *
     * @param v The related View.
     */
    public void generateUUID(View v) {
        uuid.setText(UUID.randomUUID().toString());
    }

    /** Sets the profile UUID to default.
     *
     * @param v The related View.
     */
    public void defaultUUID(View v) {
        uuid.setText(DEFAULT_UUID.toString());
    }

    @Override
    public void onBMICalculated(final float result) {
        bmi.post(new Runnable() {
            @Override
            public void run() {
                bmi.setText(String.format("%.1f", result));
            }
        });
    }
}
