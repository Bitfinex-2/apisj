package org.apis.gui.controller.popup;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Ellipse;
import javafx.scene.control.TextField;
import org.apis.db.sql.DBManager;
import org.apis.gui.controller.base.BasePopupController;
import org.apis.gui.manager.AppManager;
import org.apis.gui.manager.ImageManager;
import org.apis.gui.manager.PopupManager;
import org.apis.gui.manager.StringManager;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.net.URL;
import java.util.ResourceBundle;

public class PopupTokenAddController extends BasePopupController {

    @FXML private AnchorPane rootPane;
    @FXML private ImageView addrCircleImg, resultAddrCircleImg;
    @FXML private TextField tokenAddressTextField, nameTextField, symbolTextField, decimalTextField, totalSupplyTextField;
    @FXML private Label addTokenTitle, addTokenDesc, contractAddrLabel, nameLabel, minNumLabel, previewLabel, noBtn, addBtn, supplyLabel, symbolLabel;
    @FXML private ScrollPane scrollPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Multilingual Support
        languageSetting();

        Ellipse ellipse = new Ellipse(12, 12);
        ellipse.setCenterX(12);
        ellipse.setCenterY(12);

        addrCircleImg.setClip(ellipse);

        Ellipse ellipse2 = new Ellipse(12, 12);
        ellipse2.setCenterX(12);
        ellipse2.setCenterY(12);
        resultAddrCircleImg.setClip(ellipse2);
        resultAddrCircleImg.imageProperty().bind(addrCircleImg.imageProperty());

        AppManager.settingTextFieldStyle(tokenAddressTextField);
        AppManager.settingTextFieldLineStyle(nameTextField);

        tokenAddressTextField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

                if(oldValue){

                    String contractAddress = tokenAddressTextField.getText();
                    String tokenName = AppManager.getInstance().getTokenName(contractAddress);
                    String tokenSymbol = AppManager.getInstance().getTokenSymbol(contractAddress);
                    BigInteger totalSupply = AppManager.getInstance().getTokenTotalSupply(contractAddress);
                    long decimal = AppManager.getInstance().getTokenDecimals(contractAddress);

                    nameTextField.setText(tokenName);
                    symbolTextField.setText(tokenSymbol);
                    totalSupplyTextField.setText(totalSupply.toString());
                    decimalTextField.setText(Long.toString(decimal));
                    addrCircleImg.setImage(ImageManager.getIdenticons(contractAddress));
                }
            }
        });

        tokenAddressTextField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                int maxlangth = 40;
                if (tokenAddressTextField.getText().trim().length() > maxlangth) {
                    tokenAddressTextField.setText(tokenAddressTextField.getText().trim().substring(0, maxlangth));
                }
            }
        });

        tokenAddressTextField.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode() == KeyCode.TAB){
                    nameTextField.requestFocus();
                }else if(event.getCode() == KeyCode.ENTER){
                    nameTextField.requestFocus();
                }
            }
        });

        nameTextField.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode() == KeyCode.TAB){
                    tokenAddressTextField.requestFocus();
                }else if(event.getCode() == KeyCode.ENTER){
                    tokenAddressTextField.requestFocus();
                }
            }
        });
    }

    public void languageSetting() {
        addTokenTitle.textProperty().bind(StringManager.getInstance().popup.tokenAddAddTokenTitle);
        addTokenDesc.textProperty().bind(StringManager.getInstance().popup.tokenAddAddTokenDesc);
        contractAddrLabel.textProperty().bind(StringManager.getInstance().popup.tokenEditContractAddrLabel);
        nameLabel.textProperty().bind(StringManager.getInstance().popup.tokenEditNameLabel);
        nameTextField.promptTextProperty().bind(StringManager.getInstance().popup.tokenEditNamePlaceholder);
        minNumLabel.textProperty().bind(StringManager.getInstance().popup.tokenEditMinNumLabel);
        previewLabel.textProperty().bind(StringManager.getInstance().popup.tokenEditPreviewLabel);
        noBtn.textProperty().bind(StringManager.getInstance().common.noButton);
        addBtn.textProperty().bind(StringManager.getInstance().common.addButton);
        supplyLabel.textProperty().bind(StringManager.getInstance().common.supplyLabel);
        symbolLabel.textProperty().bind(StringManager.getInstance().common.symbolLabel);
    }

    public void addBtnClicked() {
        String tokenAddress = tokenAddressTextField.getText();
        String tokenName = nameTextField.getText();
        String tokenSymbol = symbolTextField.getText();
        String tokenDecimal = decimalTextField.getText();
        String totalSupply = totalSupplyTextField.getText();

        byte[] addr = Hex.decode(tokenAddress);
        long decimal = Long.parseLong(tokenDecimal);
        BigInteger supply = new BigInteger(totalSupply);
        DBManager.getInstance().updateTokens(addr, tokenName, tokenSymbol, decimal, supply);
        AppManager.getInstance().initTokens();

        exit();
        if(handler != null){
            handler.add();
        }
    }

    @Override
    public void exit(){
        PopupManager.getInstance().showMainPopup(rootPane, "popup_token_list.fxml", zIndex);
        parentRequestFocus();
    }

    private PopupAddTokenImpl handler;
    public void setHandler(PopupAddTokenImpl handler){
        this.handler = handler;
    }
    public interface PopupAddTokenImpl{
        void add();
    }

    public void requestFocus() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                tokenAddressTextField.requestFocus();
                System.out.println("requestFocus");
            }
        });
    }
}
