package com.example.capstonex;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {

    private List<DocumentModel> documentList;

    public DocumentAdapter(List<DocumentModel> documentList) {
        this.documentList = documentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentModel document = documentList.get(position);
        holder.tvDocTitle.setText(document.getTitle());
        
        if (document.getTimestamp() != null) {
            SimpleDateFormat sfd = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            holder.tvDocInfo.setText("Uploaded: " + sfd.format(document.getTimestamp().toDate()));
        }

        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(document.getDownloadUrl()));
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDocTitle, tvDocInfo;
        MaterialButton btnView;
        ImageView ivDocIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocTitle = itemView.findViewById(R.id.tvDocTitle);
            tvDocInfo = itemView.findViewById(R.id.tvDocInfo);
            btnView = itemView.findViewById(R.id.btnDownloadDoc);
            ivDocIcon = itemView.findViewById(R.id.ivDocIcon);
        }
    }
}
