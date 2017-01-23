package de.prob2.ui.project;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
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

public class NewProjectStage extends Stage {
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
	private Map<String, Preference> preferencesMap = new HashMap<>();
	private StageManager stageManager;

	@Inject
	private NewProjectStage(CurrentProject currentProject, StageManager stageManager) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.initModality(Modality.APPLICATION_MODAL);
		stageManager.loadFXML(this, "new_project_stage.fxml");
	}

	@FXML
	public void initialize() {
		finishButton.disableProperty().bind(projectNameField.lengthProperty().lessThanOrEqualTo(0));
		locationField.setText(this.currentProject.getDefaultLocation().toString());

		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		machinesTableView.setRowFactory(tableView -> {
			final TableRow<MachineTableItem> row = new TableRow<>();
			final ContextMenu contextMenu = new ContextMenu();
			final MenuItem removeMenuItem = new MenuItem("Remove Machine");
			removeMenuItem.setOnAction(event -> machinesTableView.getItems().remove(row.getItem()));
			contextMenu.getItems().add(removeMenuItem);
			row.setOnMouseClicked(event -> {
				if (event.getButton() == MouseButton.SECONDARY) {
					if (row.isEmpty()) {
						contextMenu.getItems().get(0).setDisable(true);
					} else {
						contextMenu.getItems().get(0).setDisable(false);
					}
					contextMenu.show(row, event.getScreenX(), event.getScreenY());
				} else if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
					MachineStage machineStage = new MachineStage(stageManager);
					Machine machine = machineStage.editMachine(row.getItem(), machinesTableView.getItems());
					row.getItem().setMachine(machine);
					machinesTableView.refresh();
				}
			});
			return row;
		});
	}

	@FXML
	void addPreference(ActionEvent event) {
		AddProBPreferencesStage addProBPreferencesStage = new AddProBPreferencesStage(stageManager);
		Preference preference = addProBPreferencesStage.showStage(preferencesMap.keySet());
		if (preference != null) {
			preferencesListView.getItems().add(preference);
			preferencesMap.put(preference.toString(), preference);

			for (MachineTableItem item : machinesTableView.getItems()) {
				item.addPreferenceProperty(preference);
			}

			TableColumn<MachineTableItem, Boolean> preferenceColumn = new TableColumn<>(preference.toString());
			preferenceColumn.setEditable(true);
			preferenceColumn.setCellFactory(p -> new CheckBoxTableCell<>());
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

		MachineStage machineStage = new MachineStage(stageManager);
		Machine machine = machineStage.addNewMachine(selectedFile, machinesTableView.getItems());

		if (machine != null) {
			machinesTableView.getItems().add(new MachineTableItem(machine, preferencesListView.getItems()));
		}
	}

	@FXML
	void selectLocation(ActionEvent event) {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle("Select Location");
		File file = dirChooser.showDialog(this.getOwner());		
		if(file != null) {
			locationField.setText(file.getAbsolutePath());
		}
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
		Path projectLoc = dir.toPath();
		List<MachineTableItem> machineItems = machinesTableView.getItems();
		List<Machine> machines = relativPaths(machineItems, projectLoc);
		Map<String, Preference> preferences = preferencesMap;
		Project newProject = new Project(projectNameField.getText(), projectDescriptionField.getText(), machines,
				preferences, dir);
		currentProject.set(newProject);
		currentProject.save();
		this.close();
	}

	private List<Machine> relativPaths(List<MachineTableItem> machineItems, Path projectLoc) {
		List<Machine> machines = new ArrayList<>();
		for (MachineTableItem machineItem : machineItems) {
			Machine machine = machineItem.get();
			Path absolute = machine.getLocation().toPath();
			Path relative = projectLoc.relativize(absolute);
			List<String> preferences = getSelectedPreferences(machineItem);
			Machine newMachine = new Machine(machine.getName(), machine.getDescription(), preferences, new File(relative.toString()));
			machines.add(newMachine);
		}
		return machines;
	}

	private List<String> getSelectedPreferences(MachineTableItem machineItem) {
		List<String> prefs = new ArrayList<>();
		for (Preference preference : preferencesListView.getItems()) {
			if (machineItem.getPreferences().get(preference).get()) {
				prefs.add(preference.toString());
			}
		}
		return prefs;
	}
}
