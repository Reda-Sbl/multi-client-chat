package com.chat.server.model;

import java.util.List;

/** Lets the View observe the Model without the Model knowing about JavaFX. */
public interface ServerListener {
    void onLog(String message);
    void onUsersChanged(List<String> usernames);
}
