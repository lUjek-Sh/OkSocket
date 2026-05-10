package com.xuhao.didi.socket.client.impl.client.action;

import java.util.Locale;

final class SocketExceptionUtils {
    private SocketExceptionUtils() {
    }

    static boolean isConnectionShutdownException(Throwable throwable) {
        int depth = 0;
        Throwable current = throwable;
        while (current != null && depth < 8) {
            String message = current.getMessage();
            if (message != null) {
                String normalizedMessage = message.toLowerCase(Locale.US);
                if (normalizedMessage.contains("broken pipe")
                        || normalizedMessage.contains("connection reset")
                        || normalizedMessage.contains("connection abort")
                        || normalizedMessage.contains("software caused connection abort")
                        || normalizedMessage.contains("socket closed")
                        || normalizedMessage.contains("end of file")
                        || normalizedMessage.contains("disconnected by server")) {
                    return true;
                }
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }
}
