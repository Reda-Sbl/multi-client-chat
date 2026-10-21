package com.chat.server.model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Accepts clients (one virtual thread each) and distributes messages. No JavaFX here. */
public class ChatServer {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final int port;
    private final ServerListener listener;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private volatile boolean running;
    private ServerSocket serverSocket;

    public ChatServer(int port, ServerListener listener) {
        this.port = port;
        this.listener = listener;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        listener.onLog("Server Started on port " + port);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            while (running) {
                listener.onLog("Waiting for Client...");
                Socket socket = serverSocket.accept();
                pool.submit(new ClientHandler(socket, this));
            }
        } catch (IOException e) {
            if (running) listener.onLog("Server error: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) { }
        clients.forEach(ClientHandler::close);
    }

    // ----- called by ClientHandler -----
    boolean isNameTaken(String name) {
        return clients.stream().anyMatch(c -> name.equalsIgnoreCase(c.getUsername()));
    }

    void register(ClientHandler c) { clients.add(c); fireUsers(); }

    void unregister(ClientHandler c) { clients.remove(c); fireUsers(); }

    void log(String msg) { listener.onLog(msg); }

    void broadcast(String line) { clients.forEach(c -> c.send(line)); }

    void broadcastChat(String sender, String text) {
        broadcast("CHAT [" + LocalTime.now().format(TIME) + "] " + sender + ": " + text);
    }

    /** Names of clients that have a username (read-only viewers are not listed). */
    List<String> activeUsers() {
        return clients.stream().filter(c -> !c.isReadOnly()).map(ClientHandler::getUsername).toList();
    }

    private void fireUsers() { listener.onUsersChanged(activeUsers()); }
}
