package org.apis.gui.controller.smartcontrect;

import com.google.zxing.WriterException;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.apis.core.CallTransaction;
import org.apis.gui.common.IdenticonGenerator;
import org.apis.gui.common.JavaFXStyle;
import org.apis.gui.controller.base.BaseViewController;
import org.apis.gui.controller.module.ApisWalletAndAmountController;
import org.apis.gui.controller.module.GasCalculatorController;
import org.apis.gui.controller.popup.PopupContractReadWriteSelectController;
import org.apis.gui.controller.popup.PopupContractWarningController;
import org.apis.gui.manager.AppManager;
import org.apis.gui.manager.GUIContractManager;
import org.apis.gui.manager.PopupManager;
import org.apis.gui.manager.StringManager;
import org.apis.gui.model.ContractModel;
import org.apis.solidity.SolidityType;
import org.apis.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SmartContractCallSendController extends BaseViewController {

    @FXML private AnchorPane parameterListPane, walletSelectViewDim;
    @FXML private VBox cSelectList, cSelectChild, parameterList;
    @FXML private ScrollPane cSelectListView;
    @FXML private GridPane cSelectHead, walletInputView;
    @FXML private TextField searchText;
    @FXML private Label cSelectHeadText, warningLabel, writeBtn, readBtn, aliasLabel, addressLabel, placeholderLabel, selectContract,readWriteContract;
    @FXML private ImageView icon, cSelectHeadImg;
    @FXML private ApisWalletAndAmountController  walletAndAmountController;
    @FXML private GasCalculatorController gasCalculatorController;

    private CallTransaction.Function selectFunction;
    private ContractModel selectContractModel;
    private CallTransaction.Function[] selectContractFunctions;
    private ArrayList<Object> selectFunctionParams = new ArrayList();
    private Image downGray = new Image("image/ic_down_gray@2x.png");
    private Image downWhite = new Image("image/ic_down_white@2x.png");// 컨트렉트 객체
    private ArrayList<ContractMethodListItemController> returnItemController = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageSetting();

        Rectangle clip = new Rectangle( this.icon.getFitWidth()-0.5, this.icon.getFitHeight()-0.5 );
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        icon.setClip(clip);

        searchText.textProperty().addListener(searchTextImpl);
        walletAndAmountController.setHandler(walletAndAmountImpl);
        gasCalculatorController.setHandler(gasCalculatorImpl);

        initPage();
        setWaleltInputViewVisible(true, true);
    }

    public void languageSetting() {

        selectContract.textProperty().bind(StringManager.getInstance().smartContract.selectContract);
        readWriteContract.textProperty().bind(StringManager.getInstance().smartContract.readWriteContract);
    }


    public void initContract() {
        parameterListPane.setVisible(false);
        parameterListPane.prefHeightProperty().setValue(0);

        cSelectHead.setStyle("-fx-background-color: #f2f2f2; -fx-border-color: #d8d8d8; -fx-border-radius : 4 4 4 4; -fx-background-radius: 4 4 4 4;");
        cSelectHeadImg.setImage(downGray);
        hideContractMethodList();
    }

    public void addMethodSelectItem(CallTransaction.Function function, String contractAddress, String medataAbi ){
        if(function == null || function.type == CallTransaction.FunctionType.constructor
                || function.name.toLowerCase().indexOf(searchText.getText().toLowerCase()) < 0){
            return;
        }

        boolean isAdded = false;
        AnchorPane anchorPane = new AnchorPane();
        anchorPane.setStyle(new JavaFXStyle(anchorPane.getStyle()).add("-fx-background-color","#ffffff").toString());
        Label label = new Label();
        if(function.type.name().equals("function")){
            isAdded = true;
            label.setText(function.name);
        }else{
            if(function.name.length() == 0){
                isAdded = true;
                label.setText("{ Fall Back }");
            }
        }

        if(!isAdded){
            return;
        }

        label.setPadding(new Insets(8,16,8,16));
        label.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                label.setStyle(new JavaFXStyle(label.getStyle()).add("-fx-background-color","#f2f2f2").toString());
            }
        });
        label.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                label.setStyle(new JavaFXStyle(label.getStyle()).add("-fx-background-color","#ffffff").toString());
            }
        });

        // method list click
        label.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                // 선택한 함수 변경
                selectFunction = function;

                // 선택한 함수로 셀렉트박스 헤드 변경
                cSelectHeadText.setText(label.getText());
                hideContractMethodList();

                // show param list view
                parameterListPane.setVisible(true);
                parameterListPane.prefHeightProperty().setValue(-1);

                // Read 인지 Write인지 체크
                boolean isRead = GUIContractManager.isReadMethod(selectFunction);

                // create method var
                int itemType = 0;
                parameterList.getChildren().clear();
                selectFunctionParams.clear();
                returnItemController.clear();

                for(int i=0; i<function.inputs.length; i++){
                    itemType = ContractMethodListItemController.ITEM_TYPE_PARAM;
                    parameterList.getChildren().add(createMethodParam(itemType, function.inputs[i], function, contractAddress, medataAbi));
                }

                // read 인 경우에만 리턴값 표기
                if(isRead) {
                    for(int i=0; i<function.outputs.length; i++){
                        itemType = ContractMethodListItemController.ITEM_TYPE_RETURN;
                        parameterList.getChildren().add( createMethodParam(itemType, function.outputs[i], function, null, null) );
                    }

                    // 인자가 없는 경우 데이터 불러오기
                    if(function.inputs.length == 0){
                        CallTransaction.Contract contract = new CallTransaction.Contract(medataAbi);
                        Object[] result = AppManager.getInstance().callConstantFunction(contractAddress, contract.getByName(function.name));

                        for(int i=0; i<function.outputs.length; i++){
                            if(function.outputs[i].type instanceof SolidityType.BoolType){
                                // BOOL
                                returnItemController.get(i).setSelected((boolean)result[i]);
                            }else if(function.outputs[i].type instanceof SolidityType.AddressType){
                                // AddressType
                                result[i] = ByteUtil.toHexString((byte[])result[i]);
                                returnItemController.get(i).setItemText(result[i].toString());
                            }else if(function.outputs[i].type instanceof SolidityType.IntType){
                                // INT, uINT
                                returnItemController.get(i).setItemText(result[i].toString());
                            }else if(function.outputs[i].type instanceof SolidityType.StringType){
                                // StringType
                                returnItemController.get(i).setItemText(result[i].toString());
                            }else if(function.outputs[i].type instanceof SolidityType.BytesType){
                                // BytesType
                                returnItemController.get(i).setItemText(result[i].toString());
                            }else if(function.outputs[i].type instanceof SolidityType.Bytes32Type){
                                // Bytes32Type
                                returnItemController.get(i).setItemText(result[i].toString());
                            }else if(function.outputs[i].type instanceof SolidityType.FunctionType){
                                // FunctionType

                            }else if(function.outputs[i].type instanceof SolidityType.ArrayType){
                                // ArrayType
                                Object[] array = (Object[])result[i];
                                if(function.outputs[i].type.getCanonicalName().indexOf("int") >=0){
                                    List<BigInteger> list = new ArrayList<>();
                                    for(int j=0; j<array.length;j++){
                                        list.add(new BigInteger(""+array[j]));
                                    }
                                    result[i] = list;
                                }else if(function.outputs[i].type.getCanonicalName().indexOf("address") >=0){
                                    List<String> list = new ArrayList<>();
                                    for(int j=0; j<array.length;j++){
                                        list.add(Hex.toHexString((byte[]) array[j]));
                                    }
                                    result[i] = list;
                                }else if(function.outputs[i].type.getCanonicalName().indexOf("bool") >=0){
                                    List<Boolean> list = new ArrayList<>();
                                    for(int j=0; j<array.length;j++){
                                        list.add((Boolean)array[j]);
                                    }
                                    result[i] = list;
                                }else{
                                    List<String> list = new ArrayList<>();
                                    for(int j=0; j<array.length;j++){
                                        list.add((String)array[j]);
                                    }
                                    result[i] = list;
                                }
                                returnItemController.get(i).setItemText(result[i].toString());
                            }

                        }
                    }
                }

                initStyleIsReadMethod();
            }
        });
        AnchorPane.setTopAnchor(label, 0.0);
        AnchorPane.setBottomAnchor(label, 0.0);
        AnchorPane.setLeftAnchor(label, 0.0);
        AnchorPane.setRightAnchor(label, 0.0);
        anchorPane.getChildren().add(label);
        cSelectList.getChildren().add(anchorPane);
    }



    @FXML
    public void contractSelectPopup(){
        PopupContractReadWriteSelectController controller = (PopupContractReadWriteSelectController)PopupManager.getInstance().showMainPopup("popup_contract_read_write_select.fxml", 0);
        controller.setHandler(new PopupContractReadWriteSelectController.PopupContractReadWriteSelectImpl() {
            @Override
            public void onClickSelect(ContractModel model) {
                selectContractModel = model;
                CallTransaction.Contract contract = new CallTransaction.Contract(model.getAbi());
                CallTransaction.Function[] functions = contract.functions;
                selectContractFunctions = functions;

                searchText.setText("");
                searchText.setDisable(false);

                aliasLabel.setText(model.getName());
                addressLabel.setText(model.getAddress());
                placeholderLabel.setVisible(false);

                try {
                    Image image = IdenticonGenerator.generateIdenticonsToImage(addressLabel.textProperty().get(), 128, 128);
                    if (image != null) {
                        icon.setImage(image);
                        image = null;
                    }
                } catch (WriterException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // get contract method list
                cSelectList.getChildren().clear();
                for (int i = 0; i < selectContractFunctions.length; i++) {

                    addMethodSelectItem(selectContractFunctions[i], selectContractModel.getAddress(), selectContractModel.getAbi());
                }

                initPage();
            }
        });
    }

    @FXML
    public void sendTransfer(InputEvent event){
        String id = ((Node)event.getSource()).getId();

        if("readBtn".equals(id)){
            String functionName = this.selectFunction.name;
            String contractAddress = this.selectContractModel.getAddress();
            String medataAbi = this.selectContractModel.getAbi();

            // 데이터 불러오기
            Object[] args = new Object[this.selectFunction.inputs.length];
            for (int i = 0; i < selectFunctionParams.size(); i++) {
                if(this.selectFunction.inputs[i].type instanceof SolidityType.BoolType){
                    SimpleBooleanProperty property = (SimpleBooleanProperty) selectFunctionParams.get(i);
                    args[i] = selectFunctionParams.get(i);
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.StringType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    args[i] = property.get();
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.ArrayType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    String strData = property.get();
                    strData = strData.replaceAll("\\[","").replaceAll("]","").replaceAll("\"","").replaceAll(" ", "");
                    String[] dataSplit = strData.split(",");

                    if(this.selectFunction.inputs[i].type.getCanonicalName().indexOf("int") >=0){
                        List<BigInteger> list = new ArrayList<>();
                        for(int j=0; j<dataSplit.length; j++){
                            if(dataSplit[j].length() != 0){
                                list.add(new BigInteger(dataSplit[j]));
                            }
                        }
                        args[i] = list;
                    } else if(this.selectFunction.inputs[i].type.getCanonicalName().indexOf("bool") >=0) {
                        List<Boolean> list = new ArrayList<>();
                        for(int j=0; j<dataSplit.length; j++){
                            if(dataSplit[j].length() != 0){
                                list.add(Boolean.parseBoolean(dataSplit[j]));
                            }
                        }
                        args[i] = list;
                    } else {
                        List<String> list = new ArrayList<>();
                        for(int j=0; j<dataSplit.length; j++){
                            if(dataSplit[j].length() != 0){
                                list.add(dataSplit[j]);
                            }
                        }
                        args[i] = list;
                    }


                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.FunctionType){

                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.BytesType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    args[i] = Hex.decode(property.get());
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.AddressType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    args[i] = Hex.decode(property.get());
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.IntType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    BigInteger integer = new BigInteger((property.get().length() == 0)?"0":property.get());
                    args[i] = integer;
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.Bytes32Type){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    args[i] = Hex.decode(property.get());
                }

            }
            CallTransaction.Contract contract = new CallTransaction.Contract(medataAbi);
            Object[] result = AppManager.getInstance().callConstantFunction(contractAddress, contract.getByName(functionName), args);
            for(int i=0; i<selectFunction.outputs.length; i++){
                if(selectFunction.outputs[i].type instanceof SolidityType.BoolType){
                    // BOOL
                    returnItemController.get(i).setSelected((boolean)result[i]);
                }else if(selectFunction.outputs[i].type instanceof SolidityType.AddressType){
                    // AddressType
                    SolidityType.AddressType addressType = (SolidityType.AddressType)selectFunction.outputs[i].type;
                    result[i] = Hex.toHexString(addressType.encode(result[i]));
                    returnItemController.get(i).setItemText(result[i].toString());
                }else if(selectFunction.outputs[i].type instanceof SolidityType.IntType){
                    // INT, uINT
                    returnItemController.get(i).setItemText(result[i].toString());
                }else if(selectFunction.outputs[i].type instanceof SolidityType.StringType){
                    // StringType
                    returnItemController.get(i).setItemText(result[i].toString());
                }else if(selectFunction.outputs[i].type instanceof SolidityType.BytesType){
                    // BytesType
                    returnItemController.get(i).setItemText(result[i].toString());
                }else if(selectFunction.outputs[i].type instanceof SolidityType.Bytes32Type){
                    // Bytes32Type
                    returnItemController.get(i).setItemText(result[i].toString());
                }else if(selectFunction.outputs[i].type instanceof SolidityType.FunctionType){
                    // FunctionType

                }else if(selectFunction.outputs[i].type instanceof SolidityType.ArrayType){
                    // ArrayType
                    Object[] array = (Object[])result[i];
                    if(selectFunction.outputs[i].type.getCanonicalName().indexOf("int") >=0){
                        List<BigInteger> list = new ArrayList<>();
                        for(int j=0; j<array.length;j++){
                            list.add(new BigInteger(""+array[j]));
                        }
                        result[i] = list;
                    }else if(selectFunction.outputs[i].type.getCanonicalName().indexOf("address") >=0){
                        List<String> list = new ArrayList<>();
                        for(int j=0; j<array.length;j++){
                            list.add(Hex.toHexString((byte[]) array[j]));
                        }
                        result[i] = list;
                    }else{
                        List<String> list = new ArrayList<>();
                        for(int j=0; j<array.length;j++){
                            list.add((String)array[j]);
                        }
                        result[i] = list;
                    }
                    returnItemController.get(i).setItemText(result[i].toString());
                }
            }

        }else if("writeBtn".equals(id)){
            String address = this.walletAndAmountController.getAddress();
            String value = this.walletAndAmountController.getAmount().toString();
            String gasPrice = this.gasCalculatorController.getGasPrice().toString();
            String gasLimit = this.gasCalculatorController.getGasLimit().toString();
            byte[] contractAddress = selectContractModel.getAddressByte();

            Object[] args = new Object[this.selectFunction.inputs.length];
            for (int i = 0; i < selectFunctionParams.size(); i++) {
                if(this.selectFunction.inputs[i].type instanceof SolidityType.BoolType){
                    SimpleBooleanProperty property = (SimpleBooleanProperty) selectFunctionParams.get(i);
                    args[i] = property.get();
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.StringType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    args[i] = property.get();
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.ArrayType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    String strData = property.get();
                    strData = strData.replaceAll("\\[","").replaceAll("]","").replaceAll("\"","").replaceAll(" ", "");
                    String[] dataSplit = strData.split(",");

                    if(this.selectFunction.inputs[i].type.getCanonicalName().indexOf("int") >=0){
                        List<BigInteger> list = new ArrayList<>();
                        for(int j=0; j<dataSplit.length; j++){
                            if(dataSplit[j].length() != 0){
                                list.add(new BigInteger(dataSplit[j]));
                            }
                        }
                        args[i] = list;
                    }else if(this.selectFunction.inputs[i].type.getCanonicalName().indexOf("bool") >= 0) {
                        List<Boolean> list = new ArrayList<>();
                        for(int j=0; j<dataSplit.length; j++){
                            if(dataSplit[j].length() != 0){
                                list.add(Boolean.parseBoolean(dataSplit[j]));
                            }
                        }
                        args[i] = list;
                    } else {
                        List<String> list = new ArrayList<>();
                        for(int j=0; j<dataSplit.length; j++){
                            if(dataSplit[j].length() != 0){
                                list.add(dataSplit[j]);
                            }
                        }
                        args[i] = list;
                    }


                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.FunctionType){

                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.BytesType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    if(property.get().length() == 0){
                        args[i] = Hex.decode("0");
                    }else{
                        args[i] = Hex.decode(property.get());
                    }
                    args[i] = Hex.decode(property.get());
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.AddressType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    if(property.get().length() == 0){
                        args[i] = Hex.decode("0000000000000000000000000000000000000000");
                    }else{
                        args[i] = Hex.decode(property.get());
                    }
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.IntType){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    BigInteger integer = new BigInteger((property.get() == null || property.get().equals(""))?"0":property.get());
                    args[i] = integer;
                }else if(this.selectFunction.inputs[i].type instanceof SolidityType.Bytes32Type){
                    SimpleStringProperty property = (SimpleStringProperty) selectFunctionParams.get(i);
                    if(property.get().length() == 0){
                        args[i] = Hex.decode("0");
                    }else{
                        args[i] = Hex.decode(property.get());
                    }
                    args[i] = Hex.decode(property.get());
                }
            }

            CallTransaction.Contract contract = new CallTransaction.Contract(this.selectContractModel.getAbi());
            CallTransaction.Function setter = contract.getByName(selectFunction.name);
            byte[] functionCallBytes = setter.encode(args);

            // 완료 팝업 띄우기
            PopupContractWarningController controller = (PopupContractWarningController) PopupManager.getInstance().showMainPopup("popup_contract_warning.fxml", 0);
            controller.setData(address, value, gasPrice, gasLimit, contractAddress, functionCallBytes);

        }

    }


    public void showContractMethodList(){
        this.cSelectListView.setVisible(true);
        this.cSelectListView.prefHeightProperty().setValue(-1);
        this.cSelectChild.prefHeightProperty().setValue(-1);
    }

    public void hideContractMethodList(){
        this.cSelectListView.setVisible(false);
        this.cSelectListView.prefHeightProperty().setValue(0);
        this.cSelectChild.prefHeightProperty().setValue(40);
    }



    @FXML
    private void onMouseClicked(InputEvent event) {
        String fxid = ((Node)event.getSource()).getId();

        // Contract Read and Write Select Box
        if(fxid.equals("cSelectHead")) {
            if(this.cSelectList.getChildren().size() == 0){

            }else{
                if(this.cSelectListView.isVisible() == true) {
                    hideContractMethodList();
                } else {
                    showContractMethodList();
                }
            }
        }else if(fxid.equals("btnByteCodePreGasUsed")){
            estimateGasLimit();
        }
    }

    public void update(){
        walletAndAmountController.update();
    }

    public void initStyleIsReadMethod(){

        // Read 인지 Write인지 체크
        if(selectFunction != null) {
            if (GUIContractManager.isReadMethod(selectFunction)) {
                readBtn.setVisible(true);
                writeBtn.setVisible(false);

                // 지갑선택란 숨김
                setWaleltInputViewVisible(false, false);
            } else {
                readBtn.setVisible(false);
                writeBtn.setVisible(true);

                // 지갑선택란 표기
                setWaleltInputViewVisible(true, false);
            }
            if(selectFunction.inputs.length == 0){
                writeBtn.setVisible(false);
                readBtn.setVisible(false);
            }
        }else {
            readBtn.setVisible(false);
            writeBtn.setVisible(false);

            // 지갑선택란 숨김
            setWaleltInputViewVisible(false, false);
        }

        if(handler != null){
            if(selectFunction == null || !GUIContractManager.isReadMethod(selectFunction)){
                handler.isReadMethod(false);
            }else{
                handler.isReadMethod(true);
            }
        }
    }





    public Node createMethodParam(int itemType, CallTransaction.Param param, CallTransaction.Function function, String contractAddress, String medataAbi){
        try {
            String paramName = param.name;
            String dataTypeName = param.type.getName();
            int dataType = 0;

            if(param.type instanceof SolidityType.BoolType){
                dataType = ContractMethodListItemController.DATA_TYPE_BOOL;
            }else if(param.type instanceof SolidityType.StringType){
                dataType = ContractMethodListItemController.DATA_TYPE_STRING;
            }else if(param.type instanceof SolidityType.AddressType){
                dataType = ContractMethodListItemController.DATA_TYPE_ADDRESS;
            }else if(param.type instanceof SolidityType.IntType){
                dataType = ContractMethodListItemController.DATA_TYPE_INT;
            }

            URL methodListItem = getClass().getClassLoader().getResource("scene/smartcontrect/contract_method_list_item.fxml");
            FXMLLoader loader = new FXMLLoader(methodListItem);
            Node node = loader.load();
            ContractMethodListItemController itemController = (ContractMethodListItemController)loader.getController();
            itemController.setData(itemType, paramName, dataType, dataTypeName);
            itemController.setItemText("");

            if(itemType == ContractMethodListItemController.ITEM_TYPE_RETURN) {
                returnItemController.add(itemController);
            }else{
                itemController.setHandler(new ContractMethodListItemController.ContractMethodListItemImpl() {
                    @Override
                    public void change(Object oldValue, Object newValue) {

                    }
                });
            }

            if(param.type instanceof SolidityType.BoolType){
                // BOOL

                // param 등록
                SimpleBooleanProperty booleanProperty = new SimpleBooleanProperty();
                booleanProperty.bind(itemController.selectedProperty());
                if(itemType == ContractMethodListItemController.ITEM_TYPE_PARAM) {
                    selectFunctionParams.add(booleanProperty);
                }

            }else if(param.type instanceof SolidityType.AddressType){
                // AddressType
                itemController.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("[0-9a-fA-F]*")) {
                        itemController.setItemText(newValue.replaceAll("[^0-9a-fA-F]", ""));
                    }
                    if(itemController.getText().length() > 40){
                        itemController.setItemText(itemController.getText().substring(0, 40));
                    }
                });

                // param 등록
                SimpleStringProperty stringProperty = new SimpleStringProperty();
                stringProperty.bind(itemController.textProperty());
                if(itemType == ContractMethodListItemController.ITEM_TYPE_PARAM) {
                    selectFunctionParams.add(stringProperty);
                }

            }else if(param.type instanceof SolidityType.IntType){
                // INT, uINT
                itemController.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("[0-9]*")) {
                        itemController.setItemText(newValue.replaceAll("[^0-9]", ""));
                    }
                });

                // param 등록
                SimpleStringProperty stringProperty = new SimpleStringProperty();
                stringProperty.bind(itemController.textProperty());
                if(itemType == ContractMethodListItemController.ITEM_TYPE_PARAM) {
                    selectFunctionParams.add(stringProperty);
                }

            }else if(param.type instanceof SolidityType.StringType){
                // StringType

                // param 등록
                SimpleStringProperty stringProperty = new SimpleStringProperty();
                stringProperty.bind(itemController.textProperty());
                if(itemType == ContractMethodListItemController.ITEM_TYPE_PARAM) {
                    selectFunctionParams.add(stringProperty);
                }

            }else if(param.type instanceof SolidityType.BytesType){
                // BytesType
                itemController.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("[0-9a-fA-F]*")) {
                        itemController.setItemText(newValue.replaceAll("[^0-9a-fA-F]", ""));
                    }
                });

                // param 등록
                SimpleStringProperty stringProperty = new SimpleStringProperty();
                stringProperty.bind(itemController.textProperty());
                if(itemType == ContractMethodListItemController.ITEM_TYPE_PARAM) {
                    selectFunctionParams.add(stringProperty);
                }

            }else if(param.type instanceof SolidityType.Bytes32Type){
                // Bytes32Type
                itemController.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("[0-9a-fA-F]*")) {
                        itemController.setItemText(newValue.replaceAll("[^0-9a-fA-F]", ""));
                    }
                });

                // param 등록
                SimpleStringProperty stringProperty = new SimpleStringProperty();
                stringProperty.bind(itemController.textProperty());
                if(itemType == ContractMethodListItemController.ITEM_TYPE_PARAM) {
                    selectFunctionParams.add(stringProperty);
                }

            }else if(param.type instanceof SolidityType.FunctionType){
                // FunctionType


            }else if(param.type instanceof SolidityType.ArrayType){
                // ArrayType

                // param 등록
                SimpleStringProperty stringProperty = new SimpleStringProperty();
                stringProperty.bind(itemController.textProperty());
                if(itemType == ContractMethodListItemController.ITEM_TYPE_PARAM) {
                    selectFunctionParams.add(stringProperty);
                }
            }

            return node;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void estimateGasLimit(){
        long gasUsed = checkSendFunctionPreGasPrice(selectFunction, selectContractModel.getAddress(), selectContractModel.getAbi(), walletAndAmountController.getAmount());
        gasCalculatorController.setGasLimit(Long.toString(gasUsed));
    }
    private long checkSendFunctionPreGasPrice(CallTransaction.Function function,  String contractAddress, String medataAbi, BigInteger value){

        Object[] args = new Object[function.inputs.length];

        // 초기화
        CallTransaction.Param param = null;
        for(int i=0; i<function.inputs.length; i++){
            param = function.inputs[i];

            if(param.type instanceof SolidityType.BoolType){
                // BOOL
                SimpleBooleanProperty booleanProperty = (SimpleBooleanProperty)selectFunctionParams.get(i);
                args[i] = booleanProperty.get();

            }else if(param.type instanceof SolidityType.AddressType){
                // AddressType
                SimpleStringProperty simpleStringProperty = (SimpleStringProperty)selectFunctionParams.get(i);
                args[i] = simpleStringProperty.get();
                if(args[i] == null || args[i].toString().length() == 0){
                    args[i] = "0000000000000000000000000000000000000000";
                }
            }else if(param.type instanceof SolidityType.IntType){
                // INT, uINT
                SimpleStringProperty simpleStringProperty = (SimpleStringProperty)selectFunctionParams.get(i);
                try{
                    args[i] = new BigInteger(simpleStringProperty.get());
                }catch (NumberFormatException e){
                    args[i] = 0;
                }

            }else if(param.type instanceof SolidityType.StringType){
                // StringType
                SimpleStringProperty simpleStringProperty = (SimpleStringProperty)selectFunctionParams.get(i);
                args[i] = simpleStringProperty.get();

            }else if(param.type instanceof SolidityType.BytesType){
                // BytesType
                SimpleStringProperty simpleStringProperty = (SimpleStringProperty)selectFunctionParams.get(i);
                args[i] = simpleStringProperty.get();

            }else if(param.type instanceof SolidityType.Bytes32Type){
                // Bytes32Type
                SimpleStringProperty simpleStringProperty = (SimpleStringProperty)selectFunctionParams.get(i);
                args[i] = simpleStringProperty.get();

            }else if(param.type instanceof SolidityType.FunctionType){
                // FunctionType
                args[i] = new byte[0];

            }else if(param.type instanceof SolidityType.ArrayType){
                SimpleStringProperty simpleStringProperty = (SimpleStringProperty)selectFunctionParams.get(i);
                System.out.println("simpleStringProperty : "+simpleStringProperty.get());
                // ArrayType
                if(param.type.getCanonicalName().indexOf("int") >= 0){
                    List<BigInteger> list = new ArrayList<>();
                    args[i] = list;
                }else if(param.type.getCanonicalName().indexOf("address") >= 0){
                    List<String> list = new ArrayList<>();
                    args[i] = list;
                }else{
                    List<String> list = new ArrayList<>();
                    args[i] = list;
                }

            }
        } //for function.inputs

        String functionName = function.name;
        byte[] address = Hex.decode(walletAndAmountController.getAddress());
        long preGasUsed = AppManager.getInstance().getPreGasUsed(medataAbi, address, Hex.decode(contractAddress), value, functionName, args);
        if(preGasUsed < 0){
            warningLabel.setVisible(true);
        }else{
            warningLabel.setVisible(false);
        }

        return preGasUsed;
    }



    // 컨트랙트 선택시, 메소드 설정 부분 초기화
    public void initPage(){
        // function header
        this.cSelectHeadText.setText("Select a function");
        this.writeBtn.setVisible(false);
        this.readBtn.setVisible(false);
        setWaleltInputViewVisible(true, true);

        initContract();
    }



    // 딤처리.
    public void setWaleltInputViewVisible(boolean isVisible, boolean isPlaceHolder){
        if(isPlaceHolder){
            walletInputView.setVisible(true);
            walletInputView.setPrefHeight(-1);
            walletSelectViewDim.setVisible(true);
            //walletSelectViewDim.setPrefHeight(200);
        }else {
            walletInputView.setVisible(isVisible);
            walletInputView.setPrefHeight((isVisible)?-1:0);
            walletSelectViewDim.setVisible(false);
            walletSelectViewDim.setPrefHeight(0);
        }
    }

    public BigInteger getAmount() {
        return this.walletAndAmountController.getAmount();
    }

    public BigInteger getBalance() {
        return this.walletAndAmountController.getBalance();
    }

    public BigInteger getTotalFee() {
        return this.gasCalculatorController.getTotalFee();
    }

    public BigInteger getTotalAmount(){
        BigInteger totalFee = getTotalFee();
        // total fee
        if(totalFee.toString().indexOf("-") >= 0){
            totalFee = BigInteger.ZERO;
        }

        // total amount
        BigInteger totalAmount = getAmount().add(totalFee);

        return totalAmount;
    }

    public BigInteger getAfterBalance(){
        // total amount
        BigInteger totalAmount = getTotalAmount();

        //after balance
        BigInteger afterBalance = getBalance().subtract(totalAmount);

        return afterBalance;
    }

    public boolean isReadMethod(){
        if(selectFunction != null) {
            return GUIContractManager.isReadMethod(selectFunction);
        }else{
            return true;
        }
    }


    private ApisWalletAndAmountController.ApisAmountImpl walletAndAmountImpl= new ApisWalletAndAmountController.ApisAmountImpl() {
        @Override
        public void change(BigInteger value) {
            // check pre gas used
            if(handler != null){
                handler.onAction();
            }
        }
    };

    private GasCalculatorController.GasCalculatorImpl gasCalculatorImpl= new GasCalculatorController.GasCalculatorImpl() {
        @Override
        public void gasLimitTextFieldFocus(boolean isFocused) {
            if(handler != null){
                handler.onAction();
            }
        }

        @Override
        public void gasLimitTextFieldChangeValue(String oldValue, String newValue){
            if(handler != null){
                handler.onAction();
            }
        }

        @Override
        public void gasPriceSliderChangeValue(int value) {
            if(handler != null){
                handler.onAction();
            }
        }
    };

    private ChangeListener<String> searchTextImpl = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            // get contract method list
            if(selectContractFunctions != null) {
                cSelectList.getChildren().clear();
                for (int i = 0; i < selectContractFunctions.length; i++) {
                    addMethodSelectItem(selectContractFunctions[i], selectContractModel.getAddress(), selectContractModel.getAbi());
                }
            }
        }
    };

    private SmartContractCallSendImpl handler;
    public void setHandler(SmartContractCallSendImpl handler){
        this.handler = handler;
    }
    public interface SmartContractCallSendImpl{
        void onAction();
        void isReadMethod(boolean isReadMethod);
    }
}