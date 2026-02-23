package com.example.scheduletracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class splash_screen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Splash screen delay (e.g. 5 seconds)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {  //user already logged in
                startActivity(new Intent(splash_screen.this, dashboard.class));
            } else {
                startActivity(new Intent(splash_screen.this, login.class));   //user not logged in
            }
            finish();

        }, 2000);  //delay of 5 sec
    }
}