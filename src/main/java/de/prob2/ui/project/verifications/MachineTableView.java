package de.prob2.ui.project.verifications;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import javax.inject.Inject;

import com.google.inject.Singleton;

@Singleton
public class MachineTableView extends TableView<Machine> {
		
	@FXML
	private TableColumn<Machine, FontAwesomeIconView> machineLTLColumn;
	
	@FXML
	private TableColumn<Machine, FontAwesomeIconView> machineCBCColumn;
	
	@FXML
	private TableColumn<Machine, FontAwesomeIconView> machineModelcheckColumn;
	
	@FXML
	private TableColumn<Machine, String> machineNameColumn;
	
	private CurrentProject currentProject;
	
	@Inject
	private MachineTableView(final StageManager stageManager, final CurrentProject currentProject) {
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "machineTableView.fxml");
	}
	
	@FXML
	public void initialize() {
		machineLTLColumn.setCellValueFactory(new PropertyValueFactory<>("LTLStatus"));
		machineCBCColumn.setCellValueFactory(new PropertyValueFactory<>("CBCStatus"));
		machineModelcheckColumn.setCellValueFactory(new PropertyValueFactory<>("ModelcheckStatus"));
		machineNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		this.itemsProperty().bind(currentProject.machinesProperty());
	}
	
	
}
