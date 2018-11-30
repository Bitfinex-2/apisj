package org.apis.gui.controller.popup;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.apis.gui.controller.base.BasePopupController;
import org.apis.gui.manager.StringManager;

import java.net.URL;
import java.util.ResourceBundle;

public class PopupRegisterCommercialDomainController extends BasePopupController {

    @FXML private ImageView closeButton;
    @FXML private Label title, commercialDomainDesc1, commercialDomainDesc2, emailLabel1, emailLabel, yesButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        languageSetting();
    }
    public void languageSetting() {
        title.textProperty().bind(StringManager.getInstance().addressMasking.registerCommercialDomain);
        yesButton.textProperty().bind(StringManager.getInstance().common.yesButton);
        commercialDomainDesc1.textProperty().bind(StringManager.getInstance().popup.maskingCommercialDomainMsg1);
        commercialDomainDesc2.textProperty().bind(StringManager.getInstance().popup.maskingCommercialDomainMsg2);

    }
}
