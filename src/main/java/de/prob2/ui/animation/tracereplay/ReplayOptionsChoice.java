package de.prob2.ui.animation.tracereplay;

import de.prob.check.tracereplay.check.ReplayOptions;
import de.prob.statespace.OperationInfo;
import de.prob2.ui.internal.StageManager;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReplayOptionsChoice extends TitledPane{


	private final List<String> identifier;
	private final StageManager stageManager;


	@FXML
	CheckBox variables;

	@FXML
	CheckBox input;

	@FXML
	CheckBox output;

	@FXML
	ListView<String> usedIDs;

	@FXML
	ListView<String> ignoredIDs;

	@FXML
	Button switchLeft;

	@FXML
	Button switchRight;

	public ReplayOptionsChoice(OperationInfo operationInfo, StageManager stageManager){
		this.identifier = new ArrayList<>(operationInfo.getAllIdentifier());
		this.stageManager = stageManager;

		stageManager.loadFXML(this, "replayOptionsChoice.fxml");
		this.setText(operationInfo.getOperationName());
	}

	public ReplayOptionsChoice(List<String> identifier, StageManager stageManager) {
		this.identifier = identifier;
		this.stageManager = stageManager;


		stageManager.loadFXML( this, "replayOptionsChoice.fxml");
		this.setText("Global Variables");

	}

	@FXML
	public void initialize(){
		initWithOperation();
	}

	public void initWithOperation(){
		usedIDs.getItems().addAll(identifier);

		switchRight.setOnAction(event -> {
			ObservableList<String> selected = usedIDs.getSelectionModel().getSelectedItems();
			if(!selected.isEmpty()){
				ignoredIDs.getItems().addAll(selected);
				usedIDs.getItems().removeAll(selected);
			}
		});

		switchLeft.setOnAction(event -> {
			ObservableList<String> selected = ignoredIDs.getSelectionModel().getSelectedItems();
			if(!selected.isEmpty()){
				usedIDs.getItems().addAll(selected);
				ignoredIDs.getItems().removeAll(selected);
			}
		});
	}

	public List<String> getIgnoredIDs(){
		return ignoredIDs.getItems();
	}

	public Set<ReplayOptions.OptionFlags> getActivation(){
		Set<ReplayOptions.OptionFlags> result = new HashSet<>();
		if(variables.isSelected()){
			result.add(ReplayOptions.OptionFlags.Variables);
		}
		if(input.isSelected()){
			result.add(ReplayOptions.OptionFlags.Input);
		}
		if(output.isSelected()){
			result.add(ReplayOptions.OptionFlags.Output);
		}
		return result;
	}

	public TitledPane getSelf(){
		return this;
	}


}
