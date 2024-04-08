package de.prob2.ui.simulation.table;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationListViewDiagramItem extends ListCell<DiagramConfiguration> {

	@FXML
	private VBox itemBox;

	private DiagramConfiguration item;

	private DiagramConfiguration modifiedItem;

	private final I18n i18n;

	private final BooleanProperty savedProperty;

	public SimulationListViewDiagramItem(final StageManager stageManager, final I18n i18n, final BooleanProperty savedProperty) {
		super();
		stageManager.loadFXML(this,"simulation_list_view_item.fxml");
		this.item = null;
		this.modifiedItem = null;
		this.i18n = i18n;
		this.savedProperty = savedProperty;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.itemBox);
	}

	@Override
	protected void updateItem(final DiagramConfiguration item, final boolean empty) {
		super.updateItem(item, empty);
		if(item != null) {
			if(!item.equals(this.item)) {
				clear();
				if (item instanceof ActivationOperationConfiguration) {
					updateOperationDiagramItem((ActivationOperationConfiguration) item);
				} else if (item instanceof ActivationChoiceConfiguration) {
					updateChoiceDiagramItem((ActivationChoiceConfiguration) item);
				} else {
					updateListenerItem((UIListenerConfiguration) item);
				}
				this.item = item;
				this.setPrefHeight(itemBox.getChildren().size() * 20.0f);
				this.setGraphic(this.itemBox);
				this.setText("");
				this.setItem(modifiedItem);
			}
		} else {
			clear();
		}
	}

	public void updateOperationDiagramItem(ActivationOperationConfiguration item) {
		this.modifiedItem = new ActivationOperationConfiguration(item.getId(), item.getOpName(), item.getAfter(), item.getPriority(), item.getAdditionalGuards(),
			item.getActivationKind(), item.getFixedVariables(), item.getProbabilisticVariables(), item.getActivating(), item.isActivatingOnlyWhenExecuted(),
			item.getUpdating(), item.getWithPredicate());
		ActivationOperationConfiguration currentModifiedItem = (ActivationOperationConfiguration) this.modifiedItem;

		TextField tfID = new TextField(item.getId());
		tfID.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setId(to);
		});
		this.itemBox.getChildren().add(tfID);

		Label lbOpName = new Label(i18n.translate("simulation.item.operation"));
		lbOpName.getStyleClass().add("information");
		TextField tfOpName = new TextField(item.getOpName());
		tfOpName.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setOpName(from);
		});
		this.itemBox.getChildren().add(new HBox(lbOpName, tfOpName));

		Label lbTime = new Label(i18n.translate("simulation.item.time"));
		lbTime.getStyleClass().add("information");
		TextField tfTime = new TextField(item.getAfter());
		tfTime.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setAfter(to.isEmpty() ? "0" : to);
		});
		this.itemBox.getChildren().add(new HBox(lbTime, tfTime));

		Label lbPriority = new Label(i18n.translate("simulation.item.priority"));
		lbPriority.getStyleClass().add("information");
		TextField tfPriority = new TextField(String.valueOf(item.getPriority()));
		tfPriority.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			try {
				currentModifiedItem.setPriority(Integer.parseInt(to));
			} catch (NumberFormatException e) {
				currentModifiedItem.setPriority(0);
			}
		});
		this.itemBox.getChildren().add(new HBox(lbPriority, tfPriority));


		Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
		lbActivation.getStyleClass().add("information");
		TextField tfActivation = new TextField(item.getActivatingAsString());
		tfActivation.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setActivating(processActivating(to));
		});
		this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));

		Label lbActivationKind = new Label(i18n.translate("simulation.item.activationKind"));
		lbActivationKind.getStyleClass().add("information");
		TextField tfActivationKind = new TextField(item.getActivationKind().getName());
		tfActivationKind.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			try {
				currentModifiedItem.setActivationKind(to.isEmpty() ? null :ActivationOperationConfiguration.ActivationKind.valueOf(to));
			} catch (IllegalArgumentException e) {
				currentModifiedItem.setActivationKind(null);
			}
		});
		this.itemBox.getChildren().add(new HBox(lbActivationKind, tfActivationKind));

		Label lbAdditionalGuards = new Label(i18n.translate("simulation.item.additionalGuards"));
		lbAdditionalGuards.getStyleClass().add("information");
		TextField tfAdditionalGuards = new TextField(item.getAdditionalGuards());
		tfAdditionalGuards.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setAdditionalGuards(to.isEmpty() ? null : to);
		});
		this.itemBox.getChildren().add(new HBox(lbAdditionalGuards, tfAdditionalGuards));


		Label lbFixedVariables = new Label(i18n.translate("simulation.item.fixedVariables"));
		lbFixedVariables.getStyleClass().add("information");
		TextField tfFixedVariables = new TextField(item.getFixedVariablesAsString());
		tfFixedVariables.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setFixedVariables(processFixedVariables(to));
		});
		this.itemBox.getChildren().add(new HBox(lbFixedVariables, tfFixedVariables));

		Label lbProbabilisticVariables = new Label(i18n.translate("simulation.item.probabilisticVariables"));
		lbProbabilisticVariables.getStyleClass().add("information");
		TextField tfProbabilisticVariables = new TextField(item.getProbabilisticVariablesAsString());
		tfProbabilisticVariables.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setProbabilisticVariables(processProbabilisticVariables(to));
		});
		this.itemBox.getChildren().add(new HBox(lbProbabilisticVariables, tfProbabilisticVariables));
	}

	public void updateChoiceDiagramItem(ActivationChoiceConfiguration item) {
		this.modifiedItem = new ActivationChoiceConfiguration(item.getId(), item.getActivations());
		ActivationChoiceConfiguration currentModifiedItem = (ActivationChoiceConfiguration) this.modifiedItem;

		TextField tfID = new TextField(item.getId());
		tfID.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setId(to);
		});
		this.itemBox.getChildren().add(tfID);

		Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
		lbActivation.getStyleClass().add("information");
		TextField tfActivation = new TextField(item.getActivationsAsString());
		tfActivation.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setActivations(processChoiceActivation(to));
		});
		this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));
	}

	public void updateListenerItem(UIListenerConfiguration item) {
		this.modifiedItem = new UIListenerConfiguration(item.getId(), item.getEvent(), item.getPredicate(), item.getActivating());
		UIListenerConfiguration currentModifiedItem = (UIListenerConfiguration) this.modifiedItem;

		TextField tfID = new TextField(item.getId());
		tfID.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setId(to);
		});
		this.itemBox.getChildren().add(tfID);


		Label lbEvent = new Label(i18n.translate("simulation.item.event"));
		lbEvent.getStyleClass().add("information");
		TextField tfEvent = new TextField(item.getEvent());
		tfEvent.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setEvent(to);
		});
		this.itemBox.getChildren().add(new HBox(lbEvent, tfEvent));

		Label lbPredicate = new Label(i18n.translate("simulation.item.predicate"));
		lbPredicate.getStyleClass().add("information");
		TextField tfPredicate = new TextField(item.getPredicate());
		tfPredicate.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setPredicate(to.isEmpty() ? null : to);
		});
		this.itemBox.getChildren().add(new HBox(lbPredicate, tfPredicate));


		Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
		lbActivation.getStyleClass().add("information");
		TextField tfActivation = new TextField(item.getActivatingAsString());
		tfActivation.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			currentModifiedItem.setActivating(processActivating(to));
		});
		this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));

	}

	private static List<String> processActivating(String activating) {
		if(activating.isEmpty()) {
			return null;
		}
		return List.of(activating.replaceAll(" ", "").split(","));
	}

	private static Map<String, String> processFixedVariables(String fixedVariablesAsString) {
		if(fixedVariablesAsString.isEmpty()) {
			return null;
		}
		Map<String, String> fixedVariables = new HashMap<>();

		if(fixedVariablesAsString.startsWith("{") && fixedVariablesAsString.endsWith("}")) {
			List<String> entries = List.of(fixedVariablesAsString.substring(1, fixedVariablesAsString.length() - 2).replaceAll(" ", "").split(","));

			for (String entry : entries) {
				String[] entryAsArray = entry.split(":");
				fixedVariables.put(entryAsArray[0], entryAsArray[1]);
			}
		} else {
			List<String> entries = List.of(fixedVariablesAsString.replaceAll(" ", "").split(","));

			for (String entry : entries) {
				String[] entryAsArray = entry.split(":");
				fixedVariables.put(entryAsArray[0], entryAsArray[1]);
			}
		}

		return fixedVariables;
	}

	private static Object processProbabilisticVariables(String probabilisticVariablesAsString) {
		if(probabilisticVariablesAsString.isEmpty()) {
			return null;
		}

		Map<String, String> probabilisticVariables = new HashMap<>();

		if(probabilisticVariablesAsString.startsWith("{") && probabilisticVariablesAsString.endsWith("}")) {
			List<String> entries = List.of(probabilisticVariablesAsString.substring(1, probabilisticVariablesAsString.length() - 2).replaceAll(" ", "").split(","));

			for (String entry : entries) {
				String[] entryAsArray = entry.split(":");
				probabilisticVariables.put(entryAsArray[0], entryAsArray[1]);
			}
		} else if("uniform".equals(probabilisticVariablesAsString) || "first".equals(probabilisticVariablesAsString)) {
			return probabilisticVariablesAsString;
		} else {
			List<String> entries = List.of(probabilisticVariablesAsString.replaceAll(" ", "").split(","));

			for (String entry : entries) {
				String[] entryAsArray = entry.split(":");
				probabilisticVariables.put(entryAsArray[0], entryAsArray[1]);
			}
		}

		return probabilisticVariables;
	}


	private static Map<String, String> processChoiceActivation(String choiceActivationAsString) {
		Map<String, String> choiceActivation = new HashMap<>();

		if(choiceActivationAsString.startsWith("{") && choiceActivationAsString.endsWith("}")) {
			List<String> entries = List.of(choiceActivationAsString.substring(1, choiceActivationAsString.length() - 2).replaceAll(" ", "").split(","));

			for (String entry : entries) {
				String[] entryAsArray = entry.split(":");
				choiceActivation.put(entryAsArray[0], entryAsArray[1]);
			}
		} else {
			List<String> entries = List.of(choiceActivationAsString.replaceAll(" ", "").split(","));

			for (String entry : entries) {
				String[] entryAsArray = entry.split(":");
				choiceActivation.put(entryAsArray[0], entryAsArray[1]);
			}
		}

		return choiceActivation;
	}

	public void clear() {
		itemBox.getChildren().clear();
		this.setGraphic(this.itemBox);
		this.setText("");
	}

}
