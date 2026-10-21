package com.chat.client.view;

import com.chat.client.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.List;

/** JavaFX view. Shows what ChatClient reports and forwards user actions to it. */
public class ClientApp extends Application implements ChatListener {
    private Stage stage;
    private String host;
    private int port;
    private ChatClient client;
    private String myName = "";

    private VBox messages;          // holds the chat bubbles
    private ScrollPane scroll;
    private Circle dot;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        AppConfig cfg = AppConfig.load();
        List<String> a = getParameters().getRaw();
        host = a.size() >= 1 ? a.get(0) : cfg.host();
        port = a.size() >= 2 ? Integer.parseInt(a.get(1)) : cfg.port();
        stage.setTitle("Chat Client");
        stage.setMinWidth(420);
        stage.setMinHeight(480);
        showLogin("", false);
        stage.show();
    }

    @Override public void stop() { if (client != null) client.disconnect(); }

    // ---------------------------------------------------------------- login screen
    private void showLogin(String message, boolean isError) {
        Label title = new Label("Join the chat");
        title.getStyleClass().add("login-title");
        Label sub = new Label("Server: " + host + ":" + port);
        sub.getStyleClass().add("muted");

        TextField name = new TextField();
        name.setPromptText("Choose a username");
        Label hint = new Label("Leave it empty to join in READ-ONLY MODE");
        hint.getStyleClass().add("muted");

        Label feedback = new Label(message);
        feedback.getStyleClass().add(isError ? "error" : "muted");
        feedback.setWrapText(true);

        Button connect = new Button("CONNECT");
        connect.getStyleClass().add("primary");
        connect.setMaxWidth(Double.MAX_VALUE);
        connect.setOnAction(e -> connect(name.getText(), feedback));
        name.setOnAction(connect.getOnAction());

        GridPane card = new GridPane();
        card.getStyleClass().add("card");
        card.setVgap(10);
        card.setMaxWidth(320);
        card.add(title, 0, 0);
        card.add(sub, 0, 1);
        card.add(name, 0, 2);
        card.add(hint, 0, 3);
        card.add(connect, 0, 4);
        card.add(feedback, 0, 5);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(connect, Priority.ALWAYS);
        GridPane.setMargin(connect, new Insets(6, 0, 0, 0));

        StackPane root = new StackPane(card);
        root.getStyleClass().add("login-root");
        setScene(new Scene(root, 460, 520));
    }

    private void connect(String username, Label feedback) {
        client = new ChatClient(host, port, this);
        try {
            client.connect(username);
            myName = username == null ? "" : username.trim();
            showChat(client.isReadOnly());
        } catch (Exception e) {
            feedback.getStyleClass().setAll("error");
            feedback.setText("Cannot connect: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- chat screen
    private void showChat(boolean readOnly) {
        // header bar: status circle + "Online" + who am I + LEAVE
        dot = new Circle(6, Color.LIMEGREEN);
        statusLabel = new Label("Online");
        statusLabel.getStyleClass().add("status");
        Label who = new Label(readOnly ? "READ-ONLY MODE" : myName);
        who.getStyleClass().add(readOnly ? "badge-readonly" : "who");
        Region spacer = new Region();
        Button leave = new Button("LEAVE");
        leave.getStyleClass().add("danger");
        leave.setOnAction(e -> client.disconnect());
        HBox header = new HBox(8, dot, statusLabel, spacer, who, leave);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // message area
        messages = new VBox(8);
        messages.setPadding(new Insets(12));
        scroll = new ScrollPane(messages);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("chat-scroll");
        messages.heightProperty().addListener((o, oldV, newV) -> scroll.setVvalue(1.0));

        // input bar
        TextArea input = new TextArea();
        input.setPromptText(readOnly ? "Read-only: you cannot send messages" : "Type a message… (Enter = send, Shift+Enter = new line)");
        input.setPrefRowCount(2);
        input.setWrapText(true);
        Button send = new Button("SEND");
        send.getStyleClass().add("primary");
        send.setMaxHeight(Double.MAX_VALUE);
        input.setDisable(readOnly);
        send.setDisable(readOnly);

        Runnable doSend = () -> { client.send(input.getText()); input.clear(); };
        send.setOnAction(e -> doSend.run());
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) { e.consume(); doSend.run(); }
        });

        GridPane root = new GridPane();
        root.getStyleClass().add("chat-root");
        root.setVgap(0);
        root.add(header, 0, 0, 2, 1);
        root.add(scroll, 0, 1, 2, 1);
        root.add(input, 0, 2);
        root.add(send, 1, 2);
        GridPane.setHgrow(scroll, Priority.ALWAYS);
        GridPane.setVgrow(scroll, Priority.ALWAYS);
        GridPane.setHgrow(input, Priority.ALWAYS);
        GridPane.setMargin(input, new Insets(10, 6, 10, 10));
        GridPane.setMargin(send, new Insets(10, 10, 10, 0));
        ColumnConstraints c0 = new ColumnConstraints(); c0.setHgrow(Priority.ALWAYS);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setMinWidth(80);
        root.getColumnConstraints().addAll(c0, c1);
        header.setMaxWidth(Double.MAX_VALUE);

        setScene(new Scene(root, 480, 620));
    }

    private void setScene(Scene s) {
        s.getStylesheets().add(getClass().getResource("/client.css").toExternalForm());
        stage.setScene(s);
    }

    // ---------------------------------------------------------------- bubbles
    /** Turns a text line from the model into a bubble (own = right/blue, others = left/grey, system = centered). */
    private void addMessage(String text) {
        Node node;
        int close = text.indexOf("] ");
        int colon = close < 0 ? -1 : text.indexOf(": ", close);
        if (text.startsWith("* ") || close < 0 || colon < 0) {
            Label l = new Label(text.startsWith("* ") ? text.substring(2) : text);
            l.getStyleClass().add("system-msg");
            l.setWrapText(true);
            HBox box = new HBox(l);
            box.setAlignment(Pos.CENTER);
            node = box;
        } else {
            String time = text.substring(1, close);
            String sender = text.substring(close + 2, colon);
            String body = text.substring(colon + 2);
            boolean mine = sender.equals(myName);

            Label who = new Label(mine ? "You" : sender);
            who.getStyleClass().add("sender");
            Label msg = new Label(body);
            msg.setWrapText(true);
            msg.setMaxWidth(300);
            msg.getStyleClass().addAll("bubble", mine ? "bubble-mine" : "bubble-other");
            Label t = new Label(time);
            t.getStyleClass().add("time");

            Pos align = mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;
            VBox col = new VBox(2, who, msg, t);
            col.setAlignment(align);
            HBox row = new HBox(col);
            row.setAlignment(align);
            node = row;
        }
        messages.getChildren().add(node);
    }

    // ---------------------------------------------------------------- ChatListener (called from background threads)
    @Override public void onMessage(String text) {
        Platform.runLater(() -> { if (messages != null) addMessage(text); });
    }

    @Override public void onDisconnected(String reason) {
        Platform.runLater(() -> {
            if (dot != null) { dot.setFill(Color.CRIMSON); statusLabel.setText("Offline"); }
            boolean bad = !reason.equals("You left the chat");
            showLogin(reason, bad);
        });
    }
}
