package org.sudoku.sudoku.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.sudoku.shared.protocol.GameMessage
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.LogLevel
import java.text.SimpleDateFormat
import java.util.*

class GameClient {
    // Define the polymorphic serialization module
    private val gameMessageModule = SerializersModule {
        polymorphic(GameMessage::class) {
            subclass(GameMessage.CreateGameRequest::class)
            subclass(GameMessage.CreateGameResponse::class)
            subclass(GameMessage.JoinGameRequest::class)
            subclass(GameMessage.WaitingForPlayer::class)
            subclass(GameMessage.ListGamesRequest::class)
            subclass(GameMessage.ListGamesResponse::class)
            subclass(GameMessage.GameStart::class)
            subclass(GameMessage.MoveRequest::class)
            subclass(GameMessage.MoveResult::class)
            subclass(GameMessage.GameEnd::class)
            subclass(GameMessage.PlayerDisconnect::class)
            subclass(GameMessage.ErrorMessage::class)
            subclass(GameMessage.Ping::class)
            subclass(GameMessage.Pong::class)
        }
    }

    // Create a Json instance with custom configuration to use simple class names
    private val json = Json {
        serializersModule = gameMessageModule
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
        classDiscriminator = "type"
        // This will use simple class names instead of fully qualified names
        useArrayPolymorphism = false
    }

    private val client = HttpClient {

        install(Logging) {
            // Defines where to print the logs
            logger = object : Logger {
                override fun log(message: String) {
                    // This forces Ktor logs to appear in your Android Logcat
                    Log.d("KtorHttp", message)
                }
            }
            // ALL gives you Headers + Body.
            // Essential to see if the server sends a "Close" frame or if it just dies.
            level = LogLevel.ALL
        }

        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
            pingInterval = 15_000 // ping to keep conneciton alive with GCP.
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private val _messages = MutableSharedFlow<GameMessage>()
    val messages: SharedFlow<GameMessage> = _messages.asSharedFlow()
    
    // Ping-pong mechanism
    private var pingJob: Job? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private fun logWithTimestamp(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        Log.d(tag, "[$timestamp] $message")
    }

    suspend fun connect() {
        try {
            val wsUrl = "wss://${NetworkConfig.HOST}:${NetworkConfig.PORT}/game"
            logWithTimestamp("GameClient", "===== WEBSOCKET CONNECTION ATTEMPT =====")
            logWithTimestamp("GameClient", "Attempting to connect to: $wsUrl")
            logWithTimestamp("GameClient", "Host: ${NetworkConfig.HOST}")
            logWithTimestamp("GameClient", "Port: ${NetworkConfig.PORT}")
            
            client.webSocket(
                method = HttpMethod.Get,
                host = NetworkConfig.HOST,
                port = NetworkConfig.PORT,
                path = "/game",
                request = {
                    url.protocol = URLProtocol.WSS
                }
            ) {
                session = this
                logWithTimestamp("GameClient", "===== WEBSOCKET SESSION ESTABLISHED =====")
                logWithTimestamp("GameClient", "WebSocket connection successful!")
                
                // Start ping-pong mechanism
                startPingPong()
                
                try {
                    logWithTimestamp("GameClient", "Starting to listen for incoming frames...")
                    for (frame in incoming) {
                        logWithTimestamp("GameClient", "Frame received - Type: ${frame.frameType}")
                        
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                logWithTimestamp("GameClient", "Received TEXT frame: $text")
                                try {
                                    val message = json.decodeFromString<GameMessage>(text)
                                    logWithTimestamp("GameClient", "Parsed message type: ${message::class.simpleName}")
                                    _messages.emit(message)
                                } catch (e: Exception) {
                                    Log.e("GameClient", "Error parsing message: ${e.message}", e)
                                    logWithTimestamp("GameClient", "Failed to parse: $text")
                                }
                            }
                            is Frame.Binary -> {
                                logWithTimestamp("GameClient", "Received BINARY frame (${frame.data.size} bytes)")
                            }
                            is Frame.Close -> {
                                val reason = frame.readReason()
                                logWithTimestamp("GameClient", "===== RECEIVED CLOSE FRAME =====")
                                logWithTimestamp("GameClient", "Close code: ${reason?.code}")
                                logWithTimestamp("GameClient", "Close reason: ${reason?.message}")
                            }
                            is Frame.Ping -> {
                                logWithTimestamp("GameClient", "Received PING frame")
                            }
                            is Frame.Pong -> {
                                logWithTimestamp("GameClient", "Received PONG frame")
                            }
                        }
                    }
                    logWithTimestamp("GameClient", "Frame loop ended normally")
                } catch (e: Exception) {
                    Log.e("GameClient", "Error reading frames: ${e.message}", e)
                    logWithTimestamp("GameClient", "Exception type: ${e::class.simpleName}")
                    logWithTimestamp("GameClient", "Stack trace: ${e.stackTraceToString()}")
                } finally {
                    logWithTimestamp("GameClient", "Stopping ping-pong mechanism")
                    pingJob?.cancel()
                    pingJob = null
                }
            }
            logWithTimestamp("GameClient", "===== WEBSOCKET BLOCK EXITED =====")
        } catch (e: Exception) {
            Log.e("GameClient", "Connection failed: ${e.message}", e)
            logWithTimestamp("GameClient", "===== CONNECTION FAILURE =====")
            logWithTimestamp("GameClient", "Exception type: ${e::class.simpleName}")
            logWithTimestamp("GameClient", "Error message: ${e.message}")
            logWithTimestamp("GameClient", "Stack trace: ${e.stackTraceToString()}")
            _messages.emit(GameMessage.ErrorMessage("Connection failed: ${e.message}"))
        } finally {
            session = null
            logWithTimestamp("GameClient", "===== CONNECTION CLEANUP COMPLETE =====")
            logWithTimestamp("GameClient", "Session set to null")
        }
    }

    private fun CoroutineScope.startPingPong() {
        logWithTimestamp("GameClient", "===== STARTING PING-PONG MECHANISM =====")
        pingJob = launch {
            try {
                while (isActive) {
                    delay(15_000) // Send ping every 15 seconds
                    logWithTimestamp("GameClient", "Sending PING message")
                    send(GameMessage.Ping)
                }
            } catch (e: CancellationException) {
                logWithTimestamp("GameClient", "Ping job cancelled")
            } catch (e: Exception) {
                Log.e("GameClient", "Error in ping-pong mechanism: ${e.message}", e)
                logWithTimestamp("GameClient", "Ping-pong error: ${e.message}")
            }
        }
    }

    suspend fun send(message: GameMessage) {
        try {
            // Use the polymorphic serializer for the base class GameMessage
            val jsonString = json.encodeToString(GameMessage.serializer(), message)
            val messageType = message::class.simpleName
            
            if (message !is GameMessage.Ping) {
                logWithTimestamp("GameClient", "Sending $messageType: $jsonString")
            } else {
                logWithTimestamp("GameClient", "Sending PING")
            }
            
            session?.send(Frame.Text(jsonString))
        } catch (e: Exception) {
            Log.e("GameClient", "Error sending message: ${e.message}", e)
            logWithTimestamp("GameClient", "Send error: ${e.message}")
        }
    }

    suspend fun close() {
        logWithTimestamp("GameClient", "===== CLOSING CONNECTION =====")
        pingJob?.cancel()
        pingJob = null
        session?.close()
        client.close()
        logWithTimestamp("GameClient", "Connection closed")
    }
}
