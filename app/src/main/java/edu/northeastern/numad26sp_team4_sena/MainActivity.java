package edu.northeastern.numad26sp_team4_sena;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.northeastern.numad26sp_team4_sena.activities.HomeActivity;
import edu.northeastern.numad26sp_team4_sena.activities.LoginActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView logo = findViewById(R.id.ivSplashLogo);
        TextView title = findViewById(R.id.tvSplashTitle);
        TextView slogan = findViewById(R.id.tvSplashSlogan);

        logo.setAlpha(0f);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        title.setAlpha(0f);
        title.setTranslationY(30f);
        slogan.setAlpha(0f);

        logo.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(600)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        title.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(300)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        slogan.animate()
                .alpha(1f)
                .setStartDelay(500)
                .setDuration(400)
                .start();

        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, 1800);
    }

    private void navigateNext() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent;
        if (currentUser != null) {
            intent = new Intent(this, HomeActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
