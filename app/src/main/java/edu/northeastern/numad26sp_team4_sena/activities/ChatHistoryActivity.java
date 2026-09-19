package edu.northeastern.numad26sp_team4_sena.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.adapters.ChatMessageAdapter;
import edu.northeastern.numad26sp_team4_sena.models.Message;

public class ChatHistoryActivity extends AppCompatActivity {

    private String sessionId;
    private String currentUserId;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;
    private RecyclerView rvMessages;
    private ChatMessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            showError("Not signed in");
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        sessionId = getIntent().getStringExtra("sessionId");
        String partnerName = getIntent().getStringExtra("partnerName");
        String partnerImage = getIntent().getStringExtra("partnerImage");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (!validateSession(sessionId)) {
            finish();
            return;
        }

        setupHeader(partnerName, partnerImage);
        setupRecyclerView();

        loadMessages();
    }

    private boolean validateSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            showError("Invalid chat session");
            return false;
        }
        return true;
    }

    private void setupHeader(String partnerName, String partnerImage) {
        TextView tvPartnerName = findViewById(R.id.tvPartnerName);
        CircleImageView ivAvatar = findViewById(R.id.ivAvatar);

        String displayName = (partnerName != null && !partnerName.isEmpty())
                ? partnerName
                : getString(R.string.chat_anonymous_name);
        tvPartnerName.setText(displayName);

        if (partnerImage != null && !partnerImage.isEmpty()) {
            Picasso.get()
                    .load(partnerImage)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivAvatar);
        }
    }

    private void setupRecyclerView() {
        progressBar = findViewById(R.id.progressBar);
        rvMessages = findViewById(R.id.rvMessages);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        adapter = new ChatMessageAdapter(currentUserId);
        rvMessages.setAdapter(adapter);
    }

    private void loadMessages() {
        showProgress(true);

        firestore.collection("chats")
                .document(sessionId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    showProgress(false);

                    List<Message> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) {
                            msg.setMessageId(doc.getId());
                            messages.add(msg);
                        }
                    }

                    if (messages.isEmpty()) {
                        showInfo("No messages in this conversation");
                    } else {
                        adapter.setMessages(messages);
                        rvMessages.post(() -> rvMessages.smoothScrollToPosition(messages.size() - 1));
                    }
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    showError("Failed to load messages: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                });
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showInfo(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
