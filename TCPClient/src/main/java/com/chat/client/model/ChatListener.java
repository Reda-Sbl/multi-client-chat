package com.chat.client.model;

/** Callbacks from the model to the UI (may arrive on a non-UI thread). */
public interface ChatListener {
    void onMessage(String text);
    void onDisconnected(String reason);
}
