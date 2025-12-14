package org.sudoku.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void info(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("[" + timestamp + "] [INFO] " + message);
    }

    public static void error(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.err.println("[" + timestamp + "] [ERROR] " + message);
    }

    public static void error(String message, Exception e) {
        error(message);
        System.err.println("  Exception: " + e.getMessage());
        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < Math.min(5, trace.length); i++) {
            System.err.println("    at " + trace[i]);
        }
    }

    public static void warn(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.println("[" + timestamp + "] [WARN] " + message);
    }

    public static void separator() {
        System.out.println("==================================================");
    }
}