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
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
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
	
	private static final Logger logger = LoggerFactory.getLogger(LTLView.class);
	
	@FXML
	private TableView<LTLFormulaItem> tv_formula;
	
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
		tv_formula.setOnMouseClicked(e-> {
			if(e.getClickCount() == 2 && tv_formula.getSelectionModel().getSelectedItem() != null) {
				tv_formula.getSelectionModel().getSelectedItem().show();
			}

		});
						
		tv_formula.setRowFactory(table -> {
			final TableRow<LTLFormulaItem> row = new TableRow<>();
			
			MenuItem removeItem = new MenuItem("Remove formula");
			removeItem.setOnAction(e-> {
				LTLFormulaItem item = tv_formula.getSelectionModel().getSelectedItem();
				currentProject.removeLTLFormula(item);
			});
			removeItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem renameItem = new MenuItem("Rename formula");
			renameItem.setOnAction(e-> {
				LTLFormulaItem item = tv_formula.getSelectionModel().getSelectedItem();
				AddLTLFormulaDialog formulaDialog = injector.getInstance(AddLTLFormulaDialog.class);
				formulaDialog.setName(item.getName());
				formulaDialog.setDescription(item.getDescription());
				formulaDialog.showAndWait().ifPresent(result-> {
					if(!item.getName().equals(result.getName()) || !item.getDescription().equals(result.getDescription())) {
						item.setName(result.getName());
						item.setDescription(result.getDescription());
						refresh();
						currentProject.setSaved(false);
					}
				});
			});
			renameItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem showCounterExampleItem = new MenuItem("Show Counter Example");
			showCounterExampleItem.setOnAction(e-> {
				LTLFormulaItem item = tv_formula.getSelectionModel().getSelectedItem();
				if (currentTrace.exists()) {
					this.animations.removeTrace(currentTrace.get());
				}
				animations.addNewAnimation(item.getCounterExample());
			});
			showCounterExampleItem.setDisable(true);
			
			row.setOnMouseClicked(e-> {
				if(e.getButton() == MouseButton.SECONDARY) {
					LTLFormulaItem item = tv_formula.getSelectionModel().getSelectedItem();
					if(row.emptyProperty().get() || item.getCounterExample() == null) {
						showCounterExampleItem.setDisable(true);
					} else {
						showCounterExampleItem.setDisable(false);
					}
				}
			});
			
			row.setContextMenu(new ContextMenu(removeItem,renameItem, showCounterExampleItem));
			return row;
		});
		
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		addLTLButton.disableProperty().bind(currentTrace.existsProperty().not());
		checkAllButton.disableProperty().bind(currentTrace.existsProperty().not());
		tv_formula.itemsProperty().bind(currentProject.ltlFormulasProperty());	
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(AddLTLFormulaDialog.class).showAndWait().ifPresent(currentProject::addLTLFormula);
	}
	
	public void checkFormula(LTLFormulaItem item) {
		LTL formula = null;
		try {
			formula = new LTL(item.getFormula());
			if (currentTrace != null) {
				State stateid = currentTrace.getCurrentState();
				EvaluationCommand lcc = formula.getCommand(stateid);
				currentTrace.getStateSpace().execute(lcc);
				AbstractEvalResult result = lcc.getValue();
				
				if(result instanceof LTLOk) {
					showSuccess(item);
					item.setCheckedSuccessful();
					item.setCounterExample(null);
				} else if(result instanceof LTLCounterExample) {
					showCounterExampleFound(item);
					item.setCheckedFailed();
					item.setCounterExample(currentTrace.get());
				} else if(result instanceof LTLError) {
					showError(item, (LTLError) result);
					item.setCheckedFailed();
					item.setCounterExample(null);
				}
			}
		} catch (LtlParseException e) {
			showParseError(item, e);
			item.setCheckedFailed();
			item.setCounterExample(null);
			logger.error("Could not parse LTL formula", e);
		}
		refresh();
	}
	
	private void showParseError(LTLFormulaItem item, LtlParseException e) {
		TextArea exceptionText = new TextArea();
		Alert alert = new Alert(AlertType.ERROR);
		alert.getDialogPane().getStylesheets().add(getClass().getResource("/prob.css").toExternalForm());
		alert.setTitle(item.getName());
		alert.setHeaderText("Could not parse LTL formula");
		alert.setContentText("Message: ");
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
	}
	
	private void showError(LTLFormulaItem item, LTLError error) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(item.getName());
		alert.setHeaderText("Error while executing formula");
		alert.setContentText(error.getMessage());
		alert.showAndWait();
	}
	
	private void showSuccess(LTLFormulaItem item) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(item.getName());
		alert.setHeaderText("Success");
		alert.setContentText("LTL Check succeeded");
		alert.showAndWait();
	}
	
	private void showCounterExampleFound(LTLFormulaItem item) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(item.getName());
		alert.setHeaderText("Counter Example found");
		alert.setContentText("LTL Counter Example has been found");
		alert.showAndWait();
	}
	
	@FXML
	public void checkAll() {
		for(LTLFormulaItem item : tv_formula.getItems()) {
			item.checkFormula();
		}
	}
	
	public void refresh() {
		tv_formula.refresh();
	}
	
	public TableView<LTLFormulaItem> getTable() {
		return tv_formula;
	}
	

}
