package com.example.scheduletracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MorningReminderWorker extends Worker {

    public MorningReminderWorker(@NonNull Context context,
                                 @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.success();

        String uid = user.getUid();
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        FirebaseFirestore.getInstance()
                .collection("Tasks")
                .document(uid)
                .collection("dates")
                .document(todayKey)
                .collection("items")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int total = snapshot.size();
                    if (total > 0) {
                        showNotification(total);
                    }
                    scheduleNextMorning();   // repeat tomorrow
                });

        return Result.success();
    }

    private void scheduleNextMorning() {
        SharedPreferences prefs =
                getApplicationContext().getSharedPreferences("taskify_prefs", Context.MODE_PRIVATE);

        int hour = prefs.getInt("morning_hour", 8);
        int minute = prefs.getInt("morning_minute", 0);

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);

        target.add(Calendar.DAY_OF_MONTH, 1); // next day

        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(MorningReminderWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag("morning_reminder")
                        .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(request);
    }

    private void showNotification(int total) {
        Context context = getApplicationContext();
        String channelId = "task_reminder_channel";

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        //  Open dashboard when notification tapped
        Intent intent = new Intent(context, dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification =
                new androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Good Morning ☀️")
                        .setContentText("You have " + total + " tasks today. Let’s complete them 💪")
                        .setContentIntent(pi)   //  tap opens dashboard
                        .setAutoCancel(true)
                        .build();

        nm.notify(201, notification);
    }
}
