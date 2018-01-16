package com.example;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.util.ArrayList;
import java.util.List;

@WebSocket(maxTextMessageSize = 1_000)
public class MySocket {
    final List<String> log = new ArrayList<>();

    @OnWebSocketConnect
    public void connected(Session session) {
        log.add("{connected}");
        session.getRemote().sendStringByFuture("TEST");
    }

    @OnWebSocketClose
    public void closed(int statusCode, String reason) {
        log.add("{closed}");
    }

    @OnWebSocketMessage
    public void message(String message) {
        log.add(message);
    }
}
