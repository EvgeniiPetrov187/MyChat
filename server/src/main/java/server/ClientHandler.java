package server;

import commands.Command;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.logging.*;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;
    private Logger logger = Logger.getLogger(ClientHandler.class.getName());

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            LogManager logManager = LogManager.getLogManager();
            logManager.readConfiguration(new FileInputStream("logging.properties"));

            ExecutorService executorService = Executors.newFixedThreadPool(2);
            executorService.submit(() -> {
                try {
                    socket.setSoTimeout(120000);
                    while (true) {
                        String str = in.readUTF();
                        // disconnect/exit
                        if (str.equals(Command.END)) {
                            out.writeUTF(Command.END);
                            logger.info("Client will disconnect");
                            throw new RuntimeException("Client will disconnect");
                        }
                        // authentication
                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s", 3);
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    ClientHandler.this.sendMsg(Command.AUTH_OK + " " + nickname);
                                    server.subscribe(ClientHandler.this);
                                    socket.setSoTimeout(0);
                                    //sendMsg(server.getAuthService().publicMessage(nickname));  // public from Database
                                    System.out.println("Client: " + socket.getRemoteSocketAddress() +
                                            " connected with nick: " + nickname);
                                    break;
                                } else {
                                    ClientHandler.this.sendMsg("Account is already exist");
                                    logger.info("Account is already exist");
                                }
                            } else {
                                ClientHandler.this.sendMsg("Wrong login/password");
                                logger.info("Wrong login/password");
                            }
                        }

                        //registration
                        if (str.startsWith(Command.REG)) {
                            String[] token = str.split("\\s", 4);
                            if (token.length < 4) {
                                continue;
                            }
                            boolean regSuccess = server.getAuthService()
                                    .registration(token[1], token[2], token[3]);
                            if (regSuccess) {
                                ClientHandler.this.sendMsg(Command.REG_OK);
                                logger.info("New client "+login);
                            } else {
                                ClientHandler.this.sendMsg(Command.REG_NO);
                                logger.info("Registration fail");
                            }
                        }
                    }
                    // activity
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                out.writeUTF(Command.END);
                                break;
                            }

                            if (str.startsWith(Command.PRIVATE_MSG)) {
                                String[] token = str.split("\\s", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(ClientHandler.this, token[1], token[2]);
                            }

                            // change nickname
                            if (str.startsWith(Command.CHANGE_NICK)) {
                                String[] token = str.split("\\s+", 2);
                                if (token.length < 2) {
                                    continue;
                                }
                                if (token[1].contains(" ")) {
                                    ClientHandler.this.sendMsg("Nickname cannot contain spaces");
                                    continue;
                                }
                                if (server.getAuthService().changeNick(ClientHandler.this.nickname, token[1])) {
                                    ClientHandler.this.sendMsg(Command.CHANGE_NICK + token[1]);
                                    ClientHandler.this.sendMsg("Your nickname has been changed to " + token[1]);
                                    logger.info("Client "+nickname+" is now "+token[1]);
                                    ClientHandler.this.nickname = token[1];
                                    server.broadcastClientList();

                                } else {
                                    ClientHandler.this.sendMsg("Failed to change nickname. Nickname " + token[1] + " is already exist");
                                    logger.info("Change nickname by "+nickname+" fail");
                                }
                            }
                        } else {
                            server.broadcastMsg(ClientHandler.this, str);
                        }
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } catch (SocketTimeoutException e) {
                    ClientHandler.this.sendMsg(Command.END);
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(ClientHandler.this);
                    System.out.println("Client disconnected: " + nickname);
                    logger.info("Client disconnected: " + nickname);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
