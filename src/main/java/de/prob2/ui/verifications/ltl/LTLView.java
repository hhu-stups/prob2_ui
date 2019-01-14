package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.InvalidFileFormatException;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.MachineStatusHandler;
import de.prob2.ui.verifications.ItemSelectedFactory;
import de.prob2.ui.verifications.ltl.LTLHandleItem.HandleType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaStage;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternStage;
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
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class LTLView extends AnchorPane {

	private static final String LTL_FILE_ENDING = "*.ltl";

	private static final Logger logger = LoggerFactory.getLogger(LTLView.class);
	
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
	private Button saveLTLButton;
	@FXML
	private Button loadLTLButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableView<LTLPatternItem> tvPattern;
	@FXML
	private TableView<LTLFormulaItem> tvFormula;
	@FXML
	private TableColumn<IExecutableItem, CheckBox> formulaSelectedColumn;
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> formulaStatusColumn;
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaColumn;
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaDescriptionColumn;
	@FXML
	private TableColumn<IExecutableItem, CheckBox> patternSelectedColumn;
	@FXML
	private TableColumn<LTLPatternItem, FontAwesomeIconView> patternStatusColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternColumn;
	@FXML
	private TableColumn<LTLPatternItem, String> patternDescriptionColumn;

	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final LTLFormulaChecker checker;
	private final LTLPatternParser patternParser;
	private final LTLResultHandler resultHandler;
	private final ListProperty<Thread> currentJobThreads;
	private final LTLFileHandler ltlFileHandler;
	private final FileChooserManager fileChooserManager;
				
	@Inject
	private LTLView(final StageManager stageManager, final ResourceBundle bundle, final Injector injector,
					final CurrentTrace currentTrace, final CurrentProject currentProject,
					final LTLFormulaChecker checker, final LTLPatternParser patternParser,
					final LTLResultHandler resultHandler, final LTLFileHandler ltlFileHandler,
					final FileChooserManager fileChooserManager) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.checker = checker;
		this.patternParser = patternParser;
		this.resultHandler = resultHandler;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		this.ltlFileHandler = ltlFileHandler;
		this.fileChooserManager = fileChooserManager;
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
							.or(to.selectedProperty().not()));
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
		formulaSelectedColumn.setCellValueFactory(new ItemSelectedFactory(CheckingType.LTL, injector));
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		patternSelectedColumn.setCellValueFactory(new ItemSelectedFactory(CheckingType.LTL, injector));
		patternStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		patternColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		patternDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		CheckBox formulaSelectAll = new CheckBox();
		formulaSelectAll.setSelected(true);
		formulaSelectAll.selectedProperty().addListener((observable, from, to) -> {
			for(IExecutableItem item : tvFormula.getItems()) {
				item.setSelected(to);
				Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL);
				tvFormula.refresh();
			}
		});

		CheckBox patternSelectAll = new CheckBox();
		patternSelectAll.setSelected(true);
		patternSelectAll.selectedProperty().addListener((observable, from, to) -> {
			for(IExecutableItem item : tvPattern.getItems()) {
				item.setSelected(to);
				Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.LTL);
				tvPattern.refresh();
			}
		});
		formulaSelectedColumn.setGraphic(formulaSelectAll);
		patternSelectedColumn.setGraphic(patternSelectAll);

		addMenuButton.disableProperty().bind(currentTrace.existsProperty().not());
		cancelButton.disableProperty().bind(currentJobThreads.emptyProperty());
		checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(currentJobThreads.emptyProperty().not()));
		saveLTLButton.disableProperty().bind(currentTrace.existsProperty().not());
		loadLTLButton.disableProperty().bind(currentTrace.existsProperty().not());
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue) {
				checkMachineButton.disableProperty().bind(currentProject.getCurrentMachine().ltlFormulasProperty().emptyProperty().or(currentJobThreads.emptyProperty().not()));
				saveLTLButton.disableProperty().bind(currentProject.getCurrentMachine().ltlFormulasProperty().emptyProperty());
			} else {
				checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(currentJobThreads.emptyProperty().not()));
				saveLTLButton.disableProperty().bind(currentTrace.existsProperty().not());
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
		LTLFormulaStage formulaStage = injector.getInstance(LTLFormulaStage.class);
		loadLTLStage(formulaStage, null);
		formulaStage.setHandleItem(new LTLHandleItem<LTLFormulaItem>(HandleType.ADD, null));
		formulaStage.showAndWait();
		tvFormula.refresh();
	}
	
	private void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
		machine.removeLTLFormula(item);
		updateProject();
	}
	
	@FXML
	public void addPattern() {
		LTLPatternStage patternStage = injector.getInstance(LTLPatternStage.class);
		loadLTLStage(patternStage, null);
		patternStage.setHandleItem(new LTLHandleItem<LTLPatternItem>(LTLHandleItem.HandleType.ADD, null));
		patternStage.showAndWait();
		tvPattern.refresh();
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
		LTLFormulaStage formulaStage = injector.getInstance(LTLFormulaStage.class);
		loadLTLStage(formulaStage, item);
		formulaStage.setHandleItem(new LTLHandleItem<LTLFormulaItem>(HandleType.CHANGE, item));
		formulaStage.showAndWait();
		formulaStage.clear();
		tvFormula.refresh();
	}
	
	private void showCurrentItemDialog(LTLPatternItem item) {
		LTLPatternStage patternStage = injector.getInstance(LTLPatternStage.class);
		loadLTLStage(patternStage, item);
		patternStage.setHandleItem(new LTLHandleItem<LTLPatternItem>(HandleType.CHANGE, item));
		patternStage.showAndWait();
		patternStage.clear();
		tvPattern.refresh();
	}

	private void loadLTLStage(LTLItemStage stage, AbstractCheckableItem item) {
		stage.getEngine().getLoadWorker().stateProperty().addListener((observable, from, to) -> {
			if(to == Worker.State.SUCCEEDED && item != null) {
				stage.setData(item.getDescription(), item.getCode());
			}
		});
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

	@FXML
	private void saveLTL() {
		ltlFileHandler.save();
	}

	@FXML
	private void loadLTL() {
		Machine machine = currentProject.getCurrentMachine();
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("verifications.ltl.ltlView.fileChooser.loadLTL.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.getExtensionFilters()
				.add(new FileChooser.ExtensionFilter(
						String.format(bundle.getString("common.fileChooser.fileTypes.ltl"), LTL_FILE_ENDING),
						LTL_FILE_ENDING));
		Path ltlFile = fileChooserManager.showOpenDialog(fileChooser, FileChooserManager.Kind.LTL, stageManager.getCurrent());
		if(ltlFile == null) {
			return;
		}
		LTLData data = null;
		try {
			data = ltlFileHandler.load(ltlFile);
		} catch (IOException | InvalidFileFormatException e) {
			logger.error("Could not load LTL file: ", e);
			return;
		}
		data.getFormulas().stream()
				.filter(formula -> !machine.getLTLFormulas().contains(formula))
				.forEach(formula -> {
					formula.initialize();
					machine.addLTLFormula(formula);
				});
		data.getPatterns().stream()
				.filter(pattern -> !machine.getLTLPatterns().contains(pattern))
				.forEach(pattern -> {
					pattern.initialize();
					machine.addLTLPattern(pattern);
					patternParser.parsePattern(pattern, machine);
					patternParser.addPattern(pattern, machine);
				});
	}

	public ListProperty<Thread> currentJobThreadsProperty() {
		return currentJobThreads;
	}

}
