package de.prob2.ui.visb.ui;

import java.util.Map;
import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visb.VisBStage;

import de.prob2.ui.visb.VisBTableItem;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;

public class VisBTableItemCell extends TableCell<VisBTableItem, String> {
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

	public VisBTableItemCell(final StageManager stageManager, final ResourceBundle bundle, final Injector injector, final Map<String, VisBEvent> eventsById, final ObservableMap<VisBItem.VisBItemKey, String> attributeValues) {
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
	protected void updateItem(final String visBItem, final boolean empty){
		super.updateItem(visBItem, empty);
		// This cast is needed with JavaFX 8,
		// where TableCell.getTableRow doesn't have a generic return type.
		@SuppressWarnings({"cast", "RedundantCast"})
		final VisBTableItem item = (VisBTableItem)this.getTableRow().getItem();
		if (item == null) {
			return;
		}
		this.visBItem = item.getVisBItem();
		if(this.visBItem != null) {
			this.lbID.setText(this.visBItem.getId());
			this.lbAttribute.setText(String.format(bundle.getString("visb.item.attribute"), this.visBItem.getAttribute()));
			this.lbExpression.setText(String.format(bundle.getString("visb.item.expression"), this.visBItem.getExpression()));
			final StringExpression valueBinding = Bindings.stringValueAt(this.attributeValues, this.visBItem.getKey());
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

	public void setVisBItem(VisBItem visBItem) {
		this.visBItem = visBItem;
	}
}
