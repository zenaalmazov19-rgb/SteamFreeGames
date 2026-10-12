package com.example.steamfreegames;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("steamfreegames", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notifications", false)) return;
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 6, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, MainActivity.channelId()) : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Проверь новые раздачи игр").setContentText("Открой Steam Free Games и обнови список бесплатных предложений.").setContentIntent(pi).setAutoCancel(true);
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(5, b.build());
    }
}
