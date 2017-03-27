package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.scripting.Api;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

@Singleton
public final class ProjectView extends AnchorPane {
	@FXML
	private Label projectNameLabel;
	@FXML
	private TextField projectNameTextField;
	@FXML
	private Text projectDescriptionText;
	@FXML
	private TextArea projectDescriptionTextArea;
	@FXML
	private Button applyButton;
	@FXML
	private TabPane projectTabPane;
	@FXML
	private Button newProjectButton;

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

		projectNameLabel.textProperty().bind(currentProject.nameProperty());
		projectNameLabel.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				projectNameTextField.setManaged(true);
				projectNameTextField.setVisible(true);
				projectNameTextField.setText(projectNameLabel.getText());
				projectNameTextField.requestFocus();
				projectNameTextField.setOnKeyPressed(keyEvent -> {
					if (keyEvent.getCode().equals(KeyCode.ENTER)) {
						currentProject.changeName(projectNameTextField.getText());
						projectNameTextField.setManaged(false);
						projectNameTextField.setVisible(false);
					}
				});
				projectNameTextField.focusedProperty().addListener((observable, from, to) -> {
					if (!to) {
						projectNameTextField.setManaged(false);
						projectNameTextField.setVisible(false);
					}
				});
			}
		});
		projectDescriptionText.textProperty().bind(currentProject.descriptionProperty());
		projectDescriptionText.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				projectDescriptionTextArea.setManaged(true);
				projectDescriptionTextArea.setVisible(true);
				applyButton.setManaged(true);
				applyButton.setVisible(true);
				projectDescriptionTextArea.setText(projectDescriptionText.getText());
				projectDescriptionTextArea.requestFocus();
				projectDescriptionTextArea.positionCaret(projectDescriptionTextArea.getText().length());
				applyButton.setOnMouseClicked(mouseEvent -> {
					currentProject.changeDescription(projectDescriptionTextArea.getText());
					projectNameTextField.setManaged(false);
					projectNameTextField.setVisible(false);
					applyButton.setManaged(false);
					applyButton.setVisible(false);
				});
				projectDescriptionTextArea.focusedProperty().addListener((observable, from, to) -> {
					if (!to) {
						projectDescriptionTextArea.setManaged(false);
						projectDescriptionTextArea.setVisible(false);
					}
				});
			}
		});
		projectDescriptionTextArea.maxWidthProperty().bind(this.widthProperty().subtract(50));
		this.projectTabPane.widthProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null) {
				projectDescriptionText.setWrappingWidth(0);
				return;
			}
			projectDescriptionText.setWrappingWidth(newValue.doubleValue() - 20);
		});
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}
}
