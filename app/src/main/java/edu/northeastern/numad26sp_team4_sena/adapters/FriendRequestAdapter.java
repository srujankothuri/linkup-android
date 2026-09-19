package edu.northeastern.numad26sp_team4_sena.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.models.User;
import edu.northeastern.numad26sp_team4_sena.utils.DistanceCalculator;


public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    public enum Mode {INCOMING, OUTGOING}

    private List<User> users;
    private List<String> requestIds;
    private final Mode mode;
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAccept(User user, String requestId, int position);

        void onReject(User user, String requestId, int position);

        void onCancel(User user, String requestId, int position);
    }

    public FriendRequestAdapter(Mode mode, OnRequestActionListener listener) {
        this.users = new ArrayList<>();
        this.requestIds = new ArrayList<>();
        this.mode = mode;
        this.listener = listener;
    }

    public void setData(List<User> users, List<String> requestIds) {
        this.users = users;
        this.requestIds = requestIds;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < users.size()) {
            users.remove(position);
            requestIds.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        holder.bind(users.get(position), requestIds.get(position), position);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvUsername, tvDistance;
        MaterialButton btnAccept, btnReject;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        void bind(User user, String requestId, int position) {
            tvName.setText(user.getFullName());
            tvUsername.setText("@" + user.getUsername());

            if (user.getDistanceMiles() > 0) {
                tvDistance.setText("• " + DistanceCalculator.formatDistance(user.getDistanceMiles()));
                tvDistance.setVisibility(View.VISIBLE);
            } else {
                tvDistance.setVisibility(View.GONE);
            }

            String imageUrl = user.getProfileImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_person);
            }

            if (mode == Mode.INCOMING) {
                btnAccept.setVisibility(View.VISIBLE);
                btnAccept.setText(itemView.getContext().getString(R.string.accept));
                btnReject.setText(itemView.getContext().getString(R.string.reject));

                btnAccept.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAccept(user, requestId, position);
                    }
                });

                btnReject.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onReject(user, requestId, position);
                    }
                });
            } else {
                btnAccept.setVisibility(View.GONE);
                btnReject.setText(itemView.getContext().getString(R.string.btn_cancel));
                btnReject.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCancel(user, requestId, position);
                    }
                });
            }
        }
    }
}