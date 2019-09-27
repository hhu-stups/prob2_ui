package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.FileMenu;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.MachinesTab;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

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
	private ProjectTab projectTab;
	@FXML
	private MachinesTab machinesTab;

	private final ProjectManager projectManager;
	private final CurrentProject currentProject;
	private final FileMenu fileMenu;

	@Inject
	private ProjectView(final StageManager stageManager, final ProjectManager projectManager, final CurrentProject currentProject, final FileMenu fileMenu) {
		this.projectManager = projectManager;
		this.currentProject = currentProject;
		this.fileMenu = fileMenu;
		stageManager.loadFXML(this, "project_view.fxml");
	}

	@FXML
	public void initialize() {
		projectTabPane.visibleProperty().bind(currentProject.existsProperty());
		newProjectButton.visibleProperty().bind(projectTabPane.visibleProperty().not());
		recentProjectButton.visibleProperty().bind(projectTabPane.visibleProperty().not());
		openProjectButton.visibleProperty().bind(projectTabPane.visibleProperty().not());
		
		this.projectTabPane.widthProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null) {
				projectTab.projectDescriptionText.setWrappingWidth(0);
				return;
			}
			projectTab.projectDescriptionText.setWrappingWidth(newValue.doubleValue() - 20);
		});

		projectManager.getRecentProjects().addListener((InvalidationListener)o ->
			recentProjectButton.getItems().setAll(projectManager.getRecentProjectItems())
		);
		recentProjectButton.getItems().setAll(projectManager.getRecentProjectItems());
	}

	@FXML
	private void createNewProject() {
		fileMenu.createNewProject();
	}
	
	@FXML
	private void openProject() {
		fileMenu.handleOpen();
	}

	public void showMachines() {
		projectTabPane.getSelectionModel().select(machinesTab);
	}	
}
