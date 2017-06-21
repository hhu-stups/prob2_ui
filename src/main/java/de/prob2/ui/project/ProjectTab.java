package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.ProB2;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Text;

import java.net.URISyntaxException;

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

	private final CurrentProject currentProject;

	@Inject
	private ProjectTab(final StageManager stageManager, final CurrentProject currentProject) {
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "project_tab.fxml");
	}

	@FXML
	public void initialize() throws URISyntaxException {
		helpButton.setPathToHelp(ProB2.class.getClassLoader().getResource("help/HelpMain.html").toURI().toString());
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
}
