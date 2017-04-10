package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;

@Singleton
public class LTLView extends AnchorPane{
	
	@FXML
	private ListView<LTLFormulaItem> lv_formula;
	
	@FXML
	private Button addLTLButton;
	
	private final Injector injector;
	
	private CurrentTrace currentTrace;
	
	@Inject
	private LTLView(final StageManager stageManager, final Injector injector, final CurrentTrace currentTrace) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "ltlView.fxml");
	}
	
	@FXML
	public void initialize() {
		lv_formula.setOnMouseClicked(e-> {
			if(e.getClickCount() == 2) {
				if(lv_formula.getSelectionModel().getSelectedItem() != null) {
					lv_formula.getSelectionModel().getSelectedItem().show();
				}
			}
		});
		addLTLButton.disableProperty().bind(currentTrace.existsProperty().not());
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(AddLTLFormulaDialog.class).showAndWait().ifPresent(lv_formula.getItems()::add);
	}

}
