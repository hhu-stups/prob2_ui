package de.prob2.ui.visb.ui;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visb.visbobjects.VisBEvent;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public class ListViewEvent extends ListCell<VisBEvent> {
	@FXML
	private VBox event_box;
	@FXML
	private Label event_id;
	@FXML
	private Label label_event;
	@FXML
	private Label label_predicates;

	private VisBEvent visBEvent;

	public ListViewEvent(StageManager stageManager){
		stageManager.loadFXML(this,"list_view_event.fxml");
		this.visBEvent = null;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.event_box);
		this.getStyleClass().add("visb-item");
	}

	@Override
	protected void updateItem(final VisBEvent visBEvent, final boolean empty){
		super.updateItem(visBEvent, empty);
		if(visBEvent != null){
			this.visBEvent = visBEvent;
			event_id.setText(visBEvent.getId());
			label_event.setText(visBEvent.getEvent());
			label_predicates.setText(visBEvent.getPredicates().toString());
			this.setGraphic(this.event_box);
			this.setText("");
		}
	}

}
