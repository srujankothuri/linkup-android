package edu.northeastern.numad26sp_team4_sena.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.adapters.FriendListAdapter;
import edu.northeastern.numad26sp_team4_sena.adapters.FriendRequestAdapter;
import edu.northeastern.numad26sp_team4_sena.models.User;
import edu.northeastern.numad26sp_team4_sena.utils.DistanceCalculator;

public class FriendsActivity extends AppCompatActivity
        implements FriendRequestAdapter.OnRequestActionListener,
        FriendListAdapter.OnFriendActionListener {

    private static final String TAG = "FriendsActivity";

    private View rootView;
    private ImageButton btnBack;
    private ProgressBar progressBar;

    private LinearLayout sectionRequests;
    private TextView tvRequestsHeader;
    private RecyclerView rvRequests;
    private FriendRequestAdapter requestAdapter;

    private LinearLayout sectionOutgoing;
    private TextView tvOutgoingHeader;
    private RecyclerView rvOutgoing;
    private FriendRequestAdapter outgoingAdapter;

    private TextView tvFriendsHeader;
    private RecyclerView rvFriends;
    private FriendListAdapter friendAdapter;
    private LinearLayout emptyState;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private double currentUserLatitude = 0;
    private double currentUserLongitude = 0;
    private boolean locationLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_friends);

        rootView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        initViews();
        setupRecyclerViews();
        loadCurrentUserLocation();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        sectionRequests = findViewById(R.id.sectionRequests);
        tvRequestsHeader = findViewById(R.id.tvRequestsHeader);
        rvRequests = findViewById(R.id.rvRequests);

        sectionOutgoing = findViewById(R.id.sectionOutgoing);
        tvOutgoingHeader = findViewById(R.id.tvOutgoingHeader);
        rvOutgoing = findViewById(R.id.rvOutgoing);

        tvFriendsHeader = findViewById(R.id.tvFriendsHeader);
        rvFriends = findViewById(R.id.rvFriends);
        emptyState = findViewById(R.id.emptyState);

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupRecyclerViews() {
        requestAdapter = new FriendRequestAdapter(FriendRequestAdapter.Mode.INCOMING, this);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(requestAdapter);

        outgoingAdapter = new FriendRequestAdapter(FriendRequestAdapter.Mode.OUTGOING, this);
        rvOutgoing.setLayoutManager(new LinearLayoutManager(this));
        rvOutgoing.setAdapter(outgoingAdapter);

        friendAdapter = new FriendListAdapter(this);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(friendAdapter);
    }

    private void loadCurrentUserLocation() {
        if (currentUserId == null) {
            locationLoaded = true;
            loadData();
            return;
        }

        firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double lat = doc.getDouble("latitude");
                        Double lon = doc.getDouble("longitude");
                        if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                            currentUserLatitude = lat;
                            currentUserLongitude = lon;
                        }
                    }
                    locationLoaded = true;
                    loadData();
                })
                .addOnFailureListener(e -> {
                    locationLoaded = true;
                    loadData();
                });
    }

    private void loadData() {
        if (currentUserId == null) return;
        showLoading(true);
        loadIncomingRequests();
        loadOutgoingRequests();
        loadFriendsList();
    }

    private void loadIncomingRequests() {
        firestore.collection("friendRequests")
                .whereEqualTo("toUid", currentUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> fromUids = new ArrayList<>();
                    List<String> requestIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String fromUid = doc.getString("fromUid");
                        if (fromUid != null) {
                            fromUids.add(fromUid);
                            requestIds.add(doc.getId());
                        }
                    }

                    if (fromUids.isEmpty()) {
                        sectionRequests.setVisibility(View.GONE);
                        return;
                    }

                    fetchUsersForRequests(fromUids, requestIds, true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load incoming requests", e);
                    sectionRequests.setVisibility(View.GONE);
                });
    }

    private void loadOutgoingRequests() {
        firestore.collection("friendRequests")
                .whereEqualTo("fromUid", currentUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> toUids = new ArrayList<>();
                    List<String> requestIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String toUid = doc.getString("toUid");
                        if (toUid != null) {
                            toUids.add(toUid);
                            requestIds.add(doc.getId());
                        }
                    }

                    if (toUids.isEmpty()) {
                        sectionOutgoing.setVisibility(View.GONE);
                        return;
                    }

                    fetchUsersForRequests(toUids, requestIds, false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load outgoing requests", e);
                    sectionOutgoing.setVisibility(View.GONE);
                });
    }

    private void fetchUsersForRequests(List<String> uids, List<String> requestIds, boolean isIncoming) {
        List<User> users = new ArrayList<>();
        List<String> validRequestIds = new ArrayList<>();
        final int total = uids.size();
        final int[] loaded = {0};

        for (int i = 0; i < total; i++) {
            final int index = i;
            firestore.collection("users").document(uids.get(i))
                    .get()
                    .addOnSuccessListener(doc -> {
                        loaded[0]++;
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                double distance = DistanceCalculator.calculateDistanceMiles(
                                        currentUserLatitude, currentUserLongitude,
                                        user.getLatitude(), user.getLongitude());
                                user.setDistanceMiles(distance);

                                users.add(user);
                                validRequestIds.add(requestIds.get(index));
                            }
                        }
                        if (loaded[0] == total) {
                            if (isIncoming) {
                                updateRequestsSection(users, validRequestIds);
                            } else {
                                updateOutgoingSection(users, validRequestIds);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        loaded[0]++;
                        if (loaded[0] == total) {
                            if (isIncoming) {
                                updateRequestsSection(users, validRequestIds);
                            } else {
                                updateOutgoingSection(users, validRequestIds);
                            }
                        }
                    });
        }
    }

    private void updateRequestsSection(List<User> users, List<String> requestIds) {
        if (users.isEmpty()) {
            sectionRequests.setVisibility(View.GONE);
        } else {
            sectionRequests.setVisibility(View.VISIBLE);
            tvRequestsHeader.setText(getString(R.string.friend_requests_header, users.size()));
            requestAdapter.setData(users, requestIds);
        }
    }

    private void updateOutgoingSection(List<User> users, List<String> requestIds) {
        if (users.isEmpty()) {
            sectionOutgoing.setVisibility(View.GONE);
        } else {
            sectionOutgoing.setVisibility(View.VISIBLE);
            tvOutgoingHeader.setText(getString(R.string.sent_requests_header, users.size()));
            outgoingAdapter.setData(users, requestIds);
        }
    }

    private void loadFriendsList() {
        firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    showLoading(false);

                    if (!doc.exists()) {
                        showEmptyState(true);
                        return;
                    }

                    List<String> friendIds = (List<String>) doc.get("friendIds");
                    if (friendIds == null || friendIds.isEmpty()) {
                        showEmptyState(true);
                        return;
                    }

                    fetchFriendUsers(friendIds);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to load friends list", e);
                    showSnackbar(getString(R.string.error_load_friends));
                    showEmptyState(true);
                });
    }

    private void fetchFriendUsers(List<String> friendIds) {
        List<User> friends = new ArrayList<>();
        final int total = friendIds.size();
        final int[] loaded = {0};

        for (String friendId : friendIds) {
            firestore.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        loaded[0]++;
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                double distance = DistanceCalculator.calculateDistanceMiles(
                                        currentUserLatitude, currentUserLongitude,
                                        user.getLatitude(), user.getLongitude());
                                user.setDistanceMiles(distance);

                                friends.add(user);
                            }
                        }
                        if (loaded[0] == total) {
                            updateFriendsSection(friends);
                        }
                    })
                    .addOnFailureListener(e -> {
                        loaded[0]++;
                        if (loaded[0] == total) {
                            updateFriendsSection(friends);
                        }
                    });
        }
    }

    private void updateFriendsSection(List<User> friends) {
        if (friends.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            tvFriendsHeader.setText(getString(R.string.my_friends_header, friends.size()));
            friendAdapter.setFriends(friends);
        }
    }

    @Override
    public void onAccept(User user, String requestId, int position) {
        firestore.collection("friendRequests").document(requestId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("users").document(currentUserId)
                            .update("friendIds", FieldValue.arrayUnion(user.getUid()));

                    firestore.collection("users").document(user.getUid())
                            .update("friendIds", FieldValue.arrayUnion(currentUserId));

                    requestAdapter.removeItem(position);
                    showSnackbar(getString(R.string.now_friends, user.getFullName()));

                    if (requestAdapter.getItemCount() == 0) {
                        sectionRequests.setVisibility(View.GONE);
                    }

                    loadFriendsList();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept request", e);
                    showSnackbar(getString(R.string.error_accept_request));
                });
    }

    @Override
    public void onReject(User user, String requestId, int position) {
        firestore.collection("friendRequests").document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    requestAdapter.removeItem(position);
                    showSnackbar(getString(R.string.request_declined, user.getFullName()));

                    if (requestAdapter.getItemCount() == 0) {
                        sectionRequests.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to reject request", e);
                    showSnackbar(getString(R.string.error_decline_request));
                });
    }

    @Override
    public void onCancel(User user, String requestId, int position) {
        firestore.collection("friendRequests").document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    outgoingAdapter.removeItem(position);
                    showSnackbar(getString(R.string.request_cancelled, user.getFullName()));

                    if (outgoingAdapter.getItemCount() == 0) {
                        sectionOutgoing.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to cancel request", e);
                    showSnackbar(getString(R.string.error_cancel_request));
                });
    }


    @Override
    public void onRemoveFriend(User user, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.remove_friend_title))
                .setMessage(getString(R.string.remove_friend_message, user.getFullName()))
                .setPositiveButton(getString(R.string.remove_friend_confirm), (dialog, which) -> performRemoveFriend(user, position))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void performRemoveFriend(User user, int position) {
        firestore.collection("users").document(currentUserId)
                .update("friendIds", FieldValue.arrayRemove(user.getUid()))
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("users").document(user.getUid())
                            .update("friendIds", FieldValue.arrayRemove(currentUserId));

                    friendAdapter.removeItem(position);
                    showSnackbar(getString(R.string.friend_removed, user.getFullName()));

                    int count = friendAdapter.getItemCount();
                    if (count == 0) {
                        showEmptyState(true);
                        tvFriendsHeader.setText(getString(R.string.my_friends_header_empty));
                    } else {
                        tvFriendsHeader.setText(getString(R.string.my_friends_header, count));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove friend", e);
                    showSnackbar(getString(R.string.error_remove_friend));
                });
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvFriends.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getColor(R.color.snackbar_background));
        snackbar.setTextColor(getColor(R.color.white));
        snackbar.setActionTextColor(getColor(R.color.snackbar_action));
        snackbar.show();
    }
}