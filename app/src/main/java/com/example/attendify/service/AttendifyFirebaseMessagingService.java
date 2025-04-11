package com.example.attendify.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.attendify.R;
import com.example.attendify.ui.auth.AuthActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Service to handle Firebase Cloud Messaging (FCM) notifications
 */
public class AttendifyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "attendify_notifications";
    private static final String CHANNEL_NAME = "Attendify Notifications";
    private static final String CHANNEL_DESC = "Notifications from Attendify app";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            // Handle notification
            if (title != null && body != null) {
                sendNotification(title, body);
            }
        }

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            String notificationType = remoteMessage.getData().get("type");
            
            // Handle different notification types
            if (notificationType != null) {
                switch (notificationType) {
                    case "approval":
                        // User approved notification
                        handleApprovalNotification(remoteMessage.getData());
                        break;
                    case "check_in_reminder":
                        // Check-in reminder
                        handleCheckInReminder(remoteMessage.getData());
                        break;
                    default:
                        // Default handling
                        String title = remoteMessage.getData().get("title");
                        String body = remoteMessage.getData().get("body");
                        if (title != null && body != null) {
                            sendNotification(title, body);
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // TODO: Send token to server for user association
    }

    /**
     * Create and show a notification
     */
    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create the notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }

    /**
     * Handle user approval notification
     */
    private void handleApprovalNotification(java.util.Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");
        String approved = data.get("approved");

        if (title != null && body != null) {
            if (approved != null && approved.equals("true")) {
                // User is approved, show notification
                sendNotification(title, body);
            }
        }
    }

    /**
     * Handle check-in reminder notification
     */
    private void handleCheckInReminder(java.util.Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");

        if (title != null && body != null) {
            sendNotification(title, body);
        }
    }
} 