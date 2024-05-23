package de.prob2.ui.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.CommandInterruptedException;
import de.prob.animator.domainobjects.DotVisualizationCommand;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.PlantUmlVisualizationCommand;
import de.prob.animator.domainobjects.TableVisualizationCommand;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.dynamic.dotty.DotFormulaTask;
import de.prob2.ui.dynamic.plantuml.PlantUmlFormulaTask;
import de.prob2.ui.dynamic.table.TableFormulaTask;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.MachineProperties;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DynamicVisualizationStage extends Stage {

	private static final class DynamicCommandItemCell extends ListCell<DynamicCommandItem> {
		private DynamicCommandItemCell() {
			super();
			getStyleClass().add("dynamic-command-cell");
		}

		@Override
		protected void updateItem(final DynamicCommandItem item, final boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll("disabled");
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
			} else {
				setText(item.getName());
				setGraphic(null);
				if (!item.isAvailable()) {
					getStyleClass().add("disabled");
				}
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicVisualizationStage.class);

	// commands
	@FXML
	private ListView<DynamicCommandItem> lvChoice; // TODO: make this a treeview with categories
	@FXML
	private Label lbDescription;

	// formulas
	@FXML
	private Button addButton;
	@FXML
	private Button removeButton;
	@FXML
	private TableView<DynamicFormulaTask> tvFormula;
	@FXML
	private TableColumn<DynamicFormulaTask, Checked> statusColumn;
	@FXML
	private TableColumn<DynamicFormulaTask, String> idColumn;
	@FXML
	private TableColumn<DynamicFormulaTask, String> formulaColumn;
	@FXML
	private VBox enterFormulaBox;
	@FXML
	private ExtendedCodeArea taFormula;

	// visualizations
	@FXML
	private Label placeholderLabel;
	@FXML
	private Parent errorsView;
	@FXML
	private TextArea taErrors;

	// toolbar
	@FXML
	private Button editPreferencesButton;
	@FXML
	private Button cancelButton;
	@FXML
	private HelpButton helpButton;

	// sub-views
	@FXML
	private DynamicTableView tableView;

	private final StageManager stageManager;
	private final I18n i18n;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final Provider<NewEditDynamicFormulaStage> editFormulaStageProvider;
	private final Provider<DynamicPreferencesStage> preferencesStageProvider;
	private final AtomicBoolean needsUpdateAfterBusy;
	private final BackgroundUpdater updater;
	private final ObservableList<ErrorItem> errors = FXCollections.observableArrayList();

	// Used to remember the last selected item even when the list might be cleared temporarily,
	// e.g. when reloading the current machine.
	private DynamicCommandItem lastItem;

	@Inject
	public DynamicVisualizationStage(StageManager stageManager, I18n i18n, CurrentProject currentProject, CurrentTrace currentTrace, Provider<NewEditDynamicFormulaStage> editFormulaStageProvider, Provider<DynamicPreferencesStage> preferencesStageProvider, StopActions stopActions) {
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.editFormulaStageProvider = editFormulaStageProvider;
		this.preferencesStageProvider = preferencesStageProvider;
		this.updater = new BackgroundUpdater("Dynamic Visualization Updater");
		stopActions.add(this.updater::shutdownNow);
		this.needsUpdateAfterBusy = new AtomicBoolean();
		stageManager.loadFXML(this, "dynamic_visualization_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.refresh();

		this.helpButton.setHelpContent("mainmenu.visualisations.graphVisualisation", null);

		this.showingProperty().addListener((observable, from, to) -> {
			this.interrupt();
			if (to) {
				this.refresh();
			}
		});

		this.lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.updatePlaceholderLabel();
			this.tvFormula.itemsProperty().unbind();
			if (to == null || this.currentProject.getCurrentMachine() == null || this.currentTrace.get() == null || !this.isShowing()) {
				this.enterFormulaBox.setVisible(false);
				this.tvFormula.setVisible(false);
				this.tvFormula.setItems(FXCollections.observableArrayList());
				this.addButton.setVisible(false);
				this.removeButton.setVisible(false);
				this.interrupt();
				return;
			}
			if (!to.isAvailable()) {
				this.lbDescription.setText(String.join("\n", to.getDescription(), to.getAvailable()));
			} else {
				this.lbDescription.setText(to.getDescription());
			}

			this.tvFormula.setItems(this.getFormulaTasks(to));

			boolean needFormula = to.getArity() > 0;
			this.enterFormulaBox.setVisible(needFormula);
			this.tvFormula.setVisible(needFormula);
			this.addButton.setVisible(needFormula);
			this.removeButton.setVisible(needFormula);
			// Update the visualization automatically if possible.
			// If the command selection changed and the new command requires a formula,
			// clear the visualization and wait for the user to input one.
			// We cannot blindly execute the last formula, as we do not what it was,
			// and we do not know if it is applicable to this item and model
			if (to.isAvailable() && !needFormula) {
				this.visualize(to, null);
			} else {
				this.interrupt();
			}
			this.lastItem = to;
		});
		this.lvChoice.disableProperty().bind(this.updater.runningProperty().or(this.currentTrace.stateSpaceProperty().isNull()));

		this.currentTrace.addListener((observable, from, to) -> this.refreshLater());
		this.currentTrace.addStatesCalculatedListener(newOps -> this.refreshLater());
		this.currentTrace.animatorBusyProperty().addListener((o, from, to) -> {
			if (!to && this.needsUpdateAfterBusy.compareAndSet(true, false)) {
				Platform.runLater(this::refresh);
			}
		});
		this.currentProject.currentMachineProperty().addListener((o, from, to) -> this.reload());

		this.updater.runningProperty().addListener(o -> this.updatePlaceholderLabel());

		this.taFormula.getStyleClass().add("visualization-formula");
		this.taFormula.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					evaluateFormulaDirect();
					e.consume();
				} else {
					this.taFormula.insertText(this.taFormula.getCaretPosition(), "\n");
				}
			} else if (e.getCode().equals(KeyCode.INSERT)) {
				addFormulaDirect();
				e.consume();
			}
		});

		this.tvFormula.setEditable(true);
		this.statusColumn.setCellFactory(col -> new CheckedCell<>());
		this.statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		this.idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		this.formulaColumn.setCellValueFactory(new PropertyValueFactory<>("formula"));

		this.tvFormula.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> evaluateSelectedFormulaFromTable());

		this.lvChoice.setCellFactory(item -> new DynamicCommandItemCell());
		this.cancelButton.disableProperty().bind(this.updater.runningProperty().not());
		this.editPreferencesButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			final DynamicCommandItem item = this.lvChoice.getSelectionModel().getSelectedItem();
			return item == null || item.getRelevantPreferences().isEmpty();
		}, this.lvChoice.getSelectionModel().selectedItemProperty()));

		this.tvFormula.setRowFactory(param -> {
			final TableRow<DynamicFormulaTask> row = new TableRow<>();

			row.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2) {
					evaluateFormula(row.getItem().getFormula());
				}
			});

			MenuItem editFormula = new MenuItem(i18n.translate("dynamic.editFormula"));
			editFormula.setOnAction(event -> this.editFormulaWithDialog(row.getItem()));

			MenuItem evaluateItem = new MenuItem(i18n.translate("dynamic.evaluateFormula"));
			evaluateItem.setOnAction(event -> this.evaluateFormula(row.getItem().getFormula()));

			MenuItem dischargeItem = new MenuItem(i18n.translate("dynamic.formulaView.discharge"));
			dischargeItem.setOnAction(event -> {
				DynamicFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setChecked(Checked.SUCCESS);
			});

			MenuItem failItem = new MenuItem(this.i18n.translate("dynamic.formulaView.fail"));
			failItem.setOnAction(event -> {
				DynamicFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setChecked(Checked.FAIL);
			});

			MenuItem unknownItem = new MenuItem(this.i18n.translate("dynamic.formulaView.unknown"));
			unknownItem.setOnAction(event -> {
				DynamicFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setChecked(Checked.NOT_CHECKED);
			});

			Menu statusMenu = new Menu(this.i18n.translate("dynamic.setStatus"), null, dischargeItem, failItem, unknownItem);

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(evaluateItem, editFormula, statusMenu)));
			return row;
		});
	}

	@SuppressWarnings("unchecked")
	private ObservableList<DynamicFormulaTask> getFormulaTasks(DynamicCommandItem item) {
		MachineProperties mp = currentProject.getCurrentMachine().getMachineProperties();
		if (item instanceof TableVisualizationCommand) {
			return (ObservableList<DynamicFormulaTask>) (ObservableList<?>) mp.getTableFormulaTasksByCommand(item.getCommand());
		} else if (item instanceof DotVisualizationCommand) {
			return (ObservableList<DynamicFormulaTask>) (ObservableList<?>) mp.getDotFormulaTasksByCommand(item.getCommand());
		} else if (item instanceof PlantUmlVisualizationCommand) {
			return (ObservableList<DynamicFormulaTask>) (ObservableList<?>) mp.getPlantUmlFormulaTasksByCommand(item.getCommand());
		} else {
			throw new AssertionError("unknown dynamic visualization command class: " + item.getClass().getSimpleName());
		}
	}

	private DynamicFormulaTask createTaskOfType(DynamicCommandItem item, String id, String formula) {
		if (item instanceof TableVisualizationCommand) {
			return new TableFormulaTask(id, item.getCommand(), formula);
		} else if (item instanceof DotVisualizationCommand) {
			return new DotFormulaTask(id, item.getCommand(), formula);
		} else if (item instanceof PlantUmlVisualizationCommand) {
			return new PlantUmlFormulaTask(id, item.getCommand(), formula);
		} else {
			throw new AssertionError("unknown dynamic visualization command class: " + item.getClass().getSimpleName());
		}
	}

	private void visualizeInternal(DynamicCommandItem item, List<IEvalElement> formulas) throws InterruptedException {
		Platform.runLater(this::clearContent);
		if (item instanceof TableVisualizationCommand vc) {
			this.tableView.visualize(vc, formulas);
		} else { // TODO: more sub-views
			throw new AssertionError("unknown dynamic visualization command class: " + item.getClass().getSimpleName());
		}
	}

	private List<DynamicCommandItem> getCommandsWithTrace(Trace trace) {
		List<DynamicCommandItem> items = new ArrayList<>();
		items.addAll(TableVisualizationCommand.getAll(trace));
		items.addAll(DotVisualizationCommand.getAll(trace));
		items.addAll(PlantUmlVisualizationCommand.getAll(trace));
		return items;
	}

	private void clearContent() {
		this.taFormula.getErrors().clear();
		this.updatePlaceholderLabel();
		this.placeholderLabel.setVisible(true);
		this.errorsView.setVisible(false);
		this.taErrors.clear();
		this.errors.clear();

		this.tableView.clearContent();
		// TODO: more sub-views
	}

	private void editFormulaWithDialog(DynamicFormulaTask oldTask) {
		if (oldTask == null) {
			return;
		}

		DynamicCommandItem item = this.lvChoice.getSelectionModel().getSelectedItem();
		if (item == null) {
			return;
		}

		final NewEditDynamicFormulaStage stage = this.editFormulaStageProvider.get();
		stage.initOwner(this);
		stage.setInitialFormulaTask(oldTask, this.errors, (id, formula) -> this.createTaskOfType(item, id, formula));
		stage.showAndWait();

		DynamicFormulaTask newTask = stage.getResult();
		if (newTask != null) {
			DynamicFormulaTask added = this.currentProject.getCurrentMachine().getMachineProperties().replaceValidationTaskIfNotExist(oldTask, newTask);
			this.evaluateFormula(added.getFormula());
		}
	}

	@FXML
	private void addFormulaDirect() {
		DynamicCommandItem item = this.lvChoice.getSelectionModel().getSelectedItem();
		if (item == null) {
			return;
		}

		DynamicFormulaTask task = createTaskOfType(item, null, taFormula.getText());
		if (task != null) {
			DynamicFormulaTask added = this.currentProject.getCurrentMachine().getMachineProperties().addValidationTaskIfNotExist(task);
			this.evaluateFormula(added.getFormula());
		}
	}

	private void evaluateSelectedFormulaFromTable() {
		DynamicFormulaTask item = tvFormula.getSelectionModel().getSelectedItem();
		if (item != null) {
			this.evaluateFormula(item.getFormula());
		}
	}

	@FXML
	private void reload() {
		this.interrupt();
		this.refresh();
	}

	@FXML
	private void evaluateFormulaDirect() {
		evaluateFormula(taFormula.getText());
	}

	private void evaluateFormula(String formula) {
		DynamicCommandItem item = lvChoice.getSelectionModel().getSelectedItem();
		if (item != null) {
			this.visualize(item, formula);
		}
	}

	@FXML
	private void editPreferences() {
		final DynamicPreferencesStage preferences = this.preferencesStageProvider.get();
		preferences.initOwner(this);
		preferences.initModality(Modality.WINDOW_MODAL);
		// preferences.setToRefresh(this); // TODO: fix this
		DynamicCommandItem currentItem = lvChoice.getSelectionModel().getSelectedItem();
		if (currentItem == null) {
			return;
		}

		preferences.setIncludedPreferenceNames(currentItem.getRelevantPreferences());
		preferences.setTitle(i18n.translate("dynamic.preferences.stage.title", currentItem.getName()));
		preferences.show();
	}

	@FXML
	private void cancel() {
		this.currentTrace.getStateSpace().sendInterrupt();
		this.interrupt();
	}

	private void updatePlaceholderLabel() {
		final String text;
		if (this.updater.isRunning()) {
			text = this.i18n.translate("dynamic.placeholder.inProgress");
		} else if (this.currentTrace.get() == null) {
			text = this.i18n.translate("common.noModelLoaded");
		} else {
			final DynamicCommandItem selectedItem = this.lvChoice.getSelectionModel().getSelectedItem();
			if (selectedItem == null) {
				text = this.i18n.translate("dynamic.placeholder.selectVisualization");
			} else if (selectedItem.getArity() > 0) {
				text = this.i18n.translate("dynamic.enterFormula.placeholder");
			} else {
				// The placeholder label shouldn't be seen by the user in this case,
				// because the visualization content should be visible,
				// but clear the text anyway just in case.
				text = "";
			}
		}
		this.placeholderLabel.setText(text);
	}

	private void refreshLater() {
		if (this.currentTrace.isAnimatorBusy()) {
			this.needsUpdateAfterBusy.set(true);
		} else {
			Platform.runLater(this::refresh);
		}
	}

	public void refresh() {
		this.updatePlaceholderLabel();
		final State currentState = this.currentTrace.getCurrentState();
		if (currentState == null) {
			this.lvChoice.getItems().clear();
		} else {
			this.lvChoice.getItems().setAll(this.getCommandsWithTrace(this.currentTrace.get()));
		}

		if (this.lastItem != null && this.lvChoice.getItems().contains(this.lastItem)) {
			this.lvChoice.getSelectionModel().select(this.lastItem);
		} else {
			this.lvChoice.getSelectionModel().clearSelection();
		}
	}

	private void interrupt() {
		this.updater.cancel(true);
		this.clearContent();
	}

	private void visualize(final DynamicCommandItem item, String formula) {
		if (!item.isAvailable()) {
			return;
		}
		this.interrupt();

		this.updater.execute(() -> {
			try {
				final Trace trace = this.currentTrace.get();
				if (trace == null || (item.getArity() > 0 && (formula == null || formula.isEmpty()))) {
					return;
				}
				final List<IEvalElement> formulas;
				if (item.getArity() > 0) {
					formulas = Collections.singletonList(trace.getModel().parseFormula(formula, FormulaExpand.EXPAND));
				} else {
					formulas = Collections.emptyList();
				}
				this.visualizeInternal(item, formulas);
			} catch (CommandInterruptedException | InterruptedException e) {
				LOGGER.info("Visualization interrupted", e);
				Thread.currentThread().interrupt();
			} catch (ProBError e) {
				this.handleProBError(e);
			} catch (Exception e) {
				if (e.getCause() instanceof ProBError) {
					this.handleProBError((ProBError) e.getCause());
				} else if (e.getCause() instanceof BCompoundException) {
					this.handleProBError(new ProBError((BCompoundException) e.getCause()));
				} else {
					LOGGER.error("Visualization failed", e);
					Platform.runLater(() -> {
						this.clearContent();
						this.placeholderLabel.setVisible(false);
						this.taErrors.setText(e.getMessage());
						this.errorsView.setVisible(true);
					});
				}
			}
		});
	}

	private void handleProBError(ProBError e) {
		LOGGER.error("Visualization failed with ProBError", e);
		Platform.runLater(() -> {
			this.clearContent();
			this.placeholderLabel.setVisible(false);
			this.errors.setAll(e.getErrors());
			this.taFormula.getErrors().setAll(e.getErrors());
			this.taErrors.setText(e.getMessage());
			this.errorsView.setVisible(true);
		});
	}

	public void selectCommand(String command, String formula) {
		Objects.requireNonNull(command, "command");
		// TODO: are command items uniquely identified by their command or by the tuple (type, command)?
		final DynamicCommandItem choice = this.lvChoice.getItems().stream()
				.filter(item -> item != null && command.equals(item.getCommand()))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("Visualization command not found: " + command));

		if (formula == null || formula.isEmpty()) {
			if (choice.getArity() != 0) {
				throw new IllegalArgumentException("Visualization command needs " + choice.getArity() + " argument(s): " + command);
			}
		} else {
			if (choice.getArity() == 0) {
				throw new IllegalArgumentException("Visualization command does not take an argument: " + command);
			}
		}

		this.lvChoice.getSelectionModel().select(choice);
		this.taFormula.replaceText(formula != null ? formula : "");
		this.visualize(choice, formula);
	}

	public void selectCommand(String command) {
		this.selectCommand(command, null);
	}

	@FXML
	private void handleAddFormula() {
		DynamicCommandItem item = this.lvChoice.getSelectionModel().getSelectedItem();
		if (item == null) {
			return;
		}

		final NewEditDynamicFormulaStage stage = this.editFormulaStageProvider.get();
		stage.initOwner(this);
		stage.createNewFormulaTask((id, formula) -> this.createTaskOfType(item, id, formula));
		stage.showAndWait();

		DynamicFormulaTask task = stage.getResult();
		if (task != null) {
			DynamicFormulaTask added = this.currentProject.getCurrentMachine().getMachineProperties().addValidationTaskIfNotExist(task);
			this.evaluateFormula(added.getFormula());
		}
	}

	@FXML
	private void handleRemoveFormula() {
		DynamicFormulaTask formulaTask = this.tvFormula.getSelectionModel().getSelectedItem();
		if (formulaTask != null) {
			this.currentProject.getCurrentMachine().getMachineProperties().removeValidationTask(formulaTask);
		}
	}
}
