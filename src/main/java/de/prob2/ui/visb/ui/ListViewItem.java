package de.prob2.ui.visb.ui;

import com.google.inject.Injector;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.visb.VisBStage;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.ResourceBundle;

import static de.prob2.ui.internal.JavascriptFunctionInvoker.buildInvocation;
import static de.prob2.ui.internal.JavascriptFunctionInvoker.wrapAsString;

public class ListViewItem extends ListCell<VisBItem> {
	@FXML
	private VBox itemBox;
	@FXML
	private Label lbID;
	@FXML
	private Label lbExpression;
	@FXML
	private Label lbAttribute;
	@FXML
	private Label lbValue;

	private VisBItem visBItem;

	private final CurrentTrace currentTrace;

	private final ResourceBundle bundle;

	private final Injector injector;

	private final Map<String, VisBEvent> eventsById;

	public ListViewItem(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle, final Injector injector, final Map<String, VisBEvent> eventsById){
		stageManager.loadFXML(this,"list_view_item.fxml");
		this.visBItem = null;
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		this.injector = injector;
		this.eventsById = eventsById;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.itemBox);
		this.hoverProperty().addListener((observable, from, to) -> {
			if(visBItem != null) {
				String id = visBItem.getId();
				if(eventsById.containsKey(id)) {
					for (VisBHover hover : eventsById.get(id).getHovers()) {
						String invocation = buildInvocation("changeAttribute", wrapAsString(hover.getHoverID()), wrapAsString(hover.getHoverAttr()), to ? wrapAsString(hover.getHoverEnterVal()) : wrapAsString(hover.getHoverLeaveVal()));
						injector.getInstance(VisBStage.class).runScript(invocation);
					}
				}
			}
		});
	}

	@Override
	protected void updateItem(final VisBItem visBItem, final boolean empty){
		super.updateItem(visBItem, empty);
		this.visBItem = visBItem;
		if(visBItem != null) {
			this.lbID.setText(visBItem.getId());
			this.lbAttribute.setText(String.format(bundle.getString("visb.item.attribute"), visBItem.getAttribute()));
			this.lbExpression.setText(String.format(bundle.getString("visb.item.expression"), visBItem.getExpression()));
			if(currentTrace.isNotNull().get() && currentTrace.getCurrentState() != null) {
				if(visBItem.getValue() == null) {
					this.lbValue.setText(String.format(bundle.getString("visb.item.value"), "not initialised"));
				} else {
					this.lbValue.setText(String.format(bundle.getString("visb.item.value"), visBItem.getValue()));
				}
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
		this.lbAttribute.setText("");
		this.lbExpression.setText("");
		this.lbValue.setText("");
		this.setGraphic(this.itemBox);
		this.setText("");
	}

}
