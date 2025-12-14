package org.sudoku;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utility class for network operations.
 */
public class NetworkUtils {
    
    /**
     * Get all non-loopback IPv4 addresses of this machine
     */
    public static List<String> getLocalIpAddresses() {
        List<String> addresses = new ArrayList<>();
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                
                // Skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress addr = inetAddresses.nextElement();
                    
                    // Only IPv4 addresses
                    if (addr.getAddress().length == 4) {
                        addresses.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error getting network interfaces: " + e.getMessage());
        }
        
        return addresses;
    }
    
    /**
     * Print server connection information
     */
    public static void printServerInfo(int port) {
        System.out.println("\n========================================");
        System.out.println("  Sudoku WebSocket Server Started");
        System.out.println("========================================");
        System.out.println("Port: " + port);
        System.out.println("\nConnect Android clients to:");
        
        List<String> addresses = getLocalIpAddresses();
        if (addresses.isEmpty()) {
            System.out.println("  ws://localhost:" + port);
        } else {
            for (String addr : addresses) {
                System.out.println("  ws://" + addr + ":" + port);
            }
        }
        
        System.out.println("\nWaiting for connections...");
        System.out.println("========================================\n");
    }
}
