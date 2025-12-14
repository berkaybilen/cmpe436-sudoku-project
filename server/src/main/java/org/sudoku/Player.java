package org.sudoku;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.sudoku.shared.protocol.GameMessage;

/**
 * Represents a connected player in a game.
 */
public class Player {
    private final int playerId;
    private final String name;
    private final WebSocket session;
    private static final Gson gson = new Gson();

    public Player(int playerId, String name, WebSocket session) {
        this.playerId = playerId;
        this.name = name;
        this.session = session;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public WebSocket getSession() {
        return session;
    }

    /**
     * Send a game message to this player
     */
    public void send(GameMessage message) {
        if (session != null && session.isOpen()) {
            String json = MessageSerializer.serialize(message);
            session.send(json);
        }
    }

    /**
     * Check if this player is still connected
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @Override
    public String toString() {
        return "Player{" +
                "playerId=" + playerId +
                ", name='" + name + '\'' +
                '}';
    }
}
