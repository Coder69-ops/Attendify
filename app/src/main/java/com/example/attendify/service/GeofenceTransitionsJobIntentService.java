package com.example.attendify.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.example.attendify.R;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceTransitionsJobIntentService extends JobIntentService {
    private static final int JOB_ID = 573;
    private static final String CHANNEL_ID = "geofence_channel";

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceTransitionsJobIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            return;
        }

        if (geofencingEvent.hasError()) {
            // Handle error
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            return;
        }

        String geofenceId = triggeringGeofences.get(0).getRequestId();

        String notificationTitle;
        String notificationContent;

        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                notificationTitle = "Office Area Entered";
                notificationContent = "You are now within your office area. Don't forget to check in!";
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                notificationTitle = "Office Area Exited";
                notificationContent = "You have left your office area. Make sure you've checked out!";
                break;
            default:
                return;
        }

        // Send notification
        sendNotification(notificationTitle, notificationContent);
    }

    private void sendNotification(String title, String content) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create the notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Geofence Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Show the notification
        notificationManager.notify(
            (int) (System.currentTimeMillis() % Integer.MAX_VALUE),
            builder.build()
        );
    }
}