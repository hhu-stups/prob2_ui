package de.prob2.ui.project;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.scripting.Api;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
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
	private ListView<Preference> preferencesListView;
	@FXML
	private TabPane projectTabPane;
	@FXML
	private Button newProjectButton;
	@FXML
	private Label runconfigsPlaceholder;
	@FXML
	private Button addRunconfigButton;
	@FXML
	private ListView<Runconfiguration> runconfigurationsListView;

	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectView.class);

	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;
	private final StageManager stageManager;
	private final Injector injector;

	@Inject
	private ProjectView(final StageManager stageManager, final CurrentProject currentProject,
			final MachineLoader machineLoader, final Injector injector, final Api api) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		this.injector = injector;
		stageManager.loadFXML(this, "project_view.fxml");
	}

	@FXML
	public void initialize() {
		initProjectTab();
		initPreferencesTab();
		initRunconfigurationsTab();
	}

	private void initProjectTab() {
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

	private void initPreferencesTab() {
		preferencesListView.itemsProperty().bind(currentProject.preferencesProperty());
		preferencesListView.setCellFactory(listView -> {
			ListCell<Preference> cell = new ListCell<Preference>() {
				@Override
				public void updateItem(Preference preference, boolean empty) {
					super.updateItem(preference, empty);
					if (empty) {
						setText(null);
						setGraphic(null);
					} else {
						setText(preference.getName());
						setGraphic(null);
					}
				}
			};

			final MenuItem removePreferenceMenuItem = new MenuItem("Remove Preference");
			removePreferenceMenuItem.setOnAction(event -> currentProject.removePreference(cell.getItem()));
			removePreferenceMenuItem.disableProperty().bind(cell.emptyProperty());

			cell.setContextMenu(new ContextMenu(removePreferenceMenuItem));

			return cell;
		});
	}

	public void initRunconfigurationsTab() {
		runconfigsPlaceholder.setText("Add machines first");
		currentProject.machinesProperty().emptyProperty().addListener((observable, from, to) -> {
			if (to) {
				runconfigsPlaceholder.setText("Add machines first");
				addRunconfigButton.setDisable(true);
			} else {
				runconfigsPlaceholder.setText("No Runconfigurations");
				addRunconfigButton.setDisable(false);
			}
		});
		runconfigurationsListView.itemsProperty().bind(currentProject.runconfigurationsProperty());
		runconfigurationsListView.setCellFactory(listView -> {
			ListCell<Runconfiguration> cell = new ListCell<Runconfiguration>() {
				@Override
				public void updateItem(Runconfiguration runconfiguration, boolean empty) {
					super.updateItem(runconfiguration, empty);
					if (empty) {
						setText(null);
						setGraphic(null);
					} else {
						setText(runconfiguration.toString());
						setGraphic(null);
					}
				}
			};

			final MenuItem removeRunconfigMenuItem = new MenuItem("Remove Runconfiguration");
			removeRunconfigMenuItem.setOnAction(event -> currentProject.removeRunconfiguration(cell.getItem()));
			removeRunconfigMenuItem.disableProperty().bind(cell.emptyProperty());

			cell.setContextMenu(new ContextMenu(removeRunconfigMenuItem));

			return cell;
		});
		runconfigurationsListView.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				startAnimation(runconfigurationsListView.getSelectionModel().getSelectedItem());
			}
		});
	}

	@FXML
	private void createNewProject() {
		final Stage newProjectStage = injector.getInstance(NewProjectStage.class);
		newProjectStage.showAndWait();
		newProjectStage.toFront();
	}

	@FXML
	void addPreference() {
		injector.getInstance(PreferencesDialog.class).showAndWait().ifPresent(currentProject::addPreference);
	}

	@FXML
	void addRunconfiguration() {
		injector.getInstance(RunconfigurationsDialog.class).showAndWait()
				.ifPresent(currentProject::addRunconfiguration);
	}

	private void startAnimation(Runconfiguration runconfiguration) {
		Machine m = currentProject.getMachine(runconfiguration.getMachine());
		Map<String, String> pref = new HashMap<>();
		if (!"default".equals(runconfiguration.getPreference())) {
			pref = currentProject.getPreferencAsMap(runconfiguration.getPreference());
		}
		if (m != null && pref != null) {
			machineLoader.loadAsync(m, pref);
		} else {
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not load machine \"" + runconfiguration.getMachine()
					+ "\" with preferences: \"" + runconfiguration.getPreference() + "\"").showAndWait();
		}
	}
}
