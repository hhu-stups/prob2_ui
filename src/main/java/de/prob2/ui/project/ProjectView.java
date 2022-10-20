package de.prob2.ui.project;

import java.nio.file.Path;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.MachinesTab;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public final class ProjectView extends StackPane {
	@FXML
	private TabPane projectTabPane;
	@FXML
	private Button newProjectButton;
	@FXML
	private Button openProjectButton;
	@FXML
	private MenuButton recentProjectButton;
	@FXML
	private MachinesTab machinesTab;
	@FXML
	private Button documentButton;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ProjectManager projectManager;
	private final Provider<NewProjectStage> newProjectStageProvider;
	private final CurrentProject currentProject;
	private final Provider<SaveDocumentationStage> documentSaveStageProvider;

	@Inject
	private ProjectView(
			final StageManager stageManager,
			final FileChooserManager fileChooserManager,
			final ProjectManager projectManager,
			final Provider<NewProjectStage> newProjectStageProvider,
			final CurrentProject currentProject,
			Provider<SaveDocumentationStage> documentSaveStageProvider
	) {
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.projectManager = projectManager;
		this.newProjectStageProvider = newProjectStageProvider;
		this.currentProject = currentProject;
		this.documentSaveStageProvider = documentSaveStageProvider;
		stageManager.loadFXML(this, "project_view.fxml");
	}

	@FXML
	public void initialize() {
		projectTabPane.visibleProperty().bind(currentProject.isNotNull());
		documentButton.visibleProperty().bind(currentProject.isNotNull());
		newProjectButton.visibleProperty().bind(projectTabPane.visibleProperty().not());
		recentProjectButton.visibleProperty().bind(projectTabPane.visibleProperty().not());
		openProjectButton.visibleProperty().bind(projectTabPane.visibleProperty().not());

		projectManager.getRecentProjects().addListener((InvalidationListener)o ->
				recentProjectButton.getItems().setAll(projectManager.getRecentProjectItems())
		);
		recentProjectButton.getItems().setAll(projectManager.getRecentProjectItems());
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = newProjectStageProvider.get();
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}

	@FXML
	private void openProject() {
		final Path selected = fileChooserManager.showOpenProjectOrMachineChooser(stageManager.getMainStage());
		if (selected == null) {
			return;
		}
		projectManager.openFile(selected);
	}

	public void showMachines() {
		projectTabPane.getSelectionModel().select(machinesTab);
	}


	@FXML
	public void saveDocumentation() {
		final Stage documentSaveStage = documentSaveStageProvider.get();
		documentSaveStage.showAndWait();
		documentSaveStage.toFront();
	}
}
