package de.prob2.ui.verifications.ltl;


import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Token;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.ltl.parser.WarningListener;
import de.prob.ltl.parser.pattern.Pattern;
import de.prob.ltl.parser.pattern.PatternManager;
import de.prob.statespace.AnimationSelector;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.patterns.LTLParseListener;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternDialog;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternParser;
import de.prob2.ui.project.Project;
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
	
	private final PatternManager patternManager;	
			
	@Inject
	private LTLView(final StageManager stageManager, final Injector injector, final AnimationSelector animations,
					final CurrentTrace currentTrace, final CurrentProject currentProject, final LTLFormulaChecker checker,
					final LTLPatternParser patternParser, final PatternManager patternManager) {
		this.patternManager = patternManager;
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
		helpButton.setPathToHelp("https://www3.hhu.de/stups/prob/index.php/The_ProB_Animator_and_Model_Checker");
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
				tvFormula.itemsProperty().bind(tvMachines.getItems().get(to.intValue()).ltlFormulasProperty());
				tvPattern.itemsProperty().bind(tvMachines.getItems().get(to.intValue()).ltlPatternsProperty());
				patternManager.getPatterns().clear();
				patternManager.getPatterns().addAll(itemsToPatterns(tvMachines.getItems().get(to.intValue()).getPatterns()));
			}
		});
	}
	
	private ArrayList<Pattern> itemsToPatterns(List<LTLPatternItem> patterns) {
		ArrayList<Pattern> result = new ArrayList<>();
		for(LTLPatternItem item : patterns) {
			Pattern pattern = new Pattern();
			pattern.setBuiltin(false);
			pattern.setName(item.getName());
			pattern.setDescription(item.getDescription());
			pattern.setCode(item.getCode());
			result.add(pattern);
		}
		return result;
	}
		
	@FXML
	public void addFormula() {
		Machine machine = tvMachines.getFocusModel().getFocusedItem();
		injector.getInstance(LTLFormulaDialog.class).showAndWait().ifPresent(item -> {
			machine.addLTLFormula(item);
			currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
					currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
					currentProject.getLocation()));
		});
		tvFormula.refresh();
	}
	
	@FXML
	public void addPattern() {
		Machine machine = tvMachines.getFocusModel().getFocusedItem();
		injector.getInstance(LTLPatternDialog.class).showAndWait().ifPresent(item -> {
			machine.addLTLPattern(item);
			currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
					currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
					currentProject.getLocation()));
			addPattern(item);
		});
		tvFormula.refresh();
	}
	
	private void addPattern(LTLPatternItem item) {
		if(!patternManager.patternExists(item.getName())) {
			Pattern pattern = new Pattern();
			pattern.setBuiltin(false);
			pattern.setName(item.getName());
			pattern.setDescription(item.getDescription());
			pattern.setCode(item.getCode());
			patternManager.getPatterns().add(pattern);
			parsePattern(pattern);
		}
	}
	
	private void parsePattern(Pattern pattern) {
		LTLParseListener parseListener = new LTLParseListener();
		pattern.removeErrorListeners();
		pattern.removeWarningListeners();
		pattern.removeUpdateListeners();
		pattern.addErrorListener(parseListener);
		pattern.addWarningListener(parseListener);
		pattern.addUpdateListener(parseListener);
		pattern.updateDefinitions(patternManager);
	}
	
	public void checkFormula(LTLFormulaItem item) {
		checker.checkFormula(item);
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
				item.setData(result.getName(), result.getDescription(), result.getCode());
				tvPattern.refresh();
				currentProject.setSaved(false);
			}
			patternManager.removePattern(patternManager.getUserPattern(item.getName()));
			addPattern(item);
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
	
	public void refreshFormula() {
		tvFormula.refresh();
	}

}
