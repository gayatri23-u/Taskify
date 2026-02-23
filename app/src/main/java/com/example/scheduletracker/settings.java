package com.example.scheduletracker;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class settings extends AppCompatActivity {

    MaterialToolbar toolbarSettings;
    SwitchMaterial switchDarkMode, switchReminders;

    LinearLayout rowMorningReminder, rowNightReminder;
    TextView txtMorningTime, txtNightTime;
    LinearLayout rowProfile, rowEditProfile, rowChangePassword, rowDeleteAccount;



    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbarSettings = findViewById(R.id.toolbarSettings);
        toolbarSettings.setNavigationOnClickListener(v -> finish());

        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchReminders = findViewById(R.id.switchReminders);

        rowProfile = findViewById(R.id.rowProfile);
        rowEditProfile = findViewById(R.id.rowEditProfile);
        rowChangePassword = findViewById(R.id.rowChangePassword);
        rowDeleteAccount = findViewById(R.id.rowDeleteAccount);

        rowMorningReminder = findViewById(R.id.rowMorningReminder);
        rowNightReminder = findViewById(R.id.rowNightReminder);

        txtMorningTime = findViewById(R.id.txtMorningTime);
        txtNightTime = findViewById(R.id.txtNightTime);

        prefs = getSharedPreferences("taskify_prefs", MODE_PRIVATE);

        //onclick listeners

        //open profile page
        rowProfile.setOnClickListener(v ->
                startActivity(new Intent(settings.this, profile.class))
        );

        //open edit profile page
        rowEditProfile.setOnClickListener(v ->
                startActivity(new Intent(settings.this, edit_profile.class))
        );

        //open change password page
        rowChangePassword.setOnClickListener(v ->
                startActivity(new Intent(settings.this, change_password.class))
        );

        //open delete account page
        rowDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());



        // Load saved values
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        boolean remindersEnabled = prefs.getBoolean("reminders_enabled", true);

        int morningHour = prefs.getInt("morning_hour", 8);
        int morningMinute = prefs.getInt("morning_minute", 0);

        int nightHour = prefs.getInt("night_hour", 21);
        int nightMinute = prefs.getInt("night_minute", 30);

        switchDarkMode.setChecked(darkMode);
        switchReminders.setChecked(remindersEnabled);

        txtMorningTime.setText(formatTime(morningHour, morningMinute));
        txtNightTime.setText(formatTime(nightHour, nightMinute));

        // Dark Mode toggle
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Reminders ON/OFF
        switchReminders.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("reminders_enabled", isChecked).apply();
            if (isChecked) {
                scheduleReminders();
            } else {
                WorkManager.getInstance(this).cancelUniqueWork("morning_reminder");
                WorkManager.getInstance(this).cancelUniqueWork("night_reminder");
            }
        });

        // Morning time picker
        rowMorningReminder.setOnClickListener(v ->
                showTimePicker(true)
        );

        // Night time picker
        rowNightReminder.setOnClickListener(v ->
                showTimePicker(false)
        );
    }

    private void showTimePicker(boolean isMorning) {
        int hour = prefs.getInt(isMorning ? "morning_hour" : "night_hour", isMorning ? 8 : 21);
        int minute = prefs.getInt(isMorning ? "morning_minute" : "night_minute", isMorning ? 0 : 30);

        new TimePickerDialog(this, (view, h, m) -> {

            if (isMorning) {
                prefs.edit()
                        .putInt("morning_hour", h)
                        .putInt("morning_minute", m)
                        .apply();
                txtMorningTime.setText(formatTime(h, m));
            } else {
                prefs.edit()
                        .putInt("night_hour", h)
                        .putInt("night_minute", m)
                        .apply();
                txtNightTime.setText(formatTime(h, m));
            }

            if (switchReminders.isChecked()) {
                scheduleReminders(); // reschedule with new time
            }

        }, hour, minute, false).show();
    }

    //delete diAlog box
    private void showDeleteAccountDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Account?")
                .setMessage("This will permanently delete your account and all data. This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    //delete user acc
    private void deleteUserAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // Delete user data from Firestore (optional but recommended)
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .delete();

        FirebaseFirestore.getInstance()
                .collection("Tasks")
                .document(uid)
                .delete();

        //  Delete auth account
        user.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(settings.this, login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Re-login required to delete account", Toast.LENGTH_LONG).show();
                });
    }

    private String formatTime(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.getTime());
    }

    private void scheduleReminders() {
        int morningHour = prefs.getInt("morning_hour", 8);
        int morningMinute = prefs.getInt("morning_minute", 0);

        int nightHour = prefs.getInt("night_hour", 21);
        int nightMinute = prefs.getInt("night_minute", 30);

        scheduleWorkerAtTime(MorningReminderWorker.class, morningHour, morningMinute, "morning_reminder");
        scheduleWorkerAtTime(NightReminderWorker.class, nightHour, nightMinute, "night_reminder");
    }

    private void scheduleWorkerAtTime(Class<? extends androidx.work.Worker> workerClass,
                                      int hour, int minute, String tag) {

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        long delay = target.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(workerClass, 24, TimeUnit.HOURS)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag(tag)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.REPLACE, request);
    }
}
