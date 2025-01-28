package ua.ithillel.com.net.handler;

public interface ServerHandler {
    void onConnect(ClientHandler clientHandler);
    void onDisconnect(ClientHandler clientHandler);
    void onGlobalMessage(ClientHandler clientHandler, String message);
    void onPrivateMessage(ClientHandler clientHandler, String to, String message);
    void onAll(ClientHandler clientHandler);
    void onError(ClientHandler clientHandler, Throwable throwable);
}