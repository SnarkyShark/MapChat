package edu.temple.mapchat;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.List;

import static android.content.ContentValues.TAG;
import static edu.temple.mapchat.MainActivity.CHANNEL_ID;
import static edu.temple.mapchat.MainActivity.EXTRA_FRIEND;
import static edu.temple.mapchat.MainActivity.USERNAME_EXTRA;

public class MyMessagingService extends FirebaseMessagingService {

    private LocalBroadcastManager localBroadcastManager;

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e("fcmtrack", "received message");

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        try {
            JSONObject jsonString = new JSONObject(remoteMessage.getData().get("payload"));
            Log.e("fcmtrack", "payload: " + jsonString);
            parseJSON(jsonString);
        } catch (Exception e) {
            Log.e("fcmtrack", "issue receiving message");
        }
    }

    public void parseJSON(JSONObject json) {
        Log.e("fcmtrack", "tried to parse");

        try {
            String to = json.getString("to");
            String partner = json.getString("from");
            String content = json.getString("message");
            Log.e("fcmtrack", "to: " + to + ", sender: " + partner + ", content: " + content);


            if(isAppOnForeground(this, "edu.temple.mapchat")){
                Intent intent = new Intent("new_message");
                intent.putExtra("to", to);
                intent.putExtra("partner", partner);
                intent.putExtra("message", content);
                localBroadcastManager.sendBroadcast(intent);
                Log.d("fcmtrack", "Sent: " + content);
            }
            else{
                Log.d("fcmtrack", "app is not in the foreground");

                Intent newIntent = new Intent(this, ChatActivity.class);

                newIntent.putExtra(USERNAME_EXTRA, to);
                newIntent.putExtra(EXTRA_FRIEND, partner);
                newIntent.putExtra("content", content);
                PendingIntent pi = PendingIntent.getActivity(this,111, newIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                //Build a notification
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(this, CHANNEL_ID)
                                .setSmallIcon(R.mipmap.ic_launcher_round)
                                .setContentTitle("You have a new message.")
                                .setContentText(partner + " sent you a message.")
                                .setContentIntent(pi)
                                .setAutoCancel(true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(this);
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(1010, builder.build());
            }

        } catch (Exception e) {
            Log.e("fcmtrack", "issue parsing json");
        }
    }

    private boolean isAppOnForeground(Context context, String appPackageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(appPackageName)) {
                //                Log.e("app",appPackageName);
                return true;
            }
        }
        return false;
    }
}
