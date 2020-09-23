package de.prob2.ui.project;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

@FXMLInjected
@Singleton
public class ProjectTab extends Tab {

	@FXML
	private Label projectNameLabel;
	@FXML
	private TextField projectNameTextField;
	@FXML
	private Text projectDescriptionText;
	@FXML
	private TextArea projectDescriptionTextArea;
	@FXML
	private StackPane projectDescriptionPane;
	@FXML
	private HelpButton helpButton;
	@FXML
	private Label locationLabel;

	private final CurrentProject currentProject;
	
	private final StageManager stageManager;
	
	@Inject
	private ProjectTab(final StageManager stageManager, final CurrentProject currentProject) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "project_tab.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("project", "Project");
		
		final ChangeListener<Number> widthChangeListener = (o, from, to) -> {
			if (to == null) {
				projectDescriptionText.setWrappingWidth(0);
			} else {
				projectDescriptionText.setWrappingWidth(to.doubleValue() - 20);
			}
		};
		
		this.tabPaneProperty().addListener((o, from, to) -> {
			if (from != null) {
				from.widthProperty().removeListener(widthChangeListener);
			}
			
			if (to != null) {
				to.widthProperty().addListener(widthChangeListener);
			}
		});
		
		projectDescriptionText.visibleProperty().bind(projectDescriptionTextArea.visibleProperty().not());
		projectDescriptionText.managedProperty().bind(projectDescriptionTextArea.managedProperty().not());
		projectNameLabel.visibleProperty().bind(projectNameTextField.visibleProperty().not());
		projectNameLabel.managedProperty().bind(projectNameTextField.managedProperty().not());
		
		locationLabel.textProperty().bind(currentProject.locationProperty().asString());
		
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
				String name = projectNameTextField.getText();
				if(name.replace(" ", "").length() == 0) {
					final Alert alert = stageManager.makeAlert(AlertType.WARNING, 
							"project.projectTab.alerts.emptyNameWarning.header",
							"project.projectTab.alerts.emptyNameWarning.content");
					alert.initOwner(this.getContent().getScene().getWindow());
					alert.showAndWait();
					return;
				}
				currentProject.changeName(name);
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
		projectDescriptionPane.setOnMouseClicked(event -> {
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
		projectDescriptionTextArea.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					currentProject.changeDescription(projectDescriptionTextArea.getText());
					projectDescriptionTextArea.setManaged(false);
					projectDescriptionTextArea.setVisible(false);
				} else {
					projectDescriptionTextArea.insertText(projectDescriptionTextArea.getCaretPosition(), "\n");
				}
			}
		});
		projectDescriptionTextArea.focusedProperty().addListener((observable, from, to) -> {
			if (!to) {
				projectDescriptionTextArea.setManaged(false);
				projectDescriptionTextArea.setVisible(false);
			}
		});
	}
}
