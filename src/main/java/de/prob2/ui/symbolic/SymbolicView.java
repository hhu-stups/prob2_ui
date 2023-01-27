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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
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
	protected TableView<T> tvFormula;
	
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
	
	protected final CheckBox selectAll;
	
	public SymbolicView(final CurrentTrace currentTrace, final DisablePropertyController disablePropertyController) {
		this.currentTrace = currentTrace;
		this.disablePropertyController = disablePropertyController;
		this.selectAll = new CheckBox();
	}
	
	@FXML
	public void initialize() {
		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		addFormulaButton.disableProperty().bind(partOfDisableBinding.or(disablePropertyController.disableProperty()));
		final BooleanProperty noFormulas = new SimpleBooleanProperty();
		tvFormula.itemsProperty().addListener((o, from, to) -> {
			noFormulas.unbind();
			if (to != null) {
				noFormulas.bind(new SimpleListProperty<>(to).emptyProperty());
			} else {
				noFormulas.set(true);
			}
		});
		checkMachineButton.disableProperty().bind(partOfDisableBinding.or(noFormulas.or(selectAll.selectedProperty().not().or(disablePropertyController.disableProperty()))));
		tvFormula.disableProperty().bind(partOfDisableBinding.or(disablePropertyController.disableProperty()));
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(tvFormula,  selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
	}
}
