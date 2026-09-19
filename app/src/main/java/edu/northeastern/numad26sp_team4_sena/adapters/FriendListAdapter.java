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


public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendViewHolder> {

    private List<User> friends;
    private final OnFriendActionListener listener;

    public interface OnFriendActionListener {
        void onRemoveFriend(User user, int position);
    }

    public FriendListAdapter(OnFriendActionListener listener) {
        this.friends = new ArrayList<>();
        this.listener = listener;
    }

    public void setFriends(List<User> friends) {
        this.friends = friends;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < friends.size()) {
            friends.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        holder.bind(friends.get(position), position);
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvUsername, tvDistance;
        MaterialButton btnRemove;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        void bind(User user, int position) {
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

            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveFriend(user, position);
                }
            });
        }
    }
}