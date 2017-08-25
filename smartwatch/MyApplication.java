package reuiot2015.smartwatch;

import android.app.Application;
import android.content.Context;

/**
 * Gets the context from the Main activity and returns it.
 *
 *
 * Created by Brock on 7/18/2016.
 */
public class MyApplication extends Application {

        private static Context context;

        public void onCreate() {
            super.onCreate();
            MyApplication.context = getApplicationContext();
        }

        public static Context getAppContext() {
            return MyApplication.context;
        }
    }

