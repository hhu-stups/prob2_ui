package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.check.tracereplay.check.exploration.TraceExplorer;
import de.prob.check.tracereplay.check.ui.ToManyOptionsIdentifierMapping;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.util.*;

import static java.util.stream.Collectors.toMap;

public class ToManyOptionsRequestIdentifierMapping extends Dialog<Map<String, String>> implements ToManyOptionsIdentifierMapping {

	@FXML
	VBox oldInfos;

	@FXML
	VBox newInfos;

	@FXML
	AnchorPane oldInfosPane;

	@FXML
	AnchorPane newInfosPane;

	private final StageManager stageManager;
	private final ResourceBundle resourceBundle;

	private final Map<String, SimpleStringProperty> internalView = new HashMap<>();

	public ToManyOptionsRequestIdentifierMapping(final Injector injector, final StageManager stageManager){
		this.stageManager = stageManager;
		this.resourceBundle = injector.getInstance(ResourceBundle.class);

		stageManager.loadFXML(this, "toManyOptionsChoice.fxml");

		ButtonType buttonTypeY = new ButtonType(resourceBundle.getString("toManyOptionsRequest.alert.button.ok"), ButtonBar.ButtonData.YES);
	//	ButtonType buttonTypeN = new ButtonType(resourceBundle.getString("toManyOptionsRequest.alert.button.dontCare"), ButtonBar.ButtonData.NO);

		this.setTitle(resourceBundle.getString("toManyOptionsRequest.alert.button.title"));
	//	this.setHeaderText(resourceBundle.getString("toManyOptionsRequest.alert.button.text"));
		this.getDialogPane().getButtonTypes().addAll(buttonTypeY);


		this.setResultConverter(param ->

			internalView.entrySet().stream().collect(toMap(Map.Entry::getKey, entry -> entry.getValue().getValue()))
		);
	}


	void prepare(List<String> oldInfo, List<String> newInfo){
		for(String info : oldInfo){
			Label old = new Label(info);
			oldInfos.getChildren().add(old);

			ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList(newInfo));
			if(newInfo.contains(info)){
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
		this.setHeaderText(String.format(resourceBundle.getString("toManyOptionsRequest.alert.button.text"), section, operationName));
		prepare(oldInfo, newInfo);
		return this.showAndWait().get();
	}
}
