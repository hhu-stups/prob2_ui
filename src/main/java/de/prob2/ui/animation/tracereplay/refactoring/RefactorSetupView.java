package de.prob2.ui.animation.tracereplay.refactoring;

import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
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

	private final SimpleObjectProperty<Path> alpha= new SimpleObjectProperty<>();
	private final SimpleObjectProperty<Path> beta = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<Path> trace = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<RefactorSetup.WhatToDo> whatToDo = new SimpleObjectProperty<>();



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

		beta.set(currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation()));

		this.getDialogPane().lookupButton(buttonTypeS).disableProperty().setValue(true);

		Dialog<RefactorSetup> dialog = this;



		options.setItems(FXCollections.observableArrayList(Arrays.asList("Refactor Trace", "Check Refinement", "Conditional Replay")));
		options.setValue("Select Action");

		options.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			switch (newValue.intValue()){
				case 0:
					whatToDo.set( RefactorSetup.WhatToDo.REFACTOR_TRACE);
					firstMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file1.refactor"));
					secondMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file2.refactor"));
					traceFile.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.trace.refactor") );
					secondMachine.disableProperty().setValue(false);

					break;
				case 1:
					whatToDo.set(RefactorSetup.WhatToDo.REFINEMENT_REPLAY);
					firstMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file1.refinement"));
					secondMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file2.refinement"));
					traceFile.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.trace.refinement"));
					secondMachine.disableProperty().setValue(false);

					break;
				case 2:
					whatToDo.set(RefactorSetup.WhatToDo.OPTION_REPLAY);
					firstMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file1.replay"));
					secondMachine.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.file2.replay"));
					traceFile.textProperty().set(bundle.getString("traceModification.traceRefactorSetup.trace.replay"));
					secondMachine.disableProperty().setValue(true);
					break;
				default:
					whatToDo.set(RefactorSetup.WhatToDo.NOTHING);
					secondMachine.disableProperty().setValue(false);

					break;
			}
		});


		firstMachine.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Choose: " + firstMachine.getText());
			fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
			fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.classicalB", classicalB));
			alpha.setValue(fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent()));
			if(alpha.getValue() != null){
				firstMachine.textProperty().setValue(firstMachine.getText() + ": " + alpha.getValue().getFileName());
			}
		});

		secondMachine.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Choose: " + secondMachine.getText());
			fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
			fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.classicalB", classicalB));
			beta.setValue(fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent()));
			if(beta.getValue() != null){
				secondMachine.textProperty().setValue(secondMachine.getText() + ": " + beta.getValue().getFileName());
			}
		});

		traceFile.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Choose: " + traceFile.getText());
			fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
			fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Trace", TraceFileHandler.TRACE_FILE_EXTENSION));
			trace.setValue(fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent()));
			if(trace.getValue() != null){
				traceFile.textProperty().setValue(traceFile.getText() + ": " + trace.getValue().getFileName());
			}
		});



		this.setResultConverter(param -> {
					if (param.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
						return new RefactorSetup(RefactorSetup.WhatToDo.NOTHING, alpha.get(), beta.get(), trace.get(), checkBox.isSelected());

					} else {
						return new RefactorSetup(whatToDo.get(), alpha.get(), beta.get(), trace.get(), checkBox.isSelected());
					}
				}
		);

		ChangeListener<Path> reactionPath = (observable, oldValue, newValue) -> {
			if (allowOK()) {
				this.getDialogPane().lookupButton(buttonTypeS).disableProperty().setValue(false);
			}
		};

		ChangeListener<RefactorSetup.WhatToDo> reactionWhat = (observable, oldValue, newValue) -> {
			if (allowOK()) {
				this.getDialogPane().lookupButton(buttonTypeS).disableProperty().setValue(false);
			}
		};

		alpha.addListener(reactionPath);
		beta.addListener(reactionPath);
		trace.addListener(reactionPath);
		whatToDo.addListener(reactionWhat);


	}

	private boolean allowOK(){
		if(whatToDo.get()!= RefactorSetup.WhatToDo.NOTHING && trace.get()!= null){
			if(alpha.get()!= null && beta.get() != null){
				return true;
			}else if(alpha.get()==null&&(whatToDo.get()== RefactorSetup.WhatToDo.REFACTOR_TRACE)){
				return true; //Refactoring replay can work with one trace
			} if(beta.get()==null &&whatToDo.get()== RefactorSetup.WhatToDo.OPTION_REPLAY){
				return true; //Conditional replay can work with one trace

			}

		}

		return false;
	}


}
