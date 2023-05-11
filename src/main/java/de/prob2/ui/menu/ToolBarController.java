package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Injector;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.persistence.UIState;
import de.prob2.ui.preferences.PreferencesStage;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.NewProjectStage;
import de.prob2.ui.project.ProjectManager;
import java.nio.file.Path;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class ToolBarController {

	private final Injector injector;
	private final StageManager stageManager;
	private final UIState uiState;
	private final I18n i18n;
	@FXML
	private final FontSize fontSize;
	private final FileChooserManager fileChooserManager;
	private final ProjectManager projectManager;
	private final CurrentProject currentProject;

	@FXML
	private Button saveProjectButton;


	@Inject
	public ToolBarController(Injector injector, StageManager stageManager, UIState uiState, I18n i18n, FontSize fontsize, final ProjectManager projectManager, final FileChooserManager fileChooserManager, final CurrentProject currentProject) {
		this.injector = injector;
		this.stageManager = stageManager;
		this.uiState = uiState;
		this.i18n = i18n;
		this.fontSize = fontsize;
		this.fileChooserManager = fileChooserManager;
		this.projectManager = projectManager;
		this.currentProject = currentProject;
	}

	@FXML
	private void initialize(){
		this.saveProjectButton.disableProperty().bind(currentProject.isNull());
	}

	@FXML
	private void saveProject(){
			projectManager.saveCurrentProject();
			}
	@FXML
	private void handlePreferences() {
		final Stage preferencesStage = injector.getInstance(PreferencesStage.class);
		preferencesStage.show();
		preferencesStage.toFront();
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}

	@FXML
	private void zoomIn() {
		fontSize.setFontSize(fontSize.getFontSize() + 1);
	}

	@FXML
	private void zoomOut() {
		fontSize.setFontSize(fontSize.getFontSize() - 1);
	}

	@FXML
	private void handleOpen() {
		final Path selected = fileChooserManager.showOpenProjectOrMachineChooser(stageManager.getMainStage());
		if (selected == null) {
			return;
		}
		projectManager.openFile(selected);
	}
}
