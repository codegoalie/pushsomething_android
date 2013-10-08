package com.chrismar035.pushsomething;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.PlusClient;

public class SignInActivity extends Activity implements
        View.OnClickListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener  {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final static String PROPERTY_ACCOUNT_NAME = "account_name";

    private ProgressDialog mConnectionProgressDialog;
    private PlusClient mPlusClient;
    private ConnectionResult mConnectionResult;

    static final String TAG = "PushSomething";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Creating SignIn");
        setContentView(R.layout.activity_signin);

        mPlusClient = new PlusClient.Builder(this, this, this)
                .setVisibleActivities("http://schemas.google.com/AddActivity")
                .build();
        mConnectionProgressDialog = new ProgressDialog(this);
        mConnectionProgressDialog.setMessage("Signing in...");
        findViewById(R.id.sign_in_button).setOnClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sign_in, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "Handling sign in");

        if (v.getId() == R.id.sign_in_button && !mPlusClient.isConnected()) {
            Log.i(TAG, "Sign-in clicked and not yet connected");
            if (mConnectionResult == null) {
                mConnectionProgressDialog.show();
                Log.i(TAG, "showing progress");
            } else {
                try {
                    Log.i(TAG, "Starting resolution");
                    mConnectionResult.startResolutionForResult(this, PLAY_SERVICES_RESOLUTION_REQUEST);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "Catching resolution error");
                    mConnectionResult = null;
                    mPlusClient.connect();
                }
            }
        }
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

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection Failed");
        if (mConnectionProgressDialog.isShowing()) {
            Log.i(TAG, "Connection failed");
            // The user clicked sign in already. Start to resolve connection errors.
            if (result.hasResolution()) {
                try {
                    result.startResolutionForResult(this, PLAY_SERVICES_RESOLUTION_REQUEST);
                } catch (IntentSender.SendIntentException e) {
                    mPlusClient.connect();
                }
            }
        }

        // Save the intent to start an activity when the user clicks the sign in
        mConnectionResult = result;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // We've resolved any errors
        Log.i(TAG, "Client Connected.");
        storeAccountName();
        mConnectionProgressDialog.dismiss();
        onToMain();
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "disconnected");
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        Log.i(TAG, "onActivityResult");
        if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST && responseCode == RESULT_OK) {
            Log.i(TAG, "OK! Connecting");
            mConnectionResult = null;
            mPlusClient.connect();
        }
    }

    private void onToMain() {
        Log.i(TAG, "Sign in success. Proceeding to MainActivity.");
        Intent start_main_intent = new Intent(this, MainActivity.class);
        startActivity(start_main_intent);
        this.finish();
    }

    private void storeAccountName() {
        SharedPreferences prefs = getSharedPreferences(PROPERTY_ACCOUNT_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        String accountName = mPlusClient.getAccountName();
        Log.i(TAG, "Storing Account Name: " + accountName);
        editor.putString(PROPERTY_ACCOUNT_NAME, accountName);
        editor.commit();
    }
}
