package de.prob2.ui.animation.tracereplay.refactoring;

import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.nio.file.Path;
import java.util.*;

public class RefactorSetupView extends Dialog<RefactorSetup> {


	@FXML
	ChoiceBox<String> options;

	@FXML
	Button firstMachine;

	@FXML
	Button secondMachine;

	@FXML
	Button traceFile;

	@FXML
	CheckBox checkBox;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;

	private Path alpha;
	private Path beta;
	private Path trace;
	private RefactorSetup.WhatToDo whatToDo;

	public RefactorSetupView(final StageManager stageManager, final CurrentProject currentProject, final ResourceBundle bundle,
							final FileChooserManager fileChooserManager) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
		stageManager.loadFXML(this, "refactor_setup_view.fxml");
	}


	@FXML
	private void initialize() {
		List<String> classicalB = Arrays.asList("mch", "ref", "imp");

		ButtonType buttonTypeA = new ButtonType("Abort", ButtonBar.ButtonData.CANCEL_CLOSE);
		ButtonType buttonTypeS = new ButtonType("Start", ButtonBar.ButtonData.APPLY);
		this.getDialogPane().getButtonTypes().addAll(buttonTypeS,buttonTypeA);

		beta = currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation());


		options.setItems(FXCollections.observableArrayList(Arrays.asList("Refactor Trace", "Check Refinement", "Conditional Replay")));

		options.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			switch (newValue.intValue()){
				case 0:
					whatToDo = RefactorSetup.WhatToDo.REFACTOR_TRACE;
					firstMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file1.refactor"));
					secondMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file2.refactor")+ ": " + beta.getFileName().toString());
					traceFile.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.trace.refactor") );
					break;
				case 1:
					whatToDo = RefactorSetup.WhatToDo.REFINEMENT_REPLAY;
					firstMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file1.refinement"));
					secondMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file2.refinement"));
					traceFile.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.trace.refinement"));

					break;
				case 2:
					whatToDo= RefactorSetup.WhatToDo.OPTION_REPLAY;
					firstMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file1.replay"));
					secondMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file2.replay"));
					traceFile.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.trace.replay"));

					break;
				default:
					whatToDo = RefactorSetup.WhatToDo.NOTHING;
					break;
			}
		});


		secondMachine.disableProperty().bind(options.getSelectionModel().selectedIndexProperty().isEqualTo(2));

		firstMachine.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Choose: " + firstMachine.getText());
			fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
			fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.classicalB", classicalB));
			alpha = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
			if(alpha != null){
				firstMachine.textProperty().set(firstMachine.getText() + ": " + alpha.getFileName());
			}
		});

		secondMachine.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Choose: " + secondMachine.getText());
			fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
			fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.classicalB", classicalB));
			beta = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
			if(beta != null){
				secondMachine.textProperty().set(secondMachine.getText() + ": " + beta.getFileName());
			}
		});

		traceFile.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Choose: " + traceFile.getText());
			fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
			fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Trace", TraceFileHandler.TRACE_FILE_EXTENSION));
			trace = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent());
			if(trace != null){
				traceFile.textProperty().set(traceFile.getText() + ": " + trace.getFileName());
			}
		});


		this.setResultConverter(param -> {
					if (param.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
						return new RefactorSetup(RefactorSetup.WhatToDo.NOTHING, alpha, beta, trace, checkBox.isSelected());

					} else {
						return new RefactorSetup(whatToDo, alpha, beta, trace, checkBox.isSelected());
					}
				}
		);

	}


}
