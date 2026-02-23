package com.example.scheduletracker;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import Model.Badge;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BadgesActivity extends AppCompatActivity {

    RecyclerView recyclerBadges;
    BadgeAdapter badgeAdapter;
    List<Badge> badgeList;

    FirebaseUser currentUser;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badges);

        recyclerBadges = findViewById(R.id.recyclerBadges);

        badgeList = new ArrayList<>();
        badgeAdapter = new BadgeAdapter(badgeList);
        recyclerBadges.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerBadges.setAdapter(badgeAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        loadAllBadges();
    }

    private void loadAllBadges() {
        if (currentUser == null) return;

        db.collection("Users")
                .document(currentUser.getUid())
                .collection("badges")
                .get()
                .addOnSuccessListener(snapshots -> {

                    List<Badge> newBadges = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Badge badge = doc.toObject(Badge.class);
                        newBadges.add(badge);
                    }

                    badgeAdapter.setBadges(newBadges);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load badges", Toast.LENGTH_SHORT).show()
                );
    }
}