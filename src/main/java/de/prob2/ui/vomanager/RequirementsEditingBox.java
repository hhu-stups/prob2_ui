package de.prob2.ui.vomanager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;

import se.sawano.java.text.AlphanumericComparator;

@FXMLInjected
@Singleton
public class RequirementsEditingBox extends VBox {
	private static final Comparator<? super String> VT_ID_COMPARATOR = new AlphanumericComparator(Locale.ROOT);

	@FXML
	private TextField tfName;

	@FXML
	private TextArea taRequirement;

	@FXML
	private ChoiceBox<RequirementType> cbRequirementChoice;

	@FXML
	private ChoiceBox<String> cbRequirementLinkMachineChoice;

	@FXML
	private Button removeVoButton;

	@FXML
	private TableView<ValidationObligation> voTable;

	@FXML
	private TableColumn<ValidationObligation, Checked> voStatusColumn;

	@FXML
	private TableColumn<ValidationObligation, String> voMachineColumn;

	@FXML
	private TableColumn<ValidationObligation, String> voExpressionColumn;

	@FXML
	private Button applyButton;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private VOManagerStage voManagerStage;

	private final ObservableList<String> linkedMachineNames;

	@Inject
	public RequirementsEditingBox(final StageManager stageManager, final CurrentProject currentProject) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;

		this.linkedMachineNames = FXCollections.observableArrayList();

		stageManager.loadFXML(this, "requirements_editing_box.fxml");
	}

	@FXML
	private void initialize() {
		cbRequirementLinkMachineChoice.setItems(linkedMachineNames);
		applyButton.visibleProperty().bind(cbRequirementChoice.getSelectionModel().selectedItemProperty().isNotNull());

		removeVoButton.disableProperty().bind(voTable.getSelectionModel().selectedIndexProperty().isEqualTo(-1));

		voStatusColumn.setCellFactory(col -> new CheckedCell<>());
		voStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));

		voMachineColumn.setCellFactory(col -> new TableCell<ValidationObligation, String>() {
			@Override
			protected void updateItem(final String item, final boolean empty) {
				super.updateItem(item, empty);

				this.setText(null);
				if (empty) {
					this.setGraphic(null);
				} else {
					final ChoiceBox<String> machineChoiceBox = new ChoiceBox<>(linkedMachineNames);
					machineChoiceBox.getSelectionModel().select(item);
					machineChoiceBox.setOnAction(e -> {
						final String machine = machineChoiceBox.getValue();
						if (machine == null) {
							return;
						}
						final ValidationObligation existingVo = voTable.getItems().get(this.getIndex());
						final ValidationObligation changedVo = new ValidationObligation(machine, existingVo.getExpression());
						voTable.getItems().set(this.getIndex(), changedVo);
					});
					this.setGraphic(machineChoiceBox);
				}
			}
		});
		voMachineColumn.setCellValueFactory(new PropertyValueFactory<>("machine"));

		voExpressionColumn.setCellFactory(col -> {
			final ComboBoxTableCell<ValidationObligation, String> cell = new ComboBoxTableCell<>(new DefaultStringConverter());
			cell.setComboBoxEditable(true);
			Bindings.<ValidationObligation>select(cell.tableRowProperty(), "item").addListener((o, from, to) -> {
				if (to == null) {
					cell.getItems().clear();
				} else {
					final Machine machine = currentProject.get().getMachine(to.getMachine());
					final List<String> vtIds = new ArrayList<>(machine.getValidationTasks().keySet());
					vtIds.sort(VT_ID_COMPARATOR);
					cell.getItems().setAll(vtIds);
				}
			});
			return cell;
		});
		voExpressionColumn.setCellValueFactory(new PropertyValueFactory<>("expression"));
		voExpressionColumn.setOnEditCommit(e -> {
			final int row = e.getTablePosition().getRow();
			final ValidationObligation existingVo = voTable.getItems().get(row);
			final ValidationObligation changedVo = new ValidationObligation(existingVo.getMachine(), e.getNewValue());
			voTable.getItems().set(row, changedVo);
		});
	}

	@FXML
	private void addVo() {
		voTable.getItems().add(new ValidationObligation(linkedMachineNames.get(0), ""));
		voTable.edit(voTable.getItems().size(), voExpressionColumn);
	}

	@FXML
	private void removeVo() {
		voTable.getItems().remove(voTable.getSelectionModel().getSelectedIndex());
	}

	@FXML
	public void replaceRequirement(){
		boolean nameExists = nameExists();
		final Requirement oldRequirement = (Requirement) voManagerStage.getSelectedRequirement();

		//If another requirement has the name we have chosen we should not allow the change
		if(nameExists && !oldRequirement.getName().equals(tfName.getText())) {
			warnAlreadyExists();
			return;
		}
		ArrayList<Requirement> predecessors = new ArrayList<>(oldRequirement.getPreviousVersions());
		predecessors.add(oldRequirement);
		final Requirement newRequirement = new Requirement(tfName.getText(), cbRequirementLinkMachineChoice.getValue(), cbRequirementChoice.getValue(), taRequirement.getText(), new HashSet<>(voTable.getItems()), predecessors, null);
		currentProject.replaceRequirement(oldRequirement, newRequirement);
	}

	@FXML
	public void historyRequirement(){
		Stage table = new RequirementHistoryTable((Requirement) voManagerStage.getSelectedRequirement());
		stageManager.loadFXML(table, "requirement_history_box.fxml", this.getClass().getName());
		table.show();
		table.toFront();
	}

	@FXML
	public void addRequirement() {
		if(!tfName.getText().trim().isEmpty() && !taRequirement.getText().trim().isEmpty()) {
			boolean nameExists = nameExists();
			addRequirement(nameExists);
			voManagerStage.closeEditingBox();
		} else {
			warnNotValid();
		}
	}

	private boolean nameExists(){
		return currentProject.getRequirements().stream()
				.map(Requirement::getName)
				.collect(Collectors.toList())
				.contains(tfName.getText());
	}

	private void addRequirement(boolean nameExists) {
		if(nameExists) {
			warnAlreadyExists();
			return;
		}
		currentProject.addRequirement(new Requirement(tfName.getText(), cbRequirementLinkMachineChoice.getValue(), cbRequirementChoice.getValue(), taRequirement.getText(), new HashSet<>(voTable.getItems())));
	}

	@FXML
	private void refineRequirement(){
		Stage requirementRefineDialog = new RequirementRefineDialog(currentProject, (Requirement) voManagerStage.getSelectedRequirement());
		stageManager.loadFXML(requirementRefineDialog, "requirements_refine_dialog.fxml", this.getClass().getName());
		requirementRefineDialog.show();
		requirementRefineDialog.toFront();
	}

	public void resetRequirementEditing() {
		cbRequirementChoice.getSelectionModel().clearSelection();
		taRequirement.clear();
		tfName.clear();
		voTable.getItems().clear();
		voManagerStage.clearRequirementsSelection();
	}

	public void showRequirement(Requirement requirement, boolean edit) {
		if(edit) {
			voManagerStage.switchMode(VOManagerStage.EditType.MODIFY, VOManagerStage.Mode.REQUIREMENT);
		}
		if(requirement == null) {
			return;
		}
		tfName.setText(requirement.getName());
		cbRequirementLinkMachineChoice.getSelectionModel().select(requirement.getIntroducedAt());
		cbRequirementChoice.getSelectionModel().select(requirement.getType());
		taRequirement.setText(requirement.getText());
		voTable.getItems().setAll(requirement.getValidationObligations());
	}

	private void warnNotValid() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.requirement.notValid.header", "vomanager.warnings.requirement.notValid.content").show();
	}

	private void warnAlreadyExists() {
		stageManager.makeAlert(Alert.AlertType.INFORMATION, "vomanager.warnings.requirement.alreadyExists.header", "vomanager.warnings.requirement.alreadyExists.content").show();
	}

	public void setVoManagerStage(VOManagerStage voManagerStage) {
		this.voManagerStage = voManagerStage;
	}

	public void updateLinkedMachines(List<Machine> machines) {
		this.linkedMachineNames.setAll(machines.stream().map(Machine::getName).collect(Collectors.toList()));
	}

}
