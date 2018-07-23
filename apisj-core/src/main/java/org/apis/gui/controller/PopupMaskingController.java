package org.apis.gui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.apis.gui.manager.AppManager;

import java.net.URL;
import java.util.ResourceBundle;

public class PopupMaskingController implements Initializable {

    private int cusorTabIndex = 0;
    private int cusorStepIndex = 0;

    private Image tab1On, tab1Off, tab2On, tab2Off;
    private Image introNavi,introNaviCircle;
    @FXML
    private Pane tab1Line, tab2Line;
    @FXML
    private ImageView tab1Icon, tab2Icon;
    @FXML
    private Label tab1Label, tab2Label;
    @FXML
    private TabPane tabPane;
    @FXML
    private ImageView introNaviOne, introNaviTwo, introNaviThree, introNaviFour;

    @FXML
    private ApisSelectBoxController selectAddressController;

    public void exit(){
        AppManager.getInstance().guiFx.hideMainPopup(0);
    }

    public void setSelectedTab(int index){
        this.cusorTabIndex = index;

        tab1Line.setVisible(false);
        tab1Icon.setImage(tab1Off);
        tab1Label.setStyle("-fx-font-family: 'Open Sans Regular'; -fx-font-size:12px; ");
        tab1Label.setTextFill(Color.web("#999999"));

        tab2Line.setVisible(false);
        tab2Icon.setImage(tab2Off);
        tab2Label.setStyle("-fx-font-family: 'Open Sans Regular'; -fx-font-size:12px; ");
        tab2Label.setTextFill(Color.web("#999999"));

        if(index == 0){
            tab1Icon.setImage(tab1On);
            tab1Line.setVisible(true);
            tab1Label.setStyle("-fx-font-family: 'Open Sans SemiBold'; -fx-font-size:12px; ");
            tab1Label.setTextFill(Color.web("#910000"));

            introNaviOne.setVisible(true);
            introNaviTwo.setVisible(true);
            introNaviThree.setVisible(true);
            introNaviFour.setVisible(true);

        }else if(index == 1){
            tab2Icon.setImage(tab2On);
            tab2Line.setVisible(true);
            tab2Label.setStyle("-fx-font-family: 'Open Sans SemiBold'; -fx-font-size:12px; ");
            tab2Label.setTextFill(Color.web("#910000"));

            introNaviOne.setVisible(false);
            introNaviTwo.setVisible(false);
            introNaviThree.setVisible(false);
            introNaviFour.setVisible(false);
        }

        setStep(0);
    }
    public void setStep(int step){
        this.cusorStepIndex = step;

        SingleSelectionModel<Tab> selectionModel = tabPane.getSelectionModel();
        selectionModel.select(this.cusorTabIndex*4 + step);

        setNavi(this.cusorStepIndex );

        if(this.cusorTabIndex*4 + step < 0){
            exit();
        }
    }

    public void setNavi(int step){
        introNaviOne.setImage(introNaviCircle);
        introNaviTwo.setImage(introNaviCircle);
        introNaviThree.setImage(introNaviCircle);
        introNaviFour.setImage(introNaviCircle);

        introNaviOne.fitWidthProperty().setValue(6);
        introNaviTwo.fitWidthProperty().setValue(6);
        introNaviThree.fitWidthProperty().setValue(6);
        introNaviFour.fitWidthProperty().setValue(6);

        if(step == 0){
            introNaviOne.setImage(introNavi);
            introNaviOne.fitWidthProperty().setValue(24);
        }else if(step == 1){
            introNaviTwo.setImage(introNavi);
            introNaviTwo.fitWidthProperty().setValue(24);
        }else if(step == 2){
            introNaviThree.setImage(introNavi);
            introNaviThree.fitWidthProperty().setValue(24);
        }else if(step == 3){
            introNaviFour.setImage(introNavi);
            introNaviFour.fitWidthProperty().setValue(24);
        }
    }


    @FXML
    private void onMouseClicked(InputEvent event){
        String id = ((Node)event.getSource()).getId();
        System.out.println("id : "+id );
        if(id.equals("tab1")){
            setSelectedTab(0);
        }else if(id.equals("tab2")){
            setSelectedTab(1);
        }else if(id.equals("backBtn")){
            setStep(this.cusorStepIndex-1);
        }else if(id.equals("nextBtn")){
            setStep(this.cusorStepIndex+1);
        }else if(id.equals("suggestingBtn")){
            AppManager.getInstance().guiFx.showMainPopup("popup_email_address.fxml", 1);
        }else if(id.equals("requestBtn")){
            AppManager.getInstance().guiFx.showMainPopup("popup_success.fxml", 1);
        }else if(id.equals("subTab1")){
            setSelectedTab(1);
            setStep(0);
        }else if(id.equals("subTab2")){
            setSelectedTab(1);
            setStep(2);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tab1On = new Image("image/ic_registeralias_red@2x.png");
        tab1Off = new Image("image/ic_registeralias_grey@2x.png");
        tab2On = new Image("image/ic_registeralias_red@2x.png");
        tab2Off = new Image("image/ic_registeralias_grey@2x.png");
        introNavi = new Image("image/ic_nav@2x.png");
        introNaviCircle = new Image("image/ic_nav_circle@2x.png");

        selectAddressController.init(ApisSelectBoxController.SELECT_BOX_TYPE_ADDRESS);
        selectAddressController.setHandler(new ApisSelectBoxController.SelectEvent() {
            @Override
            public void onSelectItem() {

            }
        });

        // Tab Pane Direction Key Block
        tabPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if(event.getCode() == KeyCode.LEFT
                    || event.getCode() == KeyCode.RIGHT
                    || event.getCode() == KeyCode.UP
                    || event.getCode() == KeyCode.DOWN) {
                if(tabPane.isFocused()){
                    event.consume();
                }else{
                }
            }
        });

        setSelectedTab(0);
        setStep(0);
    }
}
