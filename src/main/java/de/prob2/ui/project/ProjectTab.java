package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Text;

@Singleton
public class ProjectTab extends Tab {

	@FXML
	private Label projectNameLabel;
	@FXML
	private TextField projectNameTextField;
	@FXML
	Text projectDescriptionText;
	@FXML
	TextArea projectDescriptionTextArea;
	@FXML
	private Button applyButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private Label locationLabel;

	private final CurrentProject currentProject;

	@Inject
	private ProjectTab(final StageManager stageManager, final CurrentProject currentProject) {
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "project_tab.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("Project.md.html");
		applyButton.managedProperty().bind(projectDescriptionTextArea.managedProperty());
		applyButton.visibleProperty().bind(projectDescriptionTextArea.visibleProperty());
		projectDescriptionText.visibleProperty().bind(projectDescriptionTextArea.visibleProperty().not());
		projectDescriptionText.managedProperty().bind(projectDescriptionTextArea.managedProperty().not());
		projectNameLabel.visibleProperty().bind(projectNameTextField.visibleProperty().not());
		projectNameLabel.managedProperty().bind(projectNameTextField.managedProperty().not());
		
		locationLabel.textProperty().bind(currentProject.locationProperty().asString());
		locationLabel.getTooltip().textProperty().bind(locationLabel.textProperty());
		
		initName();
		initDescription();
	}

	private void initName() {
		projectNameLabel.textProperty().bind(currentProject.nameProperty());
		projectNameLabel.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				editName();
			}
		});
	}
	
	private void editName() {
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

	private void initDescription() {
		projectDescriptionText.textProperty().bind(currentProject.descriptionProperty());
		projectDescriptionText.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				editDescription();
			}
		});
	}

	private void editDescription() {
		projectDescriptionTextArea.setManaged(true);
		projectDescriptionTextArea.setVisible(true);
		projectDescriptionTextArea.setText(projectDescriptionText.getText());
		projectDescriptionTextArea.requestFocus();
		projectDescriptionTextArea.positionCaret(projectDescriptionTextArea.getText().length());
		applyButton.setOnMouseClicked(mouseEvent -> {
			currentProject.changeDescription(projectDescriptionTextArea.getText());
			projectDescriptionTextArea.setManaged(false);
			projectDescriptionTextArea.setVisible(false);
		});
		projectDescriptionTextArea.focusedProperty().addListener((observable, from, to) -> {
			if (!to) {
				projectDescriptionTextArea.setManaged(false);
				projectDescriptionTextArea.setVisible(false);
			}
		});
	}
}
