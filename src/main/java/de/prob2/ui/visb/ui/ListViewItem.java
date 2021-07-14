package de.prob2.ui.visb.ui;

import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visb.VisBStage;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

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

	private final ResourceBundle bundle;

	private final Injector injector;

	private final Map<String, VisBEvent> eventsById;

	private final ObservableMap<VisBItem.VisBItemKey, String> attributeValues;

	public ListViewItem(final StageManager stageManager, final ResourceBundle bundle, final Injector injector, final Map<String, VisBEvent> eventsById, final ObservableMap<VisBItem.VisBItemKey, String> attributeValues) {
		stageManager.loadFXML(this,"list_view_item.fxml");
		this.visBItem = null;
		this.bundle = bundle;
		this.injector = injector;
		this.eventsById = eventsById;
		this.attributeValues = attributeValues;
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
						injector.getInstance(VisBStage.class).changeAttribute(hover.getHoverID(), hover.getHoverAttr(), to ? hover.getHoverEnterVal() : hover.getHoverLeaveVal());
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
			final StringExpression valueBinding = Bindings.stringValueAt(this.attributeValues, visBItem.getKey());
			this.lbValue.textProperty().bind(Bindings.format(
				bundle.getString("visb.item.value"),
				Bindings.when(valueBinding.isNull())
					.then("not initialised")
					.otherwise(Bindings.format("\"%s\"", valueBinding))
			));
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
		this.lbValue.textProperty().unbind();
		this.lbValue.setText("");
		this.setGraphic(this.itemBox);
		this.setText("");
	}

}
