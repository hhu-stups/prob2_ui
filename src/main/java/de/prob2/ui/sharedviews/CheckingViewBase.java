package de.prob2.ui.sharedviews;

import java.util.Optional;

import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
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
					final T item = this.getItem();
					if (!disableItemBinding(item).get()) {
						executeItem(item);
					}
				}
			});
			
			this.contextMenu = new ContextMenu();
			
			this.executeMenuItem = new MenuItem();
			this.executeMenuItem.setOnAction(e -> executeItem(this.getItem()));
			this.contextMenu.getItems().add(this.executeMenuItem);
			
			this.editMenuItem = new MenuItem();
			this.editMenuItem.setOnAction(e -> {
				final T oldItem = this.getItem();
				editItem(oldItem).ifPresent(newItem -> {
					final Optional<T> existingItem = items.stream().filter(newItem::settingsEqual).findAny();
					if (!existingItem.isPresent()) {
						items.set(items.indexOf(oldItem), newItem);
					}
					// FIXME Do we always want to re-execute the item after editing?
					final T itemToExecute = existingItem.orElse(newItem);
					if (!disableItemBinding(itemToExecute).get()) {
						executeItem(itemToExecute);
					}
				});
			});
			this.contextMenu.getItems().add(this.editMenuItem);
			
			this.removeMenuItem = new MenuItem();
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
	
	protected final DisablePropertyController disablePropertyController;
	
	// This is a proper ListProperty, so it supports emptyProperty(),
	// unlike TableView.itemsProperty(), which is only an ObjectProperty.
	protected final ListProperty<T> items;
	
	protected final CheckBox selectAll;
	
	protected CheckingViewBase(final DisablePropertyController disablePropertyController) {
		this.disablePropertyController = disablePropertyController;
		this.items = new SimpleListProperty<>(this, "items", FXCollections.emptyObservableList());
		this.selectAll = new CheckBox();
	}
	
	@FXML
	public void initialize() {
		checkMachineButton.disableProperty().bind(this.items.emptyProperty().or(selectAll.selectedProperty().not().or(disablePropertyController.disableProperty())));
		itemsTable.setRowFactory(table -> new RowBase());
		itemsTable.itemsProperty().bind(this.items);
		itemsTable.disableProperty().bind(disablePropertyController.disableProperty());
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
		return disablePropertyController.disableProperty().or(item.selectedProperty().not());
	}
	
	protected abstract void executeItem(final T item);
	
	protected abstract Optional<T> editItem(final T oldItem);
}
