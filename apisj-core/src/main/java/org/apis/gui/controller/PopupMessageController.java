package org.apis.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.apis.gui.manager.PopupManager;
import org.apis.gui.manager.StringManager;

import java.net.URL;
import java.util.ResourceBundle;

public class PopupMessageController extends BasePopupController {
        @FXML
    private Label title, subTitle, yesBtn;
    @FXML
    public void exit(){
        PopupManager.getInstance().hideMainPopup(0);
        PopupManager.getInstance().hideMainPopup(1);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageSetting();

    }

    public void languageSetting() {
        yesBtn.textProperty().bind(StringManager.getInstance().popup.successYes);
    }

    public void setTitle(String title){
        this.title.setText(title);
    }

    public void setSubTitle(String subTitle){
        this.subTitle.setText(subTitle);
    }
}