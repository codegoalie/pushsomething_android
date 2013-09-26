package com.chrismar035.pushsomething;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GcmBroadcastReceiver extends BroadcastReceiver {
    private static Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        context.startService(new Intent(context, GcmIntentService.class));
        setResultCode(Activity.RESULT_OK);
    }

    public static void completeIntent(Intent intent) {
        mContext.stopService(intent);
    }
}
