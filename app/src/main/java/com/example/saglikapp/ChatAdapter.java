package com.example.saglikapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private List<MessageModel> messageList;
    private final Markwon markwon;

    public ChatAdapter(List<MessageModel> messageList, Context context) {
        this.messageList = messageList;
        this.markwon = Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .build();
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageList.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserViewHolder) holder).userText.setText(message.getText());
        } else {
            markwon.setMarkdown(((BotViewHolder) holder).botText, message.getText());
        }
    }
    // Dolar işaretlerini markdown kalınına çevir
    private String preprocessText(String text) {
        // $kelime$ → **kelime**
        return text.replaceAll("\\$([^$\n]+)\\$", "**$1**");
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userText;
        UserViewHolder(View itemView) {
            super(itemView);
            userText = itemView.findViewById(R.id.userMessageTextView);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView botText;
        BotViewHolder(View itemView) {
            super(itemView);
            botText = itemView.findViewById(R.id.botMessageTextView);
        }
    }
}