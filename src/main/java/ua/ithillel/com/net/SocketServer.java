package ua.ithillel.com.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.ithillel.com.net.handler.ClientHandler;
import ua.ithillel.com.net.handler.ServerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SocketServer implements ServerHandler, AutoCloseable {
    private static final Logger log = LogManager.getLogger(SocketServer.class);

    private final Set<ClientHandler> activeConnections;
    private final ServerSocket serverSocket;
    private final AtomicInteger clientCounter;

    private static final String USER_JOINED_MESSAGE = "%s is online";
    private static final String USER_LEFT_MESSAGE = "%s is offline";
    private static final String GLOBAL_MESSAGE = "GLOBAL [%s]: %s";
    private static final String PRIVATE_MESSAGE = "PRIVATE [%s]: %s";
    private static final String USER_NOT_FOUND_MESSAGE = "User '%s' not found";
    private static final String SOMETHING_WENT_WRONG = "Something went wrong: %s";

    public SocketServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        activeConnections = new CopyOnWriteArraySet<>();
        clientCounter = new AtomicInteger(1);
    }

    public void start() {
        log.info("[SERVER] started on port {}", serverSocket.getLocalPort());
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                Runnable connection = new CmdSocketClient(clientSocket, this, clientCounter.getAndIncrement());
                new Thread(connection).start();
            } catch (IOException e) {
                log.error("[SERVER] error while starting server", e);
                break;
            }
        }
    }

    @Override
    public void onConnect(ClientHandler clientHandler) {
        String username = clientHandler.getUsername();
        log.info("[SERVER] {} joined the server", username);
        activeConnections.forEach(connection ->
        {
            if (!connection.getUsername().equals(username)) {
                connection.getMessage(String.format(USER_JOINED_MESSAGE, username));
            }
        });
        activeConnections.add(clientHandler);
    }

    @Override
    public void onDisconnect(ClientHandler clientHandler) {
        activeConnections.remove(clientHandler);
        String username = clientHandler.getUsername();

        activeConnections.forEach(connection ->
        {
            connection.getMessage(String.format(USER_LEFT_MESSAGE, username));
        });
        log.info("[SERVER] {} left the server", clientHandler.getUsername());
    }

    @Override
    public void onGlobalMessage(ClientHandler clientHandler, String message) {
        String username = clientHandler.getUsername();

        activeConnections.forEach(connection ->
        {
            if (!connection.getUsername().equals(username)) {
                connection.getMessage(String.format(GLOBAL_MESSAGE, username, message));
            }
        });

        log.info("[SERVER] global message ({}): {}", username, message);
    }

    @Override
    public void onPrivateMessage(ClientHandler clientHandler, String to, String message) {
        ClientHandler client = activeConnections.stream()
                .filter(c -> c.getUsername().equals(to))
                .findFirst()
                .orElse(null);

        if (client == null) {
            clientHandler.getMessage(String.format(USER_NOT_FOUND_MESSAGE, to));
            clientHandler.setPrivateChatUsername(null);
            return;
        }

        log.info("[SERVER] private message ({}) -> ({}) : {}", clientHandler.getUsername(), to, message);
        client.getMessage(String.format(PRIVATE_MESSAGE, clientHandler.getUsername(), message));
    }

    @Override
    public void onAll(ClientHandler clientHandler) {
        String message = activeConnections.stream()
                .map(ClientHandler::getUsername)
                .collect(Collectors.joining(", "));
        clientHandler.getMessage("Active users: " + message);
    }

    @Override
    public void onError(ClientHandler clientHandler, Throwable throwable) {
        log.error("[SERVER] error happened {}", throwable.getMessage());
        String username = clientHandler.getUsername();
        activeConnections.forEach(connection ->
        {
            if (!connection.getUsername().equals(username)) {
                connection.getMessage(String.format(SOMETHING_WENT_WRONG, username));
            }
        });
    }

    @Override
    public void close() throws Exception {
        activeConnections.forEach(c -> c.getMessage("Server is shutting down"));
        serverSocket.close();
    }
}