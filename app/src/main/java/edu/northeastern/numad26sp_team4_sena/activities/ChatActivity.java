package edu.northeastern.numad26sp_team4_sena.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.adapters.ChatMessageAdapter;
import edu.northeastern.numad26sp_team4_sena.models.Message;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "sessionId";
    private static final int MAX_MESSAGE_LENGTH = 100;
    private static final int WARN_CHARACTER_THRESHOLD = 80;

    private RecyclerView rvMessages;
    private EditText etMessage;
    private TextView tvCharCounter;
    private TextView tvMatchBanner;
    private ImageButton btnSend;


    private ChatMessageAdapter adapter;
    private String sessionId;
    private String currentUserId;
    private String partnerUid;
    private boolean isSending = false;


    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private ListenerRegistration messagesListener;
    private ListenerRegistration sessionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (!initializeFirebase()) {
            finish();
            return;
        }

        extractIntentData();
        if (!validateSessionData()) {
            finish();
            return;
        }

        setupUI();
        loadSessionAndPartner();
    }

    private boolean initializeFirebase() {
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.not_signed_in), Toast.LENGTH_SHORT).show();
            return false;
        }
        currentUserId = currentUser.getUid();
        return true;
    }

    private void extractIntentData() {
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
    }

    private boolean validateSessionData() {
        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_session), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setupUI() {
        setupRecyclerView();
        setupInputArea();
        setupBackButton();
    }

    private void setupRecyclerView() {
        rvMessages = findViewById(R.id.rvMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        adapter = new ChatMessageAdapter(currentUserId);
        rvMessages.setAdapter(adapter);
    }

    private void setupInputArea() {
        etMessage = findViewById(R.id.etMessage);
        tvCharCounter = findViewById(R.id.tvCharCounter);
        btnSend = findViewById(R.id.btnSend);
        tvMatchBanner = findViewById(R.id.tvMatchBanner);

        etMessage.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(MAX_MESSAGE_LENGTH)
        });

        tvCharCounter.setText("0/" + MAX_MESSAGE_LENGTH);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvCharCounter.setText(length + "/" + MAX_MESSAGE_LENGTH);

                if (length >= WARN_CHARACTER_THRESHOLD) {
                    tvCharCounter.setTextColor(getColor(R.color.error));
                } else {
                    tvCharCounter.setTextColor(getColor(R.color.text_hint));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void setupBackButton() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void loadSessionAndPartner() {
        firestore.collection("linkupSessions").document(sessionId).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) {
                        finish();
                        return;
                    }

                    String user1 = snap.getString("user1Uid");
                    String user2 = snap.getString("user2Uid");

                    if (user1 == null || user2 == null) {
                        finish();
                        return;
                    }

                    partnerUid = currentUserId.equals(user1) ? user2 : user1;

                    tvMatchBanner.setVisibility(View.VISIBLE);
                    loadPartnerProfile(partnerUid);
                    listenForMessages();
                    listenForSessionEnd();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadPartnerProfile(String uid) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) return;

                    String name = snap.getString("fullName");
                    String username = snap.getString("username");
                    String imageUrl = snap.getString("profileImageUrl");

                    TextView tvName = findViewById(R.id.tvPartnerName);
                    String displayName = (name != null && !name.isEmpty()) ? name : "@" + (username != null ? username : "Friend");
                    tvName.setText(displayName);

                    String bannerText = getString(R.string.chat_matched_banner_with_name, displayName);
                    tvMatchBanner.setText(bannerText);

                    CircleImageView ivAvatar = findViewById(R.id.ivAvatar);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Picasso.get()
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(ivAvatar);
                    }
                });
    }

    private void listenForMessages() {
        messagesListener = firestore
                .collection("chats")
                .document(sessionId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snapshots == null) {
                        return;
                    }

                    List<Message> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) {
                            msg.setMessageId(doc.getId());
                            messages.add(msg);
                        }
                    }

                    adapter.setMessages(messages);
                    if (!messages.isEmpty()) {
                        rvMessages.post(() -> rvMessages.smoothScrollToPosition(messages.size() - 1));
                    }
                });
    }

    private void listenForSessionEnd() {
        sessionListener = firestore.collection("linkupSessions")
                .document(sessionId)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || !snap.exists()) return;

                    String status = snap.getString("status");
                    if ("ended".equals(status)) {
                        Toast.makeText(ChatActivity.this,
                                getString(R.string.chat_partner_left),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void sendMessage() {
        if (isSending) {
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.network_error_check_connection), Toast.LENGTH_SHORT).show();
            return;
        }

        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, getString(R.string.message_cannot_be_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        isSending = true;
        btnSend.setEnabled(false);

        Message message = new Message(currentUserId, text);

        firestore.collection("chats")
                .document(sessionId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(docRef -> {
                    etMessage.setText("");
                    isSending = false;
                    btnSend.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    isSending = false;
                    btnSend.setEnabled(true);
                    Toast.makeText(ChatActivity.this, getString(R.string.chat_send_failed), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
        if (isFinishing()) {
            endSession();
        }
    }

    @Override
    public void onBackPressed() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.confirm_leave_chat_title))
                .setMessage(getString(R.string.confirm_leave_chat_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void removeListeners() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (sessionListener != null) {
            sessionListener.remove();
            sessionListener = null;
        }
    }

    private void endSession() {
        if (sessionId == null) return;

        firestore.collection("linkupSessions").document(sessionId).get()
                .addOnSuccessListener(snap -> {
                    if (!snap.exists()) return;

                    String sessionStatus = snap.getString("status");
                    if ("ended".equals(sessionStatus)) return;

                    final String user1 = snap.getString("user1Uid");
                    final String user2 = snap.getString("user2Uid");
                    final DocumentSnapshot sessionSnap = snap;

                    firestore.collection("chats")
                            .document(sessionId)
                            .collection("messages")
                            .get()
                            .addOnSuccessListener(messageDocs -> {
                                final int messageCount = messageDocs.size();

                                Long linkedUpAtMs = sessionSnap.getLong("linkedUpAtMs");
                                final long durationSeconds = linkedUpAtMs != null
                                        ? (System.currentTimeMillis() - linkedUpAtMs) / 1000
                                        : 0;

                                createSessionHistory(user1, user2, messageCount, durationSeconds, sessionSnap);
                            });
                });
    }

    private void createSessionHistory(final String user1, final String user2, final int messageCount,
                                      final long durationSeconds, final DocumentSnapshot sessionSnap) {
        if (user1 == null || user2 == null) {
            markSessionEnded(sessionSnap);
            return;
        }

        final WriteBatch batch = firestore.batch();

        batch.update(sessionSnap.getReference(), "status", "ended", "endedAt", FieldValue.serverTimestamp());
        batch.update(firestore.collection("users").document(user1),
                "isAvailable", true,
                "totalLinkups", FieldValue.increment(1));
        batch.update(firestore.collection("users").document(user2),
                "isAvailable", true,
                "totalLinkups", FieldValue.increment(1));

        firestore.collection("users").document(user1).get()
                .addOnSuccessListener(user1Doc -> {
                    firestore.collection("users").document(user2).get()
                            .addOnSuccessListener(user2Doc -> {
                                createHistoryDocuments(batch, user1, user2, user1Doc, user2Doc, messageCount, durationSeconds, sessionSnap);
                                batch.commit();
                            });
                });
    }

    private void createHistoryDocuments(WriteBatch batch, String user1, String user2,
                                        DocumentSnapshot user1Doc, DocumentSnapshot user2Doc,
                                        int messageCount, long durationSeconds, DocumentSnapshot sessionSnap) {
        if (user2Doc.exists()) {
            Map<String, Object> history1 = new HashMap<>();
            history1.put("sessionId", sessionId);
            history1.put("partnerUid", user2);
            history1.put("partnerName", user2Doc.getString("fullName"));
            history1.put("partnerUsername", user2Doc.getString("username"));
            history1.put("partnerImage", user2Doc.getString("profileImageUrl"));
            history1.put("messageCount", messageCount);
            history1.put("durationSeconds", durationSeconds);
            history1.put("linkedUpAt", sessionSnap.get("linkedUpAt"));
            history1.put("createdAt", FieldValue.serverTimestamp());

            batch.set(firestore.collection("users").document(user1)
                    .collection("chat_history").document(sessionId), history1);
        }

        if (user1Doc.exists()) {
            Map<String, Object> history2 = new HashMap<>();
            history2.put("sessionId", sessionId);
            history2.put("partnerUid", user1);
            history2.put("partnerName", user1Doc.getString("fullName"));
            history2.put("partnerUsername", user1Doc.getString("username"));
            history2.put("partnerImage", user1Doc.getString("profileImageUrl"));
            history2.put("messageCount", messageCount);
            history2.put("durationSeconds", durationSeconds);
            history2.put("linkedUpAt", sessionSnap.get("linkedUpAt"));
            history2.put("createdAt", FieldValue.serverTimestamp());

            batch.set(firestore.collection("users").document(user2)
                    .collection("chat_history").document(sessionId), history2);
        }
    }

    private void markSessionEnded(DocumentSnapshot sessionSnap) {
        WriteBatch batch = firestore.batch();
        batch.update(sessionSnap.getReference(), "status", "ended", "endedAt", FieldValue.serverTimestamp());
        batch.commit();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
