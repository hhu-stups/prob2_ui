package de.prob2.ui.project.verifications;

import java.util.Locale;

import javax.inject.Inject;

import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
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

@FXMLInjected
@Singleton
public class MachineTableView extends TableView<Machine> {
	private final I18n i18n;

	private static final class StatusIconCell extends TableCell<Machine, Machine.MachineCheckingStatus> {
		private final I18n i18n;

		private StatusIconCell(I18n i18n) {
			super();
			this.i18n = i18n;

			this.setText(null);
			final BindableGlyph iconView = new BindableGlyph("FontAwesome", FontAwesome.Glyph.BAN);
			iconView.setVisible(false);
			iconView.getStyleClass().addAll("status-icon", "none");
			this.setGraphic(iconView);
		}

		@Override
		protected void updateItem(final Machine.MachineCheckingStatus item, final boolean empty) {
			super.updateItem(item, empty);

			final BindableGlyph graphic = (BindableGlyph) this.getGraphic();
			graphic.getStyleClass().removeAll("unknown", "successful", "failed", "none");
			if (empty || item == null) {
				graphic.setVisible(false);
				this.setText("");
			} else {
				graphic.setVisible(true);
				final String styleClass;
				final FontAwesome.Glyph icon;
				final Machine.CheckingStatus status = item.getStatus();
				switch (status) {
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
				this.setText(status == Machine.CheckingStatus.NONE ? "" : i18n.format("({0,number,integer}/{1,number,integer})", item.getNumberSuccess(), item.getNumberTotal()));
			}
		}
	}

	@FXML
	private TableColumn<Machine, Machine.MachineCheckingStatus> machineTraceReplayColumn;
	@FXML
	private TableColumn<Machine, Machine.MachineCheckingStatus> machineLTLColumn;
	@FXML
	private TableColumn<Machine, Machine.MachineCheckingStatus> machineSymbolicColumn;
	@FXML
	private TableColumn<Machine, Machine.MachineCheckingStatus> machineModelcheckColumn;
	@FXML
	private TableColumn<Machine, String> machineNameColumn;

	private CurrentProject currentProject;

	@Inject
	private MachineTableView(final StageManager stageManager, final CurrentProject currentProject, I18n i18n) {
		this.currentProject = currentProject;
		this.i18n = i18n;
		stageManager.loadFXML(this, "machineTableView.fxml");
	}

	@FXML
	public void initialize() {
		machineTraceReplayColumn.setCellFactory(col -> new StatusIconCell(i18n));
		machineTraceReplayColumn.setCellValueFactory(features -> features.getValue().traceReplayStatusProperty());
		machineLTLColumn.setCellFactory(col -> new StatusIconCell(i18n));
		machineLTLColumn.setCellValueFactory(features -> features.getValue().ltlStatusProperty());
		machineSymbolicColumn.setCellFactory(col -> new StatusIconCell(i18n));
		machineSymbolicColumn.setCellValueFactory(features -> features.getValue().symbolicCheckingStatusProperty());
		machineModelcheckColumn.setCellFactory(col -> new StatusIconCell(i18n));
		machineModelcheckColumn.setCellValueFactory(features -> features.getValue().modelcheckingStatusProperty());
		machineNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		this.itemsProperty().bind(currentProject.machinesProperty());
	}
}
