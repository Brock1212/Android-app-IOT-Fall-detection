package reuiot2015.smartwatch.sensors_persistence;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.IllegalFormatException;

/** Stores samples collected from a Collector into a local file.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 */
public class JSONSampleWriter implements SampleAccumulator.SampleAccumulationListener {
    private final String[] header;

    private FileWriter writer;
    private JSONObject jsonObject;
    private JSONArray jsonSampleArray;

    public JSONSampleWriter(String[] header, String uuid, String filename) {
        this.header = header;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // Get the public directory to store to.
            File publicDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), SmartWatchValues.ALBUM_NAME + "/data");
            Log.d("JSONSampleWriter", "Save path is: " + publicDirectory.getAbsolutePath());

            if (publicDirectory.mkdirs()) Log.d("JSONSampleWriter", "Created file structure.");
            else Log.d("JSONSampleWriter", "Using existing file structure, or failed to create.");

            try {
                // Open a writer to write to the sample file.
                writer = new FileWriter(new File(publicDirectory, filename));
            } catch (IOException e) {  Log.e("JSONSampleWriter", e.getMessage()); }
        } else {
            Log.d("JSONSampleWriter", "External media is not mounted.");
        }

        jsonObject = new JSONObject();
        try { jsonObject.put("UUID", uuid);
        } catch (JSONException e) {
            Log.e("JSONSampleWriter", "Could not write UUID to JSON Object. " + e.getMessage());
        }

        jsonSampleArray = new JSONArray();
    }

    @Override
    public boolean receiveAccumulatedSamples(String[][] samples) {
        try {
            synchronized (samples) {
                for (String[] sample : samples) {
                    JSONObject jsonSample = new JSONObject();
                    for (int j = 0; j < sample.length; ++j) {
                        try { jsonSample.put(header[j], sample[j]);
                        } catch (JSONException e) {
                            Log.e("JSONSampleWriter", e.getMessage());
                        }
                    }
                    jsonSampleArray.put(jsonSample);
                }

            }
            return true;
        } catch (IllegalFormatException | NullPointerException e) {
            Log.e("JSONSampleWriter", e.getMessage());
        }
        return false;
    }

    /** Closes the output stream to the file. */
    public void release() {
        try {
            jsonObject.put("samples", jsonSampleArray);
        } catch (JSONException e) {
            Log.e("JSONSampleWriter", "Could not build JSON Object. " + e.getMessage());
        }
        if (writer != null) {
            try {
                writer.write(jsonObject.toString());
                writer.flush();
                writer.close();
            } catch (IOException e) { /** Nothing to do here. */ }
        } else {
            Log.e("JSONSampleWriter", "Whoops, writer is null for some reason.");
        }
    }
}
