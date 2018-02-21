package de.prob2.ui.project.verifications;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;

import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@Singleton
public class MachineTableView extends TableView<Machine> {
	private static final class StatusIconCell extends TableCell<Machine, Machine.CheckingStatus> {
		private StatusIconCell() {
			super();
			
			this.setText(null);
			final FontAwesomeIconView iconView = new FontAwesomeIconView();
			iconView.setVisible(false);
			iconView.getStyleClass().addAll("status-icon", "unknown");
			this.setGraphic(iconView);
		}
		
		@Override
		protected void updateItem(final Machine.CheckingStatus item, final boolean empty) {
			super.updateItem(item, empty);
			
			this.getGraphic().getStyleClass().removeAll("unknown", "successful", "failed");
			if (empty || item == null) {
				this.getGraphic().setVisible(false);
			} else {
				this.getGraphic().setVisible(true);
				final String styleClass;
				switch (item) {
					case UNKNOWN:
						styleClass = "unknown";
						break;
					
					case SUCCESSFUL:
						styleClass = "successful";
						break;
					
					case FAILED:
						styleClass = "failed";
						break;
					
					default:
						throw new IllegalArgumentException("Unknown checking status: " + item);
				}
				this.getGraphic().getStyleClass().add(styleClass);
			}
		}
	}
	
	@FXML private TableColumn<Machine, Machine.CheckingStatus> machineLTLColumn;
	@FXML private TableColumn<Machine, Machine.CheckingStatus> machineSymbolicColumn;
	@FXML private TableColumn<Machine, Machine.CheckingStatus> machineModelcheckColumn;
	@FXML private TableColumn<Machine, String> machineNameColumn;
	
	private CurrentProject currentProject;
	
	@Inject
	private MachineTableView(final StageManager stageManager, final CurrentProject currentProject) {
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "machineTableView.fxml");
	}
	
	@FXML
	public void initialize() {
		machineLTLColumn.setCellFactory(col -> new StatusIconCell());
		machineLTLColumn.setCellValueFactory(features -> features.getValue().ltlStatusProperty());
		machineSymbolicColumn.setCellFactory(col -> new StatusIconCell());
		machineSymbolicColumn.setCellValueFactory(features -> features.getValue().symbolicCheckingStatusProperty());
		machineModelcheckColumn.setCellFactory(col -> new StatusIconCell());
		machineModelcheckColumn.setCellValueFactory(features -> features.getValue().modelcheckingStatusProperty());
		machineNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		this.itemsProperty().bind(currentProject.machinesProperty());
	}
}
