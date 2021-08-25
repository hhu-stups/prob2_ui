package de.prob2.ui.simulation.table;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

public class SimulationListViewDebugItem extends ListCell<SimulationDebugItem> {

	@FXML
	private VBox itemBox;


	private SimulationDebugItem item;

	private final ResourceBundle bundle;

	public SimulationListViewDebugItem(final StageManager stageManager, final ResourceBundle bundle) {
		stageManager.loadFXML(this,"simulation_list_view_item.fxml");
		this.item = null;
		this.bundle = bundle;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.itemBox);
	}

	@Override
	protected void updateItem(final SimulationDebugItem item, final boolean empty) {
		super.updateItem(item, empty);
		this.item = item;
		if(item != null) {
			this.itemBox.getChildren().clear();
			if(item instanceof SimulationOperationDebugItem) {
				updateOperationDebugItem();
			} else {
				updateChoiceDebugItem();
			}
			this.setPrefHeight(itemBox.getChildren().size() * 20.0f);
			this.setGraphic(this.itemBox);
			this.setText("");
		} else {
			clear();
		}
	}

	public void updateOperationDebugItem() {
		SimulationOperationDebugItem item = (SimulationOperationDebugItem) this.item;
		Label lbID = new Label(item.getId());
		lbID.getStyleClass().add("id");
		this.itemBox.getChildren().add(lbID);

		Label lbOpName = new Label(String.format(bundle.getString("simulation.item.operation"), item.getOpName()));
		lbOpName.getStyleClass().add("information");
		this.itemBox.getChildren().add(lbOpName);

		Label lbTime = new Label(String.format(bundle.getString("simulation.item.time"), item.getTime()));
		lbTime.getStyleClass().add("information");
		this.itemBox.getChildren().add(lbTime);

		if(!item.getPriority().isEmpty()) {
			Label lbPriority = new Label(String.format(bundle.getString("simulation.item.priority"), item.getPriority()));
			lbPriority.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbPriority);
		}


		if(!item.getActivationsAsString().isEmpty()) {
			Label lbActivation = new Label(String.format(bundle.getString("simulation.item.activations"), item.getActivationsAsString()));
			lbActivation.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbActivation);
		}

		Label lbActivationKind = new Label(String.format(bundle.getString("simulation.item.activationKind"), item.getActivationKind()));
		lbActivationKind.getStyleClass().add("information");
		this.itemBox.getChildren().add(lbActivationKind);


		if(!item.getAdditionalGuardsAsString().isEmpty()) {
			Label lbAdditionalGuards = new Label(String.format(bundle.getString("simulation.item.additionalGuards"), item.getAdditionalGuards()));
			lbAdditionalGuards.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbAdditionalGuards);
		}

		if(!item.getFixedVariablesAsString().isEmpty()) {
			Label lbFixedVariables = new Label(String.format(bundle.getString("simulation.item.fixedVariables"), item.getFixedVariablesAsString()));
			lbFixedVariables.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbFixedVariables);
		}

		if(!item.getProbabilisticVariablesAsString().isEmpty()) {
			Label lbProbabilisticVariables = new Label(String.format(bundle.getString("simulation.item.probabilisticVariables"), item.getProbabilisticVariablesAsString()));
			lbProbabilisticVariables.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbProbabilisticVariables);
		}

		// TODO: Evaluated values
	}

	public void updateChoiceDebugItem() {
		SimulationChoiceDebugItem item = (SimulationChoiceDebugItem) this.item;
		Label lbID = new Label(item.getId());
		lbID.getStyleClass().add("id");
		this.itemBox.getChildren().add(lbID);

		if(!item.getActivationsAsString().isEmpty()) {
			Label lbActivation = new Label(String.format(bundle.getString("simulation.item.activations"), item.getActivationsAsString()));
			lbActivation.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbActivation);
		}
	}

	public void clear() {
		itemBox.getChildren().clear();
		this.setGraphic(this.itemBox);
		this.setText("");
	}
}
