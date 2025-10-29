package de.prob2.ui.simulation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationKind;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.TransitionSelection;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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
				this.setItem(modifiedItem);
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
					this.modifiedItem = new ActivationOperationConfiguration(currentItem.getId(), currentItem.getExecute(), currentItem.getAfter(), currentItem.getPriority(), currentItem.getAdditionalGuards(), currentItem.getActivationKind(), currentItem.getFixedVariables(), currentItem.getProbabilisticVariables(), currentItem.getTransitionSelection(), currentItem.getActivating(), currentItem.isActivatingOnlyWhenExecuted(), currentItem.getUpdating(), currentItem.getWithPredicate(), currentItem.getComment());
					updateOperationDiagramItem((ActivationOperationConfiguration) this.modifiedItem);
				}
				case ActivationChoiceConfiguration currentItem -> {
					this.modifiedItem = new ActivationChoiceConfiguration(currentItem.getId(), currentItem.getChooseActivation(), currentItem.getComment());
					updateChoiceDiagramItem((ActivationChoiceConfiguration) this.modifiedItem);
				}
				case UIListenerConfiguration currentItem -> {
					this.modifiedItem = new UIListenerConfiguration(currentItem.getId(), currentItem.getEvent(), currentItem.getPredicate(), currentItem.getActivating(), currentItem.getComment());
					updateListenerItem((UIListenerConfiguration) this.modifiedItem);
				}
				default -> throw new AssertionError("unknown item type");
			}

			this.setPrefHeight(itemBox.getChildren().size() * 20.0f);
			this.setGraphic(this.itemBox);
			this.setText("");
		}
	}

	private void updateOperationDiagramItem(ActivationOperationConfiguration item) {
		Label lbID = new Label(i18n.translate("simulation.item.id"));
		lbID.getStyleClass().add("information");
		lbID.setTooltip(new Tooltip(i18n.translate("simulation.item.id.hover")));

		TextField tfID = new TextField(item.getId());
		tfID.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setId(to);
		});
		tfID.disableProperty().bind(this.runningProperty);
		tfID.setTooltip(new Tooltip(i18n.translate("simulation.item.id.hover")));
		this.itemBox.getChildren().add(new HBox(lbID, tfID));

		Label lbOpName = new Label(i18n.translate("simulation.item.operation"));
		lbOpName.getStyleClass().add("information");
		lbOpName.setTooltip(new Tooltip(i18n.translate("simulation.item.operation.hover")));
		TextField tfOpName = new TextField(item.getExecute());
		tfOpName.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setExecute(from);
		});
		tfOpName.disableProperty().bind(this.runningProperty);
		tfOpName.setTooltip(new Tooltip(i18n.translate("simulation.item.operation.hover")));
		this.itemBox.getChildren().add(new HBox(lbOpName, tfOpName));

		Label lbTime = new Label(i18n.translate("simulation.item.time"));
		lbTime.getStyleClass().add("information");
		lbTime.setTooltip(new Tooltip(i18n.translate("simulation.item.time.hover")));
		TextField tfTime = new TextField(item.getAfter());
		tfTime.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setAfter(to.isEmpty() ? "0" : to);
		});
		tfTime.disableProperty().bind(this.runningProperty);
		tfTime.setTooltip(new Tooltip(i18n.translate("simulation.item.time.hover")));
		this.itemBox.getChildren().add(new HBox(lbTime, tfTime));

		Label lbPriority = new Label(i18n.translate("simulation.item.priority"));
		lbPriority.getStyleClass().add("information");
		lbPriority.setTooltip(new Tooltip(i18n.translate("simulation.item.priority.hover")));
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
		tfPriority.setTooltip(new Tooltip(i18n.translate("simulation.item.priority.hover")));
		this.itemBox.getChildren().add(new HBox(lbPriority, tfPriority));


		Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
		lbActivation.getStyleClass().add("information");
		lbActivation.setTooltip(new Tooltip(i18n.translate("simulation.item.activations.hover")));
		TextField tfActivation = new TextField(containerToString(item.getActivating()));
		tfActivation.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setActivating(parseList(to));
		});
		tfActivation.disableProperty().bind(this.runningProperty);
		tfActivation.setTooltip(new Tooltip(i18n.translate("simulation.item.activations.hover")));
		this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));

		Label lbActivationKind = new Label(i18n.translate("simulation.item.activationKind"));
		lbActivationKind.getStyleClass().add("information");
		lbActivationKind.setTooltip(new Tooltip(i18n.translate("simulation.item.activationKind.hover")));
		TextField tfActivationKind = new TextField(item.getActivationKind().getName());
		tfActivationKind.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			try {
				item.setActivationKind(ActivationKind.fromName(to));
			} catch (IllegalArgumentException ignored) {
				item.setActivationKind(ActivationKind.MULTI);
			}
		});
		tfActivationKind.disableProperty().bind(this.runningProperty);
		tfActivationKind.setTooltip(new Tooltip(i18n.translate("simulation.item.activationKind.hover")));
		this.itemBox.getChildren().add(new HBox(lbActivationKind, tfActivationKind));

		Label lbAdditionalGuards = new Label(i18n.translate("simulation.item.additionalGuards"));
		lbAdditionalGuards.getStyleClass().add("information");
		lbAdditionalGuards.setTooltip(new Tooltip(i18n.translate("simulation.item.additionalGuards.hover")));
		TextField tfAdditionalGuards = new TextField(item.getAdditionalGuards());
		tfAdditionalGuards.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setAdditionalGuards(to.isEmpty() ? null : to);
		});
		tfAdditionalGuards.disableProperty().bind(this.runningProperty);
		tfAdditionalGuards.setTooltip(new Tooltip(i18n.translate("simulation.item.additionalGuards.hover")));
		this.itemBox.getChildren().add(new HBox(lbAdditionalGuards, tfAdditionalGuards));


		Label lbFixedVariables = new Label(i18n.translate("simulation.item.fixedVariables"));
		lbFixedVariables.getStyleClass().add("information");
		lbFixedVariables.setTooltip(new Tooltip(i18n.translate("simulation.item.fixedVariables.hover")));
		TextField tfFixedVariables = new TextField(containerToString(item.getFixedVariables()));
		tfFixedVariables.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setFixedVariables(parseMap(to));
		});
		tfFixedVariables.disableProperty().bind(this.runningProperty);
		tfFixedVariables.setTooltip(new Tooltip(i18n.translate("simulation.item.fixedVariables.hover")));
		this.itemBox.getChildren().add(new HBox(lbFixedVariables, tfFixedVariables));

		Label lbProbabilisticVariables = new Label(i18n.translate("simulation.item.probabilisticVariables"));
		lbProbabilisticVariables.getStyleClass().add("information");
		lbProbabilisticVariables.setTooltip(new Tooltip(i18n.translate("simulation.item.probabilisticVariables.hover")));
		TextField tfProbabilisticVariables = new TextField(containerToString(item.getProbabilisticVariables()));
		tfProbabilisticVariables.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setProbabilisticVariables(parseProbabilisticVariables(to));
		});
		tfProbabilisticVariables.disableProperty().bind(this.runningProperty);
		tfProbabilisticVariables.setTooltip(new Tooltip(i18n.translate("simulation.item.probabilisticVariables.hover")));
		this.itemBox.getChildren().add(new HBox(lbProbabilisticVariables, tfProbabilisticVariables));

		Label lbTransitionSelection = new Label(i18n.translate("simulation.item.transitionSelection"));
		lbTransitionSelection.getStyleClass().add("information");
		lbTransitionSelection.setTooltip(new Tooltip(i18n.translate("simulation.item.transitionSelection.hover")));
		TextField tfTransitionSelection = new TextField(item.getTransitionSelection().getName());
		tfTransitionSelection.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			try {
				item.setTransitionSelection(TransitionSelection.fromName(to));
			} catch (IllegalArgumentException ignored) {
				item.setTransitionSelection(TransitionSelection.FIRST);
			}
		});
		tfTransitionSelection.disableProperty().bind(this.runningProperty);
		tfTransitionSelection.setTooltip(new Tooltip(i18n.translate("simulation.item.transitionSelection.hover")));
		this.itemBox.getChildren().add(new HBox(lbTransitionSelection, tfTransitionSelection));

		// TODO: activatingOnlyWhenExecuted, updating, withPredicate
	}

	private void updateChoiceDiagramItem(ActivationChoiceConfiguration item) {
		TextField tfID = new TextField(item.getId());
		tfID.setTooltip(new Tooltip(i18n.translate("simulation.item.id.hover")));
		tfID.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setId(to);
		});
		tfID.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(tfID);

		Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
		lbActivation.setTooltip(new Tooltip(i18n.translate("simulation.item.choiceActivations.hover")));
		lbActivation.getStyleClass().add("information");
		TextField tfActivation = new TextField(containerToString(item.getChooseActivation()));
		tfActivation.setTooltip(new Tooltip(i18n.translate("simulation.item.choiceActivations.hover")));
		tfActivation.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setChooseActivation(parseMap(to));
		});
		tfActivation.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));
	}

	private void updateListenerItem(UIListenerConfiguration item) {
		TextField tfID = new TextField(item.getId());
		tfID.setTooltip(new Tooltip(i18n.translate("simulation.item.id.hover")));
		tfID.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setId(to);
		});
		tfID.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(tfID);


		Label lbEvent = new Label(i18n.translate("simulation.item.event"));
		lbEvent.setTooltip(new Tooltip(i18n.translate("simulation.item.eventListener.hover")));
		lbEvent.getStyleClass().add("information");
		TextField tfEvent = new TextField(item.getEvent());
		tfEvent.setTooltip(new Tooltip(i18n.translate("simulation.item.eventListener.hover")));
		tfEvent.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setEvent(to);
		});
		tfEvent.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbEvent, tfEvent));

		Label lbPredicate = new Label(i18n.translate("simulation.item.predicate"));
		lbPredicate.setTooltip(new Tooltip(i18n.translate("simulation.item.predicate.hover")));
		lbPredicate.getStyleClass().add("information");
		TextField tfPredicate = new TextField(item.getPredicate());
		tfPredicate.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setPredicate(to);
		});
		tfPredicate.disableProperty().bind(this.runningProperty);
		tfPredicate.setTooltip(new Tooltip(i18n.translate("simulation.item.predicate.hover")));
		this.itemBox.getChildren().add(new HBox(lbPredicate, tfPredicate));


		Label lbActivation = new Label(i18n.translate("simulation.item.activations"));
		lbActivation.getStyleClass().add("information");
		lbActivation.setTooltip(new Tooltip(i18n.translate("simulation.item.activations.hover")));
		TextField tfActivation = new TextField(containerToString(item.getActivating()));
		tfActivation.textProperty().addListener((observable, from, to) -> {
			savedProperty.set(false);
			item.setActivating(parseList(to));
		});
		tfActivation.setTooltip(new Tooltip(i18n.translate("simulation.item.activations.hover")));
		tfActivation.disableProperty().bind(this.runningProperty);
		this.itemBox.getChildren().add(new HBox(lbActivation, tfActivation));
	}

	private String containerToString(Object o) {
		if (o == null) {
			return "";
		}

		String s = o.toString();
		if ((s.startsWith("[") && s.endsWith("]")) || (s.startsWith("{") && s.endsWith("}"))) {
			s = s.substring(1, s.length() - 1);
		}

		return s;
	}

	private List<String> parseList(String s) {
		if (s.startsWith("[") && s.endsWith("]")) {
			s = s.substring(1, s.length() - 1);
		}

		// TODO: this doesnt work when the string contains ','
		return Arrays.stream(s.split(","))
				.map(String::strip)
				.filter(s_ -> !s_.isEmpty())
				.toList();
	}

	private Map<String, String> parseMap(String s) {
		if (s.startsWith("{") && s.endsWith("}")) {
			s = s.substring(1, s.length() - 1);
		}

		// TODO: this doesnt work when the string contains ',' or '='
		return Arrays.stream(s.split(","))
				.map(e -> Arrays.stream(e.split("=", 2)).map(String::strip).filter(s_ -> !s_.isEmpty()).toArray(String[]::new))
				.filter(a -> a.length == 2)
				.collect(Collectors.toUnmodifiableMap(a -> a[0], a -> a[1], (oldValue, newValue) -> newValue));
	}

	private Map<String, Map<String, String>> parseProbabilisticVariables(String s) {
		if (s.startsWith("{") && s.endsWith("}")) {
			s = s.substring(1, s.length() - 1);
		}

		// TODO: this doesnt work when the string contains ',', '=' or '}'
		Map<String, Map<String, String>> probabilisticVariables = new HashMap<>();
		for (int i = 0, len = s.length(); i < len; ) {
			int pos = s.indexOf('=', i);
			if (pos < 0 || pos + 1 >= len) {
				break;
			}

			int entryEnd = s.indexOf('}', pos + 1);
			if (entryEnd < 0 || entryEnd + 1 >= len) {
				break;
			}

			String key = s.substring(i, pos).strip();
			if (key.isEmpty()) {
				continue;
			}

			String value = s.substring(pos + 1, entryEnd + 1).strip();
			probabilisticVariables.put(key, parseMap(value));

			int next = s.indexOf(',', entryEnd + 1);
			if (next < 0) {
				break;
			}

			i = next + 1;
		}

		return probabilisticVariables;
	}

	private void clear() {
		itemBox.getChildren().clear();
		//this.setGraphic(this.itemBox);
		this.setGraphic(null);
		this.setText("");
		this.modifiedItem = null;
	}
}
