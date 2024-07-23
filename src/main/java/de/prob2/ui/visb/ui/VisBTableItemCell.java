package de.prob2.ui.visb.ui;

import java.util.Map;

import com.google.inject.Injector;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob.animator.domainobjects.VisBItem;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visb.VisBView;
import de.prob2.ui.visb.VisBTableItem;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;

public final class VisBTableItemCell extends TableCell<VisBTableItem, VisBItem> {
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

	private final I18n i18n;

	private final Injector injector;

	private final Map<String, VisBEvent> eventsById;

	private final ObservableMap<VisBItem.VisBItemKey, String> attributeValues;

	public VisBTableItemCell(final StageManager stageManager, final I18n i18n, final Injector injector, final Map<String, VisBEvent> eventsById, final ObservableMap<VisBItem.VisBItemKey, String> attributeValues) {
		this.i18n = i18n;
		this.injector = injector;
		this.eventsById = eventsById;
		this.attributeValues = attributeValues;

		stageManager.loadFXML(this, "list_view_item.fxml");
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.itemBox);
		this.hoverProperty().addListener((observable, from, to) -> {
			if (!this.isEmpty()) {
				String id = this.getItem().getId();
				if(eventsById.containsKey(id)) {
					for (VisBHover hover : eventsById.get(id).getHovers()) {
						injector.getInstance(VisBView.class).changeAttribute(hover.getHoverID(), hover.getHoverAttr(), to ? hover.getHoverEnterVal() : hover.getHoverLeaveVal());
					}
				}
			}
		});
	}

	@Override
	protected void updateItem(VisBItem item, boolean empty){
		super.updateItem(item, empty);

		if (!empty) {
			this.lbID.setText(item.getId());
			this.lbAttribute.setText(i18n.translate("visb.item.attribute", item.getAttribute()));
			this.lbExpression.setText(i18n.translate("visb.item.expression", item.getExpression()));
			final StringExpression valueBinding = Bindings.stringValueAt(this.attributeValues, item.getKey());
			this.lbValue.textProperty().bind(i18n.translateBinding("visb.item.value",
					Bindings.when(valueBinding.isNull())
							.then(i18n.translateBinding("visb.item.value.notInitialized"))
							.otherwise(i18n.translateBinding("common.quoted", valueBinding))
			));
			this.setGraphic(this.itemBox);
			this.setText("");
		} else {
			this.lbID.setText("");
			this.lbAttribute.setText("");
			this.lbExpression.setText("");
			this.lbValue.textProperty().unbind();
			this.lbValue.setText("");
			this.setGraphic(this.itemBox);
			this.setText("");
		}
	}
}
