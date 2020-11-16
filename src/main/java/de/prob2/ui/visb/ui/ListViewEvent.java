package de.prob2.ui.visb.ui;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visb.visbobjects.VisBEvent;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

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

	public ListViewEvent(StageManager stageManager){
		stageManager.loadFXML(this,"list_view_event.fxml");
		this.visBEvent = null;
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
		if(visBEvent != null){
			this.visBEvent = visBEvent;
			lbID.setText(visBEvent.getId());
			lbEvent.setText(visBEvent.getEvent());
			lbPredicates.setText(visBEvent.getPredicates().toString());
			this.setGraphic(this.eventBox);
			this.setText("");
		}
	}

}
