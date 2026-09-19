package edu.northeastern.numad26sp_team4_sena.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.models.User;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etUsername, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private TextView tvSignIn;
    private View cardForm, rootView;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        rootView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(systemBars.bottom, imeInsets.bottom);
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
        animateCardEntry();
    }

    private void initViews() {
        tilFullName = findViewById(R.id.tilFullName);
        tilUsername = findViewById(R.id.tilUsername);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvSignIn = findViewById(R.id.tvSignIn);
        cardForm = findViewById(R.id.cardForm);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> attemptRegistration());

        tvSignIn.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void animateCardEntry() {
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        cardForm.startAnimation(slideUp);
    }

    private void attemptRegistration() {
        tilFullName.setError(null);
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String fullName = getText(etFullName);
        String username = getText(etUsername);
        String email = getText(etEmail);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

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

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_empty_email));
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_empty_password));
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_short_password));
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_no_match));
            etConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);

        firestore.collection("users")
                .whereEqualTo("username", username.toLowerCase())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        setLoading(false);
                        tilUsername.setError(getString(R.string.error_username_taken));
                        etUsername.requestFocus();
                    } else {
                        createFirebaseAccount(fullName, username.toLowerCase(), email, password);
                    }
                })
                .addOnFailureListener(e ->
                        createFirebaseAccount(fullName, username.toLowerCase(), email, password));
    }

    private void createFirebaseAccount(String fullName, String username, String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserToFirestore(firebaseUser.getUid(), fullName, username, email);
                        }
                    } else {
                        setLoading(false);
                        handleRegistrationError(task.getException());
                    }
                });
    }

    private void handleRegistrationError(Exception exception) {
        String message;
        if (exception instanceof FirebaseAuthUserCollisionException) {
            message = getString(R.string.error_email_collision);
            tilEmail.setError(" ");
            etEmail.requestFocus();
        } else if (exception instanceof FirebaseAuthWeakPasswordException) {
            message = getString(R.string.error_weak_password);
            tilPassword.setError(" ");
            etPassword.requestFocus();
        } else {
            message = getString(R.string.error_register_failed);
        }
        showSnackbar(message);
    }

    private void saveUserToFirestore(String uid, String fullName, String username, String email) {
        User user = new User(uid, fullName, username, email);
        setLoading(false);
        navigateToHome();
        firestore.collection("users").document(uid)
                .set(user)
                .addOnFailureListener(e ->
                        Log.w("RegisterActivity", "Firestore save will retry when online", e));
    }

    private void navigateToHome() {
        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        btnRegister.setAlpha(isLoading ? 0.6f : 1.0f);
        etFullName.setEnabled(!isLoading);
        etUsername.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getColor(R.color.snackbar_background));
        snackbar.setTextColor(getColor(R.color.white));
        snackbar.setActionTextColor(getColor(R.color.snackbar_action));
        snackbar.setAnchorView(btnRegister);
        snackbar.show();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
