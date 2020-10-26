package de.prob2.ui.project.verifications;

import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.controlsfx.glyphfont.FontAwesome;

import javax.inject.Inject;

@FXMLInjected
@Singleton
public class MachineTableView extends TableView<Machine> {
	private static final class StatusIconCell extends TableCell<Machine, Machine.CheckingStatus> {
		private StatusIconCell() {
			super();
			
			this.setText(null);
			final BindableGlyph iconView = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
			iconView.setVisible(false);
			iconView.getStyleClass().addAll("status-icon", "unknown");
			this.setGraphic(iconView);
		}
		
		@Override
		protected void updateItem(final Machine.CheckingStatus item, final boolean empty) {
			super.updateItem(item, empty);
			
			final BindableGlyph graphic = (BindableGlyph) this.getGraphic();
			graphic.getStyleClass().removeAll("unknown", "successful", "failed", "none");
			if (empty || item == null) {
				graphic.setVisible(false);
			} else {
				graphic.setVisible(true);
				final String styleClass;
				final FontAwesome.Glyph icon;
				switch (item) {
					case UNKNOWN:
						styleClass = "unknown";
						icon = FontAwesome.Glyph.QUESTION_CIRCLE;
						break;
					
					case SUCCESSFUL:
						styleClass = "successful";
						icon = FontAwesome.Glyph.CHECK;
						break;
					
					case FAILED:
						styleClass = "failed";
						icon = FontAwesome.Glyph.REMOVE;
						break;
					
					case NONE:
						styleClass = "none";
						icon = FontAwesome.Glyph.BAN;
						break;
					
					default:
						throw new IllegalArgumentException("Unknown checking status: " + item);
				}
				graphic.getStyleClass().add(styleClass);
				graphic.setIcon(icon);
			}
		}
	}

	@FXML private TableColumn<Machine, Machine.CheckingStatus> machineTraceReplayColumn;
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
		machineTraceReplayColumn.setCellFactory(col -> new StatusIconCell());
		machineTraceReplayColumn.setCellValueFactory(features -> features.getValue().traceReplayStatusProperty());
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
