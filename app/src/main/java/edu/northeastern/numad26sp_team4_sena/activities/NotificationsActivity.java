package edu.northeastern.numad26sp_team4_sena.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.adapters.LinkUpRequestAdapter;
import edu.northeastern.numad26sp_team4_sena.models.LinkUpRequest;

public class NotificationsActivity extends AppCompatActivity
        implements LinkUpRequestAdapter.OnRequestActionListener {

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private LinkUpRequestAdapter adapter;
    private TextView tvEmpty;
    private ListenerRegistration requestsListener;
    private ListenerRegistration expiredListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmpty);

        RecyclerView rv = findViewById(R.id.rvRequests);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LinkUpRequestAdapter(this);
        rv.setAdapter(adapter);

        listenForPendingRequests();
        listenForExpiredRequests();
    }

    private void listenForExpiredRequests() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        expiredListener = firestore.collection("linkupRequests")
                .whereEqualTo("toUid", currentUser.getUid())
                .whereEqualTo("status", "expired")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null || snapshots.isEmpty()) return;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        com.google.firebase.Timestamp createdAt = doc.getTimestamp("createdAt");
                        if (createdAt != null) {
                            long ageMs = System.currentTimeMillis() - createdAt.toDate().getTime();
                            if (ageMs < 5000) {
                                Toast.makeText(NotificationsActivity.this,
                                        getString(R.string.linkup_request_cancelled),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void listenForPendingRequests() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        requestsListener = firestore.collection("linkupRequests")
                .whereEqualTo("toUid", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null) return;
                    List<LinkUpRequest> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        LinkUpRequest req = doc.toObject(LinkUpRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId());
                            list.add(req);
                        }
                    }
                    adapter.setRequests(list);
                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onAccept(LinkUpRequest request, int position) {
        String requestId = request.getRequestId();
        String fromUid = request.getFromUid();
        String toUid = request.getToUid();

        DocumentReference requestRef = firestore.collection("linkupRequests").document(requestId);
        DocumentReference senderRef = firestore.collection("users").document(fromUid);
        DocumentReference receiverRef = firestore.collection("users").document(toUid);
        DocumentReference sessionRef = firestore.collection("linkupSessions").document();
        String sessionId = sessionRef.getId();

        firestore.runTransaction(transaction -> {
            DocumentSnapshot requestSnap = transaction.get(requestRef);
            DocumentSnapshot senderSnap = transaction.get(senderRef);

            if (!"pending".equals(requestSnap.getString("status"))) {
                throw new FirebaseFirestoreException("Request no longer pending",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            if (!Boolean.TRUE.equals(senderSnap.getBoolean("isAvailable"))) {
                throw new FirebaseFirestoreException("Sender already matched",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(requestRef, "status", "accepted", "sessionId", sessionId);

            Map<String, Object> session = new HashMap<>();
            session.put("user1Uid", fromUid);
            session.put("user2Uid", toUid);
            session.put("status", "active");
            session.put("linkedUpAt", FieldValue.serverTimestamp());
            session.put("linkedUpAtMs", System.currentTimeMillis());
            session.put("createdAt", FieldValue.serverTimestamp());
            transaction.set(sessionRef, session);

            transaction.update(senderRef, "isAvailable", false);
            transaction.update(receiverRef, "isAvailable", false);

            return sessionId;

        }).addOnSuccessListener(sid -> {
            cancelOtherPendingRequests(fromUid, requestId);
            openChat(sid);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, getString(R.string.toast_request_unavailable), Toast.LENGTH_SHORT).show();
            adapter.removeItem(position);
        });
    }

    @Override
    public void onReject(LinkUpRequest request, int position) {
        String fromUid = request.getFromUid();
        firestore.collection("linkupRequests")
                .document(request.getRequestId())
                .update("status", "expired")
                .addOnSuccessListener(v -> {
                    adapter.removeItem(position);
                    resetSenderIfNoMorePending(fromUid);
                });
    }

    private void resetSenderIfNoMorePending(String fromUid) {
        firestore.collection("linkupRequests")
                .whereEqualTo("fromUid", fromUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        firestore.collection("users").document(fromUid)
                                .update("isAvailable", true);
                    }
                });
    }

    private void cancelOtherPendingRequests(String fromUid, String acceptedRequestId) {
        firestore.collection("linkupRequests")
                .whereEqualTo("fromUid", fromUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        if (!doc.getId().equals(acceptedRequestId)) {
                            batch.update(doc.getReference(), "status", "expired");
                        }
                    }
                    batch.commit();
                });
    }

    private void openChat(String sessionId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("sessionId", sessionId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) requestsListener.remove();
        if (expiredListener != null) expiredListener.remove();
        adapter.cancelAllTimers();
    }
}
