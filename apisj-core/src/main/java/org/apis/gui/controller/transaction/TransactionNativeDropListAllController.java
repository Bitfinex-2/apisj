package org.apis.gui.controller.transaction;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.AnchorPane;
import org.apis.gui.controller.base.BaseViewController;
import org.apis.gui.manager.StringManager;

import java.net.URL;
import java.util.ResourceBundle;

public class TransactionNativeDropListAllController extends BaseViewController {
    @FXML
    private AnchorPane bgAnchor;
    @FXML
    private Label selectAllLabel;

    private TransactionNativeDropListAllImpl handler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Multilingual support
        languageSetting();

        // Background Color Setting
        bgAnchor.setOnMouseEntered(event -> bgAnchor.setStyle("-fx-background-color: #f2f2f2;"));
        bgAnchor.setOnMouseExited(event -> bgAnchor.setStyle("-fx-background-color: transparent;"));
    }

    public void languageSetting() {
        selectAllLabel.textProperty().bind(StringManager.getInstance().transaction.selectAllLabel);
    }

    @FXML
    private void onMouseClicked(InputEvent event) {
        String fxid = ((Node)event.getSource()).getId();

        if(fxid.equals("bgAnchor")) {
            if(handler != null) {
                handler.setDropLabel();
            }
        }
    }

    public String getSelectAllLabel() {
        return selectAllLabel.getText();
    }

    public void setSelectAllLabel(String selectAllLabel) {
        this.selectAllLabel.setText(selectAllLabel);
    }

    public void setHandler(TransactionNativeDropListAllImpl handler) {
        this.handler = handler;
    }

    public interface TransactionNativeDropListAllImpl {
        void setDropLabel();
    }
}