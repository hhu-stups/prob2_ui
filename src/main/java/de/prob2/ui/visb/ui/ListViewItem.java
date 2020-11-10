package de.prob2.ui.visb.ui;

import com.google.inject.Injector;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.visbobjects.VisBItem;
import de.prob2.ui.visb.VisBStage;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

import static de.prob2.ui.internal.JavascriptFunctionInvoker.buildInvocation;
import static de.prob2.ui.internal.JavascriptFunctionInvoker.wrapAsString;

public class ListViewItem extends ListCell<VisBItem> {
	@FXML
	private VBox item_box;
	@FXML
	private Label item_id;
	@FXML
	private Label label_attr;
	@FXML
	private Label label_value;
	@FXML
	private Label label_evalValue;

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
		this.setGraphic(this.item_box);
		this.hoverProperty().addListener((observable, from, to) -> {
			if(visBItem != null) {
				if (to) {
					String toID = visBItem.getId();
					String toInvocation = buildInvocation("changeAttribute", wrapAsString("#" + toID), wrapAsString("opacity"), wrapAsString("0.5"));
					injector.getInstance(VisBStage.class).runScript(toInvocation);
				} else {
					String fromID = visBItem.getId();
					String fromInvocation = buildInvocation("changeAttribute", wrapAsString("#" + fromID), wrapAsString("opacity"), wrapAsString("1.0"));
					injector.getInstance(VisBStage.class).runScript(fromInvocation);
				}
			}
		});
	}

	@Override
	protected void updateItem(final VisBItem visBItem, final boolean empty){
		super.updateItem(visBItem, empty);
		this.visBItem = visBItem;
		if(visBItem != null) {
			this.item_id.setText(visBItem.getId());
			this.label_value.setText(String.format(bundle.getString("visb.item.expression"), visBItem.getValue()));
			this.label_attr.setText(String.format(bundle.getString("visb.item.attribute"), visBItem.getAttribute()));
			if(visBItem.parsedFormula != null) {
				AbstractEvalResult result = currentTrace.getCurrentState().eval(visBItem.parsedFormula);
				this.label_evalValue.setText(String.format(bundle.getString("visb.item.value"), result.toString()));
			} else {
				this.label_evalValue.setText("");
			}
			this.setGraphic(this.item_box);
			this.setText("");
		} else {
			clear();
		}
	}

	public void clear() {
		this.item_id.setText("");
		this.label_value.setText("");
		this.label_attr.setText("");
		this.label_evalValue.setText("");
		this.setGraphic(this.item_box);
		this.setText("");
	}
}
