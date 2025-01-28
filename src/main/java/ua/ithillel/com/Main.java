package ua.ithillel.com;

import ua.ithillel.com.net.SocketServer;

public class Main {
    public static void main(String[] args) {
        try (SocketServer hillelSocketServer = new SocketServer(8080)) {
            hillelSocketServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}