package com.busyweb.firebaselogindemo.firebase;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;


/**
 * Created by BusyWeb on 9/24/2016.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FB-MessagingService";

    public interface MessageReceivedListener {
        public void MessageReceived(String message);
    }
    private static MessageReceivedListener mMessageReceivedListener;
    public static void SetMessageReceivedListener(MessageReceivedListener messageReceivedListener) {
        mMessageReceivedListener = messageReceivedListener;
    }


//    private static MessageReceivedListener mMessageReceivedListener = new MessageReceivedListener() {
//
//            @Override
//            public void CommandResultReceived(String cameraId, String downloadLink) {
//
//            }
//    };
    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String,String> data = remoteMessage.getData();
            String dataString = "";
            String dataFrom = "";
            String dataMessage = "";
            for (Map.Entry<String, String> item : data.entrySet()) {
                dataString += ("Key: " + item.getKey() + ", Value: " + item.getValue());
                dataString += "\n";

                String key = item.getKey();
                String value = item.getValue();

                if (key.equalsIgnoreCase("From")) {
                    dataFrom = value;
                } else if (key.equalsIgnoreCase("Message")) {
                    dataMessage = value;
                }
            }
            //sendNotification(dataString);

            if (mMessageReceivedListener != null) {
                String message = String.format("Message: %s, received from %s.", dataMessage, dataFrom);
                mMessageReceivedListener.MessageReceived(message);
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]


    private synchronized void sendHandlerMessage(Handler handler, int what, int delayMillis, Bundle bundle) {
        Message message = handler.obtainMessage(what, 0, 0);
        if (bundle != null) {
            message.setData(bundle);
        }
        if (delayMillis > 0) {
            handler.sendMessageDelayed(message, delayMillis);
        } else {
            handler.sendMessage(message);
        }
    }

}
