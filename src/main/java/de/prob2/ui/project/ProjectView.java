package de.prob2.ui.project;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob2.ui.beditor.BEditorStage;
import de.prob2.ui.internal.ProB2Module;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.ProBPreferences;
import de.prob2.ui.prob2fx.CurrentProject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

@Singleton
public final class ProjectView extends AnchorPane {
	@FXML
	private Label projectNameLabel;
	@FXML
	private Text projectDescriptionText;
	@FXML
	private TableView<Machine> machinesTable;
	@FXML
	private TableColumn<Machine, String> nameColumn;
	@FXML
	private TableColumn<Machine, Path> machineColumn;
	@FXML
	private TableColumn<Machine, String> descriptionColumn;
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
	private final Api api;

	@Inject
	private ProjectView(final StageManager stageManager, final CurrentProject currentProject,
			final MachineLoader machineLoader, final Injector injector, final Api api) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		this.injector = injector;
		this.api = api;
		stageManager.loadFXML(this, "project_view.fxml");
	}

	@FXML
	public void initialize() {
		initProjectTab();
		initMachinesTab();
		initPreferencesTab();
		initRunconfigurationsTab();
	}

	private void initProjectTab() {
		projectTabPane.visibleProperty().bind(currentProject.existsProperty());
		newProjectButton.visibleProperty().bind(projectTabPane.visibleProperty().not());

		projectNameLabel.textProperty().bind(currentProject.nameProperty());
		projectDescriptionText.textProperty().bind(currentProject.descriptionProperty());
		this.projectTabPane.widthProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null) {
				projectDescriptionText.setWrappingWidth(0);
				return;
			}
			projectDescriptionText.setWrappingWidth(newValue.doubleValue() - 20);
		});
	}
	
	private void initMachinesTab() {
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		machineColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<Path>(cellData.getValue().getPath()));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		machinesTable.setRowFactory(tableView -> {
			final TableRow<Machine> row = new TableRow<>();

			final MenuItem removeMachineMenuItem = new MenuItem("Remove Machine");
			removeMachineMenuItem.setOnAction(event -> currentProject.removeMachine(row.getItem()));
			removeMachineMenuItem.disableProperty().bind(row.emptyProperty());

			final MenuItem editFileMenuItem = new MenuItem("Edit File");
			editFileMenuItem.setOnAction(event -> this.showEditorStage(row.getItem()));
			editFileMenuItem.disableProperty().bind(row.emptyProperty());

			final MenuItem editExternalMenuItem = new MenuItem("Edit File in External Editor");
			editExternalMenuItem.setOnAction(event -> this.showExternalEditor(row.getItem()));
			editExternalMenuItem.disableProperty().bind(row.emptyProperty());

			row.setContextMenu(new ContextMenu(removeMachineMenuItem, editFileMenuItem, editExternalMenuItem));

			row.setOnMouseClicked(event -> {
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					injector.getInstance(EditMachinesDialog.class).editAndShow(row.getItem())
							.ifPresent(result -> currentProject.updateMachine(row.getItem(), result));
				}
			});

			return row;
		});
		machinesTable.itemsProperty().bind(currentProject.machinesProperty());
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
	void addMachine() {
		injector.getInstance(AddMachinesDialog.class).showAndWait().ifPresent(currentProject::addMachine);
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
		}
	}

	private void showEditorStage(Machine machine) {
		final BEditorStage editorStage = injector.getInstance(BEditorStage.class);
		final Path path = currentProject.getLocation().toPath().resolve(machine.getPath());
		final String text;
		try {
			text = Files.lines(path).collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException | UncheckedIOException e) {
			LOGGER.error("Could not read file " + path, e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Could not read file:\n" + path + "\n" + e).showAndWait();
			return;
		}
		editorStage.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED && !editorStage.getLoaded()) {
				editorStage.setTextEditor(text, path);
			}
		});
		editorStage.setTitle(machine.getFileName());
		editorStage.show();
	}

	private void showExternalEditor(Machine machine) {
		final StateSpace stateSpace = ProBPreferences.getEmptyStateSpace(api);
		final GetPreferenceCommand cmd = new GetPreferenceCommand("EDITOR_GUI");
		stateSpace.execute(cmd);
		final File editor = new File(cmd.getValue());
		final Path machinePath = currentProject.getLocation().toPath().resolve(machine.getPath());
		final String[] cmdline;
		if (ProB2Module.IS_MAC && editor.isDirectory()) {
			// On Mac, use the open tool to start app bundles
			cmdline = new String[] { "/usr/bin/open", "-a", editor.getAbsolutePath(), machinePath.toString() };
		} else {
			// Run normal executables directly
			cmdline = new String[] { editor.getAbsolutePath(), machinePath.toString() };
		}
		final ProcessBuilder processBuilder = new ProcessBuilder(cmdline);
		try {
			processBuilder.start();
		} catch (IOException e) {
			LOGGER.error("Failed to start external editor", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, "Failed to start external editor:\n" + e).showAndWait();
		}
	}
}
