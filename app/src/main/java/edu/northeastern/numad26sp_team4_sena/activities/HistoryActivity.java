package edu.northeastern.numad26sp_team4_sena.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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

import java.util.ArrayList;
import java.util.List;

import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.adapters.LinkUpHistoryAdapter;
import edu.northeastern.numad26sp_team4_sena.models.LinkUpHistory;

public class HistoryActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private LinkUpHistoryAdapter adapter;
    private LinearLayout llEmpty;
    private RecyclerView rvHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupRecyclerView();

        loadHistory();
    }

    private void setupRecyclerView() {
        llEmpty = findViewById(R.id.llEmpty);
        rvHistory = findViewById(R.id.rvHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LinkUpHistoryAdapter(this);
        rvHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            showError("Not signed in. Please log in again.");
            finish();
            return;
        }

        String uid = currentUser.getUid();

        firestore.collection("users")
                .document(uid)
                .collection("chat_history")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<LinkUpHistory> histories = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        LinkUpHistory history = doc.toObject(LinkUpHistory.class);
                        if (history != null) {
                            history.setSessionId(doc.getId());
                            histories.add(history);
                        }
                    }

                    adapter.setHistories(histories);

                    if (histories.isEmpty()) {
                        showEmpty();
                    } else {
                        showList();
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Failed to load chat history: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                    showEmpty();
                });
    }

    private void showEmpty() {
        llEmpty.setVisibility(View.VISIBLE);
        rvHistory.setVisibility(View.GONE);
    }

    private void showList() {
        llEmpty.setVisibility(View.GONE);
        rvHistory.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
