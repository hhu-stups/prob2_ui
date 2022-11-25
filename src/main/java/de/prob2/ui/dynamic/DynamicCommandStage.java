package de.prob2.ui.dynamic;

import java.util.Collections;
import java.util.List;

import com.google.inject.Provider;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.CommandInterruptedException;
import de.prob.animator.domainobjects.*;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DynamicCommandStage<T extends DynamicCommandItem> extends Stage {
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

	private static final class DynamicCommandFormulaItemCell extends TextFieldListCell<DynamicCommandFormulaItem> {

		private DynamicCommandFormulaItem item;

		private DynamicCommandFormulaItemCell() {
			super();
			this.item = null;
			super.setConverter(new StringConverter<DynamicCommandFormulaItem>() {
				@Override
				public String toString(DynamicCommandFormulaItem object) {
					if(object == null) {
						return "";
					}
					return object.getFormula();
				}

				@Override
				public DynamicCommandFormulaItem fromString(String string) {
					if(item == null) {
						return new DynamicCommandFormulaItem("", "", string);
					}
					item.setFormula(string);
					return item;
				}
			});

		}

		@Override
		public void updateItem(DynamicCommandFormulaItem item, boolean empty) {
			super.updateItem(item, empty);
			this.item = item;
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCommandStage.class);
	
	@FXML
	protected ListView<T> lvChoice;

	@FXML
	protected ListView<DynamicCommandFormulaItem> lvFormula;

	@FXML
	protected Button evaluateFormulaButton;

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
	
	private final Provider<DynamicPreferencesStage> preferencesStageProvider;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;
	
	protected final I18n i18n;
	
	protected final BackgroundUpdater updater;
	
	protected DynamicCommandStage(final Provider<DynamicPreferencesStage> preferencesStageProvider,
			final CurrentTrace currentTrace, final CurrentProject currentProject, final I18n i18n, final StopActions stopActions, final String threadName) {
		this.preferencesStageProvider = preferencesStageProvider;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.i18n = i18n;
		this.updater = new BackgroundUpdater(threadName);
		stopActions.add(this.updater::shutdownNow);
	}
	
	
	@FXML
	protected void initialize() {
		this.refresh();

		this.showingProperty().addListener((observable, from, to) -> {
			if(!from && to) {
				T choice = lvChoice.getSelectionModel().getSelectedItem();
				if(choice != null) {
					visualize(choice);
				}
			}
		});

		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			this.updatePlaceholderLabel();
			if (to == null || currentTrace.get() == null || !this.isShowing()) {
				lvFormula.setVisible(false);
				return;
			}
			if (!to.isAvailable()) {
				lbDescription.setText(String.join("\n", to.getDescription(), to.getAvailable()));
			} else {
				lbDescription.setText(to.getDescription());
			}
			boolean needFormula = to.getArity() > 0;
			lvFormula.setVisible(needFormula);
			addButton.setVisible(needFormula);
			removeButton.setVisible(needFormula);
			// Update the visualization automatically if possible.
			// If the command selection changed and the new command requires a formula,
			// clear the visualization and wait for the user to input one.
			if (to.isAvailable() && (!needFormula || to.equals(lastItem))) {
				visualize(to);
			} else {
				this.interrupt();
			}
			lastItem = to;
		});
		lvChoice.disableProperty().bind(this.updater.runningProperty().or(currentTrace.stateSpaceProperty().isNull()));

		currentTrace.addListener((observable, from, to) -> refresh());
		currentTrace.addStatesCalculatedListener(newOps -> Platform.runLater(this::refresh));

		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			this.interrupt();
			lvChoice.getSelectionModel().clearSelection();
			this.lastItem = null;
			this.refresh();
		});
		
		updater.runningProperty().addListener(o -> this.updatePlaceholderLabel());

		lvFormula.setEditable(true);
		lvFormula.setCellFactory(item -> new DynamicCommandFormulaItemCell());
		lvFormula.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> evaluateFormula());


		lvChoice.setCellFactory(item -> new DynamicCommandItemCell<>());
		cancelButton.disableProperty().bind(this.updater.runningProperty().not());
		editPreferencesButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			final T item = lvChoice.getSelectionModel().getSelectedItem();
			return item == null || item.getRelevantPreferences().isEmpty();
		}, lvChoice.getSelectionModel().selectedItemProperty()));
	}

	private void evaluateFormula() {
		T item = lvChoice.getSelectionModel().getSelectedItem();
		if (item == null) {
			return;
		}
		visualize(item);
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
			if(this.lastItem != null) {
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
	}
	
	protected abstract void clearContent();
	
	protected void visualize(final T item) {
		if (!item.isAvailable()) {
			return;
		}
		interrupt();

		this.updater.execute(() -> {
			try {
				final Trace trace = currentTrace.get();
				DynamicCommandFormulaItem formulaItem = lvFormula.getSelectionModel().getSelectedItem();
				if(trace == null || (item.getArity() > 0 && formulaItem != null && formulaItem.getFormula().isEmpty())) {
					return;
				}
				final List<IEvalElement> formulas;
				if (item.getArity() > 0 && formulaItem != null) {
					formulas = Collections.singletonList(trace.getModel().parseFormula(formulaItem.getFormula(), FormulaExpand.EXPAND));
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

	private void handleProBError(ProBError e)  {
		LOGGER.error("Visualization failed with ProBError", e);
		Platform.runLater(() -> {
			taErrors.setText(e.getMessage());
			errorsView.setVisible(true);
			placeholderLabel.setVisible(false);
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
			visualize(choice);
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

	protected abstract void addFormula();

	protected abstract void removeFormula();
}
