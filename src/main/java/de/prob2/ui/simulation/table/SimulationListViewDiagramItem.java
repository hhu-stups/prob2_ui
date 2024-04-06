package de.prob2.ui.simulation.table;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SimulationListViewDiagramItem extends ListCell<DiagramConfiguration> {

	@FXML
	private VBox itemBox;

	private DiagramConfiguration item;

	private final I18n i18n;

	public SimulationListViewDiagramItem(final StageManager stageManager, final I18n i18n) {
		super();
		stageManager.loadFXML(this,"simulation_list_view_item.fxml");
		this.item = null;
		this.i18n = i18n;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.itemBox);
	}

	@Override
	protected void updateItem(final DiagramConfiguration item, final boolean empty) {
		super.updateItem(item, empty);
		this.item = item;
		if(item != null) {
			this.itemBox.getChildren().clear();
			if(item instanceof ActivationOperationConfiguration) {
				updateOperationDiagramItem();
			} else if (item instanceof ActivationChoiceConfiguration){
				updateChoiceDiagramItem();
			} else {
				updateListenerItem();
			}
			this.setPrefHeight(itemBox.getChildren().size() * 20.0f);
			this.setGraphic(this.itemBox);
			this.setText("");
		} else {
			clear();
		}
	}

	public void updateOperationDiagramItem() {
		ActivationOperationConfiguration item = (ActivationOperationConfiguration) this.item;
		TextField tfID = new TextField(item.getId());
		this.itemBox.getChildren().add(tfID);

		Label lbOpName = new Label(i18n.translate("simulation.item.operation"));
		lbOpName.getStyleClass().add("information");
		TextField tfOpName = new TextField(item.getOpName());
		this.itemBox.getChildren().add(new HBox(lbOpName, tfOpName));

		Label lbTime = new Label(i18n.translate("simulation.item.time"));
		lbTime.getStyleClass().add("information");
		TextField tfTime = new TextField(item.getAfter());
		this.itemBox.getChildren().add(new HBox(lbTime, tfTime));

		if(item.getPriority() == 0) {
			Label lbPriority = new Label(i18n.translate("simulation.item.priority"));
			lbPriority.getStyleClass().add("information");
			TextField tfPriority = new TextField(String.valueOf(item.getPriority()));
			this.itemBox.getChildren().add(new HBox(lbPriority, tfPriority));
		}


		if(item.getActivating() != null) {
			Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
			lbActivation.getStyleClass().add("information");
			TextField tfActivation = new TextField(item.getActivatingAsString());
			this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));
		}

		Label lbActivationKind = new Label(i18n.translate("simulation.item.activationKind"));
		lbActivationKind.getStyleClass().add("information");
		TextField tfActivationKind = new TextField(item.getActivationKind().toString());
		this.itemBox.getChildren().add(new HBox(lbActivationKind, tfActivationKind));


		if(item.getAdditionalGuards() != null) {
			Label lbAdditionalGuards = new Label(i18n.translate("simulation.item.additionalGuards"));
			lbAdditionalGuards.getStyleClass().add("information");
			TextField tfAdditionalGuards = new TextField(item.getAdditionalGuards());
			this.itemBox.getChildren().add(new HBox(lbAdditionalGuards, tfAdditionalGuards));
		}

		if(item.getFixedVariables() != null) {
			Label lbFixedVariables = new Label(i18n.translate("simulation.item.fixedVariables"));
			lbFixedVariables.getStyleClass().add("information");
			TextField tfFixedVariables = new TextField(item.getFixedVariablesAsString());
			this.itemBox.getChildren().add(new HBox(lbFixedVariables, tfFixedVariables));
		}

		if(item.getProbabilisticVariables() != null) {
			Label lbProbabilisticVariables = new Label(i18n.translate("simulation.item.probabilisticVariables"));
			lbProbabilisticVariables.getStyleClass().add("information");
			TextField tfProbabilisticVariables = new TextField(item.getProbabilisticVariablesAsString());
			this.itemBox.getChildren().add(new HBox(lbProbabilisticVariables, tfProbabilisticVariables));
		}
	}

	public void updateChoiceDiagramItem() {
		ActivationChoiceConfiguration item = (ActivationChoiceConfiguration) this.item;
		TextField tfID = new TextField(item.getId());
		this.itemBox.getChildren().add(tfID);

		if(!item.getActivations().isEmpty()) {
			Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
			lbActivation.getStyleClass().add("information");
			TextField tfActivation = new TextField(item.getActivationsAsString());
			this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));
		}
	}

	public void updateListenerItem() {
		UIListenerConfiguration item = (UIListenerConfiguration) this.item;
		TextField tfID = new TextField(item.getId());
		this.itemBox.getChildren().add(tfID);

		if(item.getEvent() != null) {
			Label lbEvent = new Label(i18n.translate("simulation.item.event"));
			lbEvent.getStyleClass().add("information");
			TextField tfEvent = new TextField(item.getEvent());
			this.itemBox.getChildren().add(new HBox(lbEvent, tfEvent));
		}

		if(item.getPredicate() != null) {
			Label lbPredicate = new Label(i18n.translate("simulation.item.predicate"));
			lbPredicate.getStyleClass().add("information");
			TextField tfPredicate = new TextField(item.getPredicate());
			this.itemBox.getChildren().add(new HBox(lbPredicate, tfPredicate));
		}

		if(item.getActivating() != null) {
			Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
			lbActivation.getStyleClass().add("information");
			TextField tfActivation = new TextField(item.getActivatingAsString());
			this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));
		}
	}

	public void clear() {
		itemBox.getChildren().clear();
		this.setGraphic(this.itemBox);
		this.setText("");
	}
}
