package org.sudoku;

import com.google.gson.*;
import org.sudoku.shared.protocol.GameMessage;

import java.lang.reflect.Type;

/**
 * Handles JSON serialization and deserialization of GameMessage objects.
 */
public class MessageSerializer {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(GameMessage.class, new GameMessageAdapter())
            .create();

    /**
     * Serialize a GameMessage to JSON string
     */
    public static String serialize(GameMessage message) {
        JsonObject jsonObject = gson.toJsonTree(message).getAsJsonObject();
        jsonObject.addProperty("type", message.getClass().getSimpleName());
        return gson.toJson(jsonObject);
    }

    /**
     * Deserialize JSON string to GameMessage
     */
    public static GameMessage deserialize(String json) {
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            String type = jsonObject.get("type").getAsString();

            // Map type string to GameMessage class
            Class<? extends GameMessage> messageClass = getMessageClass(type);
            if (messageClass != null) {
                // Deserialize the entire JSON object (including the type field)
                // The type field will be ignored during deserialization
                return gson.fromJson(jsonObject, messageClass);
            }
            
            throw new IllegalArgumentException("Unknown message type: " + type);
        } catch (Exception e) {
            System.err.println("Error deserializing message: " + json);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Map message type name to corresponding class
     */
    private static Class<? extends GameMessage> getMessageClass(String type) {
        switch (type) {
            case "CreateGameRequest":
                return GameMessage.CreateGameRequest.class;
            case "CreateGameResponse":
                return GameMessage.CreateGameResponse.class;
            case "JoinGameRequest":
                return GameMessage.JoinGameRequest.class;
            case "WaitingForPlayer":
                return GameMessage.WaitingForPlayer.class;
            case "ListGamesRequest":
                return GameMessage.ListGamesRequest.class;
            case "ListGamesResponse":
                return GameMessage.ListGamesResponse.class;
            case "GameStart":
                return GameMessage.GameStart.class;
            case "MoveRequest":
                return GameMessage.MoveRequest.class;
            case "MoveResult":
                return GameMessage.MoveResult.class;
            case "GameEnd":
                return GameMessage.GameEnd.class;
            case "PlayerDisconnect":
                return GameMessage.PlayerDisconnect.class;
            case "ExitGameRequest":
                return GameMessage.ExitGameRequest.class;
            case "ErrorMessage":
                return GameMessage.ErrorMessage.class;
            case "Ping":
                return GameMessage.Ping.class;
            case "Pong":
                return GameMessage.Pong.class;
            default:
                return null;
        }
    }

    /**
     * Custom adapter for GameMessage sealed class
     */
    private static class GameMessageAdapter implements JsonSerializer<GameMessage>, JsonDeserializer<GameMessage> {
        @Override
        public JsonElement serialize(GameMessage src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src);
        }

        @Override
        public GameMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
                throws JsonParseException {
            return context.deserialize(json, typeOfT);
        }
    }
}
