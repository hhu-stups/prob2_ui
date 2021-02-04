package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.check.tracereplay.check.ProgressMemoryInterface;
import de.prob2.ui.internal.StageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.reactfx.StateMachine;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProgressMemory extends AnchorPane implements ProgressMemoryInterface {


	private final Map<Integer, Integer> stepToTasks;
	private final Map<Integer, Integer> stepToTaskFulfilled;
	private final Map<Integer, String> stepLabel;
	private int currentStep;
	private final double intervalSize;
	private double progressUntilNow;

	@FXML
	AnchorPane ground;

	@FXML
	Label label;

	@FXML
	ProgressBar bar;

	@FXML
	private void initialize() {
		bar.setProgress(progressUntilNow);
	}


	@Override
	public void nextStep(){
		currentStep = currentStep + 1;
		progressUntilNow = progressUntilNow + intervalSize;
		Platform.runLater(() -> {
			label.setText(stepLabel.get(currentStep));
			bar.setProgress(progressUntilNow );

		});
	}

	@Override
	public void addTask() {
		addTasks(1);
	}

	@Override
	public void addTasks(int count) {
		stepToTasks.put(currentStep, stepToTasks.get(currentStep) +count);
	}

	@Override
	public void fulfillTask() {
		fulfillTasks(1);
	}

	@Override
	public void fulfillTasks(int count) {
		stepToTaskFulfilled.put(currentStep, stepToTaskFulfilled.get(currentStep) +count);

		double progressForCurrentInterval = intervalSize * ((double) stepToTaskFulfilled.get(currentStep)/stepToTasks.get(currentStep));


		Platform.runLater(() -> bar.setProgress(progressUntilNow + progressForCurrentInterval));

	}


	public ProgressMemory(int steps, List<String> stepLabels, final StageManager stageManager, Stage progressStage){


		Scene progressScene = new Scene(this);

		progressStage.setScene(progressScene);

		stageManager.loadFXML(this, "progressBar.fxml");

		stageManager.register(progressStage, null);


		List<String> labelCopy = new ArrayList<>(stepLabels);
		if(labelCopy.size() < steps){
			labelCopy.addAll(IntStream.range(0, steps-stepLabels.size()).boxed().map(entry -> "no task label assigned").collect(Collectors.toList()));
		}

		intervalSize = 1.0/steps;
		progressUntilNow = 1.0 - intervalSize * steps;
		stepToTasks = IntStream.range(0, steps).boxed().collect(Collectors.toMap(entry -> entry, entry -> 0));
		stepToTaskFulfilled = IntStream.range(0, steps).boxed().collect(Collectors.toMap(entry -> entry, entry -> 0));
		stepLabel = IntStream.range(0, steps).boxed().collect(Collectors.toMap(entry -> entry, labelCopy::get));

	}


	public static ProgressMemory setupForTraceChecker(ResourceBundle resourceBundle, StageManager stageManager, Stage progressStage){
		List<String> labels = Arrays.asList(
				resourceBundle.getString("traceModification.progressBar.label.types"),
				resourceBundle.getString("traceModification.progressBar.label.delta"),
				resourceBundle.getString("traceModification.progressBar.label.prepare"),
				resourceBundle.getString("traceModification.progressBar.label.replay"),
				resourceBundle.getString("traceModification.progressBar.label.after")
		);

		return new ProgressMemory(5, labels, stageManager, progressStage);
	}

}
