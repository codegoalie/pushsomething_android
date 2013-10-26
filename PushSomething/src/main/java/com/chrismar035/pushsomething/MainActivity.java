package com.chrismar035.pushsomething;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.PlusClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static String PROPERTY_ACCOUNT_NAME = "account_name";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final static String API_ROOT = "http://10.0.1.185:3000/api/v1/";
    String SENDER_ID = "762651962812";
    String SERVER_WEB_ID = "762651962812.apps.googleusercontent.com";


    static final String TAG = "PushSomething";

    TextView mDisplay;
    GoogleCloudMessaging gcm;
    Context context;
    PlusClient mPlusClient;
    String regID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Creating Main");
        SharedPreferences prefs = getSharedPreferences(PROPERTY_ACCOUNT_NAME, 0);
        String account_name = prefs.getString(PROPERTY_ACCOUNT_NAME, "");
        Log.i(TAG, "Account name is: " + account_name);
        if (account_name.isEmpty()) {
            Log.i(TAG, "User not signed in. Starting SignIn");
            backToSignIn();
            return;
        }
        mPlusClient = new PlusClient.Builder(this, this, this)
                .setVisibleActivities("http://schemas.google.com/AddActivity")
                .build();

        setContentView(R.layout.activity_main);
        mDisplay = (TextView) findViewById(R.id.main_view);

        context = getApplicationContext();

        registerForGCM();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlusClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPlusClient.disconnect();
    }

    public void signOutClick(MenuItem _) {
        if (mPlusClient.isConnected()) {
            mPlusClient.clearDefaultAccount();
            mPlusClient.disconnect();
            SharedPreferences prefs = getSharedPreferences(PROPERTY_ACCOUNT_NAME, 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PROPERTY_ACCOUNT_NAME, "");
            editor.commit();
            mPlusClient.connect();
            String msg = "Signed out";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            Log.i(TAG, msg);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mDisplay.append("\n\nSigned in as: " + mPlusClient.getAccountName());
    }

    @Override
    public void onDisconnected() {
    }

    private void backToSignIn() {
        Intent backToSignInIntent = new Intent(this, SignInActivity.class);
        startActivity(backToSignInIntent);
        this.finish();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        backToSignIn();
    }

    public void clearGCMClick(MenuItem _) {
        Log.i(TAG, "Clearing GCM ID");
        final SharedPreferences prefs = getGCMPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, "");
        editor.putInt(PROPERTY_APP_VERSION, 0);
        editor.commit();
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
        }

        private void sendRegistrationToBackend() throws IOException {
            // Send to Rails here... will manually enter for now.
            JSONObject payload = new JSONObject();

            final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

            try {
                payload.put("jwt", getJWT());
                payload.put("gcm_id", regID);
                payload.put("uid", tm.getDeviceId());
                Log.i(TAG, "Registering with the server\n" + payload.toString(2));

            } catch (JSONException e) {
                Log.i(TAG, "JSON creation failed");
            }

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 10000);
            HttpConnectionParams.getSoTimeout(params);
            HttpClient client = new DefaultHttpClient(params);

            HttpPost request = new HttpPost(API_ROOT + "receivers");
            StringEntity entity = new StringEntity(payload.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            request.setEntity(entity);
            HttpResponse response = client.execute(request);


            /*URL url = new URL("http://192.168.1.74:3000/receivers");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);

                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                out.write(payload.toString().getBytes("UTF8"));
            } finally {
                urlConnection.disconnect();
            }*/
            Log.i(TAG, "Posting Complete");
        }

        private String getJWT() {
            String code = null;
            try {
                code = GoogleAuthUtil.getToken(context,
                        mPlusClient.getAccountName(),
                        "audience:server:client_id:" + SERVER_WEB_ID);
            } catch (IOException transientEx) {
                // implement back off for retries
                Log.e(TAG, "Network or Server error getting server token.");
            } catch (UserRecoverableAuthException e) {
                code = null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Log.i(TAG, "Server token gotten: " + code);
            return code;
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

    private void registerForGCM() {
        if(checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regID = getRegistrationId(context);

            if(regID.isEmpty()) {
                Log.i(TAG, "Registration ID blank. Registering in background.");
                new backgroundRegistrationTask().execute();
            }
            Log.i(TAG, "Registration successful. Reg ID: " + regID);
            mDisplay.append("\n\nAnd you're ready for push notifications! Nice work!");

        } else {
            mDisplay.setText("Google Play app not installed");
        }
    }
}
