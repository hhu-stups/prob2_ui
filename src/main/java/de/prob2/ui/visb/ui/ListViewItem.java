package de.prob2.ui.visb.ui;

import de.prob2.ui.visb.visbobjects.VisBItem;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class ListViewItem extends ListCell<VisBItem> {
    @FXML
    private VBox item_box;
    @FXML
    private Label item_id;
    @FXML
    private Label label_attr;
    @FXML
    private Label label_value;

    private VisBItem visBItem;

    public ListViewItem(StageManager stageManager){
        stageManager.loadFXML(this,"list_view_item.fxml");
        this.visBItem = null;
    }

    @FXML
    public void initialize(){
        this.setText("");
        this.setGraphic(this.item_box);
    }

    @Override
    protected void updateItem(final VisBItem visBItem, final boolean empty){
        super.updateItem(visBItem, empty);
        if(visBItem != null){
            this.visBItem = visBItem;
            this.item_id.setText(visBItem.getId());
            this.label_value.setText(visBItem.getValue());
            this.label_attr.setText(visBItem.getAttribute());
            this.setGraphic(this.item_box);
            this.setText("");
        }
    }
}
