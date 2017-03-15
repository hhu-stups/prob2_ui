package de.prob2.ui.verifications.ltl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;

@Singleton
public class LTLView extends AnchorPane{
	
	@FXML
	private ListView<LTLFormulaItem> lv_formula;
	
	private final Injector injector;
	
	@Inject
	public LTLView(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
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
		
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(AddLTLFormulaDialog.class).showAndWait().ifPresent(lv_formula.getItems()::add);
	}

}
