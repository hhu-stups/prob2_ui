package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.statespace.AnimationSelector;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.statusbar.StatusBar;
import de.prob2.ui.verifications.AbstractCheckableItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.MachineTableView;
import de.prob2.ui.verifications.MachineTableView.CheckingType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaDialog;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternDialog;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternParser;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
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
	
	private enum LTLItemType {
		Formula,Pattern;
	}
			
	@FXML
	private Button addFormulaButton;
	
	@FXML
	private Button addPatternButton;
	
	@FXML
	private Button checkSelectedMachineButton;

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
	
	@FXML
	private MachineTableView tvMachines;
		
	private final Injector injector;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final AnimationSelector animations;
	
	private final LTLFormulaChecker checker;
	
	private final LTLPatternParser patternParser;
				
	@Inject
	private LTLView(final StageManager stageManager, final Injector injector, final AnimationSelector animations,
					final CurrentTrace currentTrace, final CurrentProject currentProject, final LTLFormulaChecker checker,
					final LTLPatternParser patternParser) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.animations = animations;
		this.checker = checker;
		this.patternParser = patternParser;
		stageManager.loadFXML(this, "ltl_view.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent("HelpMain.html");
		tvMachines.setCheckingType(CheckingType.LTL);
		setOnItemClicked();
		setContextMenus();
		setBindings();
		tvMachines.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null) {
				if(from != null) {
					from.clearPatternManager();
				}
				bindMachine(to);
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
			MenuItem removeItem = new MenuItem("Remove formula");
			removeItem.setOnAction(e -> removeFormula());
			removeItem.disableProperty().bind(row.emptyProperty());
						
			MenuItem showCounterExampleItem = new MenuItem("Show Counter Example");
			showCounterExampleItem.setOnAction(e-> showCounterExample());
			showCounterExampleItem.setDisable(true);

			MenuItem openEditor = new MenuItem("Open in Editor");
			openEditor.setOnAction(e->showCurrentItemDialog(row.getItem()));

			MenuItem check = new MenuItem("Check separately");
			check.setOnAction(e-> {
				Machine machine = tvMachines.getSelectionModel().getSelectedItem();
				LTLFormulaItem item = row.getItem();
				Checked result = checkFormula(item, machine);
				item.setChecked(result);
				checker.checkMachineStatus(machine);
				injector.getInstance(StatusBar.class).setLtlStatus(result == Checked.SUCCESS ? StatusBar.LTLStatus.SUCCESSFUL : StatusBar.LTLStatus.ERROR);
				tvFormula.refresh();
			});

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
			MenuItem removeItem = new MenuItem("Remove Pattern");
			removeItem.setOnAction(e -> removePattern());
			removeItem.disableProperty().bind(row.emptyProperty());

			MenuItem openEditor = new MenuItem("Open in Editor");
			openEditor.setOnAction(e -> showCurrentItemDialog(row.getItem()));

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
		checkSelectedMachineButton.disableProperty().bind(currentTrace.existsProperty().not());
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(tvMachines.getSelectionModel().getSelectedIndex() < 0) {
				tvMachines.getSelectionModel().select(0);
			}
			Machine machine = tvMachines.getSelectionModel().getSelectedItem();
			if(newValue && machine != null) {
				checkSelectedMachineButton.disableProperty().bind(machine.ltlFormulasProperty().emptyProperty());
			}
		});
	}
	
	private void bindMachine(Machine machine) {
		tvFormula.itemsProperty().unbind();
		tvFormula.itemsProperty().bind(machine.ltlFormulasProperty());
		tvPattern.itemsProperty().unbind();
		tvPattern.itemsProperty().bind(machine.ltlPatternsProperty());
		if(currentTrace.existsProperty().get()) {
			checkSelectedMachineButton.disableProperty().bind(machine.ltlFormulasProperty().emptyProperty());
		}
		parseMachine(machine);
	}
		
	@FXML
	public void addFormula() {
		if(tvMachines.getSelectionModel().getSelectedIndex() < 0) {
			tvMachines.getSelectionModel().select(0);
		}
		Machine machine = tvMachines.getSelectionModel().getSelectedItem();
		LTLFormulaDialog formulaDialog = injector.getInstance(LTLFormulaDialog.class);
		loadLTLDialog(formulaDialog, null);
		formulaDialog
				.showAndWait()
				.ifPresent(item -> addFormula(machine, (LTLFormulaItem) item));
	}
	
	private void addFormula(Machine machine, LTLFormulaItem item) {
		if(!machine.getLTLFormulas().contains(item)) {
			machine.addLTLFormula(item);
			currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
					tvMachines.getItems(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
					currentProject.getLocation()));
			currentProject.setSaved(false);
		} else {
			showAlreadyExists(LTLItemType.Formula);
		}
	}
	
	private void removeFormula() {
		Machine machine = tvMachines.getSelectionModel().getSelectedItem();
		LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
		machine.removeLTLFormula(item);
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				tvMachines.getItems(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
				currentProject.getLocation()));
		currentProject.setSaved(false);
	}
	
	@FXML
	public void addPattern() {
		if(tvMachines.getSelectionModel().getSelectedIndex() < 0) {
			tvMachines.getSelectionModel().select(0);
		}
		Machine machine = tvMachines.getSelectionModel().getSelectedItem();
		LTLPatternDialog patternDialog = injector.getInstance(LTLPatternDialog.class);
		loadLTLDialog(patternDialog, null);
		patternDialog
				.showAndWait()
				.ifPresent(item -> addPattern(machine, (LTLPatternItem) item));
	}
	
	private void addPattern(Machine machine, LTLPatternItem item) {
		if(!machine.getLTLPatterns().contains(item)) {
			machine.addLTLPattern(item);
			currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
					tvMachines.getItems(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
					currentProject.getLocation()));
			patternParser.parsePattern(item, machine, false);
			currentProject.setSaved(false);
		} else {
			showAlreadyExists(LTLItemType.Pattern);
		}
	}
	
	private void removePattern() {
		Machine machine = tvMachines.getSelectionModel().getSelectedItem();
		LTLPatternItem item = tvPattern.getSelectionModel().getSelectedItem();
		machine.removeLTLPattern(item);
		patternParser.removePattern(item, machine);
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				tvMachines.getItems(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
				currentProject.getLocation()));
		currentProject.setSaved(false);
	}
	
	private void showAlreadyExists(LTLItemType type) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(type.name() + " already exists");
		alert.setHeaderText(type.name() + " already exists");
		alert.setContentText("Declared " + type.name() + " already exists");
		alert.showAndWait();
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
			Machine machine = tvMachines.getSelectionModel().getSelectedItem();
			machine.getPatternManager().removePattern(machine.getPatternManager().getUserPattern(item.getName()));
			item.setData(result.getName(), result.getDescription(), result.getCode());
			patternParser.parsePattern(item, machine, false);
			currentProject.setSaved(false);
			tvPattern.refresh();
		}
	}
	
	private void showCounterExample() {
		if (currentTrace.exists()) {
			this.animations.removeTrace(currentTrace.get());
		}
		animations.addNewAnimation(tvFormula.getSelectionModel().getSelectedItem().getCounterExample());
	}
	
	@FXML
	public void checkSelectedMachine() {
		Machine machine = tvMachines.getSelectionModel().getSelectedItem();
		checker.checkMachine(machine);
		checker.checkMachineStatus(machine);
		tvMachines.refresh();
		tvFormula.refresh();
	}
	
	public void parseMachine(Machine machine) {
		patternParser.parseMachine(machine);
	}
		


}
