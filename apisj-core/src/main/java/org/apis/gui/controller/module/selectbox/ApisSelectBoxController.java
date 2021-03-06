package org.apis.gui.controller.module.selectbox;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apis.contract.ContractLoader;
import org.apis.core.CallTransaction;
import org.apis.gui.common.IdenticonGenerator;
import org.apis.gui.controller.base.BaseFxmlController;
import org.apis.gui.controller.base.BaseSelectBoxHeaderController;
import org.apis.gui.controller.base.BaseSelectBoxItemController;
import org.apis.gui.controller.base.BaseViewController;
import org.apis.gui.manager.AppManager;
import org.apis.gui.manager.GUIContractManager;
import org.apis.gui.model.SelectBoxItemModel;
import org.apis.keystore.KeyStoreDataExp;
import org.apis.util.ByteUtil;
import org.apis.util.blockchain.ApisUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class ApisSelectBoxController extends BaseViewController {
    public static final int SELECT_BOX_TYPE_ALIAS = 0;
    public static final int SELECT_BOX_TYPE_DOMAIN = 1;
    private int selectBoxType = SELECT_BOX_TYPE_ALIAS;

    @FXML private AnchorPane rootPane;
    @FXML private VBox childPane, itemList;
    @FXML private GridPane header;
    @FXML private ScrollPane scrollPane;

    private boolean isReadableApisKMBT = false;
    private BaseFxmlController headerFxml;
    private ArrayList<BaseFxmlController> itemFxmlList = new ArrayList<>();
    private SelectBoxItemModel selectedItemModel = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setVisibleItemList(false);

        rootPane.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                setVisibleItemList(false);
            }
        });
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if(event.getCode() == KeyCode.PAGE_UP && event.isControlDown()) {
                event.consume();
            } else if(event.getCode() == KeyCode.PAGE_DOWN && event.isControlDown()) {
                event.consume();
            }
        });

        AppManager.getInstance().settingNodeStyle(header);
    }

    @FXML
    private void onMouseClicked(javafx.scene.input.InputEvent event){
        String id = ((Node)event.getSource()).getId();
        if(id.equals("header")){
            toggleItemListVisible();
            header.requestFocus();

            if(handler != null){
                handler.onMouseClick();
            }
        }
        event.consume();
    }

    public void init(int boxType, boolean isReadableApisKMBT){

        this.selectBoxType = boxType;
        this.isReadableApisKMBT = isReadableApisKMBT;

        setSelectBoxType(this.selectBoxType);

        // 드롭아이템 리스트 초기화
        // 내 지갑 목록이 필요한 타입 = { SELECT_BOX_TYPE_ALIAS, SELECT_BOX_TYPE_ADDRESS, SELECT_BOX_TYPE_ONLY_ADDRESS }
        // 도메인 목록이 필요한 타입 = { SELECT_BOX_TYPE_DOMAIN }
        switch (this.selectBoxType){
            case SELECT_BOX_TYPE_ALIAS :
                for (int i = 0; i < AppManager.getInstance().getKeystoreExpList().size(); i++) {
                    KeyStoreDataExp dataExp = AppManager.getInstance().getKeystoreExpList().get(i);

                    String address = dataExp.address;
                    String alias = dataExp.alias;
                    String mask = dataExp.mask;

                    SelectBoxItemModel model = new SelectBoxItemModel();
                    model.addressProperty().setValue(address);
                    model.aliasProperty().setValue(alias);
                    model.maskProperty().setValue(mask);
                    model.setBalance(AppManager.getInstance().getKeystoreExpList().get(i).balance);
                    model.setMineral(AppManager.getInstance().getKeystoreExpList().get(i).mineral);
                    model.setIdenticon(IdenticonGenerator.createIcon(address));
                    model.setUsedProofKey(dataExp.isUsedProofkey);

                    // 가지고 있는 모델일 경우 업데이트
                    // 가지고 있지 않는 모델일 경우 추가
                    boolean hasModel = false;
                    for(int j=0; j<itemFxmlList.size(); j++){
                        if(((SelectBoxItemModel)itemFxmlList.get(j).getController().getModel()).getAddress().equals(model.getAddress())){
                            updateItem(j, model);
                            hasModel = true;
                            break;
                        }
                    }
                    if(!hasModel){
                        addItem(this.selectBoxType, model);
                    }

                }

                // 가지고 있는 모델 중 지갑에 없는 모델 삭제
                for(int i=0; i<itemFxmlList.size(); i++){
                    boolean delete = true;
                    for (int j = 0; j < AppManager.getInstance().getKeystoreExpList().size(); j++) {
                        KeyStoreDataExp dataExp = AppManager.getInstance().getKeystoreExpList().get(j);
                        if(((SelectBoxItemModel)itemFxmlList.get(i).getController().getModel()).getAddress().equals(dataExp.address)){
                            delete = false;
                            break;
                        }
                    }

                    if(delete){
                        itemFxmlList.remove(i);
                        i--;
                    }
                }

                if(selectedItemModel == null){
                    selectedItemModel = (SelectBoxItemModel)itemFxmlList.get(0).getController().getModel();
                }
                setHeader(this.selectBoxType, selectedItemModel);
                break;
            case SELECT_BOX_TYPE_DOMAIN :

                String abi = ContractLoader.readABI(ContractLoader.CONTRACT_ADDRESS_MASKING);
                byte[] addressMaskingAddress = AppManager.getInstance().constants.getADDRESS_MASKING_ADDRESS();
                CallTransaction.Contract contract = new CallTransaction.Contract(abi);
                CallTransaction.Function functionGetRegistrationFee = contract.getByName("getRegistrationFee");
                CallTransaction.Function functionGetDomainInfo = contract.getByName("getDomainInfo"); //[2]:domainName, [5]needApproval, [6]isOpened
                Object[] cntResult = AppManager.getInstance().callConstantFunction(ByteUtil.toHexString(addressMaskingAddress), contract.getByName("domainCount"));

                // 도메인 리스트 등록
                int domainCount = new BigInteger(cntResult[0].toString()).intValue();

                // 데이터 불러오기
                ArrayList<String> domainName = new ArrayList<>();
                ArrayList<String> domainFee = new ArrayList<>();
                ArrayList<Object> params = new ArrayList<>();
                for(int i=0; i<domainCount; i++){
                    params.clear();
                    params.add(new SimpleStringProperty(""+i));
                    Object[] nameArgs = GUIContractManager.getContractArgs(functionGetDomainInfo.inputs, params);
                    Object[] nameResult = AppManager.getInstance().callConstantFunction(ByteUtil.toHexString(addressMaskingAddress), contract.getByName(functionGetDomainInfo.name), nameArgs);

                    if(!(Boolean) nameResult[5] && (Boolean) nameResult[6]){
                        domainName.add(nameResult[2].toString());
                        Object[] feeArgs = GUIContractManager.getContractArgs(functionGetRegistrationFee.inputs, params);
                        Object[] feeResult = AppManager.getInstance().callConstantFunction(ByteUtil.toHexString(addressMaskingAddress), contract.getByName(functionGetRegistrationFee.name), feeArgs);
                        domainFee.add(ApisUtil.readableApis(new BigInteger(feeResult[0].toString()), ',', true));
                    }
                }
                for(int i=0; i<domainName.size() ; i++){
                    SelectBoxItemModel model = new SelectBoxItemModel().setDomainId(""+i).setDomain("@"+domainName.get(i)).setApis(domainFee.get(i));
                    // 가지고 있는 모델일 경우 업데이트
                    // 가지고 있지 않는 모델일 경우 추가
                    boolean hasModel = false;
                    for(int j=0; j<itemFxmlList.size(); j++){
                        if(((SelectBoxItemModel)itemFxmlList.get(j).getController().getModel()).getDomain().equals(model.getDomain())){
                            updateItem(j, model);
                            hasModel = true;
                        }
                    }
                    if(!hasModel){
                        addItem(this.selectBoxType, model);
                    }
                }

                // 가지고 있는 모델 중 지갑에 없는 모델 삭제
                for(int i=0; i<itemFxmlList.size(); i++){
                    boolean delete = true;

                    for(int j=0; i<domainName.size() ; j++){
                        SelectBoxItemModel model = new SelectBoxItemModel().setDomainId(""+i).setDomain("@"+domainName.get(j)).setApis(domainFee.get(j));
                        if(((SelectBoxItemModel)itemFxmlList.get(i).getController().getModel()).getDomain().equals(model.getDomain())){
                            delete = false;
                            break;
                        }
                    }

                    if(delete){
                        itemFxmlList.remove(i);
                        i--;
                    }
                }

                if(selectedItemModel == null && itemFxmlList.size() > 0){
                    selectedItemModel = (SelectBoxItemModel)itemFxmlList.get(0).getController().getModel();
                }
                setHeader(this.selectBoxType, selectedItemModel);
                break;
        }

        drawItemList();

    }

    public void update(){
        init(this.selectBoxType, this.isReadableApisKMBT);
        if(selectedItemModel != null) {
            for(int i=0; i<itemFxmlList.size(); i++){

                if(this.selectBoxType == SELECT_BOX_TYPE_DOMAIN) {
                    if (((SelectBoxItemModel) itemFxmlList.get(i).getController().getModel()).getDomain().equals(selectedItemModel.getDomain())) {
                        selectedItemModel = (SelectBoxItemModel) itemFxmlList.get(i).getController().getModel();
                        setHeader(this.selectBoxType, selectedItemModel);
                        break;
                    }
                }else {
                    if (((SelectBoxItemModel) itemFxmlList.get(i).getController().getModel()).getAddress().equals(selectedItemModel.getAddress())) {
                        selectedItemModel = (SelectBoxItemModel) itemFxmlList.get(i).getController().getModel();
                        setHeader(this.selectBoxType, selectedItemModel);
                        break;
                    }
                }
            }
        }else{
            if(itemFxmlList.size() > 0){
                setHeader(this.selectBoxType, (SelectBoxItemModel)itemFxmlList.get(0).getController().getModel());
            }
        }
    }

    public void setSelectBoxType(int boxType){
        if(this.selectBoxType == SELECT_BOX_TYPE_ALIAS){
            this.scrollPane.maxHeightProperty().setValue(170);
        }else if(this.selectBoxType == SELECT_BOX_TYPE_DOMAIN){
            this.scrollPane.maxHeightProperty().setValue(162);
        }
    }
    public void setHeader(int boxType, SelectBoxItemModel model){
        try {

            String fxmlUrl = null;
            if(boxType == SELECT_BOX_TYPE_ALIAS){
                fxmlUrl = "module/selectbox/apis_selectbox_head_alias.fxml";
            }else if(boxType == SELECT_BOX_TYPE_DOMAIN){
                fxmlUrl = "module/selectbox/apis_selectbox_head_domain.fxml";
            }

            if(headerFxml == null) {
                headerFxml = new BaseFxmlController(fxmlUrl);
            }
            BaseSelectBoxHeaderController headerController = (BaseSelectBoxHeaderController)headerFxml.getController();
            headerController.setReadableApisKMBT(isReadableApisKMBT);
            headerController.setModel(model);

            header.getChildren().clear();
            header.add(headerFxml.getNode(),0,0);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void updateItem(int index, SelectBoxItemModel model){
        this.itemFxmlList.get(index).getController().setModel(model);
    }
    public void addItem(int boxType, SelectBoxItemModel model){
        try {
            String itemUrl = "module/selectbox/apis_selectbox_item_alias.fxml";
            if(boxType == SELECT_BOX_TYPE_ALIAS){
                itemUrl = "module/selectbox/apis_selectbox_item_alias.fxml";
            }else if(boxType == SELECT_BOX_TYPE_DOMAIN){
                itemUrl = "module/selectbox/apis_selectbox_item_domain.fxml";
            }
            BaseFxmlController itemFxml = new BaseFxmlController(itemUrl);
            BaseSelectBoxItemController itemController = (BaseSelectBoxItemController)itemFxml.getController();
            itemController.setReadableApisKMBT(isReadableApisKMBT);
            itemController.setModel(model);
            itemController.setHandler(new BaseSelectBoxItemController.BaseSelectBoxItemImpl() {
                @Override
                public void onMouseClicked(SelectBoxItemModel itemModel) {
                    selectedItemModel = itemModel;

                    // change header data
                    headerFxml.getController().setModel(itemModel);

                    // hide item list
                    ApisSelectBoxController.this.setVisibleItemList(false);

                    if(handler != null){
                        handler.onSelectItem();
                    }

                    header.requestFocus();
                }
            });
            itemFxmlList.add(itemFxml);

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void setTokenAddress(String tokenAddress) {
        if(selectBoxType == SELECT_BOX_TYPE_ALIAS){
            ApisSelectBoxHeadAliasController header = (ApisSelectBoxHeadAliasController)headerFxml.getController();
            header.setTokenAddress(tokenAddress);

            for(int i=0; i<itemFxmlList.size(); i++){
                ApisSelectBoxItemAliasController item = (ApisSelectBoxItemAliasController)itemFxmlList.get(i).getController();
                item.setTokenAddress(tokenAddress);
            }
        }
    }


    public void setVisibleItemList(boolean isVisible){

        if(isVisible == true){
            this.rootPane.prefHeightProperty().setValue(-1);
        }else{
            if(this.selectBoxType == SELECT_BOX_TYPE_ALIAS){
                this.rootPane.prefHeightProperty().setValue(56);
            }else if(this.selectBoxType == SELECT_BOX_TYPE_DOMAIN){
                this.rootPane.prefHeightProperty().setValue(44);
            }
        }

        scrollPane.setVisible(isVisible);
    }

    public void selectedItemWithWalletAddress(String address) {
        for(int i=0; i<itemFxmlList.size(); i++){
            if(((SelectBoxItemModel)itemFxmlList.get(i).getController().getModel()).getAddress().equals(address)){
                selectedItem(i);
                break;
            }
        }
    }

    public void selectedItemWithAddress(String address){
        for(int i=0; i<itemFxmlList.size(); i++){
            if(((SelectBoxItemModel)itemFxmlList.get(i).getController().getModel()).getAddress().equals(address)){
                selectedItem(i);
                break;
            }
        }
    }

    public void selectedItem(int i) {
        selectedItemModel = (SelectBoxItemModel) itemFxmlList.get(i).getController().getModel();
        headerFxml.getController().setModel(selectedItemModel);
        if(handler != null){
            handler.onSelectItem();
        }
    }

    private void drawItemList(){
        itemList.getChildren().clear();
        for(int i=0; i<itemFxmlList.size(); i++){
            itemList.getChildren().add(itemFxmlList.get(i).getNode());
        }
    }

    public void toggleItemListVisible(){
        setVisibleItemList(!scrollPane.isVisible()); }

    public String getAddress(){
        return ((BaseSelectBoxHeaderController)this.headerFxml.getController()).getAddress().trim();
    }

    public String getAlias(){
        return ((BaseSelectBoxHeaderController)this.headerFxml.getController()).getAlias().trim();
    }

    public BigInteger getBalance() {
        return ((BaseSelectBoxHeaderController)this.headerFxml.getController()).getBalance();
    }

    public BigInteger getMineral() {
        return  ((BaseSelectBoxHeaderController)this.headerFxml.getController()).getMineral();
    }

    public String getDomain() {
        return  ((BaseSelectBoxHeaderController)this.headerFxml.getController()).getDomain();
    }

    public String getValueApis(){
        return  ((BaseSelectBoxHeaderController)this.headerFxml.getController()).getApis();
    }
    public BigInteger getValueApisToBigInt() {
        switch (this.selectBoxType){
            case SELECT_BOX_TYPE_DOMAIN : return  new BigInteger(getValueApis()).multiply(new BigInteger("1000000000000000000"));
        }
        return null;
    }
    public String getDomainId(){
        return  ((BaseSelectBoxHeaderController)this.headerFxml.getController()).getDomainId();
    }

    public void setVisible(boolean isVisible) {
        this.rootPane.setVisible(isVisible);
    }



    private ApisSelectBoxImpl handler;
    public void setHandler(ApisSelectBoxImpl handler) { this.handler = handler; }
    public interface ApisSelectBoxImpl{
        void onMouseClick();
        void onSelectItem();
    }
}
