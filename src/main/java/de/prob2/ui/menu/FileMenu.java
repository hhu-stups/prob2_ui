package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob.animator.domainobjects.ErrorItem;
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
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

@FXMLInjected
public class FileMenu extends Menu {
	@FXML
	private MenuItem preferencesItem;
	@FXML
	private Menu recentProjectsMenu;
	@FXML
	private MenuItem saveMachineItem;
	@FXML
	private MenuItem saveProjectItem;
	@FXML
	private MenuItem extendedStaticAnalysisItem;
	@FXML
	private MenuItem viewFormattedCodeItem;
	@FXML
	private MenuItem reloadMachineItem;

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
	private void handleOpen() {
		final Path selected = fileChooserManager.showOpenProjectOrMachineChooser(stageManager.getMainStage());
		if (selected == null) {
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
}
