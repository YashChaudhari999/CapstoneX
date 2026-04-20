package com.example.capstonex;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminTopicApprovalActivity extends BaseActivity {

    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";
    /**
     * MASTER list — populated once from Firebase "Groups", NEVER modified by
     * any filter / search operation.  All filtering creates a new sub-list from
     * this collection.
     */
    private final List<DataSnapshot> masterList = new ArrayList<>();
    /**
     * Cache built from the "TopicApprovals" real-time listener.
     * Key   = groupId
     * Value = "pending" | "approved" | "notopics"
     * <p>
     * Used by the chip-filter so we don't need extra Firebase reads per tap.
     */
    private final Map<String, String> topicStatusCache = new HashMap<>();
    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView rvGroupApprovals;
    private EditText etSearchGroups;
    private ImageView ivClearSearch;
    private ChipGroup chipGroupFilter;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptySubtitle;
    private FrameLayout layoutLoading;
    private TextView tvStatTotal, tvStatPending, tvStatApproved, tvStatNoTopics;
    // ── Data ──────────────────────────────────────────────────────────────────
    private DatabaseReference mDatabase;
    private GroupTopicAdapter adapter;

    // ── Filter state ──────────────────────────────────────────────────────────
    /**
     * One of: "all" | "pending" | "approved" | "notopics"
     */
    private String currentFilter = "all";
    private String currentSearch = "";

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_topic_approval);

        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupChipFilters();

        // Start both real-time listeners
        listenToGroups();
        listenToTopicApprovals();
    }

    // ── View wiring ───────────────────────────────────────────────────────────

    private void bindViews() {
        rvGroupApprovals = findViewById(R.id.rvGroupApprovals);
        etSearchGroups = findViewById(R.id.etSearchGroups);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        layoutLoading = findViewById(R.id.layoutLoading);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatApproved = findViewById(R.id.tvStatApproved);
        tvStatNoTopics = findViewById(R.id.tvStatNoTopics);
    }

    private void setupToolbar() {
        // Back arrow click → finish (mirrors your other activities)
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new GroupTopicAdapter(mDatabase);
        rvGroupApprovals.setLayoutManager(new LinearLayoutManager(this));
        rvGroupApprovals.setAdapter(adapter);
        rvGroupApprovals.setHasFixedSize(false);
    }

    private void setupSearch() {
        etSearchGroups.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                refreshStatsAndFilter();
            }
        });

        ivClearSearch.setOnClickListener(v -> {
            etSearchGroups.setText("");
            // TextWatcher will handle the rest
        });
    }

    private void setupChipFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return; // selectionRequired keeps this safe

            int id = checkedIds.get(0);
            if (id == R.id.chipPending) currentFilter = "pending";
            else if (id == R.id.chipApproved) currentFilter = "approved";
            else if (id == R.id.chipNoTopics) currentFilter = "notopics";
            else currentFilter = "all";

            refreshStatsAndFilter();
        });
    }

    // ── Firebase listeners ────────────────────────────────────────────────────

    /**
     * Listens to "Groups" node.
     * Rebuilds masterList on every change and triggers a full refresh.
     */
    private void listenToGroups() {
        showLoading(true);
        mDatabase.child("Groups").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    masterList.add(ds);
                }
                refreshStatsAndFilter();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
            }
        });
    }

    /**
     * Listens to "TopicApprovals" node.
     * Rebuilds topicStatusCache on every change so chip-filters are always
     * accurate without additional Firebase reads.
     * <p>
     * Status rules:
     * • "approved"  → approvedTopic child exists
     * • "pending"   → topics exist but no approvedTopic yet
     * • "notopics"  → no topics submitted at all
     */
    private void listenToTopicApprovals() {
        mDatabase.child("TopicApprovals").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                topicStatusCache.clear();
                for (DataSnapshot approvalSnap : snapshot.getChildren()) {
                    String gid = approvalSnap.getKey();
                    if (gid == null) continue;

                    boolean hasApproved = approvalSnap.child("approvedTopic").exists();
                    boolean hasTopics = approvalSnap.child("submittedTopics").exists()
                            || approvalSnap.child("topic1").exists();

                    if (hasApproved) topicStatusCache.put(gid, "approved");
                    else if (hasTopics) topicStatusCache.put(gid, "pending");
                    else topicStatusCache.put(gid, "notopics");
                }
                refreshStatsAndFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // ── Core filter + stats logic ─────────────────────────────────────────────

    /**
     * Single method called whenever masterList OR topicStatusCache OR
     * currentFilter OR currentSearch changes.
     * <p>
     * 1. Computes stats from masterList + cache.
     * 2. Builds a filtered sub-list (never touches masterList).
     * 3. Pushes the sub-list to the adapter.
     * 4. Toggles empty-state visibility.
     */
    private void refreshStatsAndFilter() {
        // ── 1. Compute stats ──────────────────────────────────────
        int total = masterList.size();
        int pending = 0;
        int approved = 0;
        int noTopics = 0;

        for (DataSnapshot group : masterList) {
            String gid = group.getKey();
            String status = topicStatusCache.containsKey(gid)
                    ? topicStatusCache.get(gid) : "notopics";

            switch (status) {
                case "approved":
                    approved++;
                    break;
                case "pending":
                    pending++;
                    break;
                default:
                    noTopics++;
                    break;
            }
        }

        tvStatTotal.setText(String.valueOf(total));
        tvStatPending.setText(String.valueOf(pending));
        tvStatApproved.setText(String.valueOf(approved));
        tvStatNoTopics.setText(String.valueOf(noTopics));

        // ── 2. Build filtered list ────────────────────────────────
        String q = currentSearch.trim().toLowerCase();
        List<DataSnapshot> result = new ArrayList<>();

        for (DataSnapshot group : masterList) {
            String gid = group.getKey() == null ? "" : group.getKey();

            // a) Search filter — match group ID (case-insensitive)
            if (!q.isEmpty() && !gid.toLowerCase().contains(q)) {
                continue;
            }

            // b) Chip filter — match cached status
            if (!"all".equals(currentFilter)) {
                String cachedStatus = topicStatusCache.containsKey(gid)
                        ? topicStatusCache.get(gid) : "notopics";
                if (!cachedStatus.equals(currentFilter)) {
                    continue;
                }
            }

            result.add(group);
        }

        // ── 3. Push to adapter ────────────────────────────────────
        adapter.submitList(result);

        // ── 4. Empty state ────────────────────────────────────────
        boolean isEmpty = result.isEmpty();
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvGroupApprovals.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            if (!q.isEmpty()) {
                tvEmptySubtitle.setText("No groups match \"" + currentSearch + "\"");
            } else if (!"all".equals(currentFilter)) {
                tvEmptySubtitle.setText("No groups with status \"" + currentFilter + "\"");
            } else {
                tvEmptySubtitle.setText("No groups available yet");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showLoading(boolean show) {
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}