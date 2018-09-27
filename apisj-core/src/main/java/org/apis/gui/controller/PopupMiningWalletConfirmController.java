package org.apis.gui.controller;

import com.google.zxing.WriterException;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.scene.shape.Rectangle;
import org.apis.gui.common.IdenticonGenerator;
import org.apis.gui.manager.AppManager;
import org.apis.gui.manager.PopupManager;
import org.apis.gui.manager.StringManager;
import org.apis.gui.model.WalletItemModel;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PopupMiningWalletConfirmController extends BasePopupController {
    private WalletItemModel itemModel;
    private boolean isChangable = false;

    @FXML private Label title, subTitle, passwordLabel, addressLabel,addressComment, address, startBtn;
    @FXML private ImageView addressIcon;
    @FXML private ApisTextFieldController passwordFieldController;

    private PopupMiningWalletConfirmImpl handler;

    @FXML
    private void onMouseClicked(InputEvent event){
        String id = ((Node)event.getSource()).getId();
        if( AppManager.getInstance().startMining(this.itemModel.getId(), passwordFieldController.getText()) ){
            AppManager.getInstance().setMiningWalletId(this.itemModel.getId());
            PopupManager.getInstance().showMainPopup("popup_success.fxml",zIndex+1);
            if(handler != null) {
                handler.changeBtnColor();
            }
            AppManager.getInstance().guiFx.getWallet().initWalletList();
        }else{
            passwordFieldController.failedForm(StringManager.getInstance().common.walletPasswordCheck.get());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageSetting();
        passwordFieldController.setHandler(new ApisTextFieldController.ApisTextFieldControllerInterface() {
            @Override
            public void onFocusOut() {

            }

            @Override
            public void change(String old_text, String new_text) {
                if(new_text == null || new_text.length() == 0){
                    failedForm();
                }else{
                    succeededForm();
                }
            }
        });

        // set a clip to apply rounded border to the original image.
        // set a clip to apply rounded border to the original image.
        Rectangle clip = new Rectangle( this.addressIcon.getFitWidth()-0.5, this.addressIcon.getFitHeight()-0.5 );
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        addressIcon.setClip(clip);
    }
    public void languageSetting() {
        title.textProperty().bind(StringManager.getInstance().popup.miningWalletConfirmTitle);
        subTitle.textProperty().bind(StringManager.getInstance().popup.miningWalletConfirmSubTitle);
        addressLabel.textProperty().bind(StringManager.getInstance().popup.miningWaleltConfirmAddress);
        passwordLabel.textProperty().bind(StringManager.getInstance().popup.miningWalletConfirmPassword);
        startBtn.textProperty().bind(StringManager.getInstance().popup.miningWalletConfirmStart);
        addressComment.textProperty().bind(StringManager.getInstance().popup.miningWalletConfirmAddressComment);
    }


    public void failedForm(){
        startBtn.setStyle("-fx-border-radius : 24 24 24 24; -fx-background-radius: 24 24 24 24; -fx-background-color: #d8d8d8 ;");
        isChangable = false;
    }

    public void succeededForm(){
        startBtn.setStyle("-fx-border-radius : 24 24 24 24; -fx-background-radius: 24 24 24 24; -fx-background-color: #910000 ;");
        isChangable = true;
    }

    public void setModel(WalletItemModel model) {
        this.itemModel = model;
        address.textProperty().setValue(this.itemModel.getAddress());
        addressIcon.setImage(this.itemModel.getIdenticon());
    }

    public void setHandler(PopupMiningWalletConfirmImpl handler) {
        this.handler = handler;
    }

    public interface PopupMiningWalletConfirmImpl {
        void changeBtnColor();
    }
}
