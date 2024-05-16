package de.prob2.ui.dynamic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.CommandInterruptedException;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
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
	@FXML
	private Button editPreferencesButton;
	@FXML
	private Button cancelButton;

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

	// Used to remember the last selected item even when the list might be cleared temporarily,
	// e.g. when reloading the current machine.
	private DynamicCommandItem lastItem;

	private boolean needsUpdateAfterBusy;

	private final Provider<DynamicPreferencesStage> preferencesStageProvider;

	private final StageManager stageManager;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final I18n i18n;

	private final BackgroundUpdater updater;

	private final ObservableList<ErrorItem> errors = FXCollections.observableArrayList();

	@Inject
	public DynamicVisualizationStage(final Provider<DynamicPreferencesStage> preferencesStageProvider,
	                                 final StageManager stageManager, final CurrentTrace currentTrace,
	                                 final CurrentProject currentProject, final I18n i18n, final StopActions stopActions) {
		this.preferencesStageProvider = preferencesStageProvider;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.updater = new BackgroundUpdater("Dynamic Visualization Updater");
		stopActions.add(this.updater::shutdownNow);
		this.needsUpdateAfterBusy = false;

		stageManager.loadFXML(this, "dynamic_visualization_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.refresh();

		this.showingProperty().addListener((observable, from, to) -> {
			this.interrupt();
			if (to) {
				this.refresh();
			}
		});

		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.updatePlaceholderLabel();
			if (to == null || currentTrace.get() == null || !this.isShowing()) {
				enterFormulaBox.setVisible(false);
				tvFormula.setVisible(false);
				addButton.setVisible(false);
				removeButton.setVisible(false);
				this.interrupt();
				return;
			}
			if (!to.isAvailable()) {
				lbDescription.setText(String.join("\n", to.getDescription(), to.getAvailable()));
			} else {
				lbDescription.setText(to.getDescription());
			}
			boolean needFormula = to.getArity() > 0;
			enterFormulaBox.setVisible(needFormula);
			tvFormula.setVisible(needFormula);
			addButton.setVisible(needFormula);
			removeButton.setVisible(needFormula);
			// Update the visualization automatically if possible.
			// If the command selection changed and the new command requires a formula,
			// clear the visualization and wait for the user to input one.
			// We cannot blindly execute the last formula, as we do not what it was,
			// and we do not know if it is applicable to this item and model
			if (to.isAvailable() && !needFormula) {
				visualize(to, null);
			} else {
				this.interrupt();
			}
			lastItem = to;
		});
		lvChoice.disableProperty().bind(this.updater.runningProperty().or(currentTrace.stateSpaceProperty().isNull()));

		currentTrace.addListener((observable, from, to) -> {
			if (currentTrace.isAnimatorBusy()) {
				this.needsUpdateAfterBusy = true;
			} else {
				refresh();
			}
		});
		currentTrace.addStatesCalculatedListener(newOps -> {
			if (currentTrace.isAnimatorBusy()) {
				this.needsUpdateAfterBusy = true;
			} else {
				Platform.runLater(this::refresh);
			}
		});
		currentTrace.animatorBusyProperty().addListener((o, from, to) -> {
			if (!to && this.needsUpdateAfterBusy) {
				Platform.runLater(this::refresh);
			}
		});

		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			this.interrupt();
			this.refresh();
		});

		updater.runningProperty().addListener(o -> this.updatePlaceholderLabel());

		taFormula.getStyleClass().add("visualization-formula");
		taFormula.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					evaluateFormulaDirect();
					e.consume();
				} else {
					taFormula.insertText(taFormula.getCaretPosition(), "\n");
				}
			} else if (e.getCode().equals(KeyCode.INSERT)) {
				addFormulaDirect();
				e.consume();
			}
		});

		tvFormula.setEditable(true);
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		formulaColumn.setCellValueFactory(new PropertyValueFactory<>("formula"));

		tvFormula.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> evaluateSelectedFormulaFromTable());

		lvChoice.setCellFactory(item -> new DynamicCommandItemCell());
		cancelButton.disableProperty().bind(this.updater.runningProperty().not());
		editPreferencesButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			final DynamicCommandItem item = lvChoice.getSelectionModel().getSelectedItem();
			return item == null || item.getRelevantPreferences().isEmpty();
		}, lvChoice.getSelectionModel().selectedItemProperty()));

		this.tvFormula.setRowFactory(param -> {
			final TableRow<DynamicFormulaTask> row = new TableRow<>();

			row.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2) {
					evaluateFormula(row.getItem().getFormula());
				}
			});

			MenuItem editFormula = new MenuItem(i18n.translate("dynamic.editFormula"));
			editFormula.setOnAction(event -> editFormulaWithDialog(row.getItem()));

			MenuItem evaluateItem = new MenuItem(i18n.translate("dynamic.evaluateFormula"));
			evaluateItem.setOnAction(event -> evaluateFormula(row.getItem().getFormula()));

			MenuItem dischargeItem = new MenuItem(i18n.translate("dynamic.formulaView.discharge"));
			dischargeItem.setOnAction(event -> {
				DynamicFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setChecked(Checked.SUCCESS);
			});

			MenuItem failItem = new MenuItem(i18n.translate("dynamic.formulaView.fail"));
			failItem.setOnAction(event -> {
				DynamicFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setChecked(Checked.FAIL);
			});

			MenuItem unknownItem = new MenuItem(i18n.translate("dynamic.formulaView.unknown"));
			unknownItem.setOnAction(event -> {
				DynamicFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setChecked(Checked.NOT_CHECKED);
			});

			Menu statusMenu = new Menu(i18n.translate("dynamic.setStatus"), null, dischargeItem, failItem, unknownItem);

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(evaluateItem, editFormula, statusMenu)));
			return row;
		});
	}

	private EditDynamicFormulaStage<DynamicFormulaTask> createEditStage() {
		return new EditDynamicFormulaStage<>(stageManager, i18n, currentProject) {
			{
				this.initOwner(DynamicVisualizationStage.this);
			}

			@Override
			protected DynamicFormulaTask createNewItem(String id, String command, String formula) {
				return DynamicVisualizationStage.this.createNewTask(id, command, formula);
			}
		};
	}

	private DynamicFormulaTask createNewTask(String id, String command, String formula) {
		throw new UnsupportedOperationException(); // TODO
	}

	private void editFormulaWithDialog(DynamicFormulaTask oldTask) {
		if (oldTask == null) {
			return;
		}

		final EditDynamicFormulaStage<DynamicFormulaTask> stage = this.createEditStage();
		stage.setInitialTask(oldTask, this.errors);
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

		DynamicFormulaTask task = createNewTask(null, item.getCommand(), taFormula.getText());
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
		// TODO
		/*final DynamicPreferencesStage preferences = this.preferencesStageProvider.get();
		preferences.initOwner(this);
		preferences.initModality(Modality.WINDOW_MODAL);
		preferences.setToRefresh(this);
		DynamicCommandItem currentItem = lvChoice.getSelectionModel().getSelectedItem();
		if (currentItem == null) {
			return;
		}

		preferences.setIncludedPreferenceNames(currentItem.getRelevantPreferences());
		preferences.setTitle(i18n.translate("dynamic.preferences.stage.title", currentItem.getName()));
		preferences.show();*/
	}

	@FXML
	private void cancel() {
		currentTrace.getStateSpace().sendInterrupt();
		interrupt();
	}

	private void updatePlaceholderLabel() {
		final String text;
		if (this.updater.isRunning()) {
			text = i18n.translate("dynamic.placeholder.inProgress");
		} else if (currentTrace.get() == null) {
			text = i18n.translate("common.noModelLoaded");
		} else {
			final DynamicCommandItem selectedItem = lvChoice.getSelectionModel().getSelectedItem();
			if (selectedItem == null) {
				text = i18n.translate("dynamic.placeholder.selectVisualization");
			} else if (selectedItem.getArity() > 0) {
				text = i18n.translate("dynamic.enterFormula.placeholder");
			} else {
				// The placeholder label shouldn't be seen by the user in this case,
				// because the visualization content should be visible,
				// but clear the text anyway just in case.
				text = "";
			}
		}
		placeholderLabel.setText(text);
	}

	public void refresh() {
		this.updatePlaceholderLabel();
		final State currentState = currentTrace.getCurrentState();
		if (currentState == null) {
			lvChoice.getItems().clear();
		} else {
			lvChoice.getItems().setAll(this.getCommandsWithTrace(currentTrace.get()));
		}

		if (this.lastItem != null && this.lvChoice.getItems().contains(this.lastItem)) {
			this.lvChoice.getSelectionModel().select(this.lastItem);
		} else {
			this.lvChoice.getSelectionModel().clearSelection();
		}
	}

	private void interrupt() {
		this.updater.cancel(true);
		this.taErrors.clear();
		this.errorsView.setVisible(false);
		this.clearContent();
		this.updatePlaceholderLabel();
		taFormula.getErrors().clear();
		this.errors.clear();
	}

	private void clearContent() {
		// TODO
	}

	private void visualize(final DynamicCommandItem item, String formula) {
		if (!item.isAvailable()) {
			return;
		}
		interrupt();

		this.updater.execute(() -> {
			try {
				final Trace trace = currentTrace.get();
				if (trace == null || (item.getArity() > 0 && (formula == null || formula.isEmpty()))) {
					return;
				}
				final List<IEvalElement> formulas;
				if (item.getArity() > 0) {
					formulas = Collections.singletonList(trace.getModel().parseFormula(formula, FormulaExpand.EXPAND));
				} else {
					formulas = Collections.emptyList();
				}
				visualizeInternal(item, formulas);
			} catch (CommandInterruptedException | InterruptedException e) {
				LOGGER.info("Visualization interrupted", e);
				Thread.currentThread().interrupt();
			} catch (ProBError e) {
				handleProBError(e);
			} catch (Exception e) {
				if (e.getCause() instanceof ProBError) {
					handleProBError((ProBError) e.getCause());
				} else if (e.getCause() instanceof BCompoundException) {
					handleProBError(new ProBError((BCompoundException) e.getCause()));
				} else {
					LOGGER.error("Visualization failed", e);
					Platform.runLater(() -> {
						taErrors.setText(e.getMessage());
						errorsView.setVisible(true);
						placeholderLabel.setVisible(false);
					});
				}
			}
		});
	}

	private void handleProBError(ProBError e) {
		LOGGER.error("Visualization failed with ProBError", e);
		Platform.runLater(() -> {
			taErrors.setText(e.getMessage());
			errorsView.setVisible(true);
			placeholderLabel.setVisible(false);
			errors.setAll(e.getErrors());
			taFormula.getErrors().setAll(e.getErrors());
		});
	}

	private void visualizeInternal(final DynamicCommandItem item, final List<IEvalElement> formulas) throws InterruptedException {
		throw new UnsupportedOperationException(); // TODO
	}

	private List<DynamicCommandItem> getCommandsWithTrace(final Trace trace) {
		return List.of(); // TODO
	}

	public void selectCommand(final String command, final String formula) {
		Objects.requireNonNull(command, "command");
		final DynamicCommandItem choice = lvChoice.getItems().stream()
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

		lvChoice.getSelectionModel().select(choice);
		taFormula.replaceText(formula != null ? formula : "");
		visualize(choice, formula);
	}

	public void selectCommand(final String command) {
		this.selectCommand(command, null);
	}

	@FXML
	private void handleAddFormula() {
		DynamicCommandItem item = this.lvChoice.getSelectionModel().getSelectedItem();
		if (item == null) {
			return;
		}

		final EditDynamicFormulaStage<DynamicFormulaTask> stage = this.createEditStage();
		stage.createNewItem(item.getCommand());
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
