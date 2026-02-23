package com.example.scheduletracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WeeklySummaryWorker extends Worker {

    public WeeklySummaryWorker(@NonNull Context context,
                               @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.success();

        String uid = user.getUid();

        Calendar cal = Calendar.getInstance();
        int total = 0;
        int done = 0;
        int perfectDays = 0;   //  days where all tasks were completed

        try {
            for (int i = 0; i < 7; i++) {

                String key = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(cal.getTime());

                QuerySnapshot snapshot = Tasks.await(
                        FirebaseFirestore.getInstance()
                                .collection("Tasks")
                                .document(uid)
                                .collection("dates")
                                .document(key)
                                .collection("items")
                                .get()
                );

                int dayTotal = snapshot.size();
                int dayDone = 0;

                for (var doc : snapshot.getDocuments()) {
                    Boolean completed = doc.getBoolean("completed");
                    if (completed != null && completed) dayDone++;
                }

                total += dayTotal;
                done += dayDone;

                if (dayTotal > 0 && dayDone == dayTotal) {
                    perfectDays++;   //  full day completed
                }

                cal.add(Calendar.DAY_OF_MONTH, -1);
            }

            if (total > 0) {
                showWeeklyNotification(total, done, perfectDays);
            }

            scheduleNextWeek();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }

        return Result.success();
    }

    private void showWeeklyNotification(int total, int done, int perfectDays) {

        Context context = getApplicationContext();
        String channelId = "task_reminder_channel";

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        //  Smart weekly message
        String message;
        if (done == total && total > 0) {
            message = "Perfect week! All tasks completed 🔥";
        } else if (done >= total * 0.7) {
            message = "Great consistency! " + done + "/" + total + " tasks done 💪";
        } else {
            message = "Let’s improve next week! " + done + "/" + total + " tasks done 🌱";
        }

        // Badge logic
        if (perfectDays >= 5) {
            message = "🏅 5-Day Streak Achiever!\n" + message;
        }

        Notification notification =
                new androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("📊 Weekly Summary")
                        .setContentText(message)
                        .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                        .setAutoCancel(true)
                        .build();

        nm.notify(303, notification);
    }

    private void scheduleNextWeek() {

        long delay = 7L * 24L * 60L * 60L * 1000L; // 7 days in milliseconds

        androidx.work.OneTimeWorkRequest request =
                new androidx.work.OneTimeWorkRequest.Builder(WeeklySummaryWorker.class)
                        .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .addTag("weekly_summary")
                        .build();

        androidx.work.WorkManager.getInstance(getApplicationContext())
                .enqueue(request);
    }
}