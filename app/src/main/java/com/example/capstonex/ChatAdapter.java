package com.example.capstonex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    
    private List<MessageModel> messageList;
    private String currentUid;

    public ChatAdapter(List<MessageModel> messageList) {
        this.messageList = messageList;
        this.currentUid = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).getSenderId().equals(currentUid)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel msg = messageList.get(position);
        SimpleDateFormat sfd = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = msg.getTimestamp() != null ? sfd.format(msg.getTimestamp().toDate()) : "";

        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).tvMsg.setText(msg.getMessage());
            ((SentViewHolder) holder).tvTime.setText(time);
        } else {
            ((ReceivedViewHolder) holder).tvMsg.setText(msg.getMessage());
            ((ReceivedViewHolder) holder).tvTime.setText(time);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMsg, tvTime;
        SentViewHolder(View v) {
            super(v);
            tvMsg = v.findViewById(R.id.tvMessageSent);
            tvTime = v.findViewById(R.id.tvTimeSent);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMsg, tvTime;
        ReceivedViewHolder(View v) {
            super(v);
            tvMsg = v.findViewById(R.id.tvMessageReceived);
            tvTime = v.findViewById(R.id.tvTimeReceived);
        }
    }
}
