package de.prob2.ui.animation.tracereplay.refactoring;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.prob.scripting.ClassicalBFactory;
import de.prob.scripting.EventBFactory;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.stage.FileChooser;

public class RefactorSetupView extends Dialog<RefactorSetup> {

	private static final List<String> CLASSICAL_EXTENSIONS = Collections.unmodifiableList(Arrays.asList(EventBFactory.RODIN_MACHINE_EXTENSION, EventBFactory.ATELIER_B_EXTENSION, ClassicalBFactory.CLASSICAL_B_MACHINE_EXTENSION, ClassicalBFactory.CLASSICAL_B_REFINEMENT_EXTENSION));

	@FXML
	ComboBox<RefactorSetup.WhatToDo> options;

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
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;

	private final SimpleObjectProperty<Path> alpha = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<Path> beta = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<Path> trace = new SimpleObjectProperty<>();
	private final SimpleObjectProperty<RefactorSetup.WhatToDo> whatToDo = new SimpleObjectProperty<>();

	public RefactorSetupView(final StageManager stageManager, final CurrentProject currentProject, final I18n i18n, final FileChooserManager fileChooserManager) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		stageManager.loadFXML(this, "refactor_setup_view.fxml");
	}

	@FXML
	private void initialize() {
		ButtonType buttonTypeA = new ButtonType(i18n.translate("traceModification.traceRefactorSetup.button.abort"), ButtonBar.ButtonData.CANCEL_CLOSE);
		ButtonType buttonTypeS = new ButtonType(i18n.translate("traceModification.traceRefactorSetup.button.start"), ButtonBar.ButtonData.APPLY);
		this.getDialogPane().getButtonTypes().addAll(buttonTypeS, buttonTypeA);

		beta.set(currentProject.getLocation().resolve(currentProject.getCurrentMachine().getLocation()));

		whatToDo.bind(options.getSelectionModel().selectedItemProperty());

		this.getDialogPane().lookupButton(buttonTypeS).disableProperty().setValue(true);

		options.setItems(FXCollections.observableArrayList(RefactorSetup.WhatToDo.validValues()));
		options.setConverter(i18n.translateConverter());
		options.setPromptText(i18n.translate("traceModification.traceRefactorSetup.whatToDo.prompt"));
		options.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue == null) {
				whatToDo.set(RefactorSetup.WhatToDo.NOTHING);
				secondMachine.disableProperty().setValue(false);
			} else {
				switch (newValue) {
					case REFACTOR_TRACE:
						firstMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file1.refactor"));
						secondMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file2.refactor"));
						traceFile.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.trace.refactor"));
						secondMachine.disableProperty().setValue(false);

						break;
					case REFINEMENT_REPLAY:
						firstMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file1.refinement"));
						secondMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file2.refinement"));
						traceFile.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.trace.refinement"));
						secondMachine.disableProperty().setValue(false);

						break;
					case OPTION_REPLAY:
						firstMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file1.replay"));
						secondMachine.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.file2.replay"));
						traceFile.textProperty().set(i18n.translate("traceModification.traceRefactorSetup.trace.replay"));
						secondMachine.disableProperty().setValue(true);
						break;
				}
			}
		});

		addPathSelectionAction(firstMachine, alpha, "common.fileChooser.fileTypes.classicalB", CLASSICAL_EXTENSIONS);
		addPathSelectionAction(secondMachine, beta, "common.fileChooser.fileTypes.classicalB", CLASSICAL_EXTENSIONS);
		addPathSelectionAction(traceFile, trace, "common.fileChooser.fileTypes.proB2Trace", Collections.singletonList(TraceFileHandler.TRACE_FILE_EXTENSION));

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

		alpha.addListener(reactionPath);
		beta.addListener(reactionPath);
		trace.addListener(reactionPath);
		whatToDo.addListener((observable, oldValue, newValue) -> {
			if (allowOK()) {
				this.getDialogPane().lookupButton(buttonTypeS).disableProperty().setValue(false);
			}
		});
	}

	private void addPathSelectionAction(Button button, Property<Path> pathProperty, String extensionKey, List<String> extensions) {
		button.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(i18n.translate("traceModification.traceRefactorSetup.traceFileChooser.title", button.getText()));
			fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
			fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter(extensionKey, extensions));
			pathProperty.setValue(fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.TRACES, stageManager.getCurrent()));
			if (pathProperty.getValue() != null) {
				button.textProperty().setValue(button.getText() + ": " + pathProperty.getValue().getFileName());
			}
		});
	}

	private boolean allowOK() {
		if (whatToDo.get() != null && whatToDo.get() != RefactorSetup.WhatToDo.NOTHING && trace.get() != null) {
			if (alpha.get() != null && beta.get() != null) {
				return true;
			} else if (alpha.get() == null && (whatToDo.get() == RefactorSetup.WhatToDo.REFACTOR_TRACE)) {
				return true; //Refactoring replay can work with one trace
			}
			if (beta.get() == null && whatToDo.get() == RefactorSetup.WhatToDo.OPTION_REPLAY) {
				return true; //Conditional replay can work with one trace

			}

		}

		return false;
	}
}
