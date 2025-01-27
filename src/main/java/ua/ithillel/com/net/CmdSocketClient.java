package ua.ithillel.com.net;

import ua.ithillel.com.net.handler.ClientHandler;
import ua.ithillel.com.net.handler.ServerHandler;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdSocketClient implements ClientHandler, Runnable {
    private final Socket socket;
    private final String username;
    private final ServerHandler serverHandler;
    private BufferedReader in;
    private PrintWriter out;

    private static final Set<String> helpMessage = Set.of(
            "-h\tTo write help text",
            "-exit\tTo exit from the server",
            "-p username:message\tTo send private message to user");

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
            out.flush();
            helpMessage.forEach(this::receiveMessage);

            while (!socket.isClosed()) {
                String message = in.readLine();
                if (message.trim().equals("-exit")) {
                    break;
                } else if (message.trim().startsWith("-p")) {
                    String[] extractedParts = extract(message.trim());
                    if (extractedParts.length != 2) {
                        out.print("Invalid private message format. Use: -p username: message");
                        out.flush();
                        continue;
                    }
                    if (extractedParts[0].equals(username)) continue;
                    serverHandler.onPrivateMessage(this, extractedParts[0], extractedParts[1]);
                } else if (message.trim().equals("-h")) {
                    helpMessage.forEach(this::receiveMessage);
                } else {
                    serverHandler.onGlobalMessage(this, message.trim());
                }
            }
        } catch (IOException ex) {
            serverHandler.onError(this, ex);
        } finally {
            serverHandler.onDisconnect(this);
        }
    }

    @Override
    public void receiveMessage(String message) {
        out.println(message);
        out.flush();
    }

    @Override
    public String getUsername() {
        return username;
    }

    private String[] extract(String s) {
        String patternString = "-p\\s+(.*)\\s*:\\s*(.*)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(s);

        if (matcher.matches()) {
            String clientId = matcher.group(1);
            String message = matcher.group(2);
            return new String[]{clientId, message};
        } else {
            return new String[0];
        }
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