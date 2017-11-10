package com.mathieuclement.swiss.autoindex.android.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * @author Mathieu Clément
 * @since 15.09.2013
 */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String str = "";
        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                str += msgs[i].getOriginatingAddress();
                str += "*SEP*";
                str += msgs[i].getMessageBody().toString();
                str += "*SEP_PDU*";
            }
            //---display the new SMS message---
            //Toast.makeText(context, str, Toast.LENGTH_LONG).show();
            Log.i(getClass().getName(), str);
            broadcastFurther(context, str);
        }
    }

    private void broadcastFurther(Context context, String str) {
        Intent intent = new Intent();
        intent.setAction("com.mathieuclement.swiss.autoindex.android.app.SMS_RECEIVED");
        intent.putExtra("message", str);
        context.sendBroadcast(intent);
    }
}
