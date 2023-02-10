package de.prob2.ui.sharedviews;

import java.util.Optional;

import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
			this.removeMenuItem.setOnAction(e -> items.remove(this.getItem()));
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
				.then((ContextMenu)null)
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
	
	// This is a proper ListProperty, so it supports emptyProperty(),
	// unlike TableView.itemsProperty(), which is only an ObjectProperty.
	protected final ListProperty<T> items;
	
	protected final CheckBox selectAll;
	
	protected CheckingViewBase(final I18n i18n, final DisablePropertyController disablePropertyController) {
		this.i18n = i18n;
		this.disablePropertyController = disablePropertyController;
		this.items = new SimpleListProperty<>(this, "items", FXCollections.emptyObservableList());
		this.selectAll = new CheckBox();
	}
	
	@FXML
	public void initialize() {
		checkMachineButton.disableProperty().bind(this.items.emptyProperty().or(selectAll.selectedProperty().not().or(disablePropertyController.disableProperty())));
		itemsTable.setRowFactory(table -> new RowBase());
		itemsTable.itemsProperty().bind(this.items);
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory<>(itemsTable,  selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
		configurationColumn.setCellValueFactory(features -> {
			String configuration = configurationForItem(features.getValue());
			if (features.getValue() instanceof IValidationTask) {
				final IValidationTask task = (IValidationTask)features.getValue();
				if (task.getId() != null) {
					configuration = "[" + task.getId() + "] " + configuration;
				}
			}
			return new SimpleStringProperty(configuration);
		});
	}
	
	protected T addItem(final T newItem) {
		final Optional<T> existingItem = items.stream().filter(newItem::settingsEqual).findAny();
		if (!existingItem.isPresent()) {
			items.add(newItem);
			return newItem;
		} else {
			return existingItem.get();
		}
	}
	
	protected T replaceItem(final T oldItem, final T newItem) {
		final Optional<T> existingItem = items.stream().filter(newItem::settingsEqual).findAny();
		if (!existingItem.isPresent()) {
			items.set(items.indexOf(oldItem), newItem);
			return newItem;
		} else {
			return existingItem.get();
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
	
	protected abstract void executeItem(final T item);
	
	protected void executeItemIfEnabled(final T item) {
		if (!disableItemBinding(item).get()) {
			executeItem(item);
		}
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
