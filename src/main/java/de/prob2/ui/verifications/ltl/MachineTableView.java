package de.prob2.ui.verifications.ltl;

import javax.inject.Inject;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class MachineTableView extends TableView<Machine> {
	
	@FXML
	private TableColumn<Machine, FontAwesomeIconView> machineStatusColumn;
	
	@FXML
	private TableColumn<Machine, String> machineNameColumn;
	
	@FXML
	private TableColumn<Machine, String> machineDescriptionColumn;
	
	private CurrentProject currentProject;
	
	@Inject
	private MachineTableView(final StageManager stageManager, final CurrentProject currentProject) {
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "machineTableView.fxml");
	}
	
	@FXML
	public void initialize() {
		machineStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		machineNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		machineDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		this.itemsProperty().bind(currentProject.machinesProperty());
	}
	
}
