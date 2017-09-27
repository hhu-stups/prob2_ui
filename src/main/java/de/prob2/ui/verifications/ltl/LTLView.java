package de.prob2.ui.verifications.ltl;

import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.AbstractResultHandler;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaDialog;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternDialog;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternParser;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

@Singleton
public class LTLView extends AnchorPane{
				
	@FXML
	private Button addFormulaButton;
	
	@FXML
	private Button addPatternButton;
	
	@FXML
	private Button checkMachineButton;

	@FXML
	private HelpButton helpButton;
	
	@FXML
	private TableView<LTLPatternItem> tvPattern;
	
	@FXML
	private TableColumn<LTLPatternItem, FontAwesomeIconView> patternStatusColumn;
	
	@FXML
	private TableColumn<LTLPatternItem, String> patternNameColumn;
	
	@FXML
	private TableColumn<LTLPatternItem, String> patternDescriptionColumn;
	
	@FXML
	private TableView<LTLFormulaItem> tvFormula;
	
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> formulaStatusColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaNameColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaDescriptionColumn;
	
	private final ResourceBundle bundle;
	
	private final Injector injector;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
		
	private final LTLFormulaChecker checker;
	
	private final LTLPatternParser patternParser;
	
	private final LTLResultHandler resultHandler;
				
	@Inject
	private LTLView(final StageManager stageManager, final ResourceBundle bundle, final Injector injector,final CurrentTrace currentTrace, final CurrentProject currentProject, final LTLFormulaChecker checker, final LTLPatternParser patternParser, final LTLResultHandler resultHandler) {
		this.bundle = bundle;
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.checker = checker;
		this.patternParser = patternParser;
		this.resultHandler = resultHandler;
		stageManager.loadFXML(this, "ltl_view.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent("HelpMain.html");
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
			if(e.getClickCount() == 2 &&  item != null && currentTrace.exists()) {
				showCurrentItemDialog(item);
			}
		});
		
		tvPattern.setOnMouseClicked(e-> {
			LTLPatternItem item = tvPattern.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 &&  item != null && currentTrace.exists()) {
				showCurrentItemDialog(item);
			}
		});
	}

	/**
	 * Sets the context menus for the items LTLFormula and LTLPatterns
	 */
	private void setContextMenus() {
		tvFormula.setRowFactory(table -> {
			final TableRow<LTLFormulaItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.ltl.formula.menu.remove"));
			removeItem.setOnAction(e -> removeFormula());
			removeItem.disableProperty().bind(row.emptyProperty());
						
			MenuItem showCounterExampleItem = new MenuItem(bundle.getString("verifications.ltl.formula.menu.showCounterExample"));
			showCounterExampleItem.setOnAction(e-> currentTrace.set(tvFormula.getSelectionModel().getSelectedItem().getCounterExample()));
			showCounterExampleItem.setDisable(true);

			MenuItem openEditor = new MenuItem(bundle.getString("verifications.ltl.formula.menu.openInEditor"));
			openEditor.setOnAction(e->showCurrentItemDialog(row.getItem()));
			openEditor.disableProperty().bind(row.emptyProperty());

			MenuItem check = new MenuItem(bundle.getString("verifications.ltl.formula.menu.checkSeparately"));
			check.setOnAction(e-> {
				Thread checkingThread = new Thread(() -> {
					Machine machine = currentProject.getCurrentMachine();
					LTLFormulaItem item = row.getItem();
					Checked result = checkFormula(item, machine);
					item.setChecked(result);
					Platform.runLater(() -> {
						checker.checkMachineStatus(machine);
						tvFormula.refresh();
					});
				});
				checkingThread.start();
			});
			check.disableProperty().bind(row.emptyProperty());

			row.setOnMouseClicked(e-> {
				if(e.getButton() == MouseButton.SECONDARY) {
					LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
					if(row.emptyProperty().get() || item.getCounterExample() == null) {
						showCounterExampleItem.setDisable(true);
					} else {
						showCounterExampleItem.setDisable(false);
					}
				}
			});
			row.setContextMenu(new ContextMenu(openEditor, removeItem, showCounterExampleItem, check));
			return row;
		});
		
		tvPattern.setRowFactory(table -> {
			final TableRow<LTLPatternItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.ltl.pattern.menu.remove"));
			removeItem.setOnAction(e -> removePattern());
			removeItem.disableProperty().bind(row.emptyProperty());

			MenuItem openEditor = new MenuItem(bundle.getString("verifications.ltl.pattern.menu.openInEditor"));
			openEditor.setOnAction(e -> showCurrentItemDialog(row.getItem()));
			openEditor.disableProperty().bind(row.emptyProperty());

			row.setContextMenu(new ContextMenu(openEditor, removeItem));
			return row;
		});
	}
	
	private void setBindings() {
		patternStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		patternNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		patternDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
		addPatternButton.disableProperty().bind(currentTrace.existsProperty().not());
		checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not());
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue) {
				checkMachineButton.disableProperty().bind(currentProject.getCurrentMachine().ltlFormulasProperty().emptyProperty());
			} else {
				checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not());
			}
		});
	}
	
	private void bindMachine(Machine machine) {
		tvFormula.itemsProperty().unbind();
		tvFormula.itemsProperty().bind(machine.ltlFormulasProperty());
		tvPattern.itemsProperty().unbind();
		tvPattern.itemsProperty().bind(machine.ltlPatternsProperty());
		tvFormula.refresh();
		tvPattern.refresh();
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
			patternParser.parsePattern(item, machine, false);
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
		if(!item.getName().equals(result.getName()) || !item.getDescription().equals(result.getDescription()) ||
			!item.getCode().equals(result.getCode())) {
			item.setData(result.getName(), result.getDescription(), result.getCode());
			currentProject.setSaved(false);
			tvFormula.refresh();
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
		if(!item.getName().equals(result.getName()) || !item.getDescription().equals(result.getDescription()) ||
				!item.getCode().equals(result.getCode())) {
			Machine machine = currentProject.getCurrentMachine();
			machine.getPatternManager().removePattern(machine.getPatternManager().getUserPattern(item.getName()));
			item.setData(result.getName(), result.getDescription(), result.getCode());
			patternParser.parsePattern(item, machine, false);
			currentProject.setSaved(false);
			tvPattern.refresh();
		}
	}
	
	private void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
				currentProject.getLocation()));
	}
	
	@FXML
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		Thread checkingThread = new Thread(() -> {
			checker.checkMachine(machine);
			Platform.runLater(() -> {
				checker.checkMachineStatus(machine);
				tvFormula.refresh();
			});
		});
		checkingThread.start();
	}
	
	private void parseMachine(Machine machine) {
		patternParser.parseMachine(machine);
	}
		


}
