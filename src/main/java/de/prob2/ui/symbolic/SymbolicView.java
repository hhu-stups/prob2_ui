package de.prob2.ui.symbolic;

import com.google.inject.Injector;

import de.prob.statespace.FormalismType;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
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

@FXMLInjected
public abstract class SymbolicView<T extends SymbolicItem<?>> extends ScrollPane {
	
	public abstract class SymbolicCellFactory {

		public TableRow<T> createRow() {
			TableRow<T> row = new TableRow<>();
			
			MenuItem checkItem = new MenuItem(i18n.translate("symbolic.view.contextMenu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e -> formulaHandler.handleItem(row.getItem(), false));
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(executor.runningProperty().or(to.selectedProperty().not()));
				}
			});
			
			row.setContextMenu(new ContextMenu(checkItem));
			return row;
		}
		
	}

	@FXML
	protected HelpButton helpButton;
		
	@FXML
	protected TableView<T> tvFormula;
	
	@FXML
	protected TableColumn<T, Checked> statusColumn;
	
	@FXML
	protected TableColumn<T, String> configurationColumn;
	
	@FXML
	protected TableColumn<T, String> typeColumn;
	
	@FXML
	protected TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	
	@FXML
	protected Button addFormulaButton;
	
	@FXML
	protected Button checkMachineButton;
	
	@FXML
	protected Button cancelButton;
					
	protected final I18n i18n;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;

	protected final Injector injector;
	
	protected final SymbolicExecutor<T> executor;
	
	protected final SymbolicFormulaHandler<T> formulaHandler;

	protected final Class<T> clazz;

	protected final CheckBox selectAll;
	
	public SymbolicView(final I18n i18n, final CurrentTrace currentTrace,
	                    final CurrentProject currentProject, final Injector injector, final SymbolicExecutor<T> executor,
	                    final SymbolicFormulaHandler<T> formulaHandler, final Class<T> clazz) {
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.executor = executor;
		this.formulaHandler = formulaHandler;
		this.clazz = clazz;
		this.selectAll = new CheckBox();
	}
	
	@FXML
	public void initialize() {
		setBindings();
		final ChangeListener<Machine> machineChangeListener = (observable, oldValue, newValue) -> {
			tvFormula.itemsProperty().unbind();
			if(newValue != null) {
				tvFormula.itemsProperty().bind(formulasProperty(newValue));
			} else {
				tvFormula.getItems().clear();
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
	}
	
	protected abstract ListProperty<T> formulasProperty(Machine machine);
	
	protected abstract void removeFormula(Machine machine, T item);
	
	protected void setBindings() {
		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		addFormulaButton.disableProperty().bind(partOfDisableBinding.or(injector.getInstance(DisablePropertyController.class).disableProperty()));
		final BooleanProperty noFormulas = new SimpleBooleanProperty();
		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			noFormulas.unbind();
			if (to != null) {
				noFormulas.bind(formulasProperty(to).emptyProperty());
			} else {
				noFormulas.set(true);
			}
		});
		checkMachineButton.disableProperty().bind(partOfDisableBinding.or(noFormulas.or(selectAll.selectedProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty()))));
		cancelButton.disableProperty().bind(executor.runningProperty().not());
		tvFormula.disableProperty().bind(partOfDisableBinding.or(injector.getInstance(DisablePropertyController.class).disableProperty()));
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		configurationColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
		typeColumn.setCellValueFactory(features -> i18n.translateBinding(features.getValue().getType()));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(tvFormula,  selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
		tvFormula.setOnMouseClicked(e-> {
			T item = tvFormula.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && item != null && currentTrace.get() != null) {
				formulaHandler.handleItem(item, false);
			}
		});

	}
	
	@FXML
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		formulaHandler.handleMachine(machine);
	}
	
	protected void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		T item = tvFormula.getSelectionModel().getSelectedItem();
		removeFormula(machine, item);
	}
	
	
	@FXML
	public void cancel() {
		executor.interrupt();
	}

	protected abstract void openItem(T item);
}
