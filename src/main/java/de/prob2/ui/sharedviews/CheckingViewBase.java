package de.prob2.ui.sharedviews;

import java.util.Optional;

import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.SafeBindings;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;
import de.prob2.ui.vomanager.IValidationTask;

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

@FXMLInjected
public abstract class CheckingViewBase<T extends IExecutableItem> extends ScrollPane {
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

	@FXML
	protected TableView<T> itemsTable;

	@FXML
	protected TableColumn<T, Checked> statusColumn;

	@FXML
	protected TableColumn<T, CheckBox> shouldExecuteColumn;

	@FXML
	protected TableColumn<T, String> configurationColumn;

	@FXML
	protected Button checkMachineButton;

	private final I18n i18n;
	protected final DisablePropertyController disablePropertyController;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final CliTaskExecutor cliExecutor;

	protected final CheckBox selectAll;
	protected BooleanBinding emptyProperty;

	protected CheckingViewBase(final I18n i18n, final DisablePropertyController disablePropertyController, final CurrentTrace currentTrace, final CurrentProject currentProject, final CliTaskExecutor cliExecutor) {
		this.i18n = i18n;
		this.disablePropertyController = disablePropertyController;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.cliExecutor = cliExecutor;

		this.selectAll = new CheckBox();
	}

	protected abstract ObservableList<T> getItemsProperty(Machine machine);

	protected void addItem(Machine machine, T item) {
		this.getItemsProperty(machine).add(item);
	}

	protected void removeItem(Machine machine, T item) {
		this.getItemsProperty(machine).remove(item);
	}

	protected void replaceItem(Machine machine, T oldItem, T newItem) {
		ObservableList<T> items = this.getItemsProperty(machine);
		items.set(items.indexOf(oldItem), newItem);
	}

	@FXML
	protected void initialize() {
		// we have to use ths wrapped binding because we directly set the contained list in "this.itemsTable.itemsProperty()"
		this.emptyProperty = SafeBindings.wrappedBooleanBinding(l -> l == null || l.isEmpty(), this.itemsTable.itemsProperty());

		itemsTable.setRowFactory(table -> new RowBase());
		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			itemsTable.itemsProperty().unbind(); // unbind for safety, this should never be bound though
			if (to != null) {
				itemsTable.itemsProperty().set(this.getItemsProperty(to));
			} else {
				itemsTable.itemsProperty().set(FXCollections.observableArrayList());
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);

		checkMachineButton.disableProperty().bind(this.emptyProperty.or(selectAll.selectedProperty().not().or(disablePropertyController.disableProperty())));
		checkMachineButton.setOnAction(e -> this.executeAllSelectedItems());

		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory<>(itemsTable, selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
		configurationColumn.setCellValueFactory(features -> {
			String configuration = configurationForItem(features.getValue());
			if (features.getValue() instanceof IValidationTask<?> task) {
				if (task.getId() != null) {
					configuration = "[" + task.getId() + "] " + configuration;
				}
			}
			return new SimpleStringProperty(configuration);
		});

		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
	}

	protected T addItem(final T newItem) {
		final Optional<T> existingItem = itemsTable.getItems().stream().filter(newItem::settingsEqual).findAny();
		if (existingItem.isEmpty()) {
			addItem(currentProject.getCurrentMachine(), newItem);
			return newItem;
		} else {
			T t = existingItem.get();
			t.reset();
			return t;
		}
	}

	protected void removeItem(final T item) {
		removeItem(currentProject.getCurrentMachine(), item);
	}

	protected T replaceItem(final T oldItem, final T newItem) {
		final Optional<T> existingItem = itemsTable.getItems().stream().filter(newItem::settingsEqual).findAny();
		if (existingItem.isEmpty()) {
			replaceItem(currentProject.getCurrentMachine(), oldItem, newItem);
			return newItem;
		} else {
			T t = existingItem.get();
			t.reset();
			return t;
		}
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
	protected abstract String configurationForItem(final T item);

	protected BooleanExpression disableItemBinding(final T item) {
		return disablePropertyController.disableProperty();
	}

	protected ExecutionContext getCurrentExecutionContext() {
		return new ExecutionContext(currentProject.get(), currentProject.getCurrentMachine(), currentTrace.getStateSpace());
	}

	protected abstract void executeItemSync(final T item, final ExecutionContext context);

	protected void executeItem(final T item) {
		final ExecutionContext context = getCurrentExecutionContext();
		cliExecutor.submit(() -> executeItemSync(item, context));
	}

	protected void executeItemIfEnabled(final T item) {
		if (!disableItemBinding(item).get()) {
			executeItem(item);
		}
	}

	protected void executeAllSelectedItems() {
		final ExecutionContext context = getCurrentExecutionContext();
		cliExecutor.submit(() -> {
			for (final T item : itemsTable.getItems()) {
				if (!item.selected()) {
					continue;
				}

				item.execute(context);
			}
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
			// The returned item might already be checked
			// if there was already another item with the same configuration as newItem
			// and that existing item was already checked previously.
			if (toCheck.getChecked() == Checked.NOT_CHECKED) {
				this.executeItemIfEnabled(toCheck);
			}
			return toCheck;
		});
	}
}
