package de.prob2.ui.verifications.po;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.model.eventb.Context;
import de.prob.model.eventb.EventBMachine;
import de.prob.model.eventb.EventBModel;
import de.prob.model.eventb.ProofObligation;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

@FXMLInjected
@Singleton
public class ProofObligationView extends AnchorPane {

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final I18n i18n;

	private final MapProperty<String, ProofObligationItem> modelProofObligations;
	private final ListProperty<ProofObligationItem> savedProofObligations;
	private final ObservableList<ProofObligationItem> allProofObligations;

	@FXML
	private TableView<ProofObligationItem> tvProofObligations;

	@FXML
	private TableColumn<ProofObligationItem, Checked> poStatusColumn;
	@FXML
	private TableColumn<ProofObligationItem, String> poIdColumn;
	@FXML
	private TableColumn<ProofObligationItem, String> poColumn;

	@Inject
	private ProofObligationView(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final I18n i18n) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.i18n = i18n;
		this.modelProofObligations = new SimpleMapProperty<>(FXCollections.emptyObservableMap());
		this.savedProofObligations = new SimpleListProperty<>(FXCollections.emptyObservableList());
		this.allProofObligations = FXCollections.observableArrayList();
		stageManager.loadFXML(this, "po_view.fxml");
	}

	@FXML
	public void initialize() {
		poStatusColumn.setCellFactory(col -> new CheckedCell<>());
		poStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		poIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		poColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

		this.tvProofObligations.setRowFactory(param -> {
			final TableRow<ProofObligationItem> row = new TableRow<>();
			MenuItem editItem = new MenuItem(i18n.translate("verifications.po.poView.contextMenu.editId"));
			editItem.setOnAction(event -> this.editItem(row.getItem()));

			row.itemProperty().addListener((observable, from, to) -> {
				if (to != null) {
					row.setTooltip(new Tooltip(to.getDescription()));
				} else {
					row.setTooltip(null);
				}
			});

			row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(new ContextMenu(editItem)));
			return row;
		});
		this.tvProofObligations.setItems(this.allProofObligations.sorted(Comparator.comparing(ProofObligationItem::getName)));

		InvalidationListener invalidationListener = observable -> this.allProofObligations.setAll(combinePOs());
		this.modelProofObligations.addListener(invalidationListener);
		this.savedProofObligations.addListener(invalidationListener);

		ChangeListener<AbstractModel> modelChangeListener = (observable, from, to) -> this.modelProofObligations.set(extractModelPOs(to));
		this.currentTrace.modelProperty().addListener(modelChangeListener);
		modelChangeListener.changed(null, null, this.currentTrace.getModel());

		ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			if (to != null) {
				this.savedProofObligations.set(to.getProofObligationTasks());
			} else {
				this.savedProofObligations.set(FXCollections.emptyObservableList());
			}
		};
		this.currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, this.currentProject.getCurrentMachine());
	}

	private ObservableMap<String, ProofObligationItem> extractModelPOs(AbstractModel model) {
		// TODO: Does not yet work with .eventb files
		if (!(model instanceof EventBModel eventBModel)) {
			return FXCollections.emptyObservableMap();
		}

		List<? extends ProofObligation> pos;
		if (eventBModel.getMainComponent() instanceof EventBMachine machine) {
			pos = machine.getProofs();
		} else if (eventBModel.getMainComponent() instanceof Context context) {
			pos = context.getProofs();
		} else {
			return FXCollections.emptyObservableMap();
		}

		ObservableMap<String, ProofObligationItem> poItems = FXCollections.observableHashMap();
		pos.stream().map(ProofObligationItem::new).forEach(po -> poItems.put(po.getName(), po));
		return poItems;
	}

	private ObservableList<ProofObligationItem> combinePOs() {
		ObservableList<ProofObligationItem> visiblePOs = FXCollections.observableArrayList();
		HashMap<String, ProofObligationItem> modelProofObligationsCopy = new HashMap<>(this.modelProofObligations);
		for (ProofObligationItem savedPO : this.savedProofObligations) {
			ProofObligationItem modelPO = modelProofObligationsCopy.remove(savedPO.getName());
			if (modelPO != null) {
				if (modelPO.getId() != null) {
					throw new IllegalStateException("model POs should have no id");
				}

				visiblePOs.add(modelPO.withId(savedPO.getId()));
			} else {
				// don't silently remove saved POs that have no matching PO in the model
				visiblePOs.add(savedPO);
			}
		}
		visiblePOs.addAll(modelProofObligationsCopy.values());
		return visiblePOs;
	}

	private void editItem(ProofObligationItem item) {
		Optional<ProofObligationItem> existingSavedPO = this.savedProofObligations.stream()
			                                                .filter(savedPO -> Objects.equals(savedPO.getName(), item.getName()))
			                                                .findAny();

		TextInputDialog dialog = new TextInputDialog(item.getId() == null ? "" : item.getId());
		stageManager.register(dialog);
		dialog.setTitle(i18n.translate("verifications.po.poView.contextMenu.editId"));
		dialog.setHeaderText(i18n.translate("vomanager.validationTaskId"));
		dialog.getEditor().setPromptText(i18n.translate("common.optionalPlaceholder"));
		dialog.showAndWait().ifPresent(idText -> {
			String id = idText.trim().isEmpty() ? null : idText;
			if (!Objects.equals(id, item.getId())) {
				Machine machine = currentProject.getCurrentMachine();
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
			}
		});
	}
}
