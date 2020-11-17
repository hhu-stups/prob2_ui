package de.prob2.ui.visb.ui;


import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visb.visbobjects.VisBEvent;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

public class ListViewEvent extends ListCell<VisBEvent> {
	@FXML
	private VBox eventBox;
	@FXML
	private Label lbID;
	@FXML
	private Label lbEvent;
	@FXML
	private Label lbPredicates;

	private VisBEvent visBEvent;

	private final ResourceBundle bundle;

	public ListViewEvent(StageManager stageManager, ResourceBundle bundle){
		stageManager.loadFXML(this,"list_view_event.fxml");
		this.visBEvent = null;
		this.bundle = bundle;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.eventBox);
		this.getStyleClass().add("visb-item");
	}

	@Override
	protected void updateItem(final VisBEvent visBEvent, final boolean empty){
		super.updateItem(visBEvent, empty);
		this.visBEvent = visBEvent;
		if(visBEvent != null){
			lbID.setText(visBEvent.getId());
			lbEvent.setText(String.format(bundle.getString("visb.event.event"), visBEvent.getEvent()));
			lbPredicates.setText(String.format(bundle.getString("visb.event.predicates"), visBEvent.getPredicates().toString()));
			this.setGraphic(this.eventBox);
			this.setText("");
		} else {
			clear();
		}
	}

	public void clear() {
		this.lbID.setText("");
		this.lbEvent.setText("");
		this.lbPredicates.setText("");
		this.setGraphic(this.eventBox);
		this.setText("");
	}

}
