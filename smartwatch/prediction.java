package reuiot2015.smartwatch;

/**
 * Created by Brock on 7/6/2016.
 *
 * The prediction class is used to predict rather or not someone has fallen. It uses weka and libsvm
 * to predict. Groupings of samples are predicted at one time to make sure every instance is classified.
 *
 */

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.InputStream;

import reuiot2015.smartwatch.sensors_persistence.SmartWatchValues;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

public class prediction {
    private boolean fall = false;  //used to keep track if any of the instances are a fall.
    private static int start = 0;  //used to keep track of where in the csv file to start predicting from
    private int inarow =  0;
    //Context passed from main activity so this class can acess the model in the assets folder.
    //count is passed to the class so the prediction method knows how many new rows are in the csvfile.

    public void predict(Context c,int count){

        File drinkingdata = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), SmartWatchValues.ALBUM_NAME + "/data/default.csv");// + "/default.csv");
        Log.d("fallprediction", "Save path is: " + drinkingdata.getAbsolutePath());

        try{
            DataSource source = new DataSource(drinkingdata.getAbsolutePath());
            Instances predictiondata = source.getDataSet();                      //getting csv to predict from
            Log.d("fallprediction", predictiondata.toSummaryString());
            Log.d("fallprediction", source.getDataSet().toString());
            Log.d("fallprediction", start + " to " + predictiondata.numInstances());
            Log.d("fallprediction", "this many "+count);
            count = predictiondata.numInstances();
            //for every new group of samples we receive we will predict each instance
            for (int i = start; i < predictiondata.numInstances(); ++i){
            try {
                //set which column will be used for prediction
                if (predictiondata.classIndex() == -1)
                    predictiondata.setClassIndex(predictiondata.numAttributes() - 1);
                Instance newinst = predictiondata.instance(i);
                Log.d("fall predict newinst",newinst.toString());
                try {
                    if (c == null)
                        Log.d("fallprediction", "not getting context");
                    //get model from assets that will be used for prediction
                    AssetManager am = c.getAssets();
                    InputStream is = am.open("fallsvmmodel.model");
                    if (is == null)
                        Log.d("fallprediction", "not opening model");
                    //read in that model.
                    Classifier newsvm = (Classifier) weka.core.SerializationHelper.read(is);
                    if (is == null)
                        Log.d("fallprediction", "not reading in model");
                    else
                        Log.d("fallprediction",newsvm.toString());
                    //predict instance
                    double prediction = newsvm.classifyInstance(newinst);
                    if (is == null)
                        Log.d("fallprediction", "having trouble predicting");
                    //get string representation of prediction
                    String predictionoutput = predictiondata.classAttribute().value((int) prediction);
                    if (predictionoutput == "")
                        Log.d("fallprediction", "trouble converting to string");
                    Log.d("*****This was predicted", predictionoutput);
                    //test if prediction was fall, if so set the final prediction to fall.
                    if (predictionoutput.equals("fall") || predictionoutput.equals(" fall") || predictionoutput.equals("fall "))
                        ++inarow;
                    else
                        inarow = 0;

                    if (inarow >= 2 && inarow <= 5) fall = true;
                    if (inarow > 5) fall = false;

                } catch (Exception e) {
                    Log.d("Prediction", "could not find model");
                }
            }catch (Exception e) {
                Log.d("Prediction", "found source, trouble working with it.");
            }}
        }catch (Exception e){
            Log.d("Prediction","could not find data source");
        }
        //see if 2-5 predictions were made in a row if so then prediction is a fall
        if (fall == true)
            Log.d("***********************","Fall");
        else
            Log.d("***********************","NoFall");

        //set where in csv to start from next time
        start = count;
    }

}
