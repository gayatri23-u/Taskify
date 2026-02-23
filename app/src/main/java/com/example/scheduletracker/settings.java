package com.example.scheduletracker;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class settings extends AppCompatActivity {

    MaterialToolbar toolbarSettings;
    SwitchMaterial switchDarkMode, switchReminders;

    LinearLayout rowMorningReminder, rowNightReminder;
    TextView txtMorningTime, txtNightTime;

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbarSettings = findViewById(R.id.toolbarSettings);
        toolbarSettings.setNavigationOnClickListener(v -> finish());

        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchReminders = findViewById(R.id.switchReminders);

        rowMorningReminder = findViewById(R.id.rowMorningReminder);
        rowNightReminder = findViewById(R.id.rowNightReminder);

        txtMorningTime = findViewById(R.id.txtMorningTime);
        txtNightTime = findViewById(R.id.txtNightTime);

        prefs = getSharedPreferences("taskify_prefs", MODE_PRIVATE);

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
                WorkManager.getInstance(this).cancelAllWorkByTag("morning_reminder");
                WorkManager.getInstance(this).cancelAllWorkByTag("night_reminder");
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

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(workerClass)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .addTag(tag)
                        .build();

        WorkManager.getInstance(this).enqueue(request);
    }
}