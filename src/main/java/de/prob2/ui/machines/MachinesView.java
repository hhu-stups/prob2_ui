package de.prob2.ui.machines;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

@Singleton
public final class MachinesView extends AnchorPane {
	@FXML
	private TableView<Machine> machinesTable;
	@FXML
	private TableColumn<Machine, String> name;
	@FXML
	private TableColumn<Machine, String> machine;
	@FXML
	private TableColumn<Machine, String> description;
	private final CurrentProject currentProject;

	@Inject
	private MachinesView(final StageManager stageManager, final CurrentProject currentProject) {
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "machines_view.fxml");
	}

	@FXML
	public void initialize() {
		name.setCellValueFactory(new PropertyValueFactory<>("name"));
//		machine.setCellValueFactory(new PropertyValueFactory<>("modelName"));
		description.setCellValueFactory(new PropertyValueFactory<>("description"));
		
		machinesTable.itemsProperty().bind(currentProject.machinesProperty());
	}
}
