package edu.northeastern.numad26sp_team4_sena.adapters;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.models.LinkUpRequest;

public class LinkUpRequestAdapter extends RecyclerView.Adapter<LinkUpRequestAdapter.ViewHolder> {

    public static final long REQUEST_TIMEOUT_MS = 60_000L;

    public interface OnRequestActionListener {
        void onAccept(LinkUpRequest request, int position);

        void onReject(LinkUpRequest request, int position);
    }

    private final List<LinkUpRequest> requests = new ArrayList<>();
    private final Map<String, CountDownTimer> timers = new HashMap<>();
    private final OnRequestActionListener listener;

    public LinkUpRequestAdapter(OnRequestActionListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<LinkUpRequest> newRequests) {
        cancelAllTimers();
        requests.clear();
        requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position < 0 || position >= requests.size()) return;
        String requestId = requests.get(position).getRequestId();
        if (timers.containsKey(requestId)) {
            timers.get(requestId).cancel();
            timers.remove(requestId);
        }
        requests.remove(position);
        notifyItemRemoved(position);
    }

    public void cancelAllTimers() {
        for (CountDownTimer t : timers.values()) t.cancel();
        timers.clear();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_linkup_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LinkUpRequest request = requests.get(position);

        if (timers.containsKey(request.getRequestId())) {
            timers.get(request.getRequestId()).cancel();
        }

        CountDownTimer timer = new CountDownTimer(REQUEST_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secs = millisUntilFinished / 1000;
                holder.tvCountdown.setText(String.format(Locale.getDefault(), "0:%02d", secs));
            }

            @Override
            public void onFinish() {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < requests.size()) {
                    listener.onReject(requests.get(pos), pos);
                }
            }
        }.start();

        timers.put(request.getRequestId(), timer);

        holder.btnAccept.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < requests.size()) listener.onAccept(requests.get(pos), pos);
        });

        holder.btnReject.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < requests.size()) listener.onReject(requests.get(pos), pos);
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivSender;
        TextView tvCountdown;
        MaterialButton btnAccept, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSender = itemView.findViewById(R.id.ivSender);
            tvCountdown = itemView.findViewById(R.id.tvCountdown);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
