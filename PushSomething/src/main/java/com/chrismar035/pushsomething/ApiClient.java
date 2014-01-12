package com.chrismar035.pushsomething;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * Created by chrismar035 on 1/12/14.
 */
public class ApiClient {
    private final static String TAG = "PushSomething:ApiService";

    private Context context;

    public ApiClient(Context context) {
        this.context = context;
    }

    public void sendRegistrationToBackend(String jwt, String gcmId, String uuid) throws IOException {
        JSONObject payload = new JSONObject();

        try {
            payload.put("jwt", jwt);
            payload.put("gcm_id", gcmId);
            payload.put("uid", uuid);
            Log.i(TAG, "Registering with the server\n" + payload.toString(2));

        } catch (JSONException e) {
            Log.i(TAG, "JSON creation failed");
        }

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.getSoTimeout(params);
        HttpClient client = new DefaultHttpClient(params);

        HttpPost request = new HttpPost(get_api_root() + "receivers");
        StringEntity entity = new StringEntity(payload.toString(), HTTP.UTF_8);
        entity.setContentType("application/json");
        request.setEntity(entity);
        HttpResponse response = client.execute(request);

        Log.i(TAG, "Posting Complete " + response.toString());
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String json = reader.readLine();
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.class.getSimpleName(), 0);

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

    public void acknowledgeNotification(Integer notification_id) {
        JSONObject payload = new JSONObject();

        try {
            SharedPreferences settings = context.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
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

        HttpPost request = new HttpPost(get_api_root() + "acknowledgements");
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

    private String get_api_root() {
        try {
            ApplicationInfo ai;
            ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle aBundle = ai.metaData;
            return aBundle.getString("api_root");
        } catch(Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Error fetching API Key. This is bad.");
        }
        return "";
    }
}
