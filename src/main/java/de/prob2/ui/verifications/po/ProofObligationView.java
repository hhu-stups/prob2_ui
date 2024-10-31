package de.prob2.ui.verifications.po;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class ProofObligationView extends BorderPane {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProofObligationView.class);

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final I18n i18n;

	@FXML
	private TableView<ProofObligationItem> tvProofObligations;

	@FXML
	private TableColumn<ProofObligationItem, CheckingStatus> poStatusColumn;
	@FXML
	private TableColumn<ProofObligationItem, String> poIdColumn;
	@FXML
	private TableColumn<ProofObligationItem, String> poColumn;

	@Inject
	private ProofObligationView(StageManager stageManager, CurrentProject currentProject, I18n i18n) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.i18n = i18n;

		stageManager.loadFXML(this, "po_view.fxml");
	}

	@FXML
	public void initialize() {
		poStatusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		poStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		poIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		poColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

		this.tvProofObligations.setRowFactory(param -> {
			final TableRow<ProofObligationItem> row = new TableRow<>();
			MenuItem editItem = new MenuItem(i18n.translate("verifications.po.poView.contextMenu.editId"));
			editItem.setOnAction(event -> this.editItem(row.getItem()));

			row.itemProperty().addListener((observable, from, to) -> {
				if (to != null) {
					Tooltip tooltip = new Tooltip();
					tooltip.textProperty().bind(to.descriptionProperty());
					row.setTooltip(tooltip);
				} else {
					row.setTooltip(null);
				}
			});

			row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(new ContextMenu(editItem)));
			return row;
		});

		ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			if (to != null) {
				this.tvProofObligations.itemsProperty().set(
					to.getAllProofObligations().sorted(Comparator.comparing(ProofObligationItem::getName))
				);
			} else {
				this.tvProofObligations.itemsProperty().set(FXCollections.emptyObservableList());
			}
		};
		this.currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, this.currentProject.getCurrentMachine());
	}

	private void editItem(ProofObligationItem item) {
		Machine machine = this.currentProject.getCurrentMachine();
		TextInputDialog dialog = new TextInputDialog(item.getId() == null ? "" : item.getId());
		stageManager.register(dialog);
		dialog.setTitle(i18n.translate("verifications.po.poView.contextMenu.editId"));
		dialog.setHeaderText(i18n.translate("vomanager.validationTaskId"));
		dialog.getEditor().setPromptText(i18n.translate("common.optionalPlaceholder"));
		dialog.showAndWait().ifPresent(idText -> {
			String id = idText.trim().isEmpty() ? null : idText;
			if (!Objects.equals(id, item.getId())) {
				if (this.currentProject.getCurrentMachine() == machine) {
					Optional<ProofObligationItem> existingSavedPO = machine.getProofObligationTasks().stream()
							.filter(savedPO -> Objects.equals(savedPO.getName(), item.getName()))
							.findAny();
					if (id != null) {
						// we are adding or changing an id
						existingSavedPO.ifPresentOrElse(
								savedPO -> machine.replaceValidationTaskIfNotExist(savedPO, item.withId(id)),
								() -> machine.addValidationTaskIfNotExist(item.withId(id))
						);
					} else {
						// we are removing an id
						existingSavedPO.ifPresent(machine::removeValidationTask);
					}
				} else {
					LOGGER.warn("The machine has changed, discarding task changes");
				}
			}
		});
	}
}
