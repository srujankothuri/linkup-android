package edu.northeastern.numad26sp_team4_sena.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.activities.ChatHistoryActivity;
import edu.northeastern.numad26sp_team4_sena.models.LinkUpHistory;

public class LinkUpHistoryAdapter extends RecyclerView.Adapter<LinkUpHistoryAdapter.HistoryViewHolder> {

    private List<LinkUpHistory> histories;
    private final Context context;
    private final SimpleDateFormat dateFormat;

    public LinkUpHistoryAdapter(Context context) {
        this.context = context;
        this.histories = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    }

    public void setHistories(List<LinkUpHistory> histories) {
        this.histories = histories != null ? histories : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_linkup_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (isInvalidPosition(position)) {
            return;
        }

        LinkUpHistory history = histories.get(position);
        if (history != null) {
            holder.bind(history);
        }
    }

    @Override
    public int getItemCount() {
        return histories.size();
    }

    private boolean isInvalidPosition(int position) {
        return position < 0 || position >= histories.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CircleImageView ivPartnerAvatar;
        TextView tvPartnerName;
        TextView tvDate;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPartnerAvatar = itemView.findViewById(R.id.ivPartnerAvatar);
            tvPartnerName = itemView.findViewById(R.id.tvPartnerName);
            tvDate = itemView.findViewById(R.id.tvDate);

            itemView.setOnClickListener(this);
        }

        void bind(LinkUpHistory history) {
            String displayName = getDisplayName(history);
            tvPartnerName.setText(displayName);

            if (history.getLinkedUpAt() != null) {
                tvDate.setText(getRelativeDate(history.getLinkedUpAt()));
            } else {
                tvDate.setText("");
            }

            loadPartnerImage(history.getPartnerImage());
        }

        private String getDisplayName(LinkUpHistory history) {
            if (history.getPartnerName() != null && !history.getPartnerName().isEmpty()) {
                return history.getPartnerName();
            }
            if (history.getPartnerUsername() != null && !history.getPartnerUsername().isEmpty()) {
                return "@" + history.getPartnerUsername();
            }
            return context.getString(R.string.chat_anonymous_name);
        }

        private void loadPartnerImage(String imageUrl) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivPartnerAvatar);
            } else {
                ivPartnerAvatar.setImageResource(R.drawable.ic_person);
            }
        }

        private String getRelativeDate(Date date) {
            Calendar now = Calendar.getInstance();
            Calendar then = Calendar.getInstance();
            then.setTime(date);

            long diffMillis = now.getTimeInMillis() - then.getTimeInMillis();
            long days = diffMillis / (24 * 60 * 60 * 1000);

            if (days == 0) return "Today";
            if (days == 1) return "Yesterday";
            if (days < 7) return days + " days ago";
            if (days < 30) return (days / 7) + " weeks ago";
            return dateFormat.format(date);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            if (!isInvalidPosition(position)) {
                LinkUpHistory history = histories.get(position);
                if (history != null) {
                    openChatHistory(history);
                }
            }
        }

        private void openChatHistory(LinkUpHistory history) {
            Intent intent = new Intent(context, ChatHistoryActivity.class);
            intent.putExtra("sessionId", history.getSessionId());
            intent.putExtra("partnerName", history.getPartnerName());
            intent.putExtra("partnerImage", history.getPartnerImage());
            context.startActivity(intent);
        }
    }
}
