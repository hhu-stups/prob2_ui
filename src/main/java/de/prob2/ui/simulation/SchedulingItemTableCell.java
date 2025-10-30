package de.prob2.ui.simulation;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.simulators.Activation;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public final class SchedulingItemTableCell extends TableCell<SchedulingTableItem, SchedulingTableItem> {

	private final I18n i18n;

	@FXML
	private VBox itemBox;

	public SchedulingItemTableCell(final StageManager stageManager, final I18n i18n) {
		super();
		this.i18n = i18n;
		stageManager.loadFXML(this, "scheduling_item.fxml");
	}

	@FXML
	@SuppressWarnings("unused")
	private void initialize() {
		this.setText("");
		this.setGraphic(this.itemBox);
	}

	@Override
	public void updateItem(final SchedulingTableItem item, final boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			clear();
		} else {
			this.itemBox.getChildren().clear();
			updateItem();
			this.setPrefHeight(itemBox.getChildren().size() * 20.0f);
			this.setGraphic(this.itemBox);
			this.setText("");
		}
	}

	private void updateItem() {
		SchedulingTableItem item = this.getItem();
		Activation activation = item.getActivation();

		if(activation.operation() != null && !activation.operation().isEmpty()) {
			Label lbOperation = new Label(String.format("%s: %s", i18n.translate("simulation.item.operation"), activation.operation()));
			lbOperation.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbOperation);
		}

		Label lbActivationKind = new Label(String.format("%s: %s", i18n.translate("simulation.item.activationKind"), activation.activationKind()));
		lbActivationKind.getStyleClass().add("information");
		this.itemBox.getChildren().add(lbActivationKind);

		if(activation.additionalGuards() != null && !activation.additionalGuards().isEmpty()) {
			Label lbAdditionalGuards = new Label(String.format("%s: %s", i18n.translate("simulation.item.additionalGuards"), activation.additionalGuards()));
			lbAdditionalGuards.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbAdditionalGuards);
		}

		if(activation.transitionSelection() != null) {
			Label lbTransitionSelection = new Label(String.format("%s: %s", i18n.translate("simulation.item.transitionSelection"), activation.transitionSelection()));
			lbTransitionSelection.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbTransitionSelection);
		}

		if(activation.fixedVariables() != null && !activation.fixedVariables().isEmpty()) {
			Label lbFixedVariables = new Label(String.format("%s: %s", i18n.translate("simulation.item.fixedVariables"), activation.fixedVariables()));
			lbFixedVariables.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbFixedVariables);
		}

		if(activation.probabilisticVariables() != null && !activation.probabilisticVariables().isEmpty()) {
			Label lbProbabilisticVariables = new Label(String.format("%s: %s", i18n.translate("simulation.item.probabilisticVariables"), activation.probabilisticVariables()));
			lbProbabilisticVariables.getStyleClass().add("information");
			this.itemBox.getChildren().add(lbProbabilisticVariables);
		}
	}

	private void clear() {
		itemBox.getChildren().clear();
		this.setGraphic(this.itemBox);
		this.setText("");
	}
}
