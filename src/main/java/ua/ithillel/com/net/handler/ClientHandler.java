package ua.ithillel.com.net.handler;

public interface ClientHandler {
    void getMessage(String message);
    String getUsername();
    void setPrivateChatUsername(String username);
}
