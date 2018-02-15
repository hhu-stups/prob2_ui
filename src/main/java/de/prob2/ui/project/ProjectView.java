package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.FileMenu;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.MachinesTab;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;

@Singleton
public final class ProjectView extends AnchorPane {
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

	private final CurrentProject currentProject;
	private final FileMenu fileMenu;

	@Inject
	private ProjectView(final StageManager stageManager, final CurrentProject currentProject, final FileMenu fileMenu) {
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
		
		projectTab.projectDescriptionTextArea.maxWidthProperty().bind(this.projectTabPane.widthProperty().subtract(50));
		this.projectTabPane.widthProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null) {
				projectTab.projectDescriptionText.setWrappingWidth(0);
				return;
			}
			projectTab.projectDescriptionText.setWrappingWidth(newValue.doubleValue() - 20);
		});

		recentProjectButton.getItems().setAll(fileMenu.getRecentProjectsMenu().getItems());
		fileMenu.getRecentProjectsMenu().getItems().addListener((ListChangeListener<MenuItem>)(c -> {
			recentProjectButton.getItems().clear();
			recentProjectButton.getItems().setAll(fileMenu.getRecentProjectsMenu().getItems());
		})); 
		
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
