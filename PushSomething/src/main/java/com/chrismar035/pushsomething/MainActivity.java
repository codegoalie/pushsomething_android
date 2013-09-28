package com.chrismar035.pushsomething;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

public class MainActivity extends Activity {
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    String SENDER_ID = "762651962812";

    static final String TAG = "PushSomething";

    TextView mDisplay;
    GoogleCloudMessaging gcm;
    Context context;

    String regID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDisplay = (TextView) findViewById(R.id.main_view);
        mDisplay.append("\n" + SENDER_ID + "\n");

        context = getApplicationContext();

        if(checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regID = getRegistrationId(context);

            if(regID.isEmpty()) {
                Log.i(TAG, "Registration ID blank. Registering in background.");
                new backgroundRegistrationTask().execute();
            }

        } else {
            mDisplay.setText("Google Play app not installed");
        }
    }

    private class backgroundRegistrationTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String msg;
            try {
                if(gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                regID = gcm.register(SENDER_ID);
                msg = "Device registered, registration ID=" + regID;

                sendRegistrationToBackend();
                storeRegistrationId(context, regID);
            } catch (IOException ex) {
                msg = "BackgroundRegistration Error: " + ex.getMessage();
            }
            Log.i(TAG, msg);
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
            mDisplay.append(msg + "\n");
        }

        private void sendRegistrationToBackend() {
            // Send to Rails here... will manually enter for now.
        }

        private void storeRegistrationId(Context context, String regid) {
            final SharedPreferences prefs = getGCMPreferences(context);
            int appVersion = getAppVersion(context);
            Log.i(TAG, "Saving regId on app version" + appVersion);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROPERTY_REG_ID, regid);
            editor.putInt(PROPERTY_APP_VERSION, appVersion);
            editor.commit();
        }
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if(registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if(registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen...
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS) {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode,
                                                      this,
                                                      PLAY_SERVICES_RESOLUTION_REQUEST).show();

            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

}
