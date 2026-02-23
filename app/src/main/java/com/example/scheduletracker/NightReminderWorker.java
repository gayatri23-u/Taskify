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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NightReminderWorker extends Worker {

    public NightReminderWorker(@NonNull Context context,
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

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        String yesterdayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(cal.getTime());

        FirebaseFirestore.getInstance()
                .collection("Tasks")
                .document(uid)
                .collection("dates")
                .document(todayKey)
                .collection("items")
                .get()
                .addOnSuccessListener(todaySnap -> {

                    int total = todaySnap.size();
                    int done = 0;

                    for (QueryDocumentSnapshot doc : todaySnap) {
                        Boolean completed = doc.getBoolean("completed");
                        if (completed != null && completed) done++;
                    }

                    int pending = total - done;

                    // 🔁 Now check yesterday for streak
                    FirebaseFirestore.getInstance()
                            .collection("Tasks")
                            .document(uid)
                            .collection("dates")
                            .document(yesterdayKey)
                            .collection("items")
                            .get()
                            .addOnSuccessListener(yesterdaySnap -> {

                                boolean yesterdayAllDone = !yesterdaySnap.isEmpty();

                                for (QueryDocumentSnapshot doc : yesterdaySnap) {
                                    Boolean completed = doc.getBoolean("completed");
                                    if (completed == null || !completed) {
                                        yesterdayAllDone = false;
                                        break;
                                    }
                                }

                                if (pending > 0) {
                                    showSmartNotification(pending, yesterdayAllDone);
                                }

                                scheduleNextNight();   // repeat tomorrow
                            });
                });

        return Result.success();
    }

    //smart notification
    private void showSmartNotification(int pending, boolean streakInDanger) {

        Context context = getApplicationContext();
        String channelId = "task_reminder_channel";

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        String title;
        String message;

        if (streakInDanger) {
            title = "⚠️ Streak in danger!";
            message = "Finish today’s tasks to keep your streak alive 🔥";
        } else {
            title = "Almost there 🌙";
            message = "You still have " + pending + " tasks pending today.";
        }

        Notification notification =
                new androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .build();

        nm.notify(202, notification);
    }

    private void maybeShowSmartStreakWarning(int pending, boolean yesterdayAllDone) {
        Context context = getApplicationContext();
        String channelId = "task_reminder_channel";

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        String title = yesterdayAllDone
                ? "⚠️ Streak in danger!"
                : "Almost there 🌙";

        String msg = yesterdayAllDone
                ? "Complete today’s tasks to keep your streak alive 🔥"
                : "You still have " + pending + " tasks pending today.";

        Notification notification =
                new androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setAutoCancel(true)
                        .build();

        nm.notify(202, notification);
    }

    private void scheduleNextNight() {
        SharedPreferences prefs =
                getApplicationContext().getSharedPreferences("taskify_prefs", Context.MODE_PRIVATE);

        int hour = prefs.getInt("night_hour", 21);
        int minute = prefs.getInt("night_minute", 30);

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);

        target.add(Calendar.DAY_OF_MONTH, 1); // next day

        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(NightReminderWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag("night_reminder")
                        .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(request);
    }

    private void showNotification(int pending) {
        Context context = getApplicationContext();
        String channelId = "task_reminder_channel";

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        // Open dashboard when notification tapped
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
                        .setContentTitle("Almost there 🌙")
                        .setContentText("You still have " + pending + " tasks pending today. Finish strong 🔥")
                        .setContentIntent(pi)   // this line is important
                        .setAutoCancel(true)
                        .build();

        nm.notify(202, notification);
    }
}
