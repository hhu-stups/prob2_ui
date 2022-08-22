package de.prob2.ui.vomanager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.voparser.VOParseException;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import se.sawano.java.text.AlphanumericComparator;

@FXMLInjected
@Singleton
public class VOEditingBox extends VBox {

	@FXML
	private TextField tfVOName;

	@FXML
	private ComboBox<String> cbVOExpression;

	@FXML
	private ChoiceBox<Requirement> cbLinkRequirementChoice;

	@FXML
	private ChoiceBox<Machine> cbVOLinkMachineChoice;

	@FXML
	private TableView<IValidationTask> vtTable;

	@FXML
	private TableColumn<IValidationTask, Checked> statusColumn;

	@FXML
	private TableColumn<IValidationTask, String> idColumn;

	@FXML
	private TableColumn<IValidationTask, String> configurationColumn;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final VOChecker voChecker;

	private final VOErrorHandler voErrorHandler;

	private final RequirementHandler requirementHandler;

	private final I18n i18n;

	private VOManagerStage voManagerStage;

	@Inject
	public VOEditingBox(final StageManager stageManager, final CurrentProject currentProject, final I18n i18n, final VOChecker voChecker,
						final VOErrorHandler voErrorHandler, final RequirementHandler requirementHandler) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.voChecker = voChecker;
		this.voErrorHandler = voErrorHandler;
		this.requirementHandler = requirementHandler;
		this.i18n = i18n;
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

		statusColumn.setCellFactory(features -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		configurationColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getTaskDescription(this.i18n)));
	}

	public void resetVOEditing() {
		tfVOName.clear();
		cbVOExpression.setValue("");
		cbVOExpression.getItems().clear();
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(currentProject.getRequirements());
		cbVOLinkMachineChoice.getSelectionModel().clearSelection();
		voManagerStage.clearRequirementsSelection();
		vtTable.setItems(FXCollections.emptyObservableList());
	}

	public void showValidationObligation(ValidationObligation validationObligation, boolean edit) {
		if(edit) {
			voManagerStage.switchMode(VOManagerStage.EditType.MODIFY, VOManagerStage.Mode.VO);
		}
		if(validationObligation == null) {
			return;
		}
		tfVOName.setText(validationObligation.getId() == null ? "" : validationObligation.getId());
		cbVOExpression.setValue(validationObligation.getExpression());
		Requirement requirement = currentProject.getRequirements().stream()
				.filter(req -> req.getName().equals(validationObligation.getRequirement()))
				.findAny()
				.orElse(null);
		Machine linkedMachine = currentProject.getMachines().stream()
				.filter(machine -> machine.getValidationObligations().contains(validationObligation))
				.findAny()
				.orElse(null);
		cbVOLinkMachineChoice.getSelectionModel().select(linkedMachine);
		cbLinkRequirementChoice.getItems().clear();
		cbLinkRequirementChoice.getItems().addAll(currentProject.getRequirements());
		cbLinkRequirementChoice.getSelectionModel().select(requirement);
		vtTable.setItems(validationObligation.getTasks());
	}


	private boolean nameExists(String id, ValidationObligation oldVo){
		return id != null && currentProject.getMachines().stream()
				.flatMap(m -> m.getValidationObligations().stream())
				.map(ValidationObligation::getId)
				.anyMatch(id::equals)
				&& !oldVo.getId().equals(id);
	}

	@FXML
	private void addVO() {
		boolean voIsValid = cbLinkRequirementChoice.getValue() != null;
		if(voIsValid) {
			try {
				final ValidationObligation newVo = createNewFromCurrentSelection();
				Machine machine = cbVOLinkMachineChoice.getSelectionModel().getSelectedItem();
				voChecker.parseVO(machine, newVo);
				final ValidationObligation oldVo = (ValidationObligation) voManagerStage.getSelectedRequirement();

				if (oldVo != null && nameExists(currentVOName(), oldVo)) {
					warnAlreadyExists();
					return;
				}

				machine.getValidationObligations().add(newVo);
				requirementHandler.initListenerForVO(cbLinkRequirementChoice.getValue(), newVo);
			} catch (VOParseException e) {
				voErrorHandler.handleError(this.getScene().getWindow(), e);
			}
			voManagerStage.closeEditingBox();
		} else {
			warnNotValid();
		}
	}

	@FXML
	private void replaceVO() {
		boolean voIsValid = cbLinkRequirementChoice.getValue() != null;
		if(voIsValid) {

			try {
				final ValidationObligation oldVo = (ValidationObligation) voManagerStage.getSelectedRequirement();
				List<ValidationObligation> validationObligationList = new ArrayList<>(oldVo.getPreviousVersions());
				validationObligationList.add(oldVo);
				final ValidationObligation newVo = createNewFromCurrentSelection(validationObligationList);
				Machine machine = cbVOLinkMachineChoice.getSelectionModel().getSelectedItem();
				voChecker.parseVO(machine, newVo);

				if (nameExists(currentVOName(), oldVo)) {
					warnAlreadyExists();
					return;
				}

				voManagerStage.replaceCurrentValidationObligation(newVo);
				requirementHandler.initListenerForVO(cbLinkRequirementChoice.getValue(), newVo);
			} catch (VOParseException e) {
				voErrorHandler.handleError(this.getScene().getWindow(), e);
			}
			voManagerStage.closeEditingBox();
		} else {
			warnNotValid();
		}
	}

	private String currentVOName(){
		return tfVOName.getText().trim().isEmpty() ? null : tfVOName.getText();
	}

	private ValidationObligation createNewFromCurrentSelection(){
		return new ValidationObligation(currentVOName(), cbVOExpression.getValue(), cbLinkRequirementChoice.getValue().getName());
	}

	private ValidationObligation createNewFromCurrentSelection(List<ValidationObligation> parents){
		return new ValidationObligation(currentVOName(), cbVOExpression.getValue(), cbLinkRequirementChoice.getValue().getName(), parents);
	}

	@FXML
	private void historyVO() {
		Stage table = new VOHistoryTable<ValidationObligation>((ValidationObligation) voManagerStage.getSelectedRequirement());
		stageManager.loadFXML(table, "vo_history_box.fxml", this.getClass().getName());
		table.show();
		table.toFront();
	}

	@FXML
	private void refineVO(){

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
