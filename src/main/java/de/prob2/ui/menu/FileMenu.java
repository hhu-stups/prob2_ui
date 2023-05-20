package de.prob2.ui.menu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.command.GetInternalRepresentationCommand;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.FormulaTranslationMode;
import de.prob.model.eventb.EventBModel;
import de.prob.model.eventb.EventBPackageModel;
import de.prob.model.eventb.translate.ModelToXML;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.XTLModel;
import de.prob.scripting.Api;
import de.prob.statespace.FormalismType;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.error.WarningAlert;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.NewProjectStage;
import de.prob2.ui.project.ProjectManager;
import de.prob2.ui.project.SaveDocumentationStage;

import javafx.beans.InvalidationListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public class FileMenu extends Menu {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileMenu.class);
	
	@FXML
	private Menu recentProjectsMenu;
	@FXML
	private MenuItem saveMachineItem;
	@FXML
	private MenuItem saveProjectItem;
	@FXML
	private MenuItem reloadMachineItem;
	@FXML
	private MenuItem extendedStaticAnalysisItem;
	@FXML
	private MenuItem viewFormattedCodeItem;
	@FXML
	private Menu exportAsMenu;
	@FXML
	private MenuItem exportAsClassicalBAsciiItem;
	@FXML
	private MenuItem exportAsClassicalBUnicodeItem;
	@FXML
	private MenuItem exportAsRodinProject;
	@FXML
	private MenuItem exportAsEventBProlog;
	@FXML
	private MenuItem saveDocumentationItem;
	@FXML
	private MenuItem preferencesItem;

	private final ProjectManager projectManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final BEditorView bEditorView;
	private final Injector injector;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;

	@Inject
	private FileMenu(
		final ProjectManager projectManager,
		final CurrentProject currentProject,
		final CurrentTrace currentTrace,
		final BEditorView bEditorView,
		final Injector injector,
		final StageManager stageManager,
		final FileChooserManager fileChooserManager
	) {
		this.projectManager = projectManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.bEditorView = bEditorView;
		this.injector = injector;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		stageManager.loadFXML(this, "fileMenu.fxml");
	}

	@FXML
	private void initialize() {
		this.projectManager.getRecentProjects().addListener((InvalidationListener)o -> this.recentProjectsMenu.getItems().setAll(this.projectManager.getRecentProjectItems()));
		this.recentProjectsMenu.getItems().setAll(this.projectManager.getRecentProjectItems());

		this.saveMachineItem.disableProperty().bind(bEditorView.pathProperty().isNull().or(bEditorView.savedProperty()));
		this.saveProjectItem.disableProperty().bind(currentProject.isNull());

		this.extendedStaticAnalysisItem.disableProperty().bind(currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B));
		this.viewFormattedCodeItem.disableProperty().bind(currentTrace.isNull());
		this.exportAsMenu.disableProperty().bind(currentTrace.isNull());
		this.saveDocumentationItem.disableProperty().bind(currentProject.isNull());

		currentTrace.stateSpaceProperty().addListener((o, from, to) -> {
			if (to != null) {
				final AbstractModel model = to.getModel();
				// Pretty-printing is not supported for XTL.
				final boolean noClassicalBExport = model instanceof XTLModel;
				// ProB 2's Event-B exporters currently only work with models loaded from a Rodin project, not from an .eventb package.
				final boolean noEventBExport = !(model instanceof EventBModel) || model instanceof EventBPackageModel;
				this.exportAsClassicalBAsciiItem.setDisable(noClassicalBExport);
				this.exportAsClassicalBUnicodeItem.setDisable(noClassicalBExport);
				this.exportAsRodinProject.setDisable(noEventBExport);
				this.exportAsEventBProlog.setDisable(noEventBExport);
			}
		});

		MachineLoader machineLoader = injector.getInstance(MachineLoader.class);
		this.reloadMachineItem.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(machineLoader.loadingProperty()));
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}

	@FXML
	private void handleOpen() throws IOException {
		final Path selected = fileChooserManager.showOpenAnyFileChooser(stageManager.getMainStage());
		if (selected == null) {
			return;
		}

		if (selected.toString().endsWith(".prob2trace")) {
			projectManager.openTrace(selected);
			return;
		}
		if (selected.toString().endsWith(".json")){
			projectManager.openJson(selected);
			return;
		}
		projectManager.openFile(selected);
	}

	@FXML
	private void saveMachine() {
		this.bEditorView.handleSave();
	}

	@FXML
	private void saveProject() {
		projectManager.saveCurrentProject();
	}

	@FXML
	private void handleExtendedStaticAnalysis() {
		final List<ErrorItem> problems = currentTrace.getStateSpace().performExtendedStaticChecks();
		if (problems.isEmpty()) {
			stageManager.makeAlert(Alert.AlertType.INFORMATION, "", "menu.file.items.extendedStaticAnalysis.noProblemsFound").show();
		} else {
			final WarningAlert alert = injector.getInstance(WarningAlert.class);
			alert.getWarnings().setAll(problems);
			alert.show();
		}
	}

	@FXML
	private void handleViewFormattedCode() {
		final ViewCodeStage stage = injector.getInstance(ViewCodeStage.class);
		stage.show();
		stage.toFront();
	}

	@FXML
	private void handleExportClassicalB(final ActionEvent event) {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().addAll(
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.classicalB", "mch"),
			fileChooserManager.getAllExtensionsFilter()
		);
		chooser.setInitialFileName(MoreFiles.getNameWithoutExtension(currentProject.getCurrentMachine().getLocation()) + "_flat.mch");
		final Path path = fileChooserManager.showSaveFileChooser(chooser, FileChooserManager.Kind.NEW_MACHINE, stageManager.getCurrent());
		if (path == null) {
			return;
		}

		final GetInternalRepresentationCommand cmd = new GetInternalRepresentationCommand();
		if (event.getSource() == exportAsClassicalBAsciiItem) {
			cmd.setTranslationMode(FormulaTranslationMode.ASCII);
		} else if (event.getSource() == exportAsClassicalBUnicodeItem) {
			cmd.setTranslationMode(FormulaTranslationMode.UNICODE);
		} else {
			throw new AssertionError();
		}
		cmd.setTypeInfos(GetInternalRepresentationCommand.TypeInfos.NEEDED);

		currentTrace.getStateSpace().execute(cmd);
		final String code = cmd.getPrettyPrint();

		try {
			Files.write(path, Arrays.asList(code.split("\n")));
		} catch (IOException e) {
			LOGGER.error("Failed to save pretty-print", e);
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", path).show();
		}
	}

	@FXML
	private void handleExportRodin() {
		final Path path = fileChooserManager.showDirectoryChooser(new DirectoryChooser(), FileChooserManager.Kind.NEW_MACHINE, stageManager.getCurrent());
		if (path == null) {
			return;
		}

		new ModelToXML().writeToRodin((EventBModel)currentTrace.getStateSpace().getModel(), path.getFileName().toString(), path.getParent().toString());
	}

	@FXML
	private void handleExportEventBProlog() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().addAll(
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.eventBPackage", "eventb"),
			fileChooserManager.getAllExtensionsFilter()
		);
		chooser.setInitialFileName(MoreFiles.getNameWithoutExtension(currentProject.getCurrentMachine().getLocation()) + ".eventb");
		final Path path = fileChooserManager.showSaveFileChooser(chooser, FileChooserManager.Kind.NEW_MACHINE, stageManager.getCurrent());
		if (path == null) {
			return;
		}

		try {
			injector.getInstance(Api.class).eventb_save(currentTrace.getStateSpace(), path.toString());
		} catch (IOException e) {
			LOGGER.error("Failed to save Event-B package", e);
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", path).show();
		}
	}

	@FXML
	private void handleReloadMachine() {
		currentProject.reloadCurrentMachine();
	}

	MenuItem getPreferencesItem() {
		return preferencesItem;
	}

	@FXML
	private void handlePreferences() {
		final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
		preferencesStage.show();
		preferencesStage.toFront();
	}

	@FXML
	private void saveDocumentation() {
		final Stage documentSaveStage = injector.getInstance(SaveDocumentationStage.class);
		documentSaveStage.showAndWait();
		documentSaveStage.toFront();
	}
}
