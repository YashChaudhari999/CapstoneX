package com.example.capstonex;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the Admin Topic Approval screen.
 * <p>
 * Design decisions:
 * • displayList  - an independent copy supplied via submitList(); never the
 * same reference as the Activity's masterList.
 * • Each ViewHolder attaches a real-time listener on "TopicApprovals/{gid}".
 * The listener is stored on the holder and removed in onViewRecycled() so
 * there are no memory leaks or stale-data races when views are recycled.
 * • approveTopic() and rejectTopic() both write back Groups/{gid}/status so
 * the Activity's topicStatusCache (and therefore the chip-filters) stay in
 * sync automatically via the real-time listener.
 */
public class GroupTopicAdapter extends RecyclerView.Adapter<GroupTopicAdapter.GroupVH> {

    private final DatabaseReference db;
    // The list the adapter currently shows.  Replaced (not mutated) on every
    // submitList() call so there is no aliasing with the Activity's masterList.
    private List<DataSnapshot> displayList = new ArrayList<>();

    public GroupTopicAdapter(DatabaseReference db) {
        this.db = db;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    private static String nvl(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }

    // ── RecyclerView boilerplate ──────────────────────────────────────────────

    /**
     * Replace the displayed items.  Creates a defensive copy so the caller's
     * list and this adapter's list are always independent.
     */
    public void submitList(List<DataSnapshot> newList) {
        displayList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_group_topic, parent, false);
        return new GroupVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupVH holder, int position) {
        DataSnapshot group = displayList.get(position);
        String groupId = group.getKey();

        // ── Label ─────────────────────────────────────────────────
        holder.tvGroupId.setText("Group: " + groupId);

        // ── Remove any listener left over from a previous binding ─
        removeListenerFromHolder(holder);

        // ── Attach a fresh real-time listener for this group's topics ─
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Guard: holder may have been rebound to a different group
                // before this callback fires — discard stale deliveries.
                if (!groupId.equals(holder.activeGroupId)) return;
                populateCard(holder, snapshot, groupId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        holder.activeGroupId = groupId;
        holder.activeListener = listener;
        db.child("TopicApprovals").child(groupId).addValueEventListener(listener);
    }

    /**
     * Called by RecyclerView when a view is returned to the pool.
     * We MUST remove the Firebase listener here to prevent memory leaks
     * and callbacks arriving on recycled views.
     */
    @Override
    public void onViewRecycled(@NonNull GroupVH holder) {
        super.onViewRecycled(holder);
        removeListenerFromHolder(holder);
    }

    // ── Card population ───────────────────────────────────────────────────────

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    private void populateCard(GroupVH holder, DataSnapshot snapshot, String groupId) {
        holder.topicContainer.removeAllViews();

        // ── Collect submitted topics ──────────────────────────────
        List<DataSnapshot> topicList = new ArrayList<>();
        if (snapshot.child("submittedTopics").exists()) {
            for (DataSnapshot t : snapshot.child("submittedTopics").getChildren()) {
                topicList.add(t);
            }
        } else {
            // Flat structure: topic1 / topic2 / topic3
            for (String key : new String[]{"topic1", "topic2", "topic3"}) {
                if (snapshot.child(key).exists()) topicList.add(snapshot.child(key));
            }
        }

        boolean isNested = snapshot.child("submittedTopics").exists();
        String approvedId = "";
        if (snapshot.child("approvedTopic").exists()) {
            approvedId = snapshot.child("approvedTopic").child("topicId")
                    .getValue(String.class);
            if (approvedId == null) approvedId = "";
        }

        // ── Submission timestamp ──────────────────────────────────
        Long ts = snapshot.child("timestamp").getValue(Long.class);
        if (ts != null && ts > 0) {
            String fmt = DateFormat.format("dd MMM yyyy · hh:mm a", new Date(ts)).toString();
            holder.tvSubmittedAt.setText("Submitted: " + fmt);
            holder.tvSubmittedAt.setVisibility(View.VISIBLE);
        } else {
            holder.tvSubmittedAt.setVisibility(View.GONE);
        }

        // ── Group status badge ────────────────────────────────────
        if (topicList.isEmpty()) {
            setBadge(holder, "No Topics", R.color.colorMutedText);
        } else if (!approvedId.isEmpty()) {
            setBadge(holder, "Approved", R.color.colorAccentGreen);
        } else {
            setBadge(holder, "Pending", R.color.colorAccentOrange);
        }

        // ── Empty placeholder ─────────────────────────────────────
        if (topicList.isEmpty()) {
            TextView empty = new TextView(holder.itemView.getContext());
            empty.setText("No topics submitted yet.");
            empty.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.colorMutedText));
            empty.setTextSize(13f);
            empty.setPadding(4, 8, 4, 12);
            holder.topicContainer.addView(empty);
            return;
        }

        // ── Inflate one row per topic ─────────────────────────────
        // Accent colours cycle through topic slots (T1=blue, T2=teal, T3=orange)
        int[] badgeColors = {
                R.color.colorAccentBlue,
                R.color.colorAccentTeal,
                R.color.colorAccentOrange
        };

        for (int i = 0; i < topicList.size(); i++) {
            DataSnapshot t = topicList.get(i);
            String tid = t.getKey();
            String tTitle = nvl(t.child("title").getValue(String.class), "(No title)");
            String tDesc = nvl(t.child("description").getValue(String.class), "");
            String tDomain = t.child("domain").getValue(String.class);
            String tStatus = nvl(t.child("status").getValue(String.class), "");

            View row = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.item_topic_row, holder.topicContainer, false);

            // Widget refs
            MaterialCardView cardNum = row.findViewById(R.id.cardTopicNum);
            TextView tvNum = row.findViewById(R.id.tvTopicNum);
            TextView tvTitle = row.findViewById(R.id.tvTopicTitle);
            TextView tvDesc = row.findViewById(R.id.tvTopicDesc);
            TextView tvDomain = row.findViewById(R.id.tvDomainBadge);
            MaterialCardView cardStatus = row.findViewById(R.id.cardTopicStatus);
            TextView tvStatus = row.findViewById(R.id.tvTopicStatus);
            MaterialButton btnApprove = row.findViewById(R.id.btnApprove);
            MaterialButton btnReject = row.findViewById(R.id.btnReject);

            // Topic number badge
            tvNum.setText("T" + (i + 1));
            cardNum.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(),
                            badgeColors[i % badgeColors.length]));

            tvTitle.setText(tTitle);
            tvDesc.setText(tDesc);

            // Domain pill
            if (tDomain != null && !tDomain.trim().isEmpty()) {
                tvDomain.setText(tDomain.trim());
                tvDomain.setVisibility(View.VISIBLE);
            } else {
                tvDomain.setVisibility(View.GONE);
            }

            // ── Button states ─────────────────────────────────────
            boolean isThisApproved = !approvedId.isEmpty() && approvedId.equals(tid);
            boolean isRejected = "rejected".equalsIgnoreCase(tStatus);
            boolean anyApproved = !approvedId.isEmpty(); // another topic approved

            if (isThisApproved) {
                // ── APPROVED ──────────────────────────────────────
                applyApprovedState(holder.itemView.getContext(),
                        btnApprove, btnReject, cardStatus, tvStatus);

            } else if (isRejected || anyApproved) {
                // ── REJECTED / LOCKED (another was approved) ──────
                applyRejectedState(holder.itemView.getContext(),
                        btnApprove, btnReject, cardStatus, tvStatus,
                        isRejected ? "Rejected" : "Not Approved");

            } else {
                // ── PENDING — wire up live buttons ─────────────────
                applyPendingState(btnApprove, btnReject, cardStatus);

                final String finalApprovedId = approvedId; // effectively final
                btnApprove.setOnClickListener(v ->
                        approveTopic(groupId, tid, tTitle, snapshot, topicList, isNested));

                btnReject.setOnClickListener(v ->
                        rejectTopic(groupId, tid, snapshot, topicList, isNested));
            }

            holder.topicContainer.addView(row);
        }
    }

    private void applyApprovedState(android.content.Context ctx,
                                    MaterialButton btnApprove, MaterialButton btnReject,
                                    MaterialCardView cardStatus, TextView tvStatus) {

        btnApprove.setText("Approved ✓");
        btnApprove.setEnabled(false);
        btnApprove.setBackgroundColor(
                ContextCompat.getColor(ctx, R.color.colorAccentGreen));
        btnReject.setVisibility(View.GONE);
        cardStatus.setCardBackgroundColor(
                ContextCompat.getColor(ctx, R.color.colorAccentGreen));
        tvStatus.setText("Approved");
        cardStatus.setVisibility(View.VISIBLE);
    }

    private void applyRejectedState(android.content.Context ctx,
                                    MaterialButton btnApprove, MaterialButton btnReject,
                                    MaterialCardView cardStatus, TextView tvStatus, String label) {

        btnApprove.setText("Approve");
        btnApprove.setEnabled(false);
        btnApprove.setBackgroundColor(
                ContextCompat.getColor(ctx, R.color.colorMutedText));
        btnReject.setText("Rejected ✗");
        btnReject.setEnabled(false);
        btnReject.setVisibility(View.VISIBLE);
        cardStatus.setCardBackgroundColor(
                ContextCompat.getColor(ctx, R.color.colorAccentRed));
        tvStatus.setText(label);
        cardStatus.setVisibility(View.VISIBLE);
    }

    // ── Firebase writes ───────────────────────────────────────────────────────

    private void applyPendingState(MaterialButton btnApprove,
                                   MaterialButton btnReject, MaterialCardView cardStatus) {

        btnApprove.setText("Approve");
        btnApprove.setEnabled(true);
        btnReject.setText("Reject");
        btnReject.setEnabled(true);
        btnReject.setVisibility(View.VISIBLE);
        cardStatus.setVisibility(View.GONE);
    }

    /**
     * Approves one topic and rejects all others in a single atomic batch write.
     * Also updates Groups/{gid}/status so the Activity's chip-filter cache
     * updates immediately via the real-time listener.
     */
    private void approveTopic(String gid, String tid, String title,
                              DataSnapshot rootSnap, List<DataSnapshot> topicList, boolean isNested) {

        Map<String, Object> updates = new HashMap<>();

        // Write approvedTopic node
        Map<String, Object> approvedData = new HashMap<>();
        approvedData.put("topicId", tid);
        approvedData.put("title", title);
        approvedData.put("approvedAt", ServerValue.TIMESTAMP);
        approvedData.put("approvedBy", FirebaseAuth.getInstance().getUid());
        updates.put("/TopicApprovals/" + gid + "/approvedTopic", approvedData);

        // Mark every topic approved or rejected
        for (DataSnapshot t : topicList) {
            String status = t.getKey().equals(tid) ? "approved" : "rejected";
            String path = isNested
                    ? "/TopicApprovals/" + gid + "/submittedTopics/" + t.getKey() + "/status"
                    : "/TopicApprovals/" + gid + "/" + t.getKey() + "/status";
            updates.put(path, status);
        }

        // Reflect decision in the Groups node (drives chip-filter cache)
        updates.put("/Groups/" + gid + "/status", "Topic Approved");

        db.updateChildren(updates);
    }

    // ── Listener cleanup helper ───────────────────────────────────────────────

    /**
     * Rejects a single topic without locking the others.
     * If ALL topics end up rejected, sets Groups/{gid}/status accordingly.
     */
    private void rejectTopic(String gid, String tid,
                             DataSnapshot rootSnap, List<DataSnapshot> topicList, boolean isNested) {

        Map<String, Object> updates = new HashMap<>();

        // Reject this specific topic
        String path = isNested
                ? "/TopicApprovals/" + gid + "/submittedTopics/" + tid + "/status"
                : "/TopicApprovals/" + gid + "/" + tid + "/status";
        updates.put(path, "rejected");

        // Check whether every topic will now be rejected
        boolean allWillBeRejected = true;
        for (DataSnapshot t : topicList) {
            if (t.getKey().equals(tid)) continue; // this one is being rejected now
            String s = nvl(t.child("status").getValue(String.class), "");
            if (!"rejected".equalsIgnoreCase(s)) {
                allWillBeRejected = false;
                break;
            }
        }

        if (allWillBeRejected) {
            updates.put("/Groups/" + gid + "/status", "All Topics Rejected");
        }

        db.updateChildren(updates);
    }

    // ── Tiny null-safety helper ───────────────────────────────────────────────

    private void removeListenerFromHolder(GroupVH holder) {
        if (holder.activeListener != null && holder.activeGroupId != null) {
            db.child("TopicApprovals").child(holder.activeGroupId)
                    .removeEventListener(holder.activeListener);
            holder.activeListener = null;
            holder.activeGroupId = null;
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    private void setBadge(GroupVH holder, String text, int colorRes) {
        holder.tvStatusBadge.setText(text);
        holder.cardStatusBadge.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), colorRes));
    }

    // ── Group-level badge helper ──────────────────────────────────────────────

    static class GroupVH extends RecyclerView.ViewHolder {
        final TextView tvGroupId;
        final TextView tvSubmittedAt;
        final TextView tvStatusBadge;
        final MaterialCardView cardStatusBadge;
        final LinearLayout topicContainer;

        // Live listener tracking — used for cleanup on recycle
        ValueEventListener activeListener = null;
        String activeGroupId = null;

        GroupVH(View v) {
            super(v);
            tvGroupId = v.findViewById(R.id.tvGroupId);
            tvSubmittedAt = v.findViewById(R.id.tvSubmittedAt);
            tvStatusBadge = v.findViewById(R.id.tvStatusBadge);
            cardStatusBadge = v.findViewById(R.id.cardStatusBadge);
            topicContainer = v.findViewById(R.id.topicContainer);
        }
    }
}