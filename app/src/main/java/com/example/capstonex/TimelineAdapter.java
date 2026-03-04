package com.example.capstonex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    private List<TimelineModel> timelineList;

    public TimelineAdapter(List<TimelineModel> timelineList) {
        this.timelineList = timelineList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimelineModel model = timelineList.get(position);
        holder.tvTitle.setText(model.getTitle());
        holder.tvDate.setText("Due: " + model.getDueDate());
        holder.tvStatus.setText(model.getStatus().toUpperCase());
        holder.pbProgress.setProgress(model.getProgress());

        if ("Completed".equalsIgnoreCase(model.getStatus())) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorAccentGreen));
        } else if ("Overdue".equalsIgnoreCase(model.getStatus())) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorAccentRed));
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorAccentOrange));
        }
    }

    @Override
    public int getItemCount() {
        return timelineList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus, tvDate;
        ProgressBar pbProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTimelineTitle);
            tvStatus = itemView.findViewById(R.id.tvTimelineStatus);
            tvDate = itemView.findViewById(R.id.tvTimelineDate);
            pbProgress = itemView.findViewById(R.id.pbTimeline);
        }
    }
}
