package de.prob2.ui.sharedviews;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.SafeBindings;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.ISelectableTask;
import de.prob2.ui.verifications.IValidationTask;
import de.prob2.ui.verifications.ItemSelectedFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public abstract class CheckingViewBase<T extends ISelectableTask> extends ScrollPane {
	protected class RowBase extends TableRow<T> {
		protected final ContextMenu contextMenu;
		protected final MenuItem executeMenuItem;
		protected final MenuItem editMenuItem;
		protected final MenuItem removeMenuItem;

		protected RowBase() {
			// Execute item (if possible) when double-clicked.
			this.setOnMouseClicked(event -> {
				if (!this.isEmpty() && event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					executeItemIfEnabled(this.getItem());
				}
			});

			this.contextMenu = new ContextMenu();

			this.executeMenuItem = new MenuItem(i18n.translate("sharedviews.checking.contextMenu.execute"));
			this.executeMenuItem.setOnAction(e -> executeItem(this.getItem()));
			this.contextMenu.getItems().add(this.executeMenuItem);

			this.editMenuItem = new MenuItem(i18n.translate("sharedviews.checking.contextMenu.edit"));
			this.editMenuItem.setOnAction(e -> {
				final T oldItem = this.getItem();
				showItemDialog(oldItem).ifPresent(newItem -> {
					final T itemToExecute = replaceItem(oldItem, newItem);
					// FIXME Do we always want to re-execute the item after editing?
					executeItemIfEnabled(itemToExecute);
				});
			});
			this.contextMenu.getItems().add(this.editMenuItem);

			this.removeMenuItem = new MenuItem(i18n.translate("sharedviews.checking.contextMenu.remove"));
			this.removeMenuItem.setOnAction(e -> removeItem(this.getItem()));
			this.contextMenu.getItems().add(removeMenuItem);

			this.itemProperty().addListener((o, from, to) -> {
				if (to == null) {
					executeMenuItem.disableProperty().unbind();
					executeMenuItem.disableProperty().set(true);
				} else {
					executeMenuItem.disableProperty().bind(disableItemBinding(to));
				}
			});

			this.contextMenuProperty().bind(Bindings.when(this.emptyProperty())
				                                .then((ContextMenu) null)
				                                .otherwise(this.contextMenu));
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckingViewBase.class);

	@FXML
	protected TableView<T> itemsTable;

	@FXML
	protected TableColumn<T, CheckingStatus> statusColumn;

	@FXML
	protected TableColumn<T, CheckBox> shouldExecuteColumn;

	@FXML
	protected TableColumn<T, String> configurationColumn;

	@FXML
	protected Button checkMachineButton;

	private final StageManager stageManager;
	private final I18n i18n;
	protected final DisablePropertyController disablePropertyController;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final CheckingExecutors checkingExecutors;

	protected final CheckBox selectAll;
	protected BooleanBinding emptyProperty;

	protected CheckingViewBase(
		StageManager stageManager,
		I18n i18n,
		DisablePropertyController disablePropertyController,
		CurrentTrace currentTrace,
		CurrentProject currentProject,
		CheckingExecutors checkingExecutors
	) {
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.disablePropertyController = disablePropertyController;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.checkingExecutors = checkingExecutors;

		this.selectAll = new CheckBox();
	}

	protected abstract ObservableList<T> getItemsProperty(Machine machine);

	@FXML
	protected void initialize() {
		// we have to use ths wrapped binding because we directly set the contained list in "this.itemsTable.itemsProperty()"
		this.emptyProperty = SafeBindings.wrappedBooleanBinding(l -> l == null || l.isEmpty(), this.itemsTable.itemsProperty());

		itemsTable.setItems(FXCollections.emptyObservableList());
		itemsTable.setRowFactory(table -> new RowBase());
		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			itemsTable.itemsProperty().unbind(); // unbind for safety, this should never be bound though
			if (to != null) {
				itemsTable.setItems(this.getItemsProperty(to));
			} else {
				itemsTable.setItems(FXCollections.emptyObservableList());
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);

		checkMachineButton.disableProperty().bind(this.emptyProperty.or(selectAll.selectedProperty().not().or(disablePropertyController.disableProperty())));
		checkMachineButton.setOnAction(e -> this.executeAllSelectedItems());

		statusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory<>(itemsTable, selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
		configurationColumn.setCellValueFactory(features -> {
			String configuration = configurationForItem(features.getValue());
			if (features.getValue().getId() != null) {
				configuration = "[" + features.getValue().getId() + "] " + configuration;
			}
			return new SimpleStringProperty(configuration);
		});

		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
	}

	protected T addItem(T newItem) {
		return this.currentProject.getCurrentMachine().addValidationTaskIfNotExist(newItem);
	}

	protected void removeItem(final T item) {
		this.currentProject.getCurrentMachine().removeValidationTask(item);
	}

	protected T replaceItem(final T oldItem, final T newItem) {
		return this.currentProject.getCurrentMachine().replaceValidationTaskIfNotExist(oldItem, newItem);
	}

	/**
	 * Describe the item's configuration as a string,
	 * which will be displayed in the {@link #configurationColumn}.
	 * If the item is an instance of {@link IValidationTask},
	 * the validation task ID (if any) is automatically prepended to this configuration string.
	 *
	 * @param item the item to describe
	 * @return a string description of the item's configuration
	 */
	protected String configurationForItem(final T item) {
		return item.getTaskDescription(i18n);
	}

	protected BooleanExpression disableItemBinding(final T item) {
		return disablePropertyController.disableProperty();
	}

	protected ExecutionContext getCurrentExecutionContext() {
		Trace trace = currentTrace.get();
		return new ExecutionContext(currentProject.get(), currentProject.getCurrentMachine(), trace.getStateSpace(), trace);
	}

	/**
	 * Execute an item without any user interaction,
	 * i. e. without showing any alerts or changing the current trace.
	 * Can be overridden to perform additional actions whenever an item is executed from the view,
	 * regardless of whether a single item or multiple items are executed.
	 * This method is meant for overriding only and shouldn't be called directly outside of {@link CheckingViewBase}.
	 * 
	 * @param item the item to execute
	 * @param executors the executors to use for executing the item
	 * @param context the project/animator context in which to execute the item
	 * @return a future that completes once the item has been executed
	 */
	protected CompletableFuture<?> executeItemNoninteractiveImpl(T item, CheckingExecutors executors, ExecutionContext context) {
		return item.execute(executors, context);
	}

	/**
	 * Execute a single item in response to a user action.
	 * Can be overridden to perform additional actions when the user executes a specific item in the view,
	 * e. g. to show an error alert on failure or update the current trace to the result of the check.
	 * This method is meant for overriding only and shouldn't be called directly outside of {@link CheckingViewBase} -
	 * other code should call {@link #executeItem(ISelectableTask)} instead.
	 *
	 * @param item the item to execute
	 * @param executors the executors to use for executing the item
	 * @param context the project/animator context in which to execute the item
	 * @return a future that completes once the item has been executed
	 */
	protected CompletableFuture<?> executeItemImpl(T item, CheckingExecutors executors, ExecutionContext context) {
		return executeItemNoninteractiveImpl(item, checkingExecutors, context);
	}

	protected void handleCheckException(Throwable exc) {
		if (exc instanceof CancellationException || exc instanceof CompletionException && exc.getCause() instanceof CancellationException) {
			LOGGER.trace("Check was canceled (this is not an error)", exc);
			return;
		}

		LOGGER.error("Unhandled exception during checking", exc);
		stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
	}

	/**
	 * Execute a single item in response to a user action.
	 * This method cannot be overridden directly -
	 * subclasses can override {@link #executeItemImpl(ISelectableTask, CheckingExecutors, ExecutionContext)} instead.
	 * 
	 * @param item the item to execute
	 */
	protected final void executeItem(T item) {
		final ExecutionContext context = getCurrentExecutionContext();
		executeItemImpl(item, checkingExecutors, context).exceptionally(exc -> {
			handleCheckException(exc);
			return null;
		});
	}

	protected final void executeItemIfEnabled(T item) {
		if (!disableItemBinding(item).get()) {
			executeItem(item);
		}
	}

	protected final void executeAllSelectedItems() {
		final ExecutionContext context = getCurrentExecutionContext();
		CompletableFuture<?> future = CompletableFuture.completedFuture(null);
		for (T item : itemsTable.getItems()) {
			if (!item.selected()) {
				continue;
			}

			future = future.thenCompose(res -> executeItemNoninteractiveImpl(item, checkingExecutors, context));
		}
		future.exceptionally(exc -> {
			handleCheckException(exc);
			return null;
		});
	}

	/**
	 * Show a dialog asking the user to input a new item or edit an existing one.
	 *
	 * @param oldItem the existing item to edit, or {@code null} to ask the user to create a new item
	 * @return the created/edited item, or {@link Optional#empty()} if the user cancelled/closed the dialog
	 */
	protected abstract Optional<T> showItemDialog(final T oldItem);

	@FXML
	protected Optional<T> askToAddItem() {
		return this.showItemDialog(null).map(newItem -> {
			final T toCheck = this.addItem(newItem);
			this.itemsTable.getSelectionModel().select(toCheck);
			// The returned item might already be checked
			// if there was already another item with the same configuration as newItem
			// and that existing item was already checked previously.
			if (toCheck.getStatus() == CheckingStatus.NOT_CHECKED) {
				this.executeItemIfEnabled(toCheck);
			}
			return toCheck;
		});
	}
}
