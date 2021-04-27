package server;

import commands.Command;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.*;

public class Server {
    private final int PORT = 29189;
    private ServerSocket server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Logger logger = Logger.getLogger(Server.class.getName());

    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() throws IOException {

        LogManager logManager = LogManager.getLogManager();
        logManager.readConfiguration(new FileInputStream("logging.properties"));
        clients = new CopyOnWriteArrayList<>();
        if (!DataBaseService.connect()) {
            throw new RuntimeException("Failed to connect to Database");
        }
        authService = new DataBaseService();
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started");
            logger.info("Server started");

            while (true) {
                socket = server.accept();
                System.out.println("Client connected");
                logger.info("Client connected");
                System.out.println("client: " + socket.getRemoteSocketAddress());
                logger.info("client: " + socket.getRemoteSocketAddress());
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                logger.info("Socket closed");
                DataBaseService.disconnect();
                logger.info("Database disconnected");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
                logger.info("Server closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg) throws SQLException, IOException {
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
        logger.info(sender.getNickname()+" sent message to All users");
        authService.savePrivateMessage(sender.getNickname(),"allusers", msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void privateMsg(ClientHandler sender,String receiver, String msg) throws SQLException, IOException {
        String message = String.format("[ %s ] to [ %s ]: %s", sender.getNickname(), receiver, msg);
        authService.savePrivateMessage(sender.getNickname(), receiver, msg);
        logger.info(sender.getNickname()+" sent message to "+receiver);
        for (ClientHandler c : clients) {
            if(c.getNickname().equals(receiver)){
                c.sendMsg(message);
                if(!c.equals(sender)){
                    sender.sendMsg(message);
                }
                return;
            }
        }
        sender.sendMsg("Not found user: "+ receiver);
        logger.info("Message fail");
    }

    public void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastClientList();
        logger.info("Client "+clientHandler.getLogin()+" subscribed as "+ clientHandler.getNickname());
    }

    public void unsubscribe(ClientHandler clientHandler){
        clients.remove(clientHandler);
        broadcastClientList();
        logger.info("Client "+clientHandler.getLogin()+" unsubscribed");
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthenticated(String login){
        for (ClientHandler c : clients) {
            if(c.getLogin().equals(login)){
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList(){
        StringBuilder sb = new StringBuilder(Command.CLIENT_LIST);

        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }

        String msg = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }
}
