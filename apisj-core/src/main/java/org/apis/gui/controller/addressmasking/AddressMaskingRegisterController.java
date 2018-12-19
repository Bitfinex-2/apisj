package org.apis.gui.controller.addressmasking;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import org.apis.contract.ContractLoader;
import org.apis.core.CallTransaction;
import org.apis.gui.controller.base.BaseViewController;
import org.apis.gui.controller.module.ApisButtonEsimateGasLimitController;
import org.apis.gui.controller.module.selectbox.ApisSelectBoxController;
import org.apis.gui.controller.module.GasCalculatorController;
import org.apis.gui.manager.AppManager;
import org.apis.gui.manager.ImageManager;
import org.apis.gui.manager.StringManager;
import org.apis.gui.manager.StyleManager;
import org.apis.util.blockchain.ApisUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.net.URL;
import java.util.ResourceBundle;

public class AddressMaskingRegisterController extends BaseViewController {

    private String abi = ContractLoader.readABI(ContractLoader.CONTRACT_ADDRESS_MASKING);
    private byte[] addressMaskingAddress = AppManager.getInstance().constants.getADDRESS_MASKING_ADDRESS();
    private CallTransaction.Contract contract = new CallTransaction.Contract(abi);
    private CallTransaction.Function functionRegisterMask = contract.getByName("registerMask");


    @FXML private ApisSelectBoxController selectAddressController, selectDomainController, selectPayerController;
    @FXML private GasCalculatorController gasCalculatorController;
    @FXML private ApisButtonEsimateGasLimitController btnStartPreGasUsedController;
    @FXML private GridPane hintAddressLabel, hintMessageLabel;
    @FXML private Label selectedDomainLabel, totalBalance;
    @FXML private TextField addrMaskingIDTextField, addressTextField;
    @FXML private ImageView domainDragDrop, idIcon, registerAddressIcon;
    @FXML private Label idIcon2, warningLabel, recipientInputBtn, registerAddressLabel, registerAddressDesc, registerAddressMsg, selectDomainLabel
            , selectDomainDesc, publicDomainMsg, registerIdLabel, idMsg, idMsg2, payerLabel, totalApisLabel;
    @FXML private Label selectDomainMsg1, selectDomainMsg2, selectDomainMsg3, selectDomainMsg4;

    private Image domainDragDropGrey = new Image("image/bg_domain_dragdrop_grey@2x.png");
    private Image domainDragDropColor = new Image("image/bg_domain_dragdrop_color@2x.png");
    private Image domainDragDropCheck = new Image("image/bg_domain_dragdrop_check@2x.png");
    private boolean isMyAddressSelected = true;

    private boolean isEnabled = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageSetting();

        AppManager.settingTextFieldStyle(addrMaskingIDTextField);
        AppManager.settingTextFieldStyle(addressTextField);

        this.addrMaskingIDTextField.setText("");
        this.addrMaskingIDTextField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                addrMaskingIDTextField.setText(addrMaskingIDTextField.getText().replaceAll("@",""));

                if(newValue.length() > 8){
                    addrMaskingIDTextField.setText(oldValue);
                }

                settingLayoutData();

            }
        });

        this.addressTextField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!addressTextField.getText().matches("[0-9a-fA-F]*")) {
                    addressTextField.setText(addressTextField.getText().replaceAll("[^0-9a-fA-F]", ""));
                }

                int maxlangth = 40;
                if (addressTextField.getText().trim().length() > maxlangth) {
                    addressTextField.setText(addressTextField.getText().trim().substring(0, maxlangth));
                }

                settingLayoutData();
            }
        });

        this.warningLabel.setVisible(false);


        selectAddressController.init(ApisSelectBoxController.SELECT_BOX_TYPE_ALIAS, false);
        selectAddressController.setHandler(new ApisSelectBoxController.ApisSelectBoxImpl() {
            @Override
            public void onSelectItem() {

                settingLayoutData();
            }

            @Override
            public void onMouseClick() {

            }
        });

        selectDomainController.init(ApisSelectBoxController.SELECT_BOX_TYPE_DOMAIN, false);
        selectDomainController.setHandler(new ApisSelectBoxController.ApisSelectBoxImpl() {
            @Override
            public void onSelectItem() {

                // get layout data
                settingLayoutData();
            }

            @Override
            public void onMouseClick() {

            }
        });

        selectPayerController.init(ApisSelectBoxController.SELECT_BOX_TYPE_ALIAS, false);
        selectPayerController.setHandler(new ApisSelectBoxController.ApisSelectBoxImpl() {
            @Override
            public void onSelectItem() {

                settingLayoutData();
            }

            @Override
            public void onMouseClick() {

            }
        });

        btnStartPreGasUsedController.setHandler(new ApisButtonEsimateGasLimitController.ApisButtonEsimateGasLimitImpl() {
            @Override
            public void onMouseClicked(ApisButtonEsimateGasLimitController controller) {
                esimateGasLimit();
                isEnabled = true;
                settingLayoutData();
            }
        });


        this.idMsg.setVisible(false);
        this.idMsg.setText("");
        this.hintMessageLabel.setVisible(false);
        this.hintMessageLabel.setPrefHeight(-1);

        this.idMsg2.setVisible(false);
        this.idMsg2.setText("");
        this.hintAddressLabel.setVisible(false);
        this.hintAddressLabel.setPrefHeight(1);



    }

    private void languageSetting(){
        registerAddressLabel.textProperty().bind(StringManager.getInstance().addressMasking.registerAddressLabel);
        registerAddressDesc.textProperty().bind(StringManager.getInstance().addressMasking.registerAddressDesc);
        registerAddressMsg.textProperty().bind(StringManager.getInstance().addressMasking.registerAddressMsg);
        selectDomainLabel.textProperty().bind(StringManager.getInstance().addressMasking.selectDomainLabel);
        selectDomainDesc.textProperty().bind(StringManager.getInstance().addressMasking.selectDomainDesc);
        registerIdLabel.textProperty().bind(StringManager.getInstance().addressMasking.registerIdLabel);
        addrMaskingIDTextField.promptTextProperty().bind(StringManager.getInstance().addressMasking.registerIdPlaceholder);
        recipientInputBtn.textProperty().bind(StringManager.getInstance().common.directInputButton);
        payerLabel.textProperty().bind(StringManager.getInstance().addressMasking.payer);
        totalApisLabel.textProperty().bind(StringManager.getInstance().addressMasking.totalApis);

        selectDomainMsg2.textProperty().bind(StringManager.getInstance().addressMasking.domainMsg2);
        selectDomainMsg4.textProperty().bind(StringManager.getInstance().addressMasking.domainMsg4);
    }

    @FXML
    private void onMouseClicked(InputEvent event){
        String id = ((Node)event.getSource()).getId();

        if(id.equals("recipientInputBtn")) {
            if(isMyAddressSelected) {
                isMyAddressSelected = false;

                StyleManager.backgroundColorStyle(recipientInputBtn, StyleManager.AColor.C000000);
                StyleManager.borderColorStyle(recipientInputBtn, StyleManager.AColor.C000000);
                StyleManager.fontColorStyle(recipientInputBtn, StyleManager.AColor.Cffffff);
                addressTextField.setText("");
                selectAddressController.setVisible(false);
                addressTextField.setVisible(true);
            } else {
                isMyAddressSelected = true;

                StyleManager.backgroundColorStyle(recipientInputBtn, StyleManager.AColor.Cf8f8fb);
                StyleManager.borderColorStyle(recipientInputBtn, StyleManager.AColor.C999999);
                StyleManager.fontColorStyle(recipientInputBtn, StyleManager.AColor.C999999);
                selectAddressController.setVisible(true);
                addressTextField.setVisible(false);
            }

            settingLayoutData();
        } else if(id.equals("btnPay")){

        }
    }

    public void domainDragDropMouseEntered() {
        this.domainDragDrop.setImage(domainDragDropColor);
    }

    public void domainDragDropMouseExited() {
        this.domainDragDrop.setImage(domainDragDropGrey);
    }

    public void update() {
        selectAddressController.update();
        selectDomainController.update();
        selectPayerController.update();

        settingLayoutData();
    }

    public void settingLayoutData(){
        boolean isUseAddress = false;
        boolean isUseMaskingId = false;

        String domain = selectDomainController.getDomain().trim();
        String maskingId = addrMaskingIDTextField.getText().trim();
        String valueApis = selectDomainController.getValueApis().trim();
        BigInteger mineral = selectPayerController.getMineral();
        String address = null;
        String mask = null;

        if(isMyAddressSelected){
            address = selectAddressController.getAddress().trim();
        }else{
            address = addressTextField.getText();
        }

        mask = AppManager.getInstance().getMaskWithAddress(address);

        totalBalance.setText(ApisUtil.readableApis(selectPayerController.getBalance(),',', true));
        gasCalculatorController.setMineral(mineral);

        this.selectedDomainLabel.setText(domain);
        this.selectDomainMsg1.setText(domain);
        this.selectDomainMsg3.setText(valueApis+"APIS");

        // 도메인 체크
        if(address.length() < 40 || (mask != null && mask.length() > 0)){
            isUseAddress = false;

            this.registerAddressIcon.setVisible(true);
            this.registerAddressIcon.setImage(ImageManager.icErrorRed);

            if(address.length() < 40) {
                this.registerAddressMsg.textProperty().unbind();
                this.registerAddressMsg.textProperty().bind(StringManager.getInstance().addressMasking.registerAddressMsg3);
            }else{
                this.registerAddressMsg.textProperty().unbind();
                this.registerAddressMsg.textProperty().bind(StringManager.getInstance().addressMasking.registerAddressMsg2);
            }
            this.registerAddressMsg.setTextFill(Color.web("#910000"));

        }else{
            isUseAddress = true;

            this.registerAddressIcon.setVisible(true);
            this.registerAddressIcon.setImage(ImageManager.icCheckGreen);

            this.registerAddressMsg.textProperty().unbind();
            this.registerAddressMsg.textProperty().bind(StringManager.getInstance().addressMasking.registerAddressMsg);
            this.registerAddressMsg.setTextFill(Color.web("#36b25b"));
        }

        if(maskingId != null && maskingId.length() > 0){

            String addressUsed = AppManager.getInstance().getAddressWithMask(maskingId+domain);
            if(addressUsed != null){
                isUseMaskingId = false;

                // used
                this.idIcon.setVisible(true);
                this.idIcon.setImage(ImageManager.icErrorRed);
                this.idMsg.setVisible(true);
                this.idMsg.setTextFill(Color.web("#910000"));
                this.idMsg.setText(maskingId+domain+" "+StringManager.getInstance().addressMasking.isAlreadyInUse.get());
                this.hintMessageLabel.setVisible(true);
                this.hintMessageLabel.setPrefHeight(-1);

                this.idIcon2.setVisible(true);
                this.idMsg2.setVisible(true);
                this.idMsg2.setText(address);
                this.hintAddressLabel.setVisible(true);
                this.hintAddressLabel.setPrefHeight(-1);
            }else{
                isUseMaskingId = true;

                // not used
                this.idIcon.setVisible(true);
                this.idIcon.setImage(ImageManager.icCheckGreen);
                this.idMsg.setVisible(true);
                this.idMsg.setTextFill(Color.web("#36b25b"));
                this.idMsg.setText(maskingId+domain+" "+StringManager.getInstance().addressMasking.isAvailable.get());
                this.hintMessageLabel.setVisible(true);
                this.hintMessageLabel.setPrefHeight(-1);

                this.idIcon2.setVisible(false);
                this.idMsg2.setVisible(false);
                this.idMsg2.setText("");
                this.hintAddressLabel.setVisible(false);
                this.hintAddressLabel.setPrefHeight(0);
            }
        }else{
            isUseMaskingId = false;

            this.idIcon.setVisible(false);
            this.idMsg.setVisible(false);
            this.hintMessageLabel.setVisible(false);
            this.hintMessageLabel.setPrefHeight(0);
            this.idIcon2.setVisible(false);
            this.idMsg2.setVisible(false);
            this.idMsg2.setText("");
            this.hintAddressLabel.setVisible(false);
            this.hintAddressLabel.setPrefHeight(0);
        }


        if(isUseAddress && isUseMaskingId){

        }else{
            this.isEnabled = false;
        }

        btnStartPreGasUsedController.setCompiled((isUseAddress && isUseMaskingId));


        if(handler != null){
            handler.settingLayoutData();
        }

    }
    public String getAddress() {
        if(isMyAddressSelected){
            return this.selectAddressController.getAddress();
        }else{
            return this.addressTextField.getText().trim();
        }
    }

    private void esimateGasLimit(){
        String payerAddress = selectPayerController.getAddress();
        String address = selectAddressController.getAddress();
        if(!isMyAddressSelected){
            address = addressTextField.getText();
        }

        BigInteger value = selectDomainController.getValueApisToBigInt();
        String maskingId = addrMaskingIDTextField.getText();
        Object[] args = new Object[3];
        args[0] = Hex.decode(address);   //_faceAddress
        args[1] = maskingId;   //_name
        args[2] = new BigInteger(selectDomainController.getDomainId());   //_domainId

        long checkGas = AppManager.getInstance().getPreGasUsed(abi, Hex.decode(payerAddress), addressMaskingAddress, value, functionRegisterMask.name, args);

        if(checkGas > 0) {
            String preGasUsed = Long.toString(checkGas);
            gasCalculatorController.setGasLimit(preGasUsed);
        }else{
            gasCalculatorController.setGasLimit("0");
        }
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }


    public String getPayerAddress() {
        return this.selectPayerController.getAddress();
    }

    public String getMask() {
        return this.addrMaskingIDTextField.getText().trim();
    }

    public String getDomainId() {
        return this.selectDomainController.getDomainId();
    }

    public String getDomain() {
        return  this.selectDomainController.getDomain();
    }

    public BigInteger getValue() {
        return this.selectDomainController.getValueApisToBigInt();
    }

    public BigInteger getGasLimit() {
        return this.gasCalculatorController.getGasLimit();
    }

    public BigInteger getGasPrice() {
        return this.gasCalculatorController.getGasPrice();
    }

    private AddressMaskingRegisterImpl handler;
    public void setHandler(AddressMaskingRegisterImpl handler){
        this.handler = handler;
    }

    public interface AddressMaskingRegisterImpl{
        void settingLayoutData();
    }
}
