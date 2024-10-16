package de.prob2.ui.vomanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.voparser.VOParseException;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
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
public final class RequirementsEditingBox extends VBox {
	private static final Comparator<? super String> VT_ID_COMPARATOR = new AlphanumericComparator(Locale.ROOT);

	@FXML
	private TextField tfName;

	@FXML
	private TextArea taRequirement;

	@FXML
	private ChoiceBox<String> cbRequirementLinkMachineChoice;

	@FXML
	private VBox voTableBox;

	@FXML
	private Button removeVoButton;

	@FXML
	private TableView<ValidationObligation> voTable;

	@FXML
	private TableColumn<ValidationObligation, CheckingStatus> voStatusColumn;

	@FXML
	private TableColumn<ValidationObligation, String> voMachineColumn;

	@FXML
	private TableColumn<ValidationObligation, String> voExpressionColumn;

	@FXML
	private Button applyButton;

	@FXML
	private Button historyButton;

	@FXML
	private Button refineButton;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private VOManagerStage voManagerStage;

	private final ObservableList<String> linkedMachineNames;

	private final ObjectProperty<Requirement> oldRequirement;

	@Inject
	public RequirementsEditingBox(final StageManager stageManager, final CurrentProject currentProject) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;

		this.linkedMachineNames = FXCollections.observableArrayList();
		this.oldRequirement = new SimpleObjectProperty<>(this, "oldRequirement", null);

		stageManager.loadFXML(this, "requirements_editing_box.fxml");
	}

	@FXML
	private void initialize() {
		ChangeListener<String> nameListener = (o, from, to) -> {
			tfName.getStyleClass().remove("text-field-error");
			if (to == null || to.isEmpty()) {
				tfName.getStyleClass().add("text-field-error");
			}
		};
		tfName.textProperty().addListener(nameListener);
		nameListener.changed(tfName.textProperty(), null, tfName.getText());

		cbRequirementLinkMachineChoice.setItems(linkedMachineNames);
		ChangeListener<String> linkMachineListener = (o, from, to) -> {
			cbRequirementLinkMachineChoice.getStyleClass().remove("text-field-error");
			if (to == null) {
				cbRequirementLinkMachineChoice.getStyleClass().add("text-field-error");
			}
		};
		cbRequirementLinkMachineChoice.valueProperty().addListener(linkMachineListener);
		nameListener.changed(cbRequirementLinkMachineChoice.valueProperty(), null, cbRequirementLinkMachineChoice.getValue());

		// When creating a new requirement,
		// VOs can only be added after the requirement has been saved.
		voTableBox.visibleProperty().bind(oldRequirement.isNotNull());
		removeVoButton.disableProperty().bind(voTable.getSelectionModel().selectedIndexProperty().isEqualTo(-1));

		voStatusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		voStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

		voMachineColumn.setCellFactory(col -> new TableCell<>() {
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
						tryParseVo(changedVo);
						updateRequirementVos();
					});
					this.setGraphic(machineChoiceBox);
				}
			}
		});
		voMachineColumn.setCellValueFactory(new PropertyValueFactory<>("machine"));

		voExpressionColumn.setCellFactory(col -> {
			final ComboBoxTableCell<ValidationObligation, String> cell = new ComboBoxTableCell<>(new DefaultStringConverter());
			cell.setComboBoxEditable(true);
			final ObjectExpression<ValidationObligation> rowItem = Bindings.select(cell.tableRowProperty(), "item");
			// Prevent the binding object from getting garbage-collected
			// (which also removes the listener)
			// by storing it on the cell.
			cell.getProperties().put(col, rowItem);
			rowItem.addListener((o, from, to) -> {
				if (to == null) {
					cell.getItems().clear();
				} else {
					final Machine machine = currentProject.get().getMachine(to.getMachine());
					if(machine == null){
						return;
					}
					final List<String> vtIds = new ArrayList<>(machine.getValidationTaskIds());
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
			tryParseVo(changedVo);
			updateRequirementVos();
		});

		applyButton.disableProperty().bind(tfName.textProperty().isNull()
			.or(Bindings.isEmpty(tfName.textProperty()))
			.or(cbRequirementLinkMachineChoice.valueProperty().isNull()));
		historyButton.disableProperty().bind(oldRequirement.isNull());
		refineButton.disableProperty().bind(oldRequirement.isNull());
	}

	private void tryParseVo(final ValidationObligation vo) {
		final Machine machine = currentProject.get().getMachine(vo.getMachine());
		try {
			vo.parse(machine);
		} catch (VOParseException exc) {
			Alert alert = stageManager.makeExceptionAlert(exc, "vomanager.error.parsing");
			alert.initOwner(this.getScene().getWindow());
			alert.show();
		}
	}

	private void updateRequirementVos() {
		final Requirement oldReq = oldRequirement.get();
		assert oldReq != null;
		final List<Requirement> predecessors = new ArrayList<>(oldReq.getPreviousVersions());
		predecessors.add(oldReq);
		Requirement newRequirement = new Requirement(oldReq.getName(), oldReq.getIntroducedAt(), oldReq.getText(), new HashSet<>(voTable.getItems()), predecessors, null);
		currentProject.replaceRequirement(oldReq, newRequirement);
		oldRequirement.set(newRequirement);
	}

	@FXML
	private void addVo() {
		assert !linkedMachineNames.isEmpty();

		final ValidationObligation newVo;
		if (voTable.getItems().isEmpty()) {
			// No VOs exist yet - nothing can be copied.
			// Start with the first machine and an empty expression.
			newVo = new ValidationObligation(linkedMachineNames.get(0), "");
		} else {
			// At least one VO exists - copy the last one and auto-select the next machine.
			final ValidationObligation existingVo = voTable.getItems().get(voTable.getItems().size() - 1);
			assert !linkedMachineNames.isEmpty();
			final int lastMachineIndex = linkedMachineNames.indexOf(existingVo.getMachine());
			assert lastMachineIndex != -1;

			final String nextMachine;
			if (lastMachineIndex == linkedMachineNames.size() - 1) {
				// We're already at the last machine in the list.
				// Try to find any other machine that doesn't have a VO yet,
				// otherwise default to the first machine again.
				final List<String> machinesWithoutVos = new ArrayList<>(linkedMachineNames);
				for (final ValidationObligation vo : voTable.getItems()) {
					machinesWithoutVos.remove(vo.getMachine());
				}
				if (machinesWithoutVos.isEmpty()) {
					nextMachine = linkedMachineNames.get(0);
				} else {
					nextMachine = machinesWithoutVos.get(0);
				}
			} else {
				// Select the next machine in the list.
				nextMachine = linkedMachineNames.get(lastMachineIndex + 1);
			}

			// Copy the validation expression from the last VO.
			newVo = new ValidationObligation(nextMachine, existingVo.getExpression());
		}

		voTable.getItems().add(newVo);
		updateRequirementVos();
		voTable.edit(voTable.getItems().size()-1, voExpressionColumn);
	}

	@FXML
	private void removeVo() {
		voTable.getItems().remove(voTable.getSelectionModel().getSelectedIndex());
		updateRequirementVos();
	}

	@FXML
	private void applyRequirement(){
		boolean nameExists = nameExists();

		//If another requirement has the name we have chosen we should not allow the change
		if (nameExists && (oldRequirement.get() == null || !oldRequirement.get().getName().equals(tfName.getText()))) {
			warnAlreadyExists();
			return;
		}

		final List<Requirement> predecessors;
		if (oldRequirement.get() == null) {
			predecessors = Collections.emptyList();
		} else {
			predecessors = new ArrayList<>(oldRequirement.get().getPreviousVersions());
			predecessors.add(oldRequirement.get());
		}
		Requirement newRequirement = new Requirement(tfName.getText(), cbRequirementLinkMachineChoice.getValue(), taRequirement.getText(), new HashSet<>(voTable.getItems()), predecessors, null);

		if (oldRequirement.get() == null) {
			currentProject.addRequirement(newRequirement);
		} else {
			currentProject.replaceRequirement(oldRequirement.get(), newRequirement);
		}
		voManagerStage.closeEditingBox();
	}

	@FXML
	public void historyRequirement(){
		Stage table = new RequirementHistoryTable(voManagerStage.getSelectedItem().getRequirement());
		stageManager.loadFXML(table, "requirement_history_box.fxml", this.getClass().getName());
		table.show();
		table.toFront();
	}

	private boolean nameExists(){
		return currentProject.getRequirements().stream()
				.map(Requirement::getName)
				.collect(Collectors.toSet())
				.contains(tfName.getText());
	}

	@FXML
	private void refineRequirement(){
		Stage requirementRefineDialog = new RequirementRefineDialog(currentProject, voManagerStage.getSelectedItem().getRequirement());
		stageManager.loadFXML(requirementRefineDialog, "requirements_refine_dialog.fxml", this.getClass().getName());
		requirementRefineDialog.show();
		requirementRefineDialog.toFront();
	}

	public void resetRequirementEditing() {
		this.oldRequirement.set(null);
		taRequirement.clear();
		tfName.clear();
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			cbRequirementLinkMachineChoice.getSelectionModel().select(currentMachine.getName());
		} else {
			cbRequirementLinkMachineChoice.getSelectionModel().selectFirst();
		}
		voTable.getItems().clear();
		voManagerStage.clearRequirementsSelection();
	}

	public void showRequirement(Requirement requirement) {
		if(requirement == null) {
			return;
		}
		this.oldRequirement.set(requirement);
		tfName.setText(requirement.getName());
		cbRequirementLinkMachineChoice.getSelectionModel().select(requirement.getIntroducedAt());
		taRequirement.setText(requirement.getText());
		voTable.getItems().setAll(requirement.getValidationObligations());
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
