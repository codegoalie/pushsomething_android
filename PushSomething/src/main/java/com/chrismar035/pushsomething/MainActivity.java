package com.chrismar035.pushsomething;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.UUID;

public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static String PROPERTY_ACCOUNT_NAME = "account_name";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private final static String API_ROOT = "http://pushsomething.com/api/v1/";
    String SENDER_ID = "762651962812";
    String SERVER_WEB_ID = "762651962812.apps.googleusercontent.com";


    static final String TAG = "PushSomething";

    GoogleCloudMessaging gcm;
    Context context;
    PlusClient mPlusClient;
    String regID;

    private NotificationsDataSource dataSource;
    private SimpleCursorAdapter dataAdapter;

    private BroadcastReceiver receiver = new UpdateNotificationList();

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

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
        context = getApplicationContext();

        final ListView listview = (ListView) findViewById(R.id.notification_list);

        dataSource = new NotificationsDataSource(this);
        try {
            dataSource.open();

            Cursor cursor = dataSource.getAllNotifications();

            // The desired columns to be bound
            String[] columns = new String[] {
                    NotificationTableHelper.COLUMN_TITLE,
                    NotificationTableHelper.COLUMN_BODY
            };

            // the XML defined views which the data will be bound to
            int[] to = new int[] {
                    R.id.title,
                    R.id.body
            };
            dataAdapter = new SimpleCursorAdapter(this, R.layout.notification_list_item, cursor, columns, to, 0);
            listview.setAdapter(dataAdapter);
        } catch (SQLException e) {
            Log.e(TAG, "Unable to open Notification data source on Create");
            Toast.makeText(this, "Unable to read notification", Toast.LENGTH_LONG).show();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.chrismar035.pushsomething.UpdateNotificationList");

        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlusClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try { dataSource.open(); }
        catch (SQLException e) { Log.e(TAG, "Unable to open Notification data source on Resume"); }
        checkPlayServices();
    }

    @Override
    protected void onStop() {
        super.onStop();
        dataSource.close();
        mPlusClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) { Log.e(TAG, "Skipping notification update receiver unregister"); }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Signed in as: " + mPlusClient.getAccountName());
        registerForGCM();
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        backToSignIn();
    }

    public void signOutClick(MenuItem _) {
        clearGCM();
        deleteNotifications();
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

    public void clearGCM() {
        Log.i(TAG, "Clearing GCM ID");
        final SharedPreferences prefs = getGCMPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, "");
        editor.putInt(PROPERTY_APP_VERSION, 0);
        editor.commit();
    }

    public void deleteNotificationsClick(MenuItem _) {
        deleteNotifications();
    }

    public void deleteNotifications() {
        Cursor cursor = dataSource.getAllNotifications();

        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {
            dataSource.deleteNotification(cursor);
            cursor.moveToNext();
        }

        dataAdapter.changeCursor(dataSource.getAllNotifications());
    }

    public class UpdateNotificationList extends BroadcastReceiver {
        private static final String TAG = "PushSomething.UpdateNotificationList";

        @Override
        public void onReceive(Context context, Intent intent) {
            Cursor cursor = dataSource.getAllNotifications();
            dataAdapter.changeCursor(cursor);
            Log.i(TAG, "Notified notification data source of changed data");
        }
    }

    private void backToSignIn() {
        Intent backToSignInIntent = new Intent(this, SignInActivity.class);
        startActivity(backToSignInIntent);
        this.finish();
    }

    private class backgroundRegistrationTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String msg;
            try {
                if(gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
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
            JSONObject payload = new JSONObject();

            try {
                payload.put("jwt", getJWT());
                payload.put("gcm_id", regID);
                payload.put("uid", getUUID());
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

            Log.i(TAG, "Posting Complete " + response.toString());
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String json = reader.readLine();
            SharedPreferences prefs = getSharedPreferences(MainActivity.class.getSimpleName(), 0);

            Log.i(TAG, "JSON response: " + json);
            try {
                JSONObject full_body = new JSONObject(json);
                JSONObject receiver = full_body.getJSONObject("receiver");
                SharedPreferences.Editor editor = prefs.edit();
                Integer server_id = receiver.getInt("id");
                String auth_token = receiver.getString("auth_token");

                editor.putInt("server_id", server_id);
                editor.putString("auth_token", auth_token);
                editor.commit();

                Log.i(TAG, "Committing server id: " + server_id + "|auth_token: " + auth_token);
            } catch (JSONException e) {
                Log.e(TAG, "Parsing registration JSON failed!!");
                e.printStackTrace();
            }
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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void registerForGCM() {
        if(checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
            regID = getRegistrationId(context);

            if(regID.isEmpty()) {
                Log.i(TAG, "Registration ID blank. Registering in background.");
                new backgroundRegistrationTask().execute();
            }
            Log.i(TAG, "Registration successful. Reg ID: " + regID);

        } else {
            Toast.makeText(this, "Google Play app not installed", Toast.LENGTH_LONG).show();
        }
    }

    private String getUUID() {
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());

        Log.i(TAG, "committing uuid " + deviceUuid.toString());
        SharedPreferences settings = getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("uuid", deviceUuid.toString());
        editor.commit();

        return deviceUuid.toString();
    }
}
