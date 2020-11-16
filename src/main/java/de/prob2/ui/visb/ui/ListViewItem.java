package de.prob2.ui.visb.ui;

import com.google.inject.Injector;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.VisBStage;
import de.prob2.ui.visb.visbobjects.VisBItem;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

import static de.prob2.ui.internal.JavascriptFunctionInvoker.buildInvocation;
import static de.prob2.ui.internal.JavascriptFunctionInvoker.wrapAsString;

public class ListViewItem extends ListCell<VisBItem> {
	@FXML
	private VBox itemBox;
	@FXML
	private Label lbID;
	@FXML
	private Label lbAttribute;
	@FXML
	private Label lbExpression;
	@FXML
	private Label lbValue;

	private VisBItem visBItem;

	private final CurrentTrace currentTrace;

	private final ResourceBundle bundle;

	private final Injector injector;

	public ListViewItem(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle, final Injector injector){
		stageManager.loadFXML(this,"list_view_item.fxml");
		this.visBItem = null;
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		this.injector = injector;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.itemBox);
		this.hoverProperty().addListener((observable, from, to) -> {
			if(visBItem != null) {
				String id = visBItem.getId();
				String invocation = buildInvocation("changeAttribute", wrapAsString("#" + id), wrapAsString("opacity"), to ? wrapAsString("0.5") : wrapAsString("1.0"));
				injector.getInstance(VisBStage.class).runScript(invocation);
			}
		});
	}

	@Override
	protected void updateItem(final VisBItem visBItem, final boolean empty){
		super.updateItem(visBItem, empty);
		this.visBItem = visBItem;
		if(visBItem != null) {
			this.lbID.setText(visBItem.getId());
			this.lbExpression.setText(String.format(bundle.getString("visb.item.expression"), visBItem.getValue()));
			this.lbAttribute.setText(String.format(bundle.getString("visb.item.attribute"), visBItem.getAttribute()));
			if(visBItem.parsedFormula != null && currentTrace.isNotNull().get() && currentTrace.getCurrentState() != null) {
				AbstractEvalResult result = currentTrace.getCurrentState().eval(visBItem.parsedFormula);
				this.lbValue.setText(String.format(bundle.getString("visb.item.value"), result.toString()));
			} else {
				this.lbValue.setText("");
			}
			this.setGraphic(this.itemBox);
			this.setText("");
		} else {
			clear();
		}
	}

	public void clear() {
		this.lbID.setText("");
		this.lbExpression.setText("");
		this.lbAttribute.setText("");
		this.lbValue.setText("");
		this.setGraphic(this.itemBox);
		this.setText("");
	}

}
