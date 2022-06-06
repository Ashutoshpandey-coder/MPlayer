package com.soumya_music.mplayer.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import com.soumya_music.mplayer.R;
import com.soumya_music.mplayer.activities.SongDetailsActivity;

public class NotificationUtils {

    private static final String CHANNEL_ID = "100";
    private static final int NOTIFICATION_ID = 200;
    public static void createNotification(Context context, String songName,int pausePlayIcon) {
        MediaSessionCompat mediaSession = new MediaSessionCompat(context,"Service");
        androidx.media.app.NotificationCompat.MediaStyle mediaStyle =  new androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2, 3)
                .setMediaSession(mediaSession.getSessionToken());
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Audio control", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID);
        notification
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(songName)
                .setContentText("Unknown artist")
                .setOnlyAlertOnce(true)
                .setContentIntent(whenClickOnNotification(context))
                .addAction(R.drawable.previous, "prev-song", actionPrevPendingIntent(context))
                .addAction(pausePlayIcon, "play-song", actionPlayPendingIntent(context))
                .addAction(R.drawable.next, "next-song", actionNextPendingIntent(context))
                .addAction(R.drawable.ic_cancel, "cancel-notification", actionCancelPendingIntent(context))
                .setStyle(mediaStyle)
                .setOngoing(true)
                .setNotificationSilent()
                .build();
        notificationManager.notify(NOTIFICATION_ID, notification.build());
    }

    private static PendingIntent whenClickOnNotification(Context context){
        Intent intent = new Intent(context, SongDetailsActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    private static PendingIntent actionPrevPendingIntent(Context context) {
        Intent intent = new Intent(context, NotificationReceiver.class)
                .setAction(SongDetailsActivity.ACTION_PREV);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent actionNextPendingIntent(Context context) {
        Intent intent = new Intent(context, NotificationReceiver.class)
                .setAction(SongDetailsActivity.ACTION_NEXT);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent actionPlayPendingIntent(Context context) {
        Intent intent = new  Intent(context, NotificationReceiver.class);
        intent.setAction(SongDetailsActivity.ACTION_PAUSE);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void clearAllNotification( Context context) {
        NotificationManager notificationManager =(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private static PendingIntent actionCancelPendingIntent(Context context) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(SongDetailsActivity.ACTION_CANCEL);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
