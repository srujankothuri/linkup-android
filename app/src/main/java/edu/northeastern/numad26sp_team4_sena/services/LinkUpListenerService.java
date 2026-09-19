package edu.northeastern.numad26sp_team4_sena.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.activities.NotificationsActivity;

public class LinkUpListenerService extends Service {

    public static final String CHANNEL_SERVICE = "linkup_service";
    public static final String CHANNEL_INCOMING = "linkup_incoming";
    private static final int NOTIF_SERVICE_ID = 1;
    private static final int NOTIF_INCOMING_ID = 2;

    private ListenerRegistration sessionListener;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_SERVICE_ID, buildServiceNotification());
        listenForIncomingLinkUps();
        return START_STICKY;
    }

    private void listenForIncomingLinkUps() {
        if (sessionListener != null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            stopSelf();
            return;
        }

        String uid = currentUser.getUid();
        sessionListener = FirebaseFirestore.getInstance()
                .collection("linkupRequests")
                .whereEqualTo("toUid", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            showIncomingNotification(dc.getDocument().getId());
                        }
                    }
                });
    }

    private void showIncomingNotification(String sessionId) {
        Intent intent = new Intent(this, NotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_INCOMING)
                .setSmallIcon(R.drawable.ic_wifi_tethering)
                .setContentTitle(getString(R.string.notif_incoming_title))
                .setContentText(getString(R.string.notif_incoming_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIF_INCOMING_ID, notification);
    }

    private Notification buildServiceNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, NotificationsActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_wifi_tethering)
                .setContentTitle(getString(R.string.notif_service_title))
                .setContentText(getString(R.string.notif_service_text))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setSilent(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);

        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_SERVICE, getString(R.string.notif_channel_service_name),
                NotificationManager.IMPORTANCE_MIN);
        serviceChannel.setDescription(getString(R.string.notif_channel_service_desc));
        serviceChannel.setShowBadge(false);

        NotificationChannel incomingChannel = new NotificationChannel(
                CHANNEL_INCOMING, getString(R.string.notif_channel_incoming_name),
                NotificationManager.IMPORTANCE_HIGH);
        incomingChannel.setDescription(getString(R.string.notif_channel_incoming_desc));

        nm.createNotificationChannel(serviceChannel);
        nm.createNotificationChannel(incomingChannel);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartIntent = new Intent(this, LinkUpListenerService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                this, 1, restartIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000, pendingIntent);

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sessionListener != null) sessionListener.remove();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
