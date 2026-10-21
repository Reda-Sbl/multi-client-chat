package com.chat.client;

import com.chat.client.view.ClientApp;
import javafx.application.Application;

/** Entry point: java -jar TCPClient.jar <ServerIPAddress> <PortNumber> */
public class TCPClient {
    public static void main(String[] args) { Application.launch(ClientApp.class, args); }
}
