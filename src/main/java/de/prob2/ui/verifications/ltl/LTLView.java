package de.prob2.ui.verifications.ltl;


import java.net.URISyntaxException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.AnimationSelector;
import de.prob2.ui.ProB2;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaChecker;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaDialog;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternDialog;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternParser;
import de.prob2.ui.project.Project;
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
	private TableView<Machine> tvMachines;
		
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
	public void initialize() throws URISyntaxException {
		helpButton.setPathToHelp(ProB2.class.getClassLoader().getResource("help/HelpMain.html").toURI().toString());
		tvFormula.setOnMouseClicked(e-> {
			LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 &&  item != null) {
				showCurrentItemDialog(item);
			}
		});
		
		tvPattern.setOnMouseClicked(e-> {
			LTLPatternItem item = tvPattern.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 &&  item != null) {
				showCurrentItemDialog(item);
			}
		});
								
		tvFormula.setRowFactory(table -> {
			final TableRow<LTLFormulaItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem("Remove formula");
			removeItem.setOnAction(e -> {
				Machine machine = tvMachines.getFocusModel().getFocusedItem();
				LTLFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
				machine.removeLTLFormula(item);
				currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
						tvMachines.getItems(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
						currentProject.getLocation()));
				currentProject.setSaved(false);
			});
			removeItem.disableProperty().bind(row.emptyProperty());
						
			MenuItem showCounterExampleItem = new MenuItem("Show Counter Example");
			showCounterExampleItem.setOnAction(e-> showCounterExample());
			showCounterExampleItem.setDisable(true);
			
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
			row.setContextMenu(new ContextMenu(removeItem, showCounterExampleItem));
			return row;
		});
		
		tvPattern.setRowFactory(table -> {
			final TableRow<LTLPatternItem> row = new TableRow<>();
			MenuItem removeItem = new MenuItem("Remove Pattern");
			removeItem.setOnAction(e -> {
				Machine machine = tvMachines.getFocusModel().getFocusedItem();
				LTLPatternItem item = tvPattern.getSelectionModel().getSelectedItem();
				machine.removeLTLPattern(item);
				patternParser.removePattern(item, machine);
				currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
						tvMachines.getItems(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
						currentProject.getLocation()));
				currentProject.setSaved(false);
			});
			removeItem.disableProperty().bind(row.emptyProperty());
			row.setContextMenu(new ContextMenu(removeItem));
			return row;
		});
		
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		
		patternStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		patternNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		patternDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
		addPatternButton.disableProperty().bind(currentTrace.existsProperty().not());
		checkSelectedMachineButton.disableProperty().bind(currentTrace.existsProperty().not());
		tvMachines.getFocusModel().focusedIndexProperty().addListener((observable, from, to) -> {
			if(to.intValue() >= 0) {
				Machine newMachine = tvMachines.getItems().get(to.intValue());
				if(from.intValue() >= 0) {
					Machine oldMachine = tvMachines.getItems().get(from.intValue());
					oldMachine.clearPatternManager();
				}
				tvFormula.itemsProperty().bind(newMachine.ltlFormulasProperty());
				tvPattern.itemsProperty().bind(newMachine.ltlPatternsProperty());
				parseMachine(newMachine);
			}
		});
	}
		
	@FXML
	public void addFormula() {
		Machine machine = tvMachines.getFocusModel().getFocusedItem();
		injector.getInstance(LTLFormulaDialog.class).showAndWait().ifPresent(item -> {
			if(!machine.getFormulas().contains(item)) {
				machine.addLTLFormula(item);
				currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
						currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
						currentProject.getLocation()));
			} else {
				showAlreadyExists(LTLItemType.Formula);
			}
		});
		tvFormula.refresh();
	}
	
	@FXML
	public void addPattern() {
		Machine machine = tvMachines.getFocusModel().getFocusedItem();
		injector.getInstance(LTLPatternDialog.class).showAndWait().ifPresent(item -> {
			if(!machine.getPatterns().contains(item)) {
				machine.addLTLPattern(item);
				currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
						currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
						currentProject.getLocation()));
				patternParser.parsePattern(item, machine, false);
			} else {
				showAlreadyExists(LTLItemType.Pattern);
			}
		});
		tvPattern.refresh();
	}
	
	private void showAlreadyExists(LTLItemType type) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(type.name() + " already exists");
		alert.setHeaderText(type.name() + " already exists");
		alert.setContentText("Declared " + type.name() + " already exists");
		alert.showAndWait();
	}
			
	public void checkFormula(LTLFormulaItem item, Machine machine) {
		checker.checkFormula(item, machine);
	}
			
	private void showCurrentItemDialog(LTLFormulaItem item) {
		LTLFormulaDialog formulaDialog = injector.getInstance(LTLFormulaDialog.class);
		formulaDialog.setData(item.getName(), item.getDescription(), item.getFormula());
		formulaDialog.showAndWait().ifPresent(result-> {
			if(!item.getName().equals(result.getName()) || !item.getDescription().equals(result.getDescription()) ||
				!item.getFormula().equals(result.getFormula())) {
				item.setData(result.getName(), result.getDescription(), result.getFormula());
				tvFormula.refresh();
				currentProject.setSaved(false);
			}
		});
		formulaDialog.clear();
	}
	
	private void showCurrentItemDialog(LTLPatternItem item) {
		LTLPatternDialog patternDialog = injector.getInstance(LTLPatternDialog.class);
		patternDialog.setData(item.getName(), item.getDescription(), item.getCode());
		patternDialog.showAndWait().ifPresent(result-> {
			if(!item.getName().equals(result.getName()) || !item.getDescription().equals(result.getDescription()) ||
					!item.getCode().equals(result.getCode())) {
				Machine machine = tvMachines.getFocusModel().getFocusedItem();
				machine.getPatternManager().removePattern(machine.getPatternManager().getUserPattern(item.getName()));
				item.setData(result.getName(), result.getDescription(), result.getCode());
				patternParser.parsePattern(item, machine, false);
				tvPattern.refresh();
				currentProject.setSaved(false);
			}
		});
		patternDialog.clear();
	}
	
	private void showCounterExample() {
		if (currentTrace.exists()) {
			this.animations.removeTrace(currentTrace.get());
		}
		animations.addNewAnimation(tvFormula.getSelectionModel().getSelectedItem().getCounterExample());
	}
	
	@FXML
	public void checkSelectedMachine() {
		checker.checkMachine(tvMachines.getFocusModel().getFocusedItem());
		tvMachines.refresh();
	}
	
	public void parseMachine(Machine machine) {
		patternParser.parseMachine(machine);
	}
	
	public void refreshFormula() {
		tvFormula.refresh();
	}
	
	public void refreshPattern() {
		tvPattern.refresh();
	}

}
