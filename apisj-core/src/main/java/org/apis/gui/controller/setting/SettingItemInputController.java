package org.apis.gui.controller.setting;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingItemInputController implements Initializable {
    @FXML
    private Label contents;
    @FXML
    private TextField textField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ImageView coverImg;

    private Image privateIcon, publicIcon;
    public final static String SETTING_ITEM_INPUT_TEXT = "textField";
    public final static String SETTING_ITEM_INPUT_PASS = "passwordField";
    private String inputFlag = SETTING_ITEM_INPUT_TEXT;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Image setting
        privateIcon = new Image("image/ic_private@2x.png");
        publicIcon = new Image("image/ic_public@2x.png");

        // Add listener
        textField.focusedProperty().addListener(inputListener);
        passwordField.focusedProperty().addListener(inputListener);
    }

    public void setInput(String field) {
        if(field.equals(SETTING_ITEM_INPUT_TEXT)) {
            passwordField.setVisible(false);
            coverImg.setVisible(false);
            textField.setVisible(true);
            textField.setPadding(new Insets(0, 16, 0, 16));

        } else if(field.equals(SETTING_ITEM_INPUT_PASS)) {
            textField.textProperty().bindBidirectional(passwordField.textProperty());
        }
    }

    private ChangeListener<Boolean> inputListener = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue observable, Boolean oldValue, Boolean newValue) {
            inputFocused();
        }
    };

    private void inputFocused() {
        if(textField.isFocused()) {
            textField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #999999; -fx-border-radius : 4 4 4 4; -fx-background-radius: 4 4 4 4;" +
                    " -fx-font-family: 'Noto Sans KR Medium'; -fx-font-size:12px; -fx-text-fill: #2b2b2b;");
        } else {
            textField.setStyle("-fx-background-color: #f2f2f2; -fx-border-color: #d8d8d8; -fx-border-radius : 4 4 4 4; -fx-background-radius: 4 4 4 4;" +
                    " -fx-font-family: 'Noto Sans KR Medium'; -fx-font-size:12px; -fx-text-fill: #2b2b2b;");
        }

        if(passwordField.isFocused()) {
            passwordField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #999999; -fx-border-radius : 4 4 4 4; -fx-background-radius: 4 4 4 4;" +
                    " -fx-font-family: 'Noto Sans KR Medium'; -fx-font-size:12px; -fx-text-fill: #2b2b2b;");
        } else {
            passwordField.setStyle("-fx-background-color: #f2f2f2; -fx-border-color: #d8d8d8; -fx-border-radius : 4 4 4 4; -fx-background-radius: 4 4 4 4;" +
                    " -fx-font-family: 'Noto Sans KR Medium'; -fx-font-size:12px; -fx-text-fill: #2b2b2b;");
        }
    }

    @FXML
    private void onMouseClicked(InputEvent event) {
        String fxid = ((Node)event.getSource()).getId();

        if(fxid.equals("coverImg")) {
            if(textField.isVisible()) {
                coverImg.setImage(privateIcon);
                textField.setVisible(false);
                passwordField.setVisible(true);

            } else {
                coverImg.setImage(publicIcon);
                textField.setVisible(true);
                passwordField.setVisible(false);
            }
        }
    }

    public void setContents(String contents) {
        this.contents.setText(contents);
    }

    public String getContents() {
        return this.contents.getText();
    }

    public void setTextField(String text) {
        this.textField.setText(text);
    }

    public String getTextField() {
        return this.textField.getText();
    }

}
