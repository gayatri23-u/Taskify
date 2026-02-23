package com.example.scheduletracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class dashboard extends AppCompatActivity {

    GridLayout gridCalendar;
    TextView txtMonthYear, txtStreakCount, txtTodayProgress, txtMonthlyCompletion;
    ImageView btnPrevMonth, btnNextMonth, btnMenu;
    CardView cardAddTasks, cardViewTasks;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseUser currentUser;

    Calendar currentCalendar = Calendar.getInstance();

    // day -> [totalTasks, completedTasks]
    Map<Integer, int[]> dayStatusMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        MaterialToolbar toolbar = findViewById(R.id.toolbarDashboard);
        CardView cardBadges = findViewById(R.id.cardBadges);

        cardBadges.setOnClickListener(v ->
                startActivity(new Intent(dashboard.this, BadgesActivity.class))
        );

        gridCalendar = findViewById(R.id.gridCalendar);
        txtMonthYear = findViewById(R.id.txtMonthYear);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        txtStreakCount = findViewById(R.id.txtStreakCount);
        txtTodayProgress = findViewById(R.id.txtTodayProgress);
        txtMonthlyCompletion = findViewById(R.id.txtMonthlyCompletion);

        cardAddTasks = findViewById(R.id.cardAddTasks);
        cardViewTasks = findViewById(R.id.cardViewTasks);
        btnMenu = findViewById(R.id.btnMenu);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            loadCalendarFromFirebase();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            loadCalendarFromFirebase();
        });

        cardAddTasks.setOnClickListener(v ->
                startActivity(new Intent(dashboard.this, add_task.class)));

        cardViewTasks.setOnClickListener(v ->
                startActivity(new Intent(dashboard.this, view_task.class)));

        btnMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(dashboard.this, btnMenu);
            popupMenu.getMenuInflater().inflate(R.menu.toolbar_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.menu_settings) {
                    // Open Settings
                    startActivity(new Intent(dashboard.this, settings.class));
                    return true;


                } else if (id == R.id.menu_logout) {
                    showLogoutDialog();   // show confirmation first
                    return true;
                }

                return false;
            });

            popupMenu.show();
        });

        //stats details
        loadCalendarFromFirebase();
        loadTodayProgress();
        loadCurrentStreak();
        loadMonthlyCompletion();

        //reminders call
        scheduleDailyReminders();

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }

        //create all the batches whether locked or unlocked
        seedBadgesIfMissing();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadCalendarFromFirebase();   // refresh calendar
        loadTodayProgress();          // refresh today progress
        loadCurrentStreak();          // refresh streak
        loadMonthlyCompletion();      // refresh monthly stats
    }

    //  Load calendar data from Firebase
    private void loadCalendarFromFirebase() {
        if (currentUser == null) return;

        dayStatusMap.clear();
        gridCalendar.removeAllViews();

        SimpleDateFormat sdfMonth = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        txtMonthYear.setText(sdfMonth.format(currentCalendar.getTime()));

        String uid = currentUser.getUid();
        int month = currentCalendar.get(Calendar.MONTH) + 1;
        int year = currentCalendar.get(Calendar.YEAR);

        db.collection("Tasks")
                .document(uid)
                .collection("dates")
                .get()
                .addOnSuccessListener(dateSnapshots -> {

                    int[] pending = {0};

                    for (QueryDocumentSnapshot dateDoc : dateSnapshots) {
                        String[] parts = dateDoc.getId().split("-");
                        int y = Integer.parseInt(parts[0]);
                        int m = Integer.parseInt(parts[1]);
                        int d = Integer.parseInt(parts[2]);

                        if (y == year && m == month) {
                            pending[0]++;

                            db.collection("Tasks")
                                    .document(uid)
                                    .collection("dates")
                                    .document(dateDoc.getId())
                                    .collection("items")
                                    .get()
                                    .addOnSuccessListener(items -> {

                                        int total = items.size();
                                        int done = 0;

                                        for (QueryDocumentSnapshot doc : items) {
                                            Boolean completed = doc.getBoolean("completed");
                                            if (completed != null && completed) done++;
                                        }

                                        dayStatusMap.put(d, new int[]{total, done});

                                        pending[0]--;
                                        if (pending[0] == 0) {
                                            renderCalendar();   // ✅ render ONLY after all data loaded
                                        }
                                    });
                        }
                    }

                    if (pending[0] == 0) {
                        renderCalendar(); // no tasks at all in this month
                    }
                });
    }

    private void renderCalendar() {
        gridCalendar.removeAllViews();

        Calendar tempCal = (Calendar) currentCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) addEmptyCell();

        Calendar today = Calendar.getInstance();

        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday =
                    day == today.get(Calendar.DAY_OF_MONTH) &&
                            currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                            currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR);

            int total = dayStatusMap.containsKey(day) ? dayStatusMap.get(day)[0] : 0;
            int done = dayStatusMap.containsKey(day) ? dayStatusMap.get(day)[1] : 0;

            addDayCell(day, isToday, total, done);
        }
    }

    private void addEmptyCell() {
        View v = new View(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(40);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        v.setLayoutParams(params);
        gridCalendar.addView(v);
    }

    private void addDayCell(int day, boolean isToday, int totalTasks, int completedTasks) {
        FrameLayout cell = new FrameLayout(this);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(42);   // consistent height
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        cell.setLayoutParams(params);

        // Background logic
        if (totalTasks == 0) {
            cell.setBackgroundResource(R.drawable.bg_day_inactive);
        } else if (completedTasks == totalTasks) {
            cell.setBackgroundResource(R.drawable.bg_day_success); // 🟢 GREEN
        } else {
            cell.setBackgroundResource(R.drawable.bg_day_failed);  // 🔴 RED
        }

        //  Today highlight (border or overlay)
        if (isToday) {
            cell.setForeground(getDrawable(R.drawable.bg_day_today)); // optional border drawable
        }

        TextView tv = new TextView(this);
        tv.setText(String.valueOf(day));
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);

        //  Today date in BLACK
        if (isToday) tv.setTextColor(Color.BLACK);
        else tv.setTextColor(Color.WHITE);

        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        tv.setLayoutParams(tvParams);

        cell.addView(tv);

        //  Tap → open that day in view_task
        cell.setOnClickListener(v -> {
            String dateKey = String.format(Locale.getDefault(),
                    "%04d-%02d-%02d",
                    currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH) + 1,
                    day
            );

            showStatsBottomSheetForDate(dateKey);
        });
        gridCalendar.addView(cell);
    }

    // ===== Stats logic (unchanged but works live) =====

    private void loadTodayProgress() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
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

                    int total = 0;
                    int done = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        // 🔥 Only count tasks that are not placeholder/invalid
                        String title = doc.getString("title");
                        if (title == null || title.trim().isEmpty()) continue;

                        total++;

                        Boolean completed = doc.getBoolean("completed");
                        if (completed != null && completed) done++;
                    }

                    TextView txtTodayProgress = findViewById(R.id.txtTodayProgress);
                    txtTodayProgress.setText(total == 0
                            ? "No tasks today"
                            : done + " / " + total + " Tasks Done");
                });
    }

    private void loadCurrentStreak() {
        Calendar cal = Calendar.getInstance();
        calculateStreak(cal, 0);
    }

    private void calculateStreak(Calendar cal, int streak) {
        String uid = currentUser.getUid();
        String key = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        db.collection("Tasks").document(uid)
                .collection("dates").document(key)
                .collection("items")
                .get()
                .addOnSuccessListener(snapshot -> {

                    boolean allDone = false;
                    if (!snapshot.isEmpty()) {
                        allDone = true;
                        for (QueryDocumentSnapshot doc : snapshot) {
                            Boolean completed = doc.getBoolean("completed");
                            if (completed == null || !completed) {
                                allDone = false;
                                break;
                            }
                        }
                    }

                    if (allDone) {
                        cal.add(Calendar.DAY_OF_MONTH, -1);
                        calculateStreak(cal, streak + 1);
                    } else {
                        txtStreakCount.setText(streak + " Days");
                        loadTotalCompletedTasksAndUnlockBadges(streak);
                    }
                });
    }

    private void loadMonthlyCompletion() {
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        int month = currentCalendar.get(Calendar.MONTH) + 1;
        int year = currentCalendar.get(Calendar.YEAR);

        db.collection("Tasks").document(uid)
                .collection("dates")
                .get()
                .addOnSuccessListener(dateSnapshots -> {

                    int[] total = {0};
                    int[] done = {0};
                    int[] pending = {0};

                    for (QueryDocumentSnapshot dateDoc : dateSnapshots) {
                        String[] parts = dateDoc.getId().split("-");
                        int y = Integer.parseInt(parts[0]);
                        int m = Integer.parseInt(parts[1]);

                        if (y == year && m == month) {
                            pending[0]++;

                            db.collection("Tasks")
                                    .document(uid)
                                    .collection("dates")
                                    .document(dateDoc.getId())
                                    .collection("items")
                                    .get()
                                    .addOnSuccessListener(items -> {
                                        for (QueryDocumentSnapshot doc : items) {
                                            total[0]++;
                                            Boolean completed = doc.getBoolean("completed");
                                            if (completed != null && completed) done[0]++;
                                        }

                                        pending[0]--;
                                        if (pending[0] == 0) {
                                            if (total[0] == 0) {
                                                txtMonthlyCompletion.setText("0%");
                                            } else {
                                                int percent = (done[0] * 100) / total[0];
                                                txtMonthlyCompletion.setText(percent + "%");
                                            }
                                        }
                                    });
                        }
                    }

                    if (pending[0] == 0) {
                        txtMonthlyCompletion.setText("0%");
                    }
                });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showLogoutDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        builder.setView(view);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(dashboard.this, login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            dialog.dismiss();
        });

        dialog.show();
    }


    //show stast bottom sheett for dates
    private void showStatsBottomSheetForDate(String dateKey) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_stats, null);

        TextView txtTitle = view.findViewById(R.id.txtTitle);
        TextView txtStreak = view.findViewById(R.id.txtStreak);
        TextView txtToday = view.findViewById(R.id.txtTodayProgress);
        TextView txtMonth = view.findViewById(R.id.txtMonthlyCompletion);

        dialog.setContentView(view);
        dialog.show();

        // Set title as selected date
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey);
            String pretty = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(d);
            txtTitle.setText("Stats for " + pretty);
        } catch (Exception e) {
            txtTitle.setText("Stats for " + dateKey);
        }

        loadStatsForDateInto(dateKey, txtStreak, txtToday, txtMonth);
    }

    private void loadStatsForDateInto(String dateKey,
                                      TextView txtStreak,
                                      TextView txtToday,
                                      TextView txtMonth) {

        if (currentUser == null) return;
        String uid = currentUser.getUid();

        db.collection("Tasks")
                .document(uid)
                .collection("dates")
                .document(dateKey)
                .collection("items")
                .get()
                .addOnSuccessListener(snapshot -> {

                    int total = snapshot.size();
                    int done = 0;

                    for (QueryDocumentSnapshot doc : snapshot) {
                        Boolean completed = doc.getBoolean("completed");
                        if (completed != null && completed) done++;
                    }

                    // Reuse fields meaningfully for that day
                    txtToday.setText(done + " / " + total + " Done");

                    if (total == 0) {
                        txtMonth.setText("0%");
                    } else {
                        int percent = (done * 100) / total;
                        txtMonth.setText(percent + "%");
                    }

                    // Streak label for that date (optional wording)
                    txtStreak.setText(done == total && total > 0
                            ? "All tasks completed 🎉"
                            : "Tasks completed: " + done);
                })
                .addOnFailureListener(e -> {
                    txtToday.setText("0 / 0 Done");
                    txtMonth.setText("0%");
                    txtStreak.setText("No data");
                });
    }

    private void scheduleDailyReminders() {
        scheduleDailyReminder(MorningReminderWorker.class, 8, 0, "morning_reminder");
        scheduleDailyReminder(NightReminderWorker.class, 21, 30, "night_reminder");

    }

    private void scheduleDailyReminder(Class<? extends androidx.work.Worker> workerClass,
                                       int hour, int minute, String tag) {

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1); // next day
        }

        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        androidx.work.OneTimeWorkRequest request =
                new androidx.work.OneTimeWorkRequest.Builder(workerClass)
                        .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .addTag(tag)
                        .build();

        androidx.work.WorkManager.getInstance(this)
                .enqueueUniqueWork(
                        tag,
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        request
                );
    }


    //badge unlock logic
    private void checkAndUnlockBadges(int currentStreak, int totalTasksCompletedThisWeek, int totalTasksCompletedOverall) {

        if (currentUser == null) return;
        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (currentStreak >= 3) unlockBadge(db, uid, "focus_master");

        if (currentStreak >= 5) unlockBadge(db, uid, "5_day_streak");

        if (currentStreak >= 7) unlockBadge(db, uid, "perfect_week");

        if (totalTasksCompletedOverall >= 50) unlockBadge(db, uid, "task_master");

        if (totalTasksCompletedOverall >= 100) unlockBadge(db, uid, "productivity_pro");

        checkWeekendWarriorUnlock();
        checkEarlyBirdUnlock();
        checkNightOwlUnlock();
    }

    private void unlockBadge(FirebaseFirestore db, String uid, String badgeId) {

        Map<String, Object> update = new HashMap<>();
        update.put("unlocked", true);

        db.collection("Users")
                .document(uid)
                .collection("badges")
                .document(badgeId)
                .set(update, com.google.firebase.firestore.SetOptions.merge());
    }

    private void loadTotalCompletedTasksAndUnlockBadges(int currentStreak) {
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        db.collection("Tasks").document(uid).collection("dates").get()
                .addOnSuccessListener(dateSnapshots -> {
                    final int[] totalTasksCompleted = {0};

                    if (dateSnapshots.isEmpty()) {
                        checkAndUnlockBadges(currentStreak, 0, 0);
                        return;
                    }

                    AtomicInteger pendingDates = new AtomicInteger(dateSnapshots.size());

                    for (QueryDocumentSnapshot dateDoc : dateSnapshots) {
                        dateDoc.getReference().collection("items").get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        for (QueryDocumentSnapshot itemDoc : task.getResult()) {
                                            if (itemDoc.getBoolean("completed") != null && itemDoc.getBoolean("completed")) {
                                                totalTasksCompleted[0]++;
                                            }
                                        }
                                    }
                                    if (pendingDates.decrementAndGet() == 0) {
                                        checkAndUnlockBadges(currentStreak, 0, totalTasksCompleted[0]);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    checkAndUnlockBadges(currentStreak, 0, 0);
                });
    }


    private void seedBadgesIfMissing() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Define all badges your app supports
        Map<String, Map<String, Object>> allBadges = new HashMap<>();

        Map<String, Object> badge1 = new HashMap<>();
        badge1.put("title", "5-Day Streak Achiever");
        badge1.put("desc", "Completed all tasks for 5 days");
        badge1.put("unlocked", false);
        badge1.put("icon", "ic_badge_streak");
        allBadges.put("5_day_streak", badge1);

        Map<String, Object> badge2 = new HashMap<>();
        badge2.put("title", "Perfect Week");
        badge2.put("desc", "Completed all tasks for 7 days");
        badge2.put("unlocked", false);
        badge2.put("icon", "ic_badge_week");
        allBadges.put("perfect_week", badge2);

        Map<String, Object> badge3 = new HashMap<>();
        badge3.put("title", "Task Master");
        badge3.put("desc", "Completed 50 tasks");
        badge3.put("unlocked", false);
        badge3.put("icon", "ic_badge_master");
        allBadges.put("task_master", badge3);

        Map<String, Object> earlyBird = new HashMap<>();
        earlyBird.put("title", "Early Bird");
        earlyBird.put("desc", "Completed a task before 9 AM");
        earlyBird.put("unlocked", false);
        earlyBird.put("icon", "ic_badge_early_bird");
        allBadges.put("early_bird", earlyBird);

        Map<String, Object> nightOwl = new HashMap<>();
        nightOwl.put("title", "Night Owl");
        nightOwl.put("desc", "Completed a task after 10 PM");
        nightOwl.put("unlocked", false);
        nightOwl.put("icon", "ic_badge_night_owl");
        allBadges.put("night_owl", nightOwl);

        Map<String, Object> consistency = new HashMap<>();
        consistency.put("title", "Consistency Champ");
        consistency.put("desc", "Completed tasks on 10 different days");
        consistency.put("unlocked", false);
        consistency.put("icon", "ic_badge_consistency");
        allBadges.put("consistency_champ", consistency);

        Map<String, Object> focus = new HashMap<>();
        focus.put("title", "Focus Master");
        focus.put("desc", "Completed all tasks for 3 days in a row");
        focus.put("unlocked", false);
        focus.put("icon", "ic_badge_focus");
        allBadges.put("focus_master", focus);

        Map<String, Object> weekend = new HashMap<>();
        weekend.put("title", "Weekend Warrior");
        weekend.put("desc", "Completed tasks on both Saturday & Sunday");
        weekend.put("unlocked", false);
        weekend.put("icon", "ic_badge_weekend");
        allBadges.put("weekend_warrior", weekend);

        Map<String, Object> pro = new HashMap<>();
        pro.put("title", "Productivity Pro");
        pro.put("desc", "Completed 100 tasks in total");
        pro.put("unlocked", false);
        pro.put("icon", "ic_badge_pro");
        allBadges.put("productivity_pro", pro);

        // Fetch existing badges for this user
        db.collection("Users")
                .document(uid)
                .collection("badges")
                .get()
                .addOnSuccessListener(snapshot -> {

                    // Track which badge IDs already exist
                    Set<String> existingIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        existingIds.add(doc.getId());
                    }

                    // Create only missing ones
                    for (Map.Entry<String, Map<String, Object>> entry : allBadges.entrySet()) {
                        String badgeId = entry.getKey();
                        Map<String, Object> data = entry.getValue();

                        if (!existingIds.contains(badgeId)) {
                            db.collection("Users")
                                    .document(uid)
                                    .collection("badges")
                                    .document(badgeId)
                                    .set(data);
                        }
                    }
                });
    }


    private void checkWeekendWarriorUnlock() {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);

        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            unlockBadge(FirebaseFirestore.getInstance(), currentUser.getUid(), "weekend_warrior");
        }
    }


    private void checkEarlyBirdUnlock() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour < 9) {
            unlockBadge(FirebaseFirestore.getInstance(), currentUser.getUid(), "early_bird");
        }
    }

    private void checkNightOwlUnlock() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour >= 22) {
            unlockBadge(FirebaseFirestore.getInstance(), currentUser.getUid(), "night_owl");
        }
    }
}
