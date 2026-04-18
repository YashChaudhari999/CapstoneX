package com.example.capstonex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class LogbookAdapter extends RecyclerView.Adapter<LogbookAdapter.ViewHolder> {

    private final List<LogEntryModel> logList;

    public LogbookAdapter(List<LogEntryModel> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_logbook_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LogEntryModel log = logList.get(position);
        
        holder.tvWeekLabel.setText("LOG (" + log.getFromDate() + " - " + log.getToDate() + ")");
        holder.tvWorkText.setText(log.getWorkDone());
        
        if (log.isSigned()) {
            holder.tvStatusBadge.setText("SIGNED");
            holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.colorAccentGreen));
        } else {
            holder.tvStatusBadge.setText("PENDING");
            holder.tvStatusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.colorAccentOrange));
        }

        // Grade and Remark can be added here if they exist in the model
        holder.tvGrade.setVisibility(View.GONE); 
        holder.tvMentorRemarkLabel.setVisibility(View.GONE);
        holder.tvMentorRemark.setVisibility(View.GONE);
        holder.dividerRemark.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWeekLabel, tvStatusBadge, tvWorkText, tvGrade, tvMentorRemark, tvMentorRemarkLabel;
        View dividerRemark;
        MaterialButton btnSignLog;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWeekLabel = itemView.findViewById(R.id.tvWeekLabel);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvWorkText = itemView.findViewById(R.id.tvWorkText);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvMentorRemarkLabel = itemView.findViewById(R.id.tvMentorRemarkLabel);
            tvMentorRemark = itemView.findViewById(R.id.tvMentorRemark);
            dividerRemark = itemView.findViewById(R.id.dividerRemark);
            btnSignLog = itemView.findViewById(R.id.btnSignLog);
        }
    }
}
