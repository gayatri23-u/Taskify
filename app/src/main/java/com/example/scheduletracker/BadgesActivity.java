package com.example.scheduletracker;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import Model.Badge;

public class BadgesActivity extends AppCompatActivity {

    RecyclerView recyclerBadges;
    BadgeAdapter adapter;
    List<Badge> badges = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badges);

        recyclerBadges = findViewById(R.id.recyclerBadges);
        recyclerBadges.setLayoutManager(new LinearLayoutManager(this));

        //  Adapter init
        adapter = new BadgeAdapter(badges);
        recyclerBadges.setAdapter(adapter);

        //  Load from Firebase
        loadBadgesFromFirebase();
    }

    private void loadBadgesFromFirebase() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .collection("badges")
                .get()
                .addOnSuccessListener(snapshot -> {

                    badges.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {

                        String title = doc.getString("title");
                        String desc = doc.getString("description");
                        Boolean unlocked = doc.getBoolean("unlocked");

                        if (title == null) title = "Badge";
                        if (desc == null) desc = "";
                        if (unlocked == null) unlocked = false;

                        badges.add(new Badge(title, desc, unlocked));
                    }

                    // 🔁 RecyclerView refresh
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load badges", Toast.LENGTH_SHORT).show()
                );
    }
}