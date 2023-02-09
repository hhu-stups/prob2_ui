package de.prob2.ui.vomanager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.voparser.VOException;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import se.sawano.java.text.AlphanumericComparator;

@FXMLInjected
@Singleton
public class VOEditingBox extends VBox {
	@FXML
	private ComboBox<String> cbVOExpression;

	@FXML
	private ChoiceBox<Requirement> cbLinkRequirementChoice;

	@FXML
	private ChoiceBox<Machine> cbVOLinkMachineChoice;

	@FXML
	private Button refineVOButton;

	@FXML
	private TableView<IValidationTask> vtTable;

	@FXML
	private TableColumn<IValidationTask, Checked> statusColumn;

	@FXML
	private TableColumn<IValidationTask, String> idColumn;

	@FXML
	private TableColumn<IValidationTask, String> typeColumn;

	@FXML
	private TableColumn<IValidationTask, String> configurationColumn;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final VOChecker voChecker;

	private final VOErrorHandler voErrorHandler;

	private final I18n i18n;

	private VOManagerStage voManagerStage;

	private final ObjectProperty<ValidationObligation> oldVo;

	@Inject
	public VOEditingBox(final StageManager stageManager, final CurrentProject currentProject, final I18n i18n, final VOChecker voChecker,
						final VOErrorHandler voErrorHandler) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.voChecker = voChecker;
		this.voErrorHandler = voErrorHandler;
		this.i18n = i18n;
		this.oldVo = new SimpleObjectProperty<>(this, "oldVo", null);
		stageManager.loadFXML(this, "vo_editing_box.fxml");
	}

	@FXML
	private void initialize() {
		cbLinkRequirementChoice.setConverter(new StringConverter<Requirement>() {
			@Override
			public String toString(Requirement object) {
				if(object == null) {
					return "";
				}
				return object.getName();
			}

			@Override
			public Requirement fromString(String string) {
				return null;
			}
		});
		// FIXME Moving VOs between requirements like this doesn't work right now
		cbLinkRequirementChoice.disableProperty().bind(this.oldVo.isNotNull());

		final ListProperty<String> vtIds = new SimpleListProperty<>(FXCollections.observableArrayList());
		final Comparator<? super String> idComparator = new AlphanumericComparator(Locale.ROOT);
		final MapChangeListener<String, IValidationTask> vtListener = change -> {
			final List<String> ids = new ArrayList<>(change.getMap().keySet());
			ids.sort(idComparator);
			vtIds.setAll(ids);
		};
		cbVOLinkMachineChoice.valueProperty().addListener((o, from, to) -> {
			if (from != null) {
				from.validationTasksProperty().removeListener(vtListener);
				vtIds.clear();
			}

			if (to != null) {
				to.validationTasksProperty().addListener(vtListener);
				final List<String> ids = new ArrayList<>(to.validationTasksProperty().keySet());
				ids.sort(idComparator);
				vtIds.setAll(ids);
			}
		});
		cbVOExpression.itemsProperty().bind(vtIds);

		refineVOButton.disableProperty().bind(this.oldVo.isNull());

		statusColumn.setCellFactory(features -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		typeColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getTaskType(this.i18n)));
		configurationColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getTaskDescription(this.i18n)));
	}

	public void resetVOEditing() {
		this.oldVo.set(null);
		cbVOExpression.setValue("");
		cbVOExpression.getItems().clear();
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(currentProject.getRequirements());
		cbVOLinkMachineChoice.getSelectionModel().clearSelection();
		voManagerStage.clearRequirementsSelection();
		vtTable.setItems(FXCollections.emptyObservableList());
	}

	public void showValidationObligation(ValidationObligation validationObligation, Requirement requirement) {
		if(validationObligation == null) {
			return;
		}
		this.oldVo.set(validationObligation);
		cbVOExpression.setValue(validationObligation.getExpression());
		Machine linkedMachine = currentProject.get().getMachine(validationObligation.getMachine());
		cbVOLinkMachineChoice.getSelectionModel().select(linkedMachine);
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(currentProject.getRequirements());
		cbLinkRequirementChoice.getSelectionModel().select(requirement);
		vtTable.setItems(validationObligation.getTasks());
	}

	@FXML
	private void applyVO() {
		final Requirement requirement = cbLinkRequirementChoice.getValue();
		if(requirement != null) {
			try {
				final ValidationObligation newVo = createNewFromCurrentSelection();
				Machine machine = cbVOLinkMachineChoice.getSelectionModel().getSelectedItem();
				voChecker.parseVO(machine, newVo);

				if (this.oldVo.get() == null && requirement.getValidationObligation(machine).isPresent()) {
					warnAlreadyExists();
					return;
				}

				final Set<ValidationObligation> updatedVos = new HashSet<>(requirement.getValidationObligations());
				if (this.oldVo.get() != null) {
					updatedVos.remove(this.oldVo.get());
				}
				updatedVos.add(newVo);

				final List<Requirement> predecessors = new ArrayList<>(requirement.getPreviousVersions());
				predecessors.add(requirement);
				final Requirement updatedRequirement = new Requirement(requirement.getName(), requirement.getIntroducedAt(), requirement.getType(), requirement.getText(), updatedVos, predecessors, requirement.getParent());
				currentProject.replaceRequirement(requirement, updatedRequirement);
			} catch (VOException e) {
				voErrorHandler.handleError(this.getScene().getWindow(), e);
			}
			voManagerStage.closeEditingBox();
		} else {
			warnNotValid();
		}
	}

	private ValidationObligation createNewFromCurrentSelection(){
		return new ValidationObligation(cbVOLinkMachineChoice.getValue().getName(), cbVOExpression.getValue());
	}

	@FXML
	private void refineVO(){
		Stage table = new VORefineDialog(currentProject, voManagerStage.getSelectedItem().getVo(), voChecker, cbLinkRequirementChoice, voErrorHandler);
		stageManager.loadFXML(table, "vo_refine_dialog.fxml", this.getClass().getName());
		table.show();
		table.toFront();
	}


	private void warnNotValid() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.vo.notValid.header", "vomanager.warnings.vo.notValid.content").show();
	}

	private void warnAlreadyExists() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.vo.alreadyExists.header", "vomanager.warnings.vo.alreadyExists.content").show();
	}

	public void setVoManagerStage(VOManagerStage voManagerStage) {
		this.voManagerStage = voManagerStage;
	}

	public void updateLinkedMachines(List<Machine> machines) {
		cbVOLinkMachineChoice.getItems().clear();
		cbVOLinkMachineChoice.getItems().addAll(machines);
	}

	public Requirement getLinkedRequirement() {
		return cbLinkRequirementChoice.getValue();
	}

	public Machine getLinkedMachine() {
		return cbVOLinkMachineChoice.getValue();
	}

}
