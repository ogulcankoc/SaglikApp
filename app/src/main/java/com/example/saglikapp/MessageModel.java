package com.example.saglikapp;

public class MessageModel {
    private String text;
    private boolean isUser;

    public MessageModel(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public String getText() {
        return text;
    }

    // YENİ: Metni güncellemek için setter ekledik (Akış/Streaming için)
    public void setText(String text) {
        this.text = text;
    }

    public boolean isUser() {
        return isUser;
    }
}