package de.prob2.ui.symbolic;

import com.google.inject.Injector;

import de.prob.statespace.FormalismType;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.executor.CliTaskExecutor;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@FXMLInjected
public abstract class SymbolicView<T extends IExecutableItem> extends ScrollPane {
	@FXML
	protected HelpButton helpButton;
		
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
	
	@FXML
	protected Button cancelButton;
					
	protected final I18n i18n;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;

	protected final Injector injector;
	
	protected CliTaskExecutor cliExecutor;

	protected final CheckBox selectAll;
	
	public SymbolicView(final I18n i18n, final CurrentTrace currentTrace, final CurrentProject currentProject, final Injector injector, final CliTaskExecutor cliExecutor) {
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.cliExecutor = cliExecutor;
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
		cancelButton.disableProperty().bind(cliExecutor.runningProperty().not());
		tvFormula.disableProperty().bind(partOfDisableBinding.or(injector.getInstance(DisablePropertyController.class).disableProperty()));
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(tvFormula,  selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
	}
	
	@FXML
	public void cancel() {
		cliExecutor.interruptAll();
		currentTrace.getStateSpace().sendInterrupt();
	}
}
