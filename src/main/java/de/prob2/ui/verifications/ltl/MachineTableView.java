package de.prob2.ui.verifications.ltl;

import javax.inject.Inject;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class MachineTableView extends TableView<Machine> {
	
	public enum CheckingType {
		LTL, CBC;
	}
	
	@FXML
	private TableColumn<Machine, FontAwesomeIconView> machineStatusColumn;
	
	@FXML
	private TableColumn<Machine, String> machineNameColumn;
	
	@FXML
	private TableColumn<Machine, String> machineDescriptionColumn;
	
	private CurrentProject currentProject;
	
	public SimpleObjectProperty<CheckingType> typeProperty;
	
	@Inject
	private MachineTableView(final StageManager stageManager, final CurrentProject currentProject) {
		this.currentProject = currentProject;
		this.typeProperty = new SimpleObjectProperty<CheckingType>();
		stageManager.loadFXML(this, "machineTableView.fxml");
	}
	
	public void setCheckingType(CheckingType type) {
		typeProperty.set(type);
	}
	
	@FXML
	public void initialize() {
		typeProperty.addListener((observable, from, to) -> {
			if(to == CheckingType.LTL) {
				machineStatusColumn.setCellValueFactory(new PropertyValueFactory<>("ltlStatus"));
			} else {
				machineStatusColumn.setCellValueFactory(new PropertyValueFactory<>("cbcStatus"));
			}
		});
		machineNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		machineDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		this.itemsProperty().bind(currentProject.machinesProperty());
	}
	
}
