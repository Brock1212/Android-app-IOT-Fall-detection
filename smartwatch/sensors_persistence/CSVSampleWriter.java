package reuiot2015.smartwatch.sensors_persistence;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.IllegalFormatException;

import reuiot2015.smartwatch.prediction;
import reuiot2015.smartwatch.MyApplication;

import java.lang.*;

/** Stores samples collected from a Collector into a local file.
 *
 * @author Mario A. Gutierrez (mag262@txstate.edu)
 */
public class CSVSampleWriter implements SampleAccumulator.SampleAccumulationListener {
    private PrintWriter writer;
    static private boolean header = true;
    public CSVSampleWriter(String[] header, String filename) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // Get the public directory to store to.
            File publicDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), SmartWatchValues.ALBUM_NAME + "/data");
            Log.d("CSVSampleWriter", "Save path is: " + publicDirectory.getAbsolutePath());

            if (publicDirectory.mkdirs()) Log.d("CSVSampleWriter", "Created file structure.");
            else Log.d("CSVSampleWriter", "Using existing file structure, or failed to create.");

            try {
                // Open a writer to write to the sample file.
                writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(publicDirectory, filename))));

                // Artificially call this method to write the sample header to file.
                receiveAccumulatedSamples(new String[][] { header });
            } catch (IOException e) {  Log.e("CSVSampleWriter", e.getMessage()); }
        } else {
            Log.d("CSVSampleWriter", "External media is not mounted.");
        }
    }
/*****************************relevant****************************/
    @Override
    public boolean receiveAccumulatedSamples(String[][] samples) {
            try {
                int c = 0;
                StringBuilder sb = new StringBuilder();
                synchronized (samples) {
                    if (header == true){
                        sb.append("resultant,").append("cvfast,").append("smax,").append("smin,").append("outcome").append("\n");
                        writer.write(sb.toString()); // Write the formatted samples title to file.
                        sb.setLength(0);}
                    header = false;  // keep from writing the hrader multiple times
                    boolean l = true; // in the initial csv file both possible outcomes must be present in the correct order as predicted. in this case notfall first and fall second. see below
                    int count = 0;     // can't start predicting until after 3 samples have been collected
                    for (String[] sample : samples) {
                        if (count > 3) {
                            double[] ax = new double[3];     //acceleration x axis
                            double[] ay = new double[3];     //acceleration y axis
                            double[] az = new double[3];     //acceleration z axis
                            ax[0] = Double.parseDouble(samples[count-2][0]);     //first ax = newest sample; column 0(which holds x acceleration)
                            ax[1] = Double.parseDouble(samples[count-1][0]);   //second ax = previous sample; column 0
                            ax[2] = Double.parseDouble(samples[count][0]);    //...
                            ay[0] = Double.parseDouble(samples[count-2][1]);      //first ay = newest sample; column 1(which holds y acceleration)
                            ay[1] = Double.parseDouble(samples[count-1][1]);    //....
                            ay[2] = Double.parseDouble(samples[count][1]);    //...
                            az[0] = Double.parseDouble(samples[count-2][2]);      //first az = newest sample; column 2(which holds z acceleration)
                            az[1] = Double.parseDouble(samples[count-1][2]);    //...
                            az[2] = Double.parseDouble(samples[count][2]);    //...
                            double cvfast = calculatecvfast(ax,ay,az);
                            double currentresultant = resultant(ax[0],ay[0],az[0]);  //need resultant for current and last two
                            double nextresultant = resultant(ax[1],ay[1],az[1]);
                            double otherresultant = resultant(ax[2],ay[2],az[2]);
                            double smin = smin(currentresultant,nextresultant,otherresultant);
                            double smax = smax(currentresultant,nextresultant,otherresultant);
                            sb.append(String.valueOf(currentresultant)).append(",").append(String.valueOf(cvfast)).append(",").append(String.valueOf(smax)).append(",").append(String.valueOf(smin)).append(",");
                            //sb.append(currentresultant).append(",").append(cvfast).append(",").append(smax).append(",").append(smin).append(",");
                            /*for (int j = 0; j < sample.length - 1; ++j)
                                sb.append(sample[j]).append(", ");*/
                            int last = sample.length - 1;
                            if (l == true)
                                sb.append("notfall"/*sample[last]*/).append("\n");
                            else
                                sb.append("fall"/*sample[last]*/).append("\n");
                            writer.write(sb.toString()); // Write the formatted samples to file.
                            Log.d("******", sb.toString());
                            sb.setLength(0); // Clear contents of builder.
                            l = !l;                                  //alternate outcome to make sure prediction is working correctly.
                        }
                        ++count;
                        c = count;
                    }
                }

                writer.flush();
                MyApplication m = new MyApplication();
                prediction d = new prediction();                    //create prediction instance
                d.predict(m.getAppContext(), c);                    //call prediction function.
                return true;
            } catch (IllegalFormatException | NullPointerException e) {
                Log.e("CSVSampleWriter", e.getMessage());
            }
        return false;
    }

    /** Closes the output stream to the file. */
    public void release() {
        if (writer != null) this.writer.close();
    }

    //takes the arrays of accelerations and returns the sqrt of the max-min of all 3
    public double calculatecvfast(double[] ax, double[] ay, double[] az){

        double axmax = Math.max(ax[0],ax[1]);
        axmax = Math.max(axmax,ax[2]);              //finds max of x-axis acceleration

        double aymax = Math.max(ay[0],ay[1]);
        aymax = Math.max(aymax,ay[2]);              //finds max of y-axis acceleration

        double azmax = Math.max(az[0],az[1]);
        azmax = Math.max(azmax,az[2]);              //finds max of z-axis acceleration

        double axmin = Math.min(ax[0],ax[1]);
        axmin = Math.min(axmin,ax[2]);              //finds min of x-axis acceleration

        double aymin = Math.min(ay[0],ay[1]);
        aymin = Math.min(aymin,ay[2]);              //finds min of y-axis acceleration

        double azmin = Math.min(az[0],az[1]);
        azmin = Math.min(azmin,az[2]);              //finds min of z-axis acceleration

        return Math.sqrt(((axmax-axmin)*(axmax-axmin))+((aymax-aymin)*(aymax-aymin))+((azmax-azmin)*(azmax-azmin)));
    }


    public double resultant(double x, double y, double z){
        return Math.sqrt((x*x)+(y*y)+(z*z));
    }

    //finds min of last 3 resultants
    public double smin(double one, double two, double three){
        double smin = Math.min(one,two);
        smin = Math.min(smin,three);
        return smin;
    }

    //finds max resultant of last 3 resultants
    public double smax(double one, double two, double three){
        double smax = Math.max(one,two);
        smax = Math.max(smax,three);
        return smax;
    }
    /*****************************relevant****************************/
}
