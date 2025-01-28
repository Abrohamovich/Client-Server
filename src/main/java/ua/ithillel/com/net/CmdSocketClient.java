package ua.ithillel.com.net;

import ua.ithillel.com.net.handler.ClientHandler;
import ua.ithillel.com.net.handler.ServerHandler;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

public class CmdSocketClient implements ClientHandler, Runnable {
    private String privateChatUsername = null;
    private final Socket socket;
    private final String username;
    private final ServerHandler serverHandler;
    private BufferedReader in;
    private PrintWriter out;

    private static final String[] helpMessage = {
            "-h\tWrite user commands",
            "-exit\tExit from the server",
            "-global\tEnter the global chat",
            "-all\tSee the list of active users",
            "-p username\tEnter the private chat with user"
    };

    public CmdSocketClient(Socket socket, ServerHandler serverHandler, int clientId) {
        this.serverHandler = serverHandler;
        this.socket = socket;
        this.username = String.format("client-%d", clientId);
    }

    @Override
    public void run() {
        try (socket) {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            serverHandler.onConnect(this);

            out.println("Herzlich Willkommen " + username);
            out.println("By default you send messages to the server");
            for (String line : helpMessage) {
                out.println(line);
                out.flush();
            }

            while (!socket.isClosed()) {
                String message = in.readLine();

                if (message == null) continue;

                if (message.startsWith("-")) {
                    switch (message.trim()) {
                        case "-exit":
                            socket.close();
                            break;
                        case "-h":
                            for (String line : helpMessage) {
                                out.println(line);
                                out.flush();
                            }
                            break;
                        case "-all":
                            serverHandler.onAll(this);
                            break;
                        case "-global":
                            privateChatUsername = null;
                            out.println("You are now in global chat mode");
                            out.flush();
                            break;
                        default:
                            if (message.startsWith("-p ")) {
                                String[] parts = message.split(" ", 2);
                                if (parts.length == 2) {
                                    privateChatUsername = parts[1].trim();
                                    if (privateChatUsername.equals(username)) {
                                        out.println("You cant sand messages to yourself");
                                        out.flush();
                                        privateChatUsername = null;
                                        break;
                                    }
                                    out.println("You are now in private chat mode with " + privateChatUsername);
                                    out.flush();
                                } else {
                                    out.println("Usage: -p username");
                                    out.flush();
                                }
                            } else {
                                out.println("Unknown command. Use -h for help.");
                                out.flush();
                            }
                            break;
                    }
                } else {
                    if (privateChatUsername == null) {
                        serverHandler.onGlobalMessage(this, message);
                    } else {
                        serverHandler.onPrivateMessage(this, privateChatUsername, message);
                    }
                }
            }
        } catch (IOException ex) {
            serverHandler.onError(this, ex);
        } finally {
            serverHandler.onDisconnect(this);
        }
    }

    @Override
    public void getMessage(String message) {
        out.println(message);
        out.flush();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setPrivateChatUsername(String username) {
        privateChatUsername = username;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CmdSocketClient csc = (CmdSocketClient) o;
        return Objects.equals(socket, csc.socket) && Objects.equals(username, csc.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(socket, username);
    }
}