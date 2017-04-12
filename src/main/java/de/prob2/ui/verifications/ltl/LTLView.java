package de.prob2.ui.verifications.ltl;




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
import de.prob.statespace.State;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

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
	
	private CurrentTrace currentTrace;
		
	@Inject
	private LTLView(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace) {
		this.injector = injector;
		this.currentTrace = currentTrace;
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
				tv_formula.getItems().remove(item);
			});
			removeItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem renameItem = new MenuItem("Rename formula");
			renameItem.setOnAction(e-> {
				LTLFormulaItem item = tv_formula.getSelectionModel().getSelectedItem();
				AddLTLFormulaDialog formulaDialog = injector.getInstance(AddLTLFormulaDialog.class);
				formulaDialog.setName(item.getName());
				formulaDialog.setDescription(item.getDescription());
				formulaDialog.showAndWait().ifPresent(result-> {
					item.setName(result.getName());
					item.setDescription(result.getDescription());
				});
				refresh();
			});
			renameItem.disableProperty().bind(row.emptyProperty());
			
			row.setContextMenu(new ContextMenu(removeItem,renameItem));
			return row;
		});
		
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		addLTLButton.disableProperty().bind(currentTrace.existsProperty().not());
		checkAllButton.disableProperty().bind(currentTrace.existsProperty().not());
		
		tv_formula.itemsProperty().addListener((observable, oldValue, newValue) -> {
			System.out.println("test");
		});
		
		
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(AddLTLFormulaDialog.class).showAndWait().ifPresent(tv_formula.getItems()::add);
	}
	
	public void checkFormula(LTLFormulaItem item) {
		LTL formula = null;
		try {
			formula = new LTL(item.getFormula());
		} catch (LtlParseException e) {
			item.setCheckedFailed();
			logger.error("Could not parse LTL formula", e);
			//TODO: show ParseError
		}
		if (currentTrace != null && formula != null) {
			State stateid = currentTrace.getCurrentState();
			EvaluationCommand lcc = formula.getCommand(stateid);
			currentTrace.getStateSpace().execute(lcc);
			AbstractEvalResult result = lcc.getValue();
			if(result instanceof LTLOk) {
				item.setCheckedSuccessful();
			} else if(result instanceof LTLCounterExample) {
				System.out.println(((LTLCounterExample) result).getMessage());
				item.setCheckedFailed();
				//TODO: case CounterExample
			} else if(result instanceof LTLError) {
				System.out.println(((LTLError) result).getMessage());
				item.setCheckedFailed();
			}
		}
		refresh();
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
