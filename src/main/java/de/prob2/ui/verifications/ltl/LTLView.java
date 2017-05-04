package de.prob2.ui.verifications.ltl;


import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.be4.ltl.core.parser.LtlParseException;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.command.EvaluationCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.LTL;
import de.prob.check.LTLCounterExample;
import de.prob.check.LTLError;
import de.prob.check.LTLOk;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

@Singleton
public class LTLView extends AnchorPane{
	
	public enum Checked {
		SUCCESS, FAIL;
	}
	
	private static final Logger logger = LoggerFactory.getLogger(LTLView.class);
	
	@FXML
	private TableView<LTLFormulaItem> tvFormula;
	
	@FXML
	private Button addLTLButton;
	
	@FXML
	private Button checkAllButton;
	
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> statusColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> nameColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> descriptionColumn;
	
	@FXML
	private TableView<Machine> tvMachines;
	
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> machineStatusColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> machineNameColumn;	
	
	private final Injector injector;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;
	
	private final AnimationSelector animations;
		
	@Inject
	private LTLView(final StageManager stageManager, final Injector injector, final AnimationSelector animations,
					final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.animations = animations;
		stageManager.loadFXML(this, "ltl_view.fxml");
	}
	
	@FXML
	public void initialize() {		
		tvFormula.setOnMouseClicked(e-> {
			if(e.getClickCount() == 2 && tvFormula.getSelectionModel().getSelectedItem() != null) {
				showCurrentItemDialog();
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
						currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
						currentProject.getLocation()));
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
		
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		machineStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		machineNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		addLTLButton.disableProperty().bind(currentTrace.existsProperty().not());
		checkAllButton.disableProperty().bind(currentTrace.existsProperty().not());
		tvMachines.itemsProperty().bind(currentProject.machinesProperty());
		tvMachines.getFocusModel().focusedIndexProperty().addListener((observable, from, to) -> {
			if(to.intValue() >= 0) {
				tvFormula.itemsProperty().bind(tvMachines.getItems().get(to.intValue()).ltlFormulasProperty());
			}
		});
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
	}
	
	public Checked checkFormula(LTLFormulaItem item) {
		LTL formula = null;
		Checked checked = Checked.SUCCESS;
		try {
			formula = new LTL(item.getFormula());
			if (currentTrace != null) {
				State stateid = currentTrace.getCurrentState();
				EvaluationCommand lcc = formula.getCommand(stateid);
				currentTrace.getStateSpace().execute(lcc);
				AbstractEvalResult result = lcc.getValue();
				if(result instanceof LTLOk) {
					showSuccess(item);
				} else if(result instanceof LTLCounterExample) {
					showCounterExampleFound(item, ((LTLCounterExample) result).getTrace(stateid.getStateSpace()));
					checked = Checked.FAIL;
				} else if(result instanceof LTLError) {
					showError(item, (LTLError) result);
					checked = Checked.FAIL;
				}
			}
		} catch (LtlParseException e) {
			showParseError(item, e);
			checked = Checked.FAIL;
			logger.error("Could not parse LTL formula", e);
		}
		tvFormula.refresh();
		return checked;
	}
	
	private void showParseError(LTLFormulaItem item, LtlParseException e) {
		TextArea exceptionText = new TextArea();
		Alert alert = new Alert(AlertType.ERROR, "Message: ");
		alert.getDialogPane().getStylesheets().add(getClass().getResource("/prob.css").toExternalForm());
		alert.setTitle(item.getName());
		alert.setHeaderText("Could not parse LTL formula");
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			e.printStackTrace(pw);
			exceptionText.setText(sw.toString());
			exceptionText.setEditable(false);
			exceptionText.getStyleClass().add("text-area-error");
		}
		StackPane pane = new StackPane(exceptionText);
		pane.setPrefSize(320, 120);
		alert.getDialogPane().setExpandableContent(pane);
		alert.getDialogPane().setExpanded(true);
		alert.showAndWait();
		item.setCheckedFailed();
		item.setCounterExample(null);
	}
	
	private void showError(LTLFormulaItem item, LTLError error) {
		Alert alert = new Alert(AlertType.ERROR, error.getMessage());
		alert.setTitle(item.getName());
		alert.setHeaderText("Error while executing formula");
		alert.showAndWait();
		item.setCheckedFailed();
		item.setCounterExample(null);
	}
	
	private void showSuccess(LTLFormulaItem item) {
		Alert alert = new Alert(AlertType.INFORMATION, "LTL Check succeeded");
		alert.setTitle(item.getName());
		alert.setHeaderText("Success");
		alert.showAndWait();
		item.setCheckedSuccessful();
		item.setCounterExample(null);
	}
	
	private void showCounterExampleFound(LTLFormulaItem item, Trace trace) {
		Alert alert = new Alert(AlertType.ERROR, "LTL Counter Example has been found");
		alert.setTitle(item.getName());
		alert.setHeaderText("Counter Example found");
		alert.showAndWait();
		item.setCheckedFailed();
		item.setCounterExample(trace);
	}
	
	private void showCurrentItemDialog() {
		if(tvFormula.getSelectionModel().getSelectedItem().showAndRegisterChange()) {
			tvFormula.refresh();
			currentProject.setSaved(false);
		}
	}
	
	private void showCounterExample() {
		if (currentTrace.exists()) {
			this.animations.removeTrace(currentTrace.get());
		}
		animations.addNewAnimation(tvFormula.getSelectionModel().getSelectedItem().getCounterExample());
	}
	
	@FXML
	public void checkAll() {
		tvFormula.getItems().forEach(item-> {
			if(this.checkFormula(item) == Checked.FAIL) {
				tvMachines.getFocusModel().getFocusedItem().setCheckedFailed();
			};
		});
		tvMachines.refresh();
	}

}
