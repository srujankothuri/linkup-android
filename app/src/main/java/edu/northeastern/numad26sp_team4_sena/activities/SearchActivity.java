package edu.northeastern.numad26sp_team4_sena.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.adapters.UserSearchAdapter;
import edu.northeastern.numad26sp_team4_sena.models.User;

public class SearchActivity extends AppCompatActivity implements UserSearchAdapter.OnUserActionListener {

    private EditText etSearch;
    private ImageButton btnBack, btnClearSearch;
    private RecyclerView rvSearchResults;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private View rootView;

    private UserSearchAdapter adapter;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String currentUserId;

    private Handler searchHandler;
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);

        rootView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        searchHandler = new Handler(Looper.getMainLooper());

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        initViews();
        setupRecyclerView();
        setupSearchListener();
        loadFriendAndPendingIdsThenUsers();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        emptyState = findViewById(R.id.emptyState);
        progressBar = findViewById(R.id.progressBar);

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.requestFocus();
        });
    }

    private void setupRecyclerView() {
        adapter = new UserSearchAdapter(this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (TextUtils.isEmpty(query)) {
                        loadAllUsers();
                    } else {
                        performSearch(query);
                    }
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(query)) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });
    }

    private void loadFriendAndPendingIdsThenUsers() {
        if (currentUserId == null) {
            loadAllUsers();
            return;
        }

        showLoading(true);
        final boolean[] friendsDone = {false};
        final boolean[] pendingDone = {false};

        firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> friendList = (List<String>) doc.get("friendIds");
                        if (friendList != null) {
                            adapter.setFriendIds(new HashSet<>(friendList));
                        }
                    }
                    friendsDone[0] = true;
                    if (pendingDone[0]) loadAllUsers();
                })
                .addOnFailureListener(e -> {
                    friendsDone[0] = true;
                    if (pendingDone[0]) loadAllUsers();
                });

        firestore.collection("friendRequests")
                .whereEqualTo("fromUid", currentUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> pending = new HashSet<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String toUid = doc.getString("toUid");
                        if (toUid != null) pending.add(toUid);
                    }
                    adapter.setPendingIds(pending);
                    pendingDone[0] = true;
                    if (friendsDone[0]) loadAllUsers();
                })
                .addOnFailureListener(e -> {
                    pendingDone[0] = true;
                    if (friendsDone[0]) loadAllUsers();
                });
    }

    private void loadAllUsers() {
        showLoading(true);
        emptyState.setVisibility(View.GONE);

        firestore.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        User user = doc.toObject(User.class);
                        if (!user.getUid().equals(currentUserId)) {
                            users.add(user);
                        }
                    }
                    showLoading(false);
                    adapter.setUsers(users);

                    if (users.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        rvSearchResults.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        rvSearchResults.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showSnackbar(getString(R.string.error_network), true);
                });
    }

    private void performSearch(String query) {
        showLoading(true);
        emptyState.setVisibility(View.GONE);

        String queryLower = query.toLowerCase();

        firestore.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        User user = doc.toObject(User.class);
                        if (user.getUid().equals(currentUserId)) continue;

                        String name = user.getFullName() != null ? user.getFullName().toLowerCase() : "";
                        String username = user.getUsername() != null ? user.getUsername().toLowerCase() : "";

                        if (name.contains(queryLower) || username.contains(queryLower)) {
                            results.add(user);
                        }
                    }

                    showLoading(false);
                    adapter.setUsers(results);

                    if (results.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        rvSearchResults.setVisibility(View.GONE);
                    } else {
                        emptyState.setVisibility(View.GONE);
                        rvSearchResults.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showSnackbar(getString(R.string.error_network), true);
                });
    }

    @Override
    public void onAddFriend(User user, int position) {
        if (currentUserId == null) return;

        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("fromUid", currentUserId);
        request.put("toUid", user.getUid());
        request.put("status", "pending");
        request.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        firestore.collection("friendRequests")
                .add(request)
                .addOnSuccessListener(docRef -> {
                    adapter.addPendingId(user.getUid());
                    showSnackbar(getString(R.string.friend_request_sent, user.getFullName()), false);
                })
                .addOnFailureListener(e ->
                        showSnackbar(getString(R.string.error_send_request), true));
    }

    @Override
    public void onUserClick(User user) {
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showSnackbar(String message, boolean isError) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getColor(R.color.snackbar_background));
        snackbar.setTextColor(getColor(R.color.white));
        snackbar.setActionTextColor(getColor(R.color.snackbar_action));
        if (isError) {
            snackbar.setAction("Retry", v -> {
                String query = etSearch.getText().toString().trim();
                if (TextUtils.isEmpty(query)) {
                    loadAllUsers();
                } else {
                    performSearch(query);
                }
            });
        }
        snackbar.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
