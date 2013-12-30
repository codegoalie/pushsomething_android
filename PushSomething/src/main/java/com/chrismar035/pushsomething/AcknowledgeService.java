package com.chrismar035.pushsomething;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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
import java.io.UnsupportedEncodingException;

public class AcknowledgeService extends IntentService {
    final static private String TAG = "PushSomething:AcknowledgeService";
    private Integer notification_id;

    String API_ROOT = "http://pushsomething.com/api/v1/";

    public AcknowledgeService() {
        super("AcknowledgeService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        Log.i(TAG, "Acknowledgement service bound");
        Log.i(TAG, "Starting Main Activity");
        final Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);

        Bundle extras = intent.getExtras();

        if(extras != null) {
            notification_id = extras.getInt("EXTRA_NOTIFICATION_ID", -1);
            Log.i(TAG, "Notification ID to ack: " + notification_id);
        }

        if(notification_id != -1) {
            acknowledgeNotification();
        }
        else {
            Log.e(TAG, "Notification ID not present in Extras! Not acknowledging");
        }

    }

    private void acknowledgeNotification() {
        JSONObject payload = new JSONObject();

        try {
            SharedPreferences settings = getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
            payload.put("auth_token", settings.getString("auth_token", "not found"));
            payload.put("uid", settings.getString("uuid", "not found"));
            payload.put("notification_id", notification_id);
            Log.i(TAG, "Acknowledging notification with the server\n" + payload.toString(2));

        } catch (JSONException e) {
            Log.i(TAG, "JSON creation failed");
        }

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.getSoTimeout(params);
        HttpClient client = new DefaultHttpClient(params);

        HttpPost request = new HttpPost(API_ROOT + "acknowledgements");
        StringEntity entity;

        try {
            entity = new StringEntity(payload.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            request.setEntity(entity);
            HttpResponse response = client.execute(request);

            Log.i(TAG, "Acknowledging Complete " + response.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
