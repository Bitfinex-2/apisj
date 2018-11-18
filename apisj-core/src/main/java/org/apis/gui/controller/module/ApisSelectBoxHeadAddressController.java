package org.apis.gui.controller.module;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import org.apis.gui.controller.base.BaseSelectBoxHeaderController;
import org.apis.gui.manager.StyleManager;
import org.apis.gui.model.SelectBoxItemModel;
import org.apis.gui.model.base.BaseModel;

import java.net.URL;
import java.util.ResourceBundle;

public class ApisSelectBoxHeadAddressController extends BaseSelectBoxHeaderController {

    @FXML private Label  addressLabel, maskLabel;
    @FXML private ImageView icon, icKnowledgekey;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // set a clip to apply rounded border to the original image.
        Rectangle clip = new Rectangle( this.icon.getFitWidth()-0.5, this.icon.getFitHeight()-0.5 );
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        icon.setClip(clip);
    }

    @Override
    public void setModel(BaseModel model) {
        this.itemModel = (SelectBoxItemModel)model;
        if(model != null) {
            addressLabel.setText(this.itemModel.getAddress());
            maskLabel.setText(this.itemModel.getMask());
            icon.setImage(this.itemModel.getIdenticon());

            // 보안키 체크
            if(itemModel.isUsedProofKey()){
                StyleManager.fontColorStyle(addressLabel, StyleManager.AColor.C2b8a3e);
                icKnowledgekey.setVisible(true);
            }else{
                StyleManager.fontColorStyle(addressLabel, StyleManager.AColor.C999999);
                icKnowledgekey.setVisible(false);
            }

        }
    }
}
