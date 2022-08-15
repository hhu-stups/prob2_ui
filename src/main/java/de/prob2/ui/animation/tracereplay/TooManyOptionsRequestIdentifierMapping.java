package de.prob2.ui.animation.tracereplay;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Injector;

import de.prob.check.tracereplay.check.exploration.TraceExplorer;
import de.prob.check.tracereplay.check.ui.ToManyOptionsIdentifierMapping;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import static java.util.stream.Collectors.toMap;

public class TooManyOptionsRequestIdentifierMapping extends Dialog<Map<String, String>> implements ToManyOptionsIdentifierMapping {

	@FXML
	VBox oldInfos;

	@FXML
	VBox newInfos;

	@FXML
	AnchorPane oldInfosPane;

	@FXML
	AnchorPane newInfosPane;

	private final StageManager stageManager;
	private final I18n i18n;

	private final Map<String, SimpleStringProperty> internalView = new HashMap<>();

	public TooManyOptionsRequestIdentifierMapping(final StageManager stageManager, final I18n i18n) {
		this.stageManager = stageManager;
		this.i18n = i18n;

		stageManager.loadFXML(this, "tooManyOptionsChoice.fxml");

		ButtonType buttonTypeY = new ButtonType(i18n.translate("tooManyOptionsRequest.alert.button.ok"), ButtonBar.ButtonData.YES);
		//	ButtonType buttonTypeN = new ButtonType(i18n.translate("tooManyOptionsRequest.alert.button.dontCare"), ButtonBar.ButtonData.NO);

		this.setTitle(i18n.translate("tooManyOptionsRequest.alert.button.title"));
		//	this.setHeaderText(i18n.translate("tooManyOptionsRequest.alert.button.text"));
		this.getDialogPane().getButtonTypes().addAll(buttonTypeY);

		this.setResultConverter(
				param -> internalView.entrySet()
					.stream()
					.collect(toMap(Map.Entry::getKey, entry -> entry.getValue().getValue()))
		);
	}

	void prepare(List<String> oldInfo, List<String> newInfo) {
		for (String info : oldInfo) {
			Label old = new Label(info);
			oldInfos.getChildren().add(old);

			ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList(newInfo));
			if (newInfo.contains(info)) {
				choiceBox.getSelectionModel().select(info);
			}

			SimpleStringProperty selectedNew = new SimpleStringProperty(choiceBox.getValue());

			choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
				selectedNew.setValue(newValue);
			});

			old.prefHeightProperty().bind(oldInfos.prefHeightProperty());
			choiceBox.prefHeightProperty().bind(newInfos.prefHeightProperty());

			old.prefWidthProperty().bind(oldInfos.prefWidthProperty());
			choiceBox.prefWidthProperty().bind(newInfos.prefWidthProperty());

			newInfos.getChildren().add(choiceBox);

			internalView.put(info, selectedNew);
		}

		oldInfos.setAlignment(Pos.BOTTOM_RIGHT);
		newInfos.setAlignment(Pos.BOTTOM_RIGHT);

		oldInfos.prefHeightProperty().bind(oldInfosPane.heightProperty());
		oldInfos.prefWidthProperty().bind(oldInfosPane.widthProperty());
		newInfos.prefHeightProperty().bind(newInfosPane.heightProperty());
		newInfos.prefWidthProperty().bind(newInfosPane.widthProperty());
	}

	@Override
	public Map<String, String> askForMapping(List<String> oldInfo, List<String> newInfo, String operationName, TraceExplorer.MappingNames section) {
		stageManager.register(this);
		this.setHeaderText(i18n.translate("tooManyOptionsRequest.alert.button.text", section, operationName));
		prepare(oldInfo, newInfo);
		return this.showAndWait().orElseGet(Collections::emptyMap);
	}
}
