package reuiot2015.smartwatch;


import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/** Collection controls to show when collection is started.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu) */
public class CollectionStartedFragment extends Fragment {
    private Button collectionStopButton, drinkingButton;
    private OnCollectionStopButtonClick collectionStopListener;
    private OnDrinkingButtonLinked drinkingButtonListener;

    public CollectionStartedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.collectionStopListener = (OnCollectionStopButtonClick) activity;
            this.drinkingButtonListener = (OnDrinkingButtonLinked) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCollectionStopButtonClick and OnDrinkingButtonLinked.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_collection_started, container, false);

        // Get the inflated Button (collection stop button).
        this.collectionStopButton = (Button) v.findViewById(R.id.button_collection_stop);
        this.drinkingButton = (Button) v.findViewById(R.id.button_is_drinking);

        // Set an on click listener and pass events along to the OnCollectionStopButtonClick listener.
        this.collectionStopButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (collectionStopListener != null) collectionStopListener.onCollectionStopButtonClick(collectionStopButton);
            }
        });

        // Send the link to the drinking button listener.
        this.drinkingButtonListener.onDrinkingButtonLinked(this.drinkingButton);

        return v;
    }

    /** An interface for receiving click events from the collection stop button. */
    public interface OnCollectionStopButtonClick {
        /** Called when the collection stop button is clicked in the CollectionStartedFragment.
         *
         * @param button A reference to the collection stop button.
         */
        void onCollectionStopButtonClick(Button button);
    }

    /** An interface for receiving the linked drinking button reference. */
    public interface OnDrinkingButtonLinked {
        /** Called when the drinking button is linked up.
         *
         * @param button The Button object reference.
         */
        void onDrinkingButtonLinked(Button button);
    }

}
