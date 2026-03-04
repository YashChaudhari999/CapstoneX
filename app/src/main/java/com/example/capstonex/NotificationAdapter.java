package com.example.capstonex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationModel> notificationList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationModel notification);
    }

    public NotificationAdapter(List<NotificationModel> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notif = notificationList.get(position);
        holder.tvTitle.setText(notif.getTitle());
        holder.tvMessage.setText(notif.getMessage());
        
        if (notif.getTimestamp() != null) {
            SimpleDateFormat sfd = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
            holder.tvTime.setText(sfd.format(notif.getTimestamp().toDate()));
        }

        holder.vIndicator.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notif));
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        View vIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            vIndicator = itemView.findViewById(R.id.vReadIndicator);
        }
    }
}
