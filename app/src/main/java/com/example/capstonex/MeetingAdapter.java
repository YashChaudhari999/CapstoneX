package com.example.capstonex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.ViewHolder> {

    private List<MeetingModel> meetingList;
    private OnMeetingClickListener listener;

    public interface OnMeetingClickListener {
        void onJoinClick(MeetingModel meeting);
    }

    public MeetingAdapter(List<MeetingModel> meetingList, OnMeetingClickListener listener) {
        this.meetingList = meetingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meeting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeetingModel meeting = meetingList.get(position);
        holder.tvTitle.setText(meeting.getTitle());
        
        if (meeting.getDateTime() != null) {
            SimpleDateFormat sfd = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
            holder.tvDateTime.setText(sfd.format(meeting.getDateTime().toDate()));
        }

        holder.btnJoin.setOnClickListener(v -> listener.onJoinClick(meeting));
    }

    @Override
    public int getItemCount() {
        return meetingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDateTime;
        MaterialButton btnJoin;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvMeetingTitle);
            tvDateTime = itemView.findViewById(R.id.tvMeetingDateTime);
            btnJoin = itemView.findViewById(R.id.btnJoinMeeting);
        }
    }
}
