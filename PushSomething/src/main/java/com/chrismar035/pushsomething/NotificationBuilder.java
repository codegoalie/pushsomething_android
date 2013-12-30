package com.chrismar035.pushsomething;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.sql.SQLException;

/**
 * Created by chrismar035 on 12/22/13.
 */

public class NotificationBuilder {
    final static String TAG = "PushSomething:NotificationBuilder";

    private Bundle extras;
    private String messageType = "";
    private Context context;

    public NotificationBuilder(Context context, Bundle extras) {
        this.context = context;
        this.extras = extras;
        Log.i(TAG, "Extras: " + this.extras.toString());
        messageType = this.extras.getString("type");
        Log.i(TAG, "Message Type: " + messageType);
    }

    public void send() {
        if(messageType.equals("notification")) {
            String title, body;
            Integer server_id;
            if(extras.containsKey("server_id")) {
                server_id = Integer.parseInt(extras.getString("server_id"));
            } else {
                server_id = -1;
            }
            if(extras.containsKey("title")) {
                title = extras.getString("title");
            } else {
                title = "<unknown title>";
            }
            if(extras.containsKey("body")) {
                body = extras.getString("body");
            } else {
                body = "";
            }

            NotificationsDataSource dataSource = new NotificationsDataSource(context);

            try {
                dataSource.open();

                sendNotification(dataSource.createNotification(server_id, title, body));

                Intent intent = new Intent();
                intent.setAction("com.chrismar035.pushsomething.UpdateNotificationList");
                context.sendBroadcast(intent);
            } catch (SQLException e) {
                Log.e(TAG, "Unable to open Notification data source");
            }
        }
        else if(messageType.equals("acknowledgement")) {
            if(extras.containsKey("notification_id")) {
                Integer notification_id = Integer.parseInt(extras.getString("notification_id"));
                Log.i(TAG, "Notification " + notification_id + " acknowledged elsewhere. Removing.");
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(notification_id);
            } else {
                Log.i(TAG, "Acknowledgement received without notification_id");
            }
        }
        else {
            Log.i(TAG, "Unknown Message Type");
        }

    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void sendNotification(Notification notification) {
        Log.i(TAG, "Sending Notification");

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent acknowledgeIntent = new Intent(context, AcknowledgeService.class);
        acknowledgeIntent.putExtra("EXTRA_NOTIFICATION_ID", (int)notification.getServerId());


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(notification.getTitle());
        acknowledgeIntent.setAction(String.valueOf(notification.getServerId()));

        if(!notification.getBody().isEmpty()) {
            mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(notification.getBody()))
                    .setContentText(notification.getBody());
        }

        mBuilder.setAutoCancel(true);

        PendingIntent contentIntent = PendingIntent.getService(context, 0, acknowledgeIntent, 0);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setDeleteIntent(contentIntent);
        mNotificationManager.notify(reasonableNotificationId(notification.getServerId()),
                                    mBuilder.build());
    }

    private int reasonableNotificationId(long id) {
        return (int) id % Integer.MAX_VALUE;
    }
}
