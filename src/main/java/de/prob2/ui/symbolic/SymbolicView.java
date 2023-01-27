package de.prob2.ui.symbolic;

import de.prob.statespace.FormalismType;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@FXMLInjected
public abstract class SymbolicView<T extends IExecutableItem> extends ScrollPane {
	@FXML
	protected TableView<T> itemsTable;
	
	@FXML
	protected TableColumn<IExecutableItem, Checked> statusColumn;
	
	@FXML
	protected TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	
	@FXML
	protected Button addFormulaButton;
	
	@FXML
	protected Button checkMachineButton;
	
	protected final CurrentTrace currentTrace;
	
	protected final DisablePropertyController disablePropertyController;
	
	// This is a proper ListProperty, so it supports emptyProperty(),
	// unlike TableView.itemsProperty(), which is only an ObjectProperty.
	protected final ListProperty<T> items;
	
	protected final CheckBox selectAll;
	
	public SymbolicView(final CurrentTrace currentTrace, final DisablePropertyController disablePropertyController) {
		this.currentTrace = currentTrace;
		this.disablePropertyController = disablePropertyController;
		this.items = new SimpleListProperty<>(this, "items", FXCollections.emptyObservableList());
		this.selectAll = new CheckBox();
	}
	
	@FXML
	public void initialize() {
		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		addFormulaButton.disableProperty().bind(partOfDisableBinding.or(disablePropertyController.disableProperty()));
		checkMachineButton.disableProperty().bind(this.items.emptyProperty().or(selectAll.selectedProperty().not().or(disablePropertyController.disableProperty())));
		itemsTable.itemsProperty().bind(this.items);
		itemsTable.disableProperty().bind(disablePropertyController.disableProperty());
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(itemsTable,  selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
	}
}
