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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


/** Used to select an existing profile.
 *
 * @author Mario A. Gutierrez
 */
public class ProfileSelectFragment extends DialogFragment {

    private ArrayAdapter<String> adapter = null;
    private String[] profiles = null;

    private ListView list = null;

    private ProfileSelectionListener listener;

    public ProfileSelectFragment() {
        // Required empty public constructor
    }

    /** Sets the profiles to use by the adapter.
     *
     * @param profiles The list of profile names.
     */
    public void setProfiles(String[] profiles) {
        this.profiles = profiles;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, profiles);
        try {
            listener = (ProfileSelectionListener) activity;
        } catch (ClassCastException e) {
            /** Nothing to do here really. */
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile_select, container, false);
        list = (ListView) v.findViewById(R.id.list_view_profiles);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listener.profileSelected(profiles[position]);
                Log.d("ProfileSelectFragment", profiles[position] + " selected.");
                getDialog().dismiss();
            }
        });
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        list.setAdapter(adapter);
    }

    public interface ProfileSelectionListener {
        void profileSelected(String name);
    }
}
