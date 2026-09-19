package edu.northeastern.numad26sp_team4_sena.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;

import edu.northeastern.numad26sp_team4_sena.services.LinkUpListenerService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && FirebaseAuth.getInstance().getCurrentUser() != null) {
            context.startForegroundService(new Intent(context, LinkUpListenerService.class));
        }
    }
}
