package edu.northeastern.numad26sp_team4_sena.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.models.Message;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 0;
    private static final String TIME_FORMAT = "h:mm a";

    private final String currentUserId;
    private List<Message> messages;
    private final SimpleDateFormat timeFormat;

    public ChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
        this.messages = new ArrayList<>();
        this.timeFormat = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (isInvalidPosition(position)) {
            return VIEW_TYPE_RECEIVED;
        }

        Message msg = messages.get(position);
        if (msg == null || msg.getSenderId() == null) {
            return VIEW_TYPE_RECEIVED;
        }

        return currentUserId.equals(msg.getSenderId()) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new MessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (isInvalidPosition(position)) {
            return;
        }

        Message msg = messages.get(position);
        if (msg == null) {
            return;
        }

        MessageViewHolder vh = (MessageViewHolder) holder;
        bindMessage(vh, msg);
    }

    private void bindMessage(MessageViewHolder holder, Message msg) {
        String text = msg.getText() != null ? msg.getText() : "";
        holder.tvMessage.setText(text);

        if (msg.getTimestamp() != null) {
            holder.tvTime.setText(timeFormat.format(msg.getTimestamp()));
        } else {
            holder.tvTime.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private boolean isInvalidPosition(int position) {
        return position < 0 || position >= messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
