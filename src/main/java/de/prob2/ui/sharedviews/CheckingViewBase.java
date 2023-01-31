package de.prob2.ui.sharedviews;

import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;
import de.prob2.ui.vomanager.IValidationTask;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@FXMLInjected
public abstract class CheckingViewBase<T extends IExecutableItem> extends ScrollPane {
	@FXML
	protected TableView<T> itemsTable;
	
	@FXML
	protected TableColumn<IExecutableItem, Checked> statusColumn;
	
	@FXML
	protected TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	
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
		itemsTable.itemsProperty().bind(this.items);
		itemsTable.disableProperty().bind(disablePropertyController.disableProperty());
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(itemsTable,  selectAll));
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
}
