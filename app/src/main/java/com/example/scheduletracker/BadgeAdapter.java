package com.example.scheduletracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import Model.Badge;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeVH> {

    private List<Badge> badges;

    public BadgeAdapter(List<Badge> badges) {
        this.badges = badges;
    }

    @NonNull
    @Override
    public BadgeVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new BadgeVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeVH holder, int position) {
        Badge badge = badges.get(position);

        holder.txtTitle.setText(badge.getTitle());
        holder.txtDesc.setText(badge.getDesc());

        if (badge.isUnlocked()) {
            holder.txtStatus.setText("Unlocked");
            holder.txtStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.green)
            );
            holder.imgBadge.setAlpha(1f);
        } else {
            holder.txtStatus.setText("Locked 🔒");
            holder.txtStatus.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.gray)
            );
            holder.imgBadge.setAlpha(0.4f);
        }
    }

    @Override
    public int getItemCount() {
        return badges == null ? 0 : badges.size();
    }

    static class BadgeVH extends RecyclerView.ViewHolder {
        ImageView imgBadge;
        TextView txtTitle, txtDesc, txtStatus;

        public BadgeVH(@NonNull View itemView) {
            super(itemView);
            imgBadge = itemView.findViewById(R.id.imgBadge);
            txtTitle = itemView.findViewById(R.id.txtBadgeTitle);
            txtDesc = itemView.findViewById(R.id.txtBadgeDesc);
            txtStatus = itemView.findViewById(R.id.txtBadgeStatus);
        }
    }

    // ✅ Firebase ke baad list update karne ke liye
    public void setBadges(List<Badge> newBadges) {
        this.badges = newBadges;
        notifyDataSetChanged();
    }
}