package edu.northeastern.numad26sp_team4_sena.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.models.User;


public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private List<User> users;
    private Set<String> friendIds;
    private Set<String> pendingIds;
    private final OnUserActionListener listener;

    public interface OnUserActionListener {
        void onAddFriend(User user, int position);

        void onUserClick(User user);
    }

    public UserSearchAdapter(OnUserActionListener listener) {
        this.users = new ArrayList<>();
        this.friendIds = new HashSet<>();
        this.pendingIds = new HashSet<>();
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    public void setFriendIds(Set<String> friendIds) {
        this.friendIds = friendIds;
        notifyDataSetChanged();
    }

    public void setPendingIds(Set<String> pendingIds) {
        this.pendingIds = pendingIds;
        notifyDataSetChanged();
    }

    public void addPendingId(String id) {
        this.pendingIds.add(id);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivUserAvatar;
        TextView tvUserName, tvUserUsername, tvDistance;
        MaterialButton btnAction;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserUsername = itemView.findViewById(R.id.tvUserUsername);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            btnAction = itemView.findViewById(R.id.btnAction);
        }

        void bind(User user, int position) {
            tvUserName.setText(user.getFullName());
            tvUserUsername.setText("@" + user.getUsername());

            tvDistance.setVisibility(View.GONE);

            if (friendIds.contains(user.getUid())) {
                btnAction.setText(R.string.added);
                btnAction.setEnabled(false);
                btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                itemView.getContext().getColor(R.color.input_background)));
                btnAction.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            } else if (pendingIds.contains(user.getUid())) {
                btnAction.setText(R.string.pending);
                btnAction.setEnabled(false);
                btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                itemView.getContext().getColor(R.color.input_background)));
                btnAction.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
            } else {
                btnAction.setText(R.string.add_friend);
                btnAction.setEnabled(true);
                btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                itemView.getContext().getColor(R.color.primary)));
                btnAction.setTextColor(itemView.getContext().getColor(R.color.white));
            }

            btnAction.setOnClickListener(v -> {
                if (listener != null && btnAction.isEnabled()) {
                    listener.onAddFriend(user, position);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}
