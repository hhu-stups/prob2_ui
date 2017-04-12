package de.prob2.ui.verifications.ltl;



import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
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
			if(e.getClickCount() == 2) {
				if(tv_formula.getSelectionModel().getSelectedItem() != null) {
					tv_formula.getSelectionModel().getSelectedItem().show();
				}
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
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(AddLTLFormulaDialog.class).showAndWait().ifPresent(tv_formula.getItems()::add);
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
	
	public List<LTLFormulaItem> getFormulas() {
		return tv_formula.getItems();
	}

}
