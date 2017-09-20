package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.scripting.Api;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.runconfigurations.RunconfigurationsTab;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

@Singleton
public final class ProjectView extends AnchorPane {
	@FXML
	private TabPane projectTabPane;
	@FXML
	private Button newProjectButton;
	@FXML
	private ProjectTab projectTab;
	@FXML
	private RunconfigurationsTab runconfigurationsTab;

	private final CurrentProject currentProject;
	private final Injector injector;

	@Inject
	private ProjectView(final StageManager stageManager, final CurrentProject currentProject, final Injector injector, final Api api) {
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "project_view.fxml");
	}

	@FXML
	public void initialize() {
		projectTabPane.visibleProperty().bind(currentProject.existsProperty());
		newProjectButton.visibleProperty().bind(projectTabPane.visibleProperty().not());
		
		projectTab.projectDescriptionTextArea.maxWidthProperty().bind(this.projectTabPane.widthProperty().subtract(50));
		this.projectTabPane.widthProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null) {
				projectTab.projectDescriptionText.setWrappingWidth(0);
				return;
			}
			projectTab.projectDescriptionText.setWrappingWidth(newValue.doubleValue() - 20);
		});
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}
	
	public void disableRunconfigurationsTable(boolean disable) {
		runconfigurationsTab.disableTable(disable);
	}
	
}
