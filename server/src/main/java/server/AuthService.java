package server;

import java.sql.SQLException;

public interface AuthService {
    String getNicknameByLoginAndPassword(String login, String password);
    boolean registration(String login, String password, String nickname);
    boolean changeNick(String nickname, String newNickName);
    void savePrivateMessage(String sender, String recipient, String message) throws SQLException;
    String publicMessage (String login) throws SQLException;
}
