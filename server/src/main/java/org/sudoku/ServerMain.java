package org.sudoku;

/**
 * Main entry point for the Sudoku WebSocket server.
 */
public class ServerMain {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Allow port override via command line argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.err.println("Using default port: " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }
        
        // Create and start the WebSocket server
        SudokuWebSocketServer server = new SudokuWebSocketServer(port);
        
        // Add shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Server] Shutting down...");
            try {
                server.stop(1000);
                System.out.println("[Server] Server stopped gracefully");
            } catch (InterruptedException e) {
                System.err.println("[Server] Error during shutdown: " + e.getMessage());
            }
        }));
        
        // Start the server
        server.start();
        
        // Print server connection information
        NetworkUtils.printServerInfo(port);
    }
}
