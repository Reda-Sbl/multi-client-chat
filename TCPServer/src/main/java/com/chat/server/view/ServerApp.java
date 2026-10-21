package com.chat.server.view;

import com.chat.server.model.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

/** JavaFX view. Only displays what the ChatServer model reports. */
public class ServerApp extends Application implements ServerListener {
    private final TextArea logArea = new TextArea();
    private final ObservableList<String> users = FXCollections.observableArrayList();
    private final Map<String, String> colors = new HashMap<>();
    private final Random random = new Random();
    private final Label countLabel = new Label("0 users online");
    private ChatServer server;

    @Override
    public void start(Stage stage) {
        int port = AppConfig.load().port();

        // header bar
        Label title = new Label("TCP Chat Server");
        title.getStyleClass().add("title");
        Circle dot = new Circle(6, Color.LIMEGREEN);
        Label running = new Label("Running on port " + port);
        running.getStyleClass().add("status");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, title, spacer, dot, running);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        // log (left) and user list (right)
        logArea.setEditable(false);
        logArea.setId("log");
        ListView<String> userList = new ListView<>(users);
        userList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) { setText(null); setStyle(""); return; }
                setText(name);
                setStyle("-fx-background-color: " + colors.computeIfAbsent(name, k -> randomColor()) + ";");
            }
        });
        userList.setPlaceholder(new Label("No users yet"));
        countLabel.getStyleClass().add("section");
        Label logTitle = new Label("Activity log");
        logTitle.getStyleClass().add("section");

        GridPane body = new GridPane();
        body.setHgap(14); body.setVgap(8); body.setPadding(new Insets(14));
        body.add(logTitle, 0, 0);
        body.add(countLabel, 1, 0);
        body.add(logArea, 0, 1);
        body.add(userList, 1, 1);
        GridPane.setHgrow(logArea, Priority.ALWAYS);
        GridPane.setVgrow(logArea, Priority.ALWAYS);
        userList.setPrefWidth(210);

        BorderPane root = new BorderPane(body);
        root.setTop(header);
        root.getStyleClass().add("server-root");

        Scene scene = new Scene(root, 820, 520);
        scene.getStylesheets().add(getClass().getResource("/server.css").toExternalForm());
        stage.setTitle("TCPServer");
        stage.setScene(scene);
        stage.show();

        server = new ChatServer(port, this);
        Thread.ofVirtual().start(() -> {
            try { server.start(); } catch (IOException e) { onLog("Could not start: " + e.getMessage()); }
        });
    }

    @Override public void stop() { if (server != null) server.stop(); }

    @Override public void onLog(String message) { Platform.runLater(() -> logArea.appendText(message + "\n")); }

    @Override public void onUsersChanged(List<String> names) {
        Platform.runLater(() -> {
            users.setAll(names);
            countLabel.setText(names.size() + (names.size() == 1 ? " user online" : " users online"));
        });
    }

    private String randomColor() {  // light pastel so dark text stays readable
        return String.format("rgb(%d,%d,%d)", 150 + random.nextInt(95), 150 + random.nextInt(95), 150 + random.nextInt(95));
    }
}
