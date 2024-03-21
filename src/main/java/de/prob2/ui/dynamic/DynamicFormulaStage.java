package de.prob2.ui.dynamic;

import java.util.Collections;
import java.util.List;

import com.google.inject.Provider;

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

public abstract class DynamicFormulaStage<T extends DynamicCommandItem, F extends DynamicFormulaTask<F>> extends Stage {

	private static final class DynamicCommandItemCell<T extends DynamicCommandItem> extends ListCell<T> {
		private DynamicCommandItemCell() {
			super();
			getStyleClass().add("dynamic-command-cell");
		}

		@Override
		protected void updateItem(final T item, final boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll("disabled");
			if (item != null && !empty) {
				setText(item.getName());
				if (!item.isAvailable()) {
					getStyleClass().add("disabled");
				}
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicFormulaStage.class);

	@FXML
	protected ListView<T> lvChoice;

	@FXML
	protected TableView<F> tvFormula;

	@FXML
	protected TableColumn<F, Checked> statusColumn;
	@FXML
	protected TableColumn<F, String> idColumn;
	@FXML
	protected TableColumn<F, String> formulaColumn;
	@FXML
	protected TextArea taErrors;

	@FXML
	protected Button addButton;

	@FXML
	protected Button removeButton;

	@FXML
	protected Label lbDescription;

	@FXML
	protected Button cancelButton;

	@FXML
	protected Button editPreferencesButton;

	@FXML
	protected Label placeholderLabel;

	@FXML
	protected Parent errorsView;

	// Used to remember the last selected item even when the list might be cleared temporarily,
	// e.g. when reloading the current machine.
	protected T lastItem;

	private boolean needsUpdateAfterBusy;

	private final Provider<DynamicPreferencesStage> preferencesStageProvider;

	protected final StageManager stageManager;

	protected final CurrentTrace currentTrace;

	protected final CurrentProject currentProject;

	protected final I18n i18n;

	protected final BackgroundUpdater updater;

	protected final ObservableList<ErrorItem> errors = FXCollections.observableArrayList();


	@FXML
	protected Button evaluateFormulaButton;

	@FXML
	protected ExtendedCodeArea taFormula;

	@FXML
	protected VBox enterFormulaBox;


	protected DynamicFormulaStage(final Provider<DynamicPreferencesStage> preferencesStageProvider,
	                              final StageManager stageManager, final CurrentTrace currentTrace,
	                              final CurrentProject currentProject, final I18n i18n, final StopActions stopActions,
	                              final String threadName) {
		this.preferencesStageProvider = preferencesStageProvider;
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.updater = new BackgroundUpdater(threadName);
		stopActions.add(this.updater::shutdownNow);

		this.needsUpdateAfterBusy = false;
	}


	@FXML
	protected void initialize() {
		this.refresh();

		/*this.showingProperty().addListener((observable, from, to) -> {
			if(!from && to) {
				T choice = lvChoice.getSelectionModel().getSelectedItem();
				if (choice != null) {
					visualize(choice);
				}
			}
		});*/

		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.updatePlaceholderLabel();
			if (to == null || currentTrace.get() == null || !this.isShowing()) {
				tvFormula.setVisible(false);
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
			if (to.isAvailable() && (!needFormula || to.equals(lastItem))) {
				visualize(to, "");
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
			lvChoice.getSelectionModel().clearSelection();
			this.lastItem = null;
			this.refresh();
		});

		updater.runningProperty().addListener(o -> this.updatePlaceholderLabel());

		taFormula.getStyleClass().add("visualization-formula");
		taFormula.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					evaluateFormulaButton();
					e.consume();
				} else {
					taFormula.insertText(taFormula.getCaretPosition(), "\n");
				}
			} else if (e.getCode().equals(KeyCode.INSERT)) {
				addFormulaButton();
				e.consume();
			}
		});

		tvFormula.setEditable(true);
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		formulaColumn.setCellValueFactory(new PropertyValueFactory<>("formula"));

		tvFormula.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> evaluateFormulaFromTable());

		lvChoice.setCellFactory(item -> new DynamicCommandItemCell<>());
		cancelButton.disableProperty().bind(this.updater.runningProperty().not());
		editPreferencesButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			final T item = lvChoice.getSelectionModel().getSelectedItem();
			return item == null || item.getRelevantPreferences().isEmpty();
		}, lvChoice.getSelectionModel().selectedItemProperty()));

		this.tvFormula.setRowFactory(param -> {
			final TableRow<F> row = new TableRow<>();

			row.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2) {
					evaluateFormula(row.getItem().getFormula());
				}
			});

			MenuItem editFormula = new MenuItem(i18n.translate("dynamic.editFormula"));
			editFormula.setOnAction(event -> editFormula(row));

			MenuItem evaluateItem = new MenuItem(i18n.translate("dynamic.evaluateFormula"));
			evaluateItem.setOnAction(event -> evaluateFormula(row.getItem().getFormula()));

			MenuItem dischargeItem = new MenuItem(i18n.translate("dynamic.formulaView.discharge"));
			dischargeItem.setOnAction(event -> {
				F item = row.getItem();
				if (item == null) {
					return;
				}
				item.setChecked(Checked.SUCCESS);
			});

			MenuItem failItem = new MenuItem(i18n.translate("dynamic.formulaView.fail"));
			failItem.setOnAction(event -> {
				F item = row.getItem();
				if (item == null) {
					return;
				}
				item.setChecked(Checked.FAIL);
			});

			MenuItem unknownItem = new MenuItem(i18n.translate("dynamic.formulaView.unknown"));
			unknownItem.setOnAction(event -> {
				F item = row.getItem();
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

	protected EditDynamicFormulaStage<F> createEditStage() {
		return new EditDynamicFormulaStage<>(stageManager, i18n, currentProject) {
			{
				this.initOwner(DynamicFormulaStage.this);
			}

			@Override
			protected F createNewItem(String id, String command, String formula) {
				return DynamicFormulaStage.this.createNewTask(id, command, formula);
			}
		};
	}

	protected abstract F createNewTask(String id, String command, String formula);

	protected void editFormula(TableRow<F> row) {
		F oldTask = row.getItem();

		final EditDynamicFormulaStage<F> stage = this.createEditStage();
		stage.setInitialTask(oldTask, this.errors);
		stage.showAndWait();

		F newTask = stage.getResult();
		if (newTask != null) {
			F added = this.currentProject.getCurrentMachine().getMachineProperties().replaceValidationTaskIfNotExist(oldTask, newTask);
			this.evaluateFormula(added.getFormula());
		}
	}

	protected void addFormula() {
		final EditDynamicFormulaStage<F> stage = this.createEditStage();
		stage.createNewItem(lastItem.getCommand());
		stage.showAndWait();

		F task = stage.getResult();
		if (task != null) {
			F added = this.currentProject.getCurrentMachine().getMachineProperties().addValidationTaskIfNotExist(task);
			this.evaluateFormula(added.getFormula());
		}
	}

	@FXML
	protected void addFormulaButton() {
		F task = createNewTask(null, lastItem.getCommand(), taFormula.getText());
		if (task != null) {
			F added = this.currentProject.getCurrentMachine().getMachineProperties().addValidationTaskIfNotExist(task);
			this.evaluateFormula(added.getFormula());
		}
	}

	private void evaluateFormulaFromTable() {
		F item = tvFormula.getSelectionModel().getSelectedItem();
		if (item != null) {
			this.evaluateFormula(item.getFormula());
		}
	}

	@FXML
	private void evaluateFormulaButton() {
		evaluateFormula(taFormula.getText());
	}

	protected void evaluateFormula(String formula) {
		T item = lvChoice.getSelectionModel().getSelectedItem();
		if (item != null) {
			this.visualize(item, formula);
		}
	}

	@FXML
	private void editPreferences() {
		final DynamicPreferencesStage preferences = this.preferencesStageProvider.get();
		preferences.initOwner(this);
		preferences.initModality(Modality.WINDOW_MODAL);
		preferences.setToRefresh(this);
		DynamicCommandItem currentItem = lvChoice.getSelectionModel().getSelectedItem();
		preferences.setIncludedPreferenceNames(currentItem.getRelevantPreferences());
		preferences.setTitle(i18n.translate("dynamic.preferences.stage.title", currentItem.getName()));
		preferences.show();
	}

	@FXML
	protected void cancel() {
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
			final T selectedItem = lvChoice.getSelectionModel().getSelectedItem();
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
		int index = lvChoice.getSelectionModel().getSelectedIndex();
		final State currentState = currentTrace.getCurrentState();
		if (currentState == null) {
			lvChoice.getItems().clear();
		} else {
			lvChoice.getItems().setAll(this.getCommandsInState(currentState));
		}
		if (index == -1) {
			if (this.lastItem != null) {
				lvChoice.getSelectionModel().select(this.lastItem);
			}
		} else {
			lvChoice.getSelectionModel().select(index);
		}
	}

	protected void interrupt() {
		this.updater.cancel(true);
		this.taErrors.clear();
		this.errorsView.setVisible(false);
		this.clearContent();
		this.updatePlaceholderLabel();
		taFormula.getErrors().clear();
		this.errors.clear();
	}

	protected abstract void clearContent();

	protected void visualize(final T item, String formula) {
		if (!item.isAvailable()) {
			return;
		}
		interrupt();

		this.updater.execute(() -> {
			try {
				final Trace trace = currentTrace.get();
				if (trace == null || (item.getArity() > 0 && formula.isEmpty())) {
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

	protected abstract void visualizeInternal(final T item, final List<IEvalElement> formulas) throws InterruptedException;

	protected abstract List<T> getCommandsInState(final State state);

	public void selectCommand(final String command, final String formula) {
		final T choice = lvChoice.getItems().stream()
			                 .filter(item -> command.equals(item.getCommand()))
			                 .findAny()
			                 .orElseThrow(() -> new IllegalArgumentException("Visualization command not found: " + command));
		lvChoice.getSelectionModel().select(choice);
		if (formula != null) {
			if (choice.getArity() == 0) {
				throw new IllegalArgumentException("Visualization command does not take an argument: " + command);
			}
			taFormula.replaceText(formula);
			visualize(choice, formula);
		}
	}

	public void selectCommand(final String command) {
		this.selectCommand(command, null);
	}


	@FXML
	private void handleAddFormula() {
		addFormula();
	}

	@FXML
	private void handleRemoveFormula() {
		removeFormula();
	}

	protected void removeFormula() {
		F formulaTask = this.tvFormula.getSelectionModel().getSelectedItem();
		if (formulaTask != null) {
			this.currentProject.getCurrentMachine().getMachineProperties().removeValidationTask(formulaTask);
		}
	}
}
