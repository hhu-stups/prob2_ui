package de.prob2.ui.animation.tracereplay;

import de.prob.check.tracereplay.check.exploration.ReplayOptions;
import de.prob.statespace.OperationInfo;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.util.*;
import java.util.stream.Collectors;

public class ReplayOptionsOverview extends Dialog<ReplayOptions> {

	private final Map<String, OperationInfo> operations;
	private final Map<String, ReplayOptionsChoice> choices;
	private ReplayOptionsChoice global = null;
	private final StageManager stageManager;

	@FXML
	Accordion globalView;

	@FXML
	Accordion operationView;


	public ReplayOptionsOverview(List<String> globalIdentifiers, Map<String, OperationInfo> operations, final StageManager stageManager){
		this.operations = operations;

		this.global = new ReplayOptionsChoice(new ArrayList<>(globalIdentifiers), stageManager);

		this.choices = new HashMap<>();

		this.stageManager = stageManager;

		stageManager.loadFXML(this, "replayOptionsOverView.fxml");
		stageManager.register(this);

		ButtonType buttonTypeI = new ButtonType("Done", ButtonBar.ButtonData.OK_DONE);

		this.getDialogPane().getButtonTypes().addAll(buttonTypeI);


		this.setResultConverter(param -> {
			Map<String, Set<ReplayOptions.OptionFlags>> flags = choices.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getActivation()));
			Map<String, List<String>> ids = choices.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getIgnoredIDs()));
			return new ReplayOptions(global.getActivation(), global.getIgnoredIDs(), flags, ids);

		});

	}

	@FXML
	public void initialize(){


		for(Map.Entry<String, OperationInfo> entry : operations.entrySet()){
			ReplayOptionsChoice replayOptionsChoice = new ReplayOptionsChoice(entry.getValue(), stageManager);
			choices.put(entry.getKey(), replayOptionsChoice);

			operationView.getPanes().add(choices.get(entry.getKey()).getSelf());
		}

		globalView.getPanes().add(0, global.getSelf());

	}
}
