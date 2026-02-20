package com.example.scheduletracker;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class dashboard extends AppCompatActivity {

    GridLayout gridCalendar;
    TextView txtMonthYear, txtStreakCount, txtTodayProgress, txtMonthlyCompletion;
    ImageView btnPrevMonth, btnNextMonth;
    CardView cardAddTasks, cardViewTasks;

    Calendar currentCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbarDashboard);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> { });
        }

        // Calendar views
        gridCalendar = findViewById(R.id.gridCalendar);
        txtMonthYear = findViewById(R.id.txtMonthYear);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        // Stats
        txtStreakCount = findViewById(R.id.txtStreakCount);
        txtTodayProgress = findViewById(R.id.txtTodayProgress);
        txtMonthlyCompletion = findViewById(R.id.txtMonthlyCompletion);

        // Action cards
        cardAddTasks = findViewById(R.id.cardAddTasks);
        cardViewTasks = findViewById(R.id.cardViewTasks);

        renderCalendar();

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            renderCalendar();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            renderCalendar();
        });

        cardAddTasks.setOnClickListener(v -> {
            startActivity(new Intent(dashboard.this, add_task.class));
        });

        cardViewTasks.setOnClickListener(v -> {
            startActivity(new Intent(dashboard.this, view_task.class));
        });

        // TEMP dashboard stats (later connect to Firebase)
        txtStreakCount.setText("7 Days");
        txtTodayProgress.setText("5 / 8 Tasks Done");
        txtMonthlyCompletion.setText("68%");
    }

    private void renderCalendar() {
        gridCalendar.removeAllViews();

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        txtMonthYear.setText(sdf.format(currentCalendar.getTime()));

        Calendar tempCal = (Calendar) currentCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            addEmptyCell();
        }

        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);

        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday =
                    day == todayDay &&
                            currentCalendar.get(Calendar.MONTH) == todayMonth &&
                            currentCalendar.get(Calendar.YEAR) == todayYear;

            int totalTasks = 8;   // demo
            int completedTasks = (day % 2 == 0) ? 8 : 3;

            addDayCell(day, isToday, totalTasks, completedTasks);
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
        params.height = dpToPx(40);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        cell.setLayoutParams(params);

        if (isToday) {
            cell.setBackgroundResource(R.drawable.bg_day_today);
        } else if (totalTasks == 0) {
            cell.setBackgroundResource(R.drawable.bg_day_inactive);
        } else if (completedTasks == totalTasks) {
            cell.setBackgroundResource(R.drawable.bg_day_success);
        } else {
            cell.setBackgroundResource(R.drawable.bg_day_failed);
        }

        TextView tv = new TextView(this);
        tv.setText(String.valueOf(day));
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(12);

        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        tv.setLayoutParams(tvParams);

        cell.addView(tv);
        gridCalendar.addView(cell);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Not logged in → send to Login
            startActivity(new Intent(dashboard.this, login.class));
            finish();
        }
    }
}