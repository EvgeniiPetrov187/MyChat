package client;

import commands.Command;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.net.Socket;

public class RegController {
    @FXML
    public TextField ipAddressReg;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField nicknameField;
    @FXML
    private TextArea textArea;

    private Controller controller;

    private Socket socket;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setResultTryToReg(String command) {
        if (command.equals(Command.REG_OK)) {
            textArea.appendText("Registration completed successfully\n");
        }
        if (command.equals(Command.REG_NO)) {
            textArea.appendText("Login or nickname is already exist\n");
        }
    }

    public void tryToReg(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknameField.getText().trim();
        String ipAddress = ipAddressReg.getText().trim();

        if (login.length() * password.length() * nickname.length() == 0) {
            return;
        }
        controller.registration(login, password, nickname, ipAddress);
    }
}
