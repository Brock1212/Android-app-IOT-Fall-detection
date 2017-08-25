package reuiot2015.smartwatch;


import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/** Collection controls to show when collection is stopped.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu) */
public class CollectionStoppedFragment extends Fragment {
    private Button button;
    private OnCollectionStartButtonClick listener;

    public CollectionStoppedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.listener = (OnCollectionStartButtonClick) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCollectionStartButtonClick.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout.
        View v = inflater.inflate(R.layout.fragment_collection_stopped, container, false);

        // Get the inflated Button (collection start button).
        this.button = (Button) v.findViewById(R.id.button_collection_start);

        // Set an on click listener and pass events along to the OnCollectionStartButtonClick listener.
        this.button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onCollectionStartButtonClick(button);
            }
        });
        return v;
    }

    /** An interface for receiving click events from the collection start button. */
    public interface OnCollectionStartButtonClick {
        /** Called when the collection start button is clicked in the CollectionStoppedFragment.
         *
         * @param button A reference to the collection start button.
         */
        void onCollectionStartButtonClick(Button button);
    }
}
