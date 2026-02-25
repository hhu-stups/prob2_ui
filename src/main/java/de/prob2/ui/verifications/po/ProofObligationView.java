package de.prob2.ui.verifications.po;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.model.eventb.Context;
import de.prob.model.eventb.EventBMachine;
import de.prob.model.eventb.EventBModel;
import de.prob.model.eventb.translate.ProofObligationToProlog;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.output.PrologTermOutput;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

@FXMLInjected
@Singleton
public final class ProofObligationView extends BorderPane {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProofObligationView.class);

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final I18n i18n;

	@FXML
	private TableView<ProofObligationItem> tvProofObligations;

	@FXML
	private TableColumn<ProofObligationItem, CheckingStatus> poStatusColumn;
	@FXML
	private TableColumn<ProofObligationItem, String> poIdColumn;
	@FXML
	private TableColumn<ProofObligationItem, String> poColumn;

	@FXML
	private Button btnDisproverAll;

	@Inject
	private ProofObligationView(StageManager stageManager, CurrentProject currentProject, CurrentTrace currentTrace, Injector injector, I18n i18n) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.injector = injector;
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
			final MenuItem showDetailsItem = new MenuItem(i18n.translate("common.contextMenu.showDetails"));
			showDetailsItem.setOnAction(event -> {
				final ProofObligationDetailsStage stage = injector.getInstance(ProofObligationDetailsStage.class);
				stage.setItems(param.getItems(), row.getItem());
				stage.show();
				stage.toFront();
			});

			MenuItem editItem = new MenuItem(i18n.translate("verifications.po.poView.contextMenu.editId"));
			editItem.setOnAction(event -> this.editItem(row.getItem()));

			row.itemProperty().addListener((observable, from, to) -> {
				if (to != null) {
					row.setTooltip(new Tooltip(to.getProofObligation().getDescription()));
				} else {
					row.setTooltip(null);
				}
			});

			row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(new ContextMenu(showDetailsItem, editItem)));
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

		SimpleListProperty<ProofObligationItem> poItems = new SimpleListProperty<>();
		poItems.bind(this.tvProofObligations.itemsProperty());
		this.btnDisproverAll.disableProperty().bind(poItems.emptyProperty());
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

	@FXML
	private void saveAsDisproverAll() throws IOException {
		final FileChooser fileChooser = new FileChooser();
		FileChooserManager fileChooserManager = injector.getInstance(FileChooserManager.class);
		FileChooser.ExtensionFilter poFilter = fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.probpo", "probpo");
		fileChooser.getExtensionFilters().setAll(poFilter);
		fileChooser.setTitle(i18n.translate("common.fileChooser.save.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName());
		Path poFile = fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.PROJECTS_AND_MACHINES, stageManager.getCurrent());
		if (poFile != null && this.currentTrace.getModel() instanceof EventBModel eventBModel) {
			try (final Writer writer = Files.newBufferedWriter(poFile)) {
				IPrologTermOutput pto = new PrologTermOutput(writer, false);
				if (eventBModel.getMainComponent() instanceof EventBMachine mch) {
					ProofObligationToProlog.toDisproverProlog(mch, pto);
				} else if (eventBModel.getMainComponent() instanceof Context ctx) {
					ProofObligationToProlog.toDisproverProlog(ctx, pto);
				}
			}
		}
	}
}
