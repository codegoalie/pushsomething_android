package com.chrismar035.pushsomething;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AcknowledgeService extends IntentService {
    final static private String TAG = "PushSomething:AcknowledgeService";

    public AcknowledgeService() {
        super("AcknowledgeService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        Log.i(TAG, "Acknowledgement service bound");
        Log.i(TAG, "Starting Main Activity");
        final Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);

        Bundle extras = intent.getExtras();
        Integer notification_id = -1;

        if(extras != null) {
            notification_id = extras.getInt("EXTRA_NOTIFICATION_ID", -1);
            Log.i(TAG, "Notification ID to ack: " + notification_id);
        }

        if(notification_id != -1) {
            ApiClient client = new ApiClient(this);
            client.acknowledgeNotification(notification_id);
        }
        else {
            Log.e(TAG, "Notification ID not present in Extras! Not acknowledging");
        }

    }



}