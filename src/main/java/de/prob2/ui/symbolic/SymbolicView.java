package de.prob2.ui.symbolic;

import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.prob.statespace.FormalismType;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;
import de.prob2.ui.verifications.MachineStatusHandler;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
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
public abstract class SymbolicView<T extends SymbolicFormulaItem> extends ScrollPane {
	
	public abstract class SymbolicCellFactory {

		public TableRow<T> createRow() {
			TableRow<T> row = new TableRow<>();
			
			MenuItem checkItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e-> {
				formulaHandler.handleItem(row.getItem(), false);
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(currentProject.getCurrentMachine(), CheckingType.SYMBOLIC);
			});
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(row.emptyProperty()
							.or(executor.currentJobThreadsProperty().emptyProperty().not())
							.or(to.selectedProperty().not()));
				}
			});
			MenuItem removeItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.remove"));
			removeItem.setOnAction(e -> removeFormula());
			removeItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem changeItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.change"));
			changeItem.setOnAction(e->openItem(row.getItem()));
			
			row.setContextMenu(new ContextMenu(checkItem, changeItem, removeItem));
			return row;
		}
		
	}

	@FXML
	protected HelpButton helpButton;
		
	@FXML
	protected TableView<T> tvFormula;
	
	@FXML
	protected TableColumn<T, BindableGlyph> formulaStatusColumn;
	
	@FXML
	protected TableColumn<T, String> formulaNameColumn;
	
	@FXML
	protected TableColumn<T, String> formulaDescriptionColumn;
	
	@FXML
	protected TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	
	@FXML
	protected Button addFormulaButton;
	
	@FXML
	protected Button checkMachineButton;
	
	@FXML
	protected Button cancelButton;
					
	protected final ResourceBundle bundle;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;

	protected final Injector injector;
	
	protected final SymbolicExecutor executor;
	
	protected final SymbolicFormulaHandler<T> formulaHandler;
	
	public SymbolicView(final ResourceBundle bundle, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final Injector injector, final SymbolicExecutor executor,
					final SymbolicFormulaHandler<T> formulaHandler) {
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.executor = executor;
		this.formulaHandler = formulaHandler;
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent(this.getClass());
		setBindings();
		setContextMenu();
		currentProject.currentMachineProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null) {
				bindMachine(newValue);
			} else {
				tvFormula.getItems().clear();
				tvFormula.itemsProperty().unbind();
			}
		});
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue) {
				injector.getInstance(DisablePropertyController.class).addDisableProperty(checkMachineButton.disableProperty(), formulasProperty(currentProject.getCurrentMachine()).emptyProperty().or(executor.currentJobThreadsProperty().emptyProperty().not()));
			} else {
				checkMachineButton.disableProperty().unbind();
				checkMachineButton.setDisable(true);
			}
		});
	}
	
	public void bindMachine(Machine machine) {
		tvFormula.itemsProperty().unbind();
		tvFormula.itemsProperty().bind(formulasProperty(machine));
		tvFormula.refresh();
	}
	
	protected abstract ListProperty<T> formulasProperty(Machine machine);
	
	protected abstract void removeFormula(Machine machine, T item);
	
	protected void setBindings() {
		final BooleanBinding partOfDisableBinding = currentTrace.existsProperty().not()
				.or(Bindings.createBooleanBinding(() -> currentTrace.getModel() == null || currentTrace.getModel().getFormalismType() != FormalismType.B, currentTrace.modelProperty()));
		injector.getInstance(DisablePropertyController.class).addDisableProperty(addFormulaButton.disableProperty(), partOfDisableBinding);
		injector.getInstance(DisablePropertyController.class).addDisableProperty(checkMachineButton.disableProperty(), partOfDisableBinding);
		cancelButton.disableProperty().bind(executor.currentJobThreadsProperty().emptyProperty());
		injector.getInstance(DisablePropertyController.class).addDisableProperty(tvFormula.disableProperty(), partOfDisableBinding);
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(CheckingType.SYMBOLIC, injector));
		CheckBox selectAll = new CheckBox();
		selectAll.setSelected(true);
		selectAll.selectedProperty().addListener((observable, from, to) -> {
			for(IExecutableItem item : tvFormula.getItems()) {
				item.setSelected(to);
				Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.SYMBOLIC);
				tvFormula.refresh();
			}
		});
		shouldExecuteColumn.setGraphic(selectAll);
		tvFormula.setOnMouseClicked(e-> {
			T item = tvFormula.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && item != null && currentTrace.exists()) {
				formulaHandler.handleItem(item, false);
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(currentProject.getCurrentMachine(), CheckingType.SYMBOLIC);
			}
		});

	}
	
	public void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				currentProject.getMachines(), currentProject.getPreferences(), currentProject.getLocation()));
	}
	
	protected abstract void setContextMenu();
	
	public void refresh() {
		tvFormula.refresh();
	}	
	@FXML
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		formulaHandler.handleMachine(machine);
		injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.SYMBOLIC);
		refresh();
	}
	
	protected void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		T item = tvFormula.getSelectionModel().getSelectedItem();
		removeFormula(machine, item);
		updateProject();
	}
	
	
	@FXML
	public void cancel() {
		executor.interrupt();
	}

	protected abstract void openItem(T item);
	
}
