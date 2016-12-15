package de.prob2.ui.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentStage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

@Singleton
public class NewProjectStage extends Stage {
	private static final Logger logger = LoggerFactory.getLogger(NewProjectStage.class);

	@FXML
	private Button finishButton;
	@FXML
	private TextField projectNameField;
	@FXML
	private TextField projectDescriptionField;
	@FXML
	private TextField locationField;
	@FXML
	private ListView<Preference> preferencesListView;
	@FXML
	private Label errorExplanationLabel;
	@FXML
	private TableView<MachineTableItem> machinesTableView;
	@FXML
	private TableColumn<MachineTableItem, String> nameColumn;
	@FXML
	private TableColumn<MachineTableItem, String> descriptionColumn;

	private CurrentProject currentProject;
	private CurrentStage currentStage;
	private Map<String, Preference> preferencesMap = new HashMap<>();

	private FXMLLoader loader;

	@Inject
	private NewProjectStage(FXMLLoader loader, CurrentProject currentProject, CurrentStage currentStage) {
		this.currentProject = currentProject;
		this.currentStage = currentStage;
		this.loader = loader;
		try {
			loader.setLocation(getClass().getResource("new_project_stage.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
		this.initModality(Modality.WINDOW_MODAL);
		this.initOwner(currentStage.get());
		currentStage.register(this, this.getClass().getName());
	}

	@FXML
	public void initialize() {
		finishButton.disableProperty().bind(projectNameField.lengthProperty().lessThanOrEqualTo(0));
		locationField.setText(System.getProperty("user.home"));

		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		machinesTableView.setRowFactory(tableView -> {
			final TableRow<MachineTableItem> row = new TableRow<>();
			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem removeMenuItem = new MenuItem("Remove Machine");
			removeMenuItem.setOnAction(event -> {
				machinesTableView.getItems().remove(row.getItem());
			});
			contextMenu.getItems().add(removeMenuItem);
			row.setOnMouseClicked(event -> {
				if (event.getButton() == MouseButton.SECONDARY) {
					if (row.isEmpty()) {
						contextMenu.getItems().get(0).setDisable(true);
					} else {
						contextMenu.getItems().get(0).setDisable(false);
					}
					contextMenu.show(row, event.getScreenX(), event.getScreenY());
				}
			});
			return row;
		});
	}

	@FXML
	void addPreference(ActionEvent event) {
		AddProBPreferencesStage addProBPreferencesStage = new AddProBPreferencesStage(loader, currentStage);
		Preference preference = addProBPreferencesStage.showStage();
		if (preference != null) {
			preferencesListView.getItems().add(preference);
			preferencesMap.put(preference.toString(), preference);

			for (MachineTableItem item : machinesTableView.getItems()) {
				item.addPreferenceProperty(preference);
			}

			TableColumn<MachineTableItem, Boolean> preferenceColumn = new TableColumn<>(preference.toString());
			preferenceColumn.setEditable(true);
			preferenceColumn.setCellFactory(
					new Callback<TableColumn<MachineTableItem, Boolean>, TableCell<MachineTableItem, Boolean>>() {
						public TableCell<MachineTableItem, Boolean> call(TableColumn<MachineTableItem, Boolean> p) {
							return new CheckBoxTableCell<>();
						}
					});
			preferenceColumn.setCellValueFactory(cellData -> cellData.getValue().getPreferenceProperty(preference));
			machinesTableView.getColumns().add(preferenceColumn);
			machinesTableView.refresh();
		}
	}

	@FXML
	void addMachine(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Add Machine");
		fileChooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter("Classical B Files", "*.mch", "*.ref", "*.imp"));

		final File selectedFile = fileChooser.showOpenDialog(this);
		if (selectedFile == null) {
			return;
		}

		AddMachineStage addMachineStage = new AddMachineStage(loader, currentStage, selectedFile);
		Machine machine = addMachineStage.showStage();

		if (machine != null) {
			machinesTableView.getItems().add(new MachineTableItem(machine, preferencesListView.getItems()));
		}
	}

	@FXML
	void selectLocation(ActionEvent event) {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Select Location");
		locationField.setText(dirChooser.showDialog(this.getOwner()).getAbsolutePath());
	}

	@FXML
	void cancel(ActionEvent event) {
		this.close();
	}

	@FXML
	void finish(ActionEvent event) {
		File dir = new File(locationField.getText());
		if (!dir.isDirectory()) {
			errorExplanationLabel.setText("The location does not exist or is invalid");
			return;
		}
		List<MachineTableItem> machineItems = machinesTableView.getItems();
		List<Machine> machines = copyMachines(machineItems, dir);
		Map<String, Preference> preferences = preferencesMap;
		Project newProject = new Project(projectNameField.getText(), projectDescriptionField.getText(), machines,
				preferences, dir);
		currentProject.changeCurrentProject(newProject);
		currentProject.save();
		this.close();
	}

	private List<Machine> copyMachines(List<MachineTableItem> machineItems, File dir) {
		String path = dir.getAbsolutePath();
		List<Machine> machines = new ArrayList<>();
		for (MachineTableItem machineItem : machineItems) {
			Machine machine = machineItem.get();
			Path source = machine.getLocation().toPath();
			File newLocation = new File(path + File.separator + machine.getLocation().getName());
			List<String> preferences = getSelectedPreferences(machineItem);
			Machine newMachine = new Machine(machine.getName(), machine.getDescription(), preferences, newLocation);
			try {
				Files.copy(source, newLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
				machines.add(newMachine);
			} catch (IOException e) {
				logger.error(
						"Could not copy file to the selected directory: " + machine.getLocation().getAbsolutePath(), e);
			}
		}
		return machines;
	}

	private List<String> getSelectedPreferences(MachineTableItem machineItem) {
		List<String> prefs = new ArrayList<>();
		for (Preference preference : preferencesListView.getItems()) {
			if(machineItem.getPreferences().get(preference).get()) {
				prefs.add(preference.toString());
			}
		}
		return prefs;
	}
}
