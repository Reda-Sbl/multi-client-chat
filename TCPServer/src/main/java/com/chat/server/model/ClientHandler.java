package com.chat.server.model;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Talks to one client. Protocol (one line each):
 *  client -> JOIN <name> | MSG <text> | ALLUSERS | LEAVE
 *  server -> CHAT <formatted> | INFO <text> | USERS <list> | REJECT <why> | BYE */
class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private String username = "";
    private boolean readOnly;
    private volatile boolean registered;

    ClientHandler(Socket socket, ChatServer server) { this.socket = socket; this.server = server; }

    String getUsername() { return username; }
    boolean isReadOnly() { return readOnly; }

    synchronized void send(String line) { if (out != null) out.println(line); }

    void close() { try { socket.close(); } catch (IOException ignored) { } }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            String first = in.readLine();
            if (first == null || !first.startsWith("JOIN")) return;
            username = first.substring(4).trim();
            readOnly = username.isEmpty();

            if (readOnly) {
                username = "viewer-" + socket.getPort();
                server.register(this);
                send("INFO READ-ONLY MODE: you can read the chat but cannot send messages.");
                server.log("Read-only viewer connected (" + username + ")");
            } else {
                if (server.isNameTaken(username)) { send("REJECT Username already taken"); return; }
                server.register(this);
                send("INFO Welcome " + username);
                server.log("Welcome " + username);
                server.broadcast("INFO " + username + " joined the chat");
            }
            registered = true;

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("LEAVE")) break;
                else if (line.equals("ALLUSERS")) send("USERS " + String.join(", ", server.activeUsers()));
                else if (line.startsWith("MSG ")) {
                    if (readOnly) send("INFO You are in READ-ONLY MODE and cannot send messages.");
                    else server.broadcastChat(username, line.substring(4));
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (registered) {
                server.unregister(this);
                server.log(username + " disconnected");
                if (!readOnly) server.broadcast("INFO " + username + " left the chat");
            }
            send("BYE");
            close();
        }
    }
}
