package com.example.capstonex;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RubricAdapter extends RecyclerView.Adapter<RubricAdapter.ViewHolder> {

    private List<RubricModel> rubricList;
    private OnRubricDeleteListener listener;

    public interface OnRubricDeleteListener {
        void onDelete(RubricModel rubric);
    }

    public RubricAdapter(List<RubricModel> rubricList, OnRubricDeleteListener listener) {
        this.rubricList = rubricList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rubric, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RubricModel rubric = rubricList.get(position);
        holder.tvCriteria.setText(rubric.getCriteria());
        holder.tvMaxMarks.setText("Max Marks: " + rubric.getMaxMarks());
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(rubric));
    }

    @Override
    public int getItemCount() {
        return rubricList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCriteria, tvMaxMarks;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCriteria = itemView.findViewById(R.id.tvRubricCriteria);
            tvMaxMarks = itemView.findViewById(R.id.tvRubricMaxMarks);
            btnDelete = itemView.findViewById(R.id.btnDeleteRubric);
        }
    }
}
