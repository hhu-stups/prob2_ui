package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.MachineStatusHandler;
import de.prob2.ui.verifications.ShouldExecuteValueFactory;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaDialog;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternDialog;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternParser;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

import java.util.ResourceBundle;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class LTLView extends AnchorPane {
	
	@FXML 
	private MenuButton addMenuButton;
	@FXML
	private MenuItem addFormulaButton;
	@FXML
	private MenuItem addPatternButton;
	@FXML
	private Button checkMachineButton;
	@FXML
	private Button cancelButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableView<LTLPatternItem> tvPattern;
	@FXML
	private TableColumn<LTLPatternItem, FontAwesomeIconView> patternStatusColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternDescriptionColumn;
	@FXML
	private TableView<LTLFormulaItem> tvFormula;
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> formulaStatusColumn;
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaColumn;
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaDescriptionColumn;
	@FXML
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	
	private final ResourceBundle bundle;
	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final LTLFormulaChecker checker;
	private final LTLPatternParser patternParser;
	private final LTLResultHandler resultHandler;
	private final ListProperty<Thread> currentJobThreads;
				
	@Inject
	private LTLView(final StageManager stageManager, final ResourceBundle bundle, final Injector injector,final CurrentTrace currentTrace, final CurrentProject currentProject, final LTLFormulaChecker checker, final LTLPatternParser patternParser, final LTLResultHandler resultHandler) {
		this.bundle = bundle;
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.checker = checker;
		this.patternParser = patternParser;
		this.resultHandler = resultHandler;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		stageManager.loadFXML(this, "ltl_view.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent(this.getClass());
		setOnItemClicked();
		setContextMenus();
		setBindings();
		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if(to != null) {
				if(from != null) {
					from.clearPatternManager();
				}
				bindMachine(to);
			} else {
				tvFormula.getItems().clear();
				tvFormula.itemsProperty().unbind();
				tvPattern.getItems().clear();
				tvPattern.itemsProperty().unbind();
			}
		});
	}
	
	private void setOnItemClicked() {
		tvFormula.setOnMouseClicked(e-> {
			LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.exists()) {
				checkItem(item);
			}
		});
	}

	/**
	 * Sets the context menus for the items LTLFormula and LTLPatterns
	 */
	private void setContextMenus() {
		tvFormula.setRowFactory(table -> {
			final TableRow<LTLFormulaItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.removeFormula"));
			removeItem.setOnAction(e -> removeFormula());
			removeItem.disableProperty().bind(row.emptyProperty());
						
			MenuItem showCounterExampleItem = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.showCounterExample"));
			showCounterExampleItem.setOnAction(e-> currentTrace.set(tvFormula.getSelectionModel().getSelectedItem().getCounterExample()));
			showCounterExampleItem.setDisable(true);

			MenuItem openEditor = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.openInEditor"));
			openEditor.setOnAction(e->showCurrentItemDialog(row.getItem()));
			openEditor.disableProperty().bind(row.emptyProperty());
			
			MenuItem showMessage = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> resultHandler.showResult(row.getItem()));

			MenuItem checkItem = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e-> checkItem(row.getItem()));
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(row.emptyProperty()
							.or(currentJobThreads.emptyProperty().not())
							.or(to.shouldExecuteProperty().not()));
					showMessage.disableProperty().bind(to.resultItemProperty().isNull()
							.or(Bindings.createBooleanBinding(() -> to.getResultItem() != null && Checked.SUCCESS == to.getResultItem().getChecked(), to.resultItemProperty())));
					showCounterExampleItem.disableProperty().bind(row.emptyProperty()
							.or(to.counterExampleProperty().isNull()));
				}
			});
			row.setContextMenu(new ContextMenu(checkItem, openEditor, removeItem, showCounterExampleItem, showMessage));
			return row;
		});
		
		tvPattern.setRowFactory(table -> {
			final TableRow<LTLPatternItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.removePattern"));
			removeItem.setOnAction(e -> removePattern());
			removeItem.disableProperty().bind(row.emptyProperty());

			MenuItem openEditor = new MenuItem(bundle.getString("verifications.ltl.ltlView.contextMenu.openInEditor"));
			openEditor.setOnAction(e -> showCurrentItemDialog(row.getItem()));
			openEditor.disableProperty().bind(row.emptyProperty());
			
			MenuItem showMessage = new MenuItem(bundle.getString("verifications.ltl.LTLView.contextMenu.showParsingMessage"));
			showMessage.setOnAction(e -> resultHandler.showResult(row.getItem()));
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull()
							.or(Bindings.createBooleanBinding(() -> to.getResultItem() != null && Checked.SUCCESS == to.getResultItem().getChecked(), to.resultItemProperty())));
				}
			});
			row.setContextMenu(new ContextMenu(openEditor, showMessage, removeItem));
			return row;
		});
	}

	private void setBindings() {
		patternStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		patternColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		patternDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		shouldExecuteColumn.setCellValueFactory(new ShouldExecuteValueFactory(CheckingType.LTL, injector));
		
		CheckBox selectAll = new CheckBox();
		selectAll.setSelected(true);
		selectAll.selectedProperty().addListener((observable, from, to) -> {
			for(IExecutableItem item : tvFormula.getItems()) {
				item.setShouldExecute(to);
				Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL);
				tvFormula.refresh();
			}
		});
		shouldExecuteColumn.setGraphic(selectAll);
		
		addMenuButton.disableProperty().bind(currentTrace.existsProperty().not());
		cancelButton.disableProperty().bind(currentJobThreads.emptyProperty());
		checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(currentJobThreads.emptyProperty().not()));
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue) {
				checkMachineButton.disableProperty().bind(currentProject.getCurrentMachine().ltlFormulasProperty().emptyProperty().or(currentJobThreads.emptyProperty().not()));
			} else {
				checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(currentJobThreads.emptyProperty().not()));
			}
		});
		
		tvFormula.disableProperty().bind(currentTrace.existsProperty().not().or(currentJobThreads.emptyProperty().not()));
	}
	
	public void bindMachine(Machine machine) {
		tvFormula.itemsProperty().unbind();
		tvFormula.itemsProperty().bind(machine.ltlFormulasProperty());
		tvPattern.itemsProperty().unbind();
		tvPattern.itemsProperty().bind(machine.ltlPatternsProperty());
		tvFormula.refresh();
		tvPattern.refresh();
		for(LTLFormulaItem formula : machine.getLTLFormulas()) {
			formula.setCounterExample(null);
			formula.setResultItem(null);
		}
		if(currentTrace.existsProperty().get()) {
			checkMachineButton.disableProperty().bind(machine.ltlFormulasProperty().emptyProperty());
		}
		parseMachine(machine);
	}
		
	@FXML
	public void addFormula() {
		Machine machine = currentProject.getCurrentMachine();
		LTLFormulaDialog formulaDialog = injector.getInstance(LTLFormulaDialog.class);
		loadLTLDialog(formulaDialog, null);
		formulaDialog
				.showAndWait()
				.ifPresent(item -> addFormula(machine, (LTLFormulaItem) item));
	}
	
	private void addFormula(Machine machine, LTLFormulaItem item) {
		if(!machine.getLTLFormulas().contains(item)) {
			machine.addLTLFormula(item);
			updateProject();
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}
	
	private void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
		machine.removeLTLFormula(item);
		updateProject();
	}
	
	@FXML
	public void addPattern() {
		Machine machine = currentProject.getCurrentMachine();
		LTLPatternDialog patternDialog = injector.getInstance(LTLPatternDialog.class);
		loadLTLDialog(patternDialog, null);
		patternDialog
				.showAndWait()
				.ifPresent(item -> addPattern(machine, (LTLPatternItem) item));
	}
	
	private void addPattern(Machine machine, LTLPatternItem item) {
		if(!machine.getLTLPatterns().contains(item)) {
			machine.addLTLPattern(item);
			updateProject();
			patternParser.parsePattern(item, machine);
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.PATTERN);
		}
	}
	
	private void removePattern() {
		Machine machine = currentProject.getCurrentMachine();
		LTLPatternItem item = tvPattern.getSelectionModel().getSelectedItem();
		machine.removeLTLPattern(item);
		patternParser.removePattern(item, machine);
		updateProject();
	}
	
				
	public Checked checkFormula(LTLFormulaItem item, Machine machine) {
		return checker.checkFormula(item, machine);
	}
			
	private void showCurrentItemDialog(LTLFormulaItem item) {
		LTLFormulaDialog formulaDialog = injector.getInstance(LTLFormulaDialog.class);
		loadLTLDialog(formulaDialog, item);
		formulaDialog.showAndWait()
					.ifPresent(result-> changeFormula(item, (LTLFormulaItem) result));
		formulaDialog.clear();
	}
	
	private void changeFormula(LTLFormulaItem item, LTLFormulaItem result) {
		Machine machine = currentProject.getCurrentMachine();
		if(!machine.getLTLFormulas().stream()
				.filter(formula -> !formula.equals(item))
				.collect(Collectors.toList())
				.contains(result)) {
			item.setData(result.getName(), result.getDescription(), result.getCode());
			item.setCounterExample(null);
			item.setResultItem(null);
			currentProject.setSaved(false);
			tvFormula.refresh();
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.FORMULA);
		}
	}
	
	private void showCurrentItemDialog(LTLPatternItem item) {
		LTLPatternDialog patternDialog = injector.getInstance(LTLPatternDialog.class);
		loadLTLDialog(patternDialog, item);
		patternDialog.showAndWait()
					.ifPresent(result-> changePattern(item, (LTLPatternItem) result));
		patternDialog.clear();
	}

	private void loadLTLDialog(LTLDialog dialog, AbstractCheckableItem item) {
		dialog.getEngine().getLoadWorker().stateProperty().addListener((observable, from, to) -> {
			if(to == Worker.State.SUCCEEDED && item != null) {
				dialog.setData(item.getName(), item.getDescription(), item.getCode());
			}
		});
	}
	
	private void changePattern(LTLPatternItem item, LTLPatternItem result) {
		Machine machine = currentProject.getCurrentMachine();
		if(!machine.getLTLPatterns().stream()
				.filter(pattern -> !pattern.equals(item))
				.collect(Collectors.toList())
				.contains(result)) {
			machine.getPatternManager().removePattern(machine.getPatternManager().getUserPattern(item.getName()));
			item.setData(result.getName(), result.getDescription(), result.getCode());
			patternParser.parsePattern(item, machine);
			currentProject.setSaved(false);
			tvPattern.refresh();
		} else {
			resultHandler.showAlreadyExists(AbstractResultHandler.ItemType.PATTERN);
		}
	}
	
	private void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				currentProject.getMachines(), currentProject.getPreferences(), currentProject.getLocation()));
	}
	
	@FXML
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			checker.checkMachine(machine);
			Platform.runLater(() -> {
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL);
				tvFormula.refresh();
			});
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	private void checkItem(LTLFormulaItem item) {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			Checked result = checkFormula(item, machine);
			item.setChecked(result);
			Platform.runLater(() -> {
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL);
				tvFormula.refresh();
			});
			if(item.getCounterExample() != null) {
				currentTrace.set(item.getCounterExample());
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "LTL Checking Thread");
		currentJobThreads.add(checkingThread);
		checkingThread.start();
	}
	
	@FXML
	public void cancel() {
		currentJobThreads.forEach(Thread::interrupt);
		currentTrace.getStateSpace().sendInterrupt();
	}
	
	private void parseMachine(Machine machine) {
		patternParser.parseMachine(machine);
	}		

	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}

}
