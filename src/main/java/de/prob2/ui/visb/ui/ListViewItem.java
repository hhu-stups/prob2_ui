package de.prob2.ui.visb.ui;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.visbobjects.VisBItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

public class ListViewItem extends ListCell<VisBItem> {
	@FXML
	private VBox item_box;
	@FXML
	private Label item_id;
	@FXML
	private Label label_attr;
	@FXML
	private Label label_value;
	@FXML
	private Label label_evalValue;

	private VisBItem visBItem;

	private final CurrentTrace currentTrace;

	private final ResourceBundle bundle;

	public ListViewItem(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle){
		stageManager.loadFXML(this,"list_view_item.fxml");
		this.visBItem = null;
		this.currentTrace = currentTrace;
		this.bundle = bundle;
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
			this.label_value.setText(String.format(bundle.getString("visb.item.expression"), visBItem.getValue()));
			this.label_attr.setText(String.format(bundle.getString("visb.item.attribute"), visBItem.getAttribute()));
			if(visBItem.parsedFormula != null) {
				AbstractEvalResult result = currentTrace.getCurrentState().eval(visBItem.parsedFormula);
				this.label_evalValue.setText(String.format(bundle.getString("visb.item.value"), result.toString()));
			} else {
				this.label_evalValue.setText("");
			}
			this.setGraphic(this.item_box);
			this.setText("");
		}
	}
}
