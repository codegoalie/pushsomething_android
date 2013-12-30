package com.chrismar035.pushsomething;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmBroadcastReceiver extends BroadcastReceiver {
    private static Context mContext;
    public static final int NOTIFICATION_ID = 1;
    static final String TAG = "PushSomething";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "PushSomething:GcmBroadcastReceiver");
        mContext = context;
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(mContext);
        String messageType = gcm.getMessageType(intent);
        Log.i(TAG, "Handling Intent");

        if(!extras.isEmpty()) {
            Log.i(TAG, "Extras is not empty");
            if(GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendStringNotification("Send error", extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendStringNotification("Deleted messages on server", extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Log.i(TAG, "Building Notification");
                NotificationBuilder builder = new NotificationBuilder(mContext, extras);
                Log.i(TAG, "Sending Notification");
                builder.send();
            }
        } else {
            Log.i(TAG, "Extras is empty. No notification sent.");
        }

        setResultCode(Activity.RESULT_OK);
    }

    private void sendStringNotification(String title, String msg) {
        Log.i(TAG, "Sending String Notification");
        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getService(mContext, 0, new Intent(mContext, AcknowledgeService.class), 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg))
                .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
