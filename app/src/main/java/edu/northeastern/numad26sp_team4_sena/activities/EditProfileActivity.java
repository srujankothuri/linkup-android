package edu.northeastern.numad26sp_team4_sena.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.numad26sp_team4_sena.R;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private ImageButton btnBack, btnLogout;
    private CircleImageView ivProfilePhoto;
    private View rootView;
    private TextView tvDisplayName, tvUsername, tvMemberSince;
    private TextView tvLinkUpCount, tvFriendsCount;
    private TextInputLayout tilFullName, tilUsername;
    private TextInputEditText etFullName, etUsername;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private boolean profileLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

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
        setupClickListeners();
        loadUserProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvUsername = findViewById(R.id.tvUsername);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        tvLinkUpCount = findViewById(R.id.tvLinkUpCount);
        tvFriendsCount = findViewById(R.id.tvFriendsCount);
        tilFullName = findViewById(R.id.tilFullName);
        tilUsername = findViewById(R.id.tilUsername);
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadUserProfile() {
        if (currentUserId == null) {
            showSnackbar(getString(R.string.not_signed_in));
            return;
        }

        setLoading(true);

        firestore.collection("users").document(currentUserId)
                .get(Source.CACHE)
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        setLoading(false);
                        profileLoaded = true;
                        populateProfile(document);
                        refreshFromServer();
                    } else {
                        loadFromServer();
                    }
                })
                .addOnFailureListener(e -> loadFromServer());
    }

    private void loadFromServer() {
        firestore.collection("users").document(currentUserId)
                .get(Source.SERVER)
                .addOnSuccessListener(document -> {
                    setLoading(false);
                    if (document.exists()) {
                        profileLoaded = true;
                        populateProfile(document);
                    } else {
                        profileLoaded = false;
                        prefillFromAuth();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.w(TAG, "Server fetch failed", e);
                    profileLoaded = false;
                    prefillFromAuth();
                });
    }

    private void refreshFromServer() {
        firestore.collection("users").document(currentUserId)
                .get(Source.SERVER)
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        populateProfile(document);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Background refresh failed", e));
    }

    private void prefillFromAuth() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            String email = user.getEmail();

            if (displayName != null && !displayName.isEmpty()) {
                etFullName.setText(displayName);
                tvDisplayName.setText(displayName);
            }
            if (email != null) {
                String suggestedUsername = email.split("@")[0];
                etUsername.setText(suggestedUsername);
                tvUsername.setText("@" + suggestedUsername);
            }
            tvLinkUpCount.setText("0");
            tvFriendsCount.setText("0");

            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            tvMemberSince.setText(getString(R.string.member_since, sdf.format(new Date())));
        }
        showSnackbar(getString(R.string.complete_your_profile));
    }

    private void populateProfile(DocumentSnapshot document) {
        String fullName = document.getString("fullName");
        String username = document.getString("username");
        String profileImageUrl = document.getString("profileImageUrl");
        Date createdAt = document.getDate("createdAt");
        Long totalLinkups = document.getLong("totalLinkups");
        List<String> friendIds = (List<String>) document.get("friendIds");
        tvDisplayName.setText(fullName != null ? fullName : "");
        tvUsername.setText(username != null ? "@" + username : "");

        if (createdAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            tvMemberSince.setText(getString(R.string.member_since, sdf.format(createdAt)));
        }

        tvLinkUpCount.setText(String.valueOf(totalLinkups != null ? totalLinkups : 0));
        tvFriendsCount.setText(String.valueOf(friendIds != null ? friendIds.size() : 0));

        etFullName.setText(fullName != null ? fullName : "");
        etUsername.setText(username != null ? username : "");

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Picasso.get()
                    .load(profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivProfilePhoto);
        }
    }

    private void saveProfile() {
        tilFullName.setError(null);
        tilUsername.setError(null);

        String fullName = getText(etFullName);
        String username = getText(etUsername);

        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError(getString(R.string.error_empty_name));
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            tilUsername.setError(getString(R.string.error_empty_username));
            etUsername.requestFocus();
            return;
        }

        if (username.length() < 3) {
            tilUsername.setError(getString(R.string.error_username_too_short));
            etUsername.requestFocus();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("uid", currentUserId);
        updates.put("fullName", fullName);
        updates.put("username", username.toLowerCase());


        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            updates.put("email", user.getEmail());
        }

        profileLoaded = true;
        tvDisplayName.setText(fullName);
        tvUsername.setText("@" + username.toLowerCase());


        rootView.requestFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
        }

        showSnackbar(getString(R.string.profile_updated));

        firestore.collection("users").document(currentUserId)
                .set(updates, SetOptions.merge())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save profile", e);
                    showSnackbar(getString(R.string.save_may_not_sync));
                });
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.confirm_logout_title))
                .setMessage(getString(R.string.confirm_logout_message))
                .setPositiveButton(getString(R.string.logout), (dialog, which) -> {
                    firebaseAuth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
        btnSave.setAlpha(isLoading ? 0.6f : 1.0f);
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getColor(R.color.snackbar_background));
        snackbar.setTextColor(getColor(R.color.white));
        snackbar.setActionTextColor(getColor(R.color.snackbar_action));
        snackbar.setAnchorView(btnSave);
        snackbar.show();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
