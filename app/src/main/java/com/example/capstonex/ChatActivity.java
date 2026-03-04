package com.example.capstonex;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends BaseActivity {

    private RecyclerView rvMessages;
    private ChatAdapter adapter;
    private List<MessageModel> messageList;
    private EditText etMessage;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String chatRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setupEdgeToEdge(findViewById(R.id.chat_root));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        // Simple chat room ID generation logic
        chatRoomId = "default_room"; 

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etChatMessage);
        
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        findViewById(R.id.btnSendMessage).setOnClickListener(v -> sendMessage());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        listenForMessages();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        MessageModel msg = new MessageModel(mAuth.getUid(), text, Timestamp.now());
        db.collection("chats").document(chatRoomId).collection("messages")
                .add(msg)
                .addOnSuccessListener(documentReference -> etMessage.setText(""));
    }

    private void listenForMessages() {
        db.collection("chats").document(chatRoomId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            MessageModel model = doc.toObject(MessageModel.class);
                            model.setId(doc.getId());
                            messageList.add(model);
                        }
                        adapter.notifyDataSetChanged();
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }
}
