package de.prob2.ui.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DiagramConfigurationListCell extends ListCell<DiagramConfiguration> {

	@FXML
	private VBox itemBox;

	private DiagramConfiguration modifiedItem;

	private final I18n i18n;

	private final BooleanProperty savedProperty;
	private final BooleanProperty runningProperty;

	public DiagramConfigurationListCell(StageManager stageManager, I18n i18n, BooleanProperty savedProperty, BooleanProperty runningProperty) {
		super();
		this.modifiedItem = null;
		this.i18n = i18n;
		this.savedProperty = savedProperty;
		this.savedProperty.addListener((observable, from, to) -> {
			if (getIndex() < 0) {
				return;
			}
			if (!from && to) {
				this.getListView().getItems().set(getIndex(), modifiedItem);
			}
		});
		this.runningProperty = runningProperty;
		stageManager.loadFXML(this,"simulation_list_view_item.fxml");
	}

	@FXML
	@SuppressWarnings("unused")
	private void initialize() {
		this.setText("");
		this.setGraphic(this.itemBox);
	}

	@Override
	protected void updateItem(final DiagramConfiguration item, final boolean empty) {
		super.updateItem(item, empty);
		if (empty || item == null) {
			clear();
		} else {
			clear();
			switch (item) {
				case ActivationOperationConfiguration currentItem -> {
					this.modifiedItem = new ActivationOperationConfiguration(currentItem.getId(), currentItem.getExecute(), currentItem.getAfter(), currentItem.getPriority(), currentItem.getAdditionalGuards(), currentItem.getActivationKind(), currentItem.getFixedVariables(), currentItem.getProbabilisticVariables(), currentItem.getActivating(), currentItem.isActivatingOnlyWhenExecuted(), currentItem.getUpdating(), currentItem.getWithPredicate());
					updateOperationDiagramItem((ActivationOperationConfiguration) this.modifiedItem);
				}
				case ActivationChoiceConfiguration currentItem -> {
					this.modifiedItem = new ActivationChoiceConfiguration(currentItem.getId(), currentItem.getChooseActivation());
					updateChoiceDiagramItem((ActivationChoiceConfiguration) this.modifiedItem);
				}
				case UIListenerConfiguration currentItem -> {
					this.modifiedItem = new UIListenerConfiguration(currentItem.getId(), currentItem.getEvent(), currentItem.getPredicate(), currentItem.getActivating());
					updateListenerItem((UIListenerConfiguration) this.modifiedItem);
				}
				default -> throw new AssertionError("unknown item type");
			}

			this.setPrefHeight(itemBox.getChildren().size() * 20.0f);
			this.setGraphic(this.itemBox);
			this.setText("");
			this.setItem(modifiedItem);
		}
	}

	private void updateOperationDiagramItem(ActivationOperationConfiguration item) {
		TextField tfID = new TextField(item.getId());
		tfID.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setId(to);
		});
		tfID.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(tfID);

		Label lbOpName = new Label(i18n.translate("simulation.item.operation"));
		lbOpName.getStyleClass().add("information");
		TextField tfOpName = new TextField(item.getExecute());
		tfOpName.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setExecute(from);
		});
		tfOpName.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbOpName, tfOpName));

		Label lbTime = new Label(i18n.translate("simulation.item.time"));
		lbTime.getStyleClass().add("information");
		TextField tfTime = new TextField(item.getAfter());
		tfTime.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setAfter(to.isEmpty() ? "0" : to);
		});
		tfTime.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbTime, tfTime));

		Label lbPriority = new Label(i18n.translate("simulation.item.priority"));
		lbPriority.getStyleClass().add("information");
		TextField tfPriority = new TextField(String.valueOf(item.getPriority()));
		tfPriority.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			try {
				item.setPriority(Integer.parseInt(to));
			} catch (NumberFormatException ignored) {
				item.setPriority(0);
			}
		});
		tfPriority.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbPriority, tfPriority));


		Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
		lbActivation.getStyleClass().add("information");
		TextField tfActivation = new TextField(collToString(item.getActivating()));
		tfActivation.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setActivating(processActivating(to));
		});
		tfActivation.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));

		Label lbActivationKind = new Label(i18n.translate("simulation.item.activationKind"));
		lbActivationKind.getStyleClass().add("information");
		TextField tfActivationKind = new TextField(item.getActivationKind().getName());
		tfActivationKind.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			try {
				item.setActivationKind(ActivationOperationConfiguration.ActivationKind.fromName(to));
			} catch (IllegalArgumentException ignored) {
				item.setActivationKind(ActivationOperationConfiguration.ActivationKind.MULTI);
			}
		});
		tfActivationKind.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbActivationKind, tfActivationKind));

		Label lbAdditionalGuards = new Label(i18n.translate("simulation.item.additionalGuards"));
		lbAdditionalGuards.getStyleClass().add("information");
		TextField tfAdditionalGuards = new TextField(item.getAdditionalGuards());
		tfAdditionalGuards.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setAdditionalGuards(to.isEmpty() ? null : to);
		});
		tfAdditionalGuards.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbAdditionalGuards, tfAdditionalGuards));


		Label lbFixedVariables = new Label(i18n.translate("simulation.item.fixedVariables"));
		lbFixedVariables.getStyleClass().add("information");
		TextField tfFixedVariables = new TextField(collToString(item.getFixedVariables()));
		tfFixedVariables.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setFixedVariables(processFixedVariables(to));
		});
		tfFixedVariables.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbFixedVariables, tfFixedVariables));

		Label lbProbabilisticVariables = new Label(i18n.translate("simulation.item.probabilisticVariables"));
		lbProbabilisticVariables.getStyleClass().add("information");
		TextField tfProbabilisticVariables = new TextField(collToString(item.getProbabilisticVariables()));
		tfProbabilisticVariables.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setProbabilisticVariables(processProbabilisticVariables(to));
		});
		tfProbabilisticVariables.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbProbabilisticVariables, tfProbabilisticVariables));
	}

	private void updateChoiceDiagramItem(ActivationChoiceConfiguration item) {
		TextField tfID = new TextField(item.getId());
		tfID.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setId(to);
		});
		tfID.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(tfID);

		Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
		lbActivation.getStyleClass().add("information");
		TextField tfActivation = new TextField(collToString(item.getChooseActivation()));
		tfActivation.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setChooseActivation(processChoiceActivation(to));
		});
		tfActivation.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));
	}

	private void updateListenerItem(UIListenerConfiguration item) {
		TextField tfID = new TextField(item.getId());
		tfID.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setId(to);
		});
		tfID.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(tfID);


		Label lbEvent = new Label(i18n.translate("simulation.item.event"));
		lbEvent.getStyleClass().add("information");
		TextField tfEvent = new TextField(item.getEvent());
		tfEvent.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setEvent(to);
		});
		tfEvent.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbEvent, tfEvent));

		Label lbPredicate = new Label(i18n.translate("simulation.item.predicate"));
		lbPredicate.getStyleClass().add("information");
		TextField tfPredicate = new TextField(item.getPredicate());
		tfPredicate.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setPredicate(to.isEmpty() ? "1=1" : to);
		});
		tfPredicate.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbPredicate, tfPredicate));


		Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
		lbActivation.getStyleClass().add("information");
		TextField tfActivation = new TextField(collToString(item.getActivating()));
		tfActivation.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setActivating(processActivating(to));
		});
		tfActivation.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));

	}

	private static String collToString(Object o) {
		var s = o.toString();
		if (s.startsWith("[")) {
			s = s.substring(1);
		}
		if (s.endsWith("]")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
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

	private void clear() {
		itemBox.getChildren().clear();
		this.setGraphic(this.itemBox);
		this.setText("");
		this.modifiedItem = null;
	}
}
