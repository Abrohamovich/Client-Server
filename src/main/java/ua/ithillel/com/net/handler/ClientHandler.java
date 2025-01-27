package ua.ithillel.com.net.handler;

public interface ClientHandler {
    void receiveMessage(String message);
    String getUsername();
}
