package com.chat.client.model;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** Socket logic for the client. Knows nothing about JavaFX. */
public class ChatClient {
    private final String host;
    private final int port;
    private final ChatListener listener;
    private final AtomicBoolean closed = new AtomicBoolean();
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean readOnly;
    private String reason = "Disconnected";

    public ChatClient(String host, int port, ChatListener listener) {
        this.host = host; this.port = port; this.listener = listener;
    }

    public boolean isReadOnly() { return readOnly; }

    /** Blank username => READ-ONLY MODE. */
    public void connect(String username) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        readOnly = username == null || username.isBlank();
        out.println("JOIN " + (readOnly ? "" : username.trim()));
        Thread.ofVirtual().start(this::readLoop);
    }

    /** Handles plain text and the commands allUsers / end / bye. */
    public void send(String text) {
        text = text.trim();
        if (text.isEmpty() || readOnly) return;
        if (text.equalsIgnoreCase("end") || text.equalsIgnoreCase("bye")) disconnect();
        else if (text.equals("allUsers")) out.println("ALLUSERS");
        else out.println("MSG " + text);
    }

    public void disconnect() {
        reason = "You left the chat";
        if (out != null) out.println("LEAVE");
        close();
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("CHAT ")) listener.onMessage(line.substring(5));
                else if (line.startsWith("INFO ")) listener.onMessage("* " + line.substring(5));
                else if (line.startsWith("USERS ")) listener.onMessage("* Active users: " + line.substring(6));
                else if (line.startsWith("REJECT ")) { reason = line.substring(7); break; }
                else if (line.equals("BYE")) break;
            }
        } catch (IOException ignored) {
            if (!closed.get()) reason = "Connection lost";
        } finally { close(); }
    }

    private void close() {
        if (closed.compareAndSet(false, true)) {
            try { socket.close(); } catch (IOException ignored) { }
            listener.onDisconnected(reason);
        }
    }
}
