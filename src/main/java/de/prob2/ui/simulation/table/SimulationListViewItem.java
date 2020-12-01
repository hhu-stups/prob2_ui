package de.prob2.ui.simulation.table;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

public class SimulationListViewItem extends ListCell<SimulationItem> {

	@FXML
	private VBox itemBox;


	private SimulationItem item;

	private final CurrentTrace currentTrace;

	private final ResourceBundle bundle;

	public SimulationListViewItem(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		stageManager.loadFXML(this,"simulation_list_view_item.fxml");
		this.item = null;
		this.currentTrace = currentTrace;
		this.bundle = bundle;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.itemBox);
	}

	@Override
	protected void updateItem(final SimulationItem item, final boolean empty) {
		super.updateItem(item, empty);
		this.item = item;
		if(item != null) {
			this.itemBox.getChildren().clear();

			Label lbOpName = new Label(item.getOpName());
			lbOpName.getStyleClass().add("name");
			this.itemBox.getChildren().add(lbOpName);

			if(!item.getTime().isEmpty()) {
				Label lbTime = new Label(item.getTime());
				lbTime.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbTime);
			}

			if(!item.getDelay().isEmpty()) {
				Label lbDelay = new Label(item.getDelay());
				lbDelay.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbDelay);
			}

			if(!item.getProbability().isEmpty()) {
				Label lbProbability = new Label(item.getProbability());
				lbProbability.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbProbability);
			}

			if(!item.getChoiceID().isEmpty()) {
				Label lbChoiceID = new Label(item.getChoiceID());
				lbChoiceID.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbChoiceID);
			}

			if(!item.getValues().isEmpty()) {
				Label lbVariableValues = new Label(item.getValues());
				lbVariableValues.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbVariableValues);
			}

			if(!item.getValuesProbability().isEmpty()) {
				Label lbVariableValuesProbability = new Label(item.getValuesProbability());
				lbVariableValuesProbability.getStyleClass().add("information");
				this.itemBox.getChildren().add(lbVariableValuesProbability);
			}
			this.setPrefHeight(itemBox.getChildren().size() * 20.0f);
			this.setGraphic(this.itemBox);
			this.setText("");
		} else {
			clear();
		}
	}

	public void clear() {
		itemBox.getChildren().clear();
		this.setGraphic(this.itemBox);
		this.setText("");
	}
}
