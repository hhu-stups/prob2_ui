package de.prob2.ui.visb.ui;

import com.google.inject.Injector;

import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visb.VisBView;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

public final class ListViewEvent extends ListCell<VisBEvent> {
	@FXML
	private VBox eventBox;
	@FXML
	private Label lbID;
	@FXML
	private Label lbEvent;
	@FXML
	private Label lbPredicates;

	private VisBEvent visBEvent;

	private final I18n i18n;

	private final Injector injector;

	public ListViewEvent(final StageManager stageManager, final I18n i18n, final Injector injector) {
		stageManager.loadFXML(this,"list_view_event.fxml");
		this.visBEvent = null;
		this.i18n = i18n;
		this.injector = injector;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.eventBox);
		this.getStyleClass().add("visb-item");
		this.hoverProperty().addListener((observable, from, to) -> {
			if(visBEvent != null) {
				for (VisBHover hover : visBEvent.getHovers()) {
					injector.getInstance(VisBView.class).changeAttribute(hover.getHoverID(), hover.getHoverAttr(), to ? hover.getHoverEnterVal() : hover.getHoverLeaveVal());
				}
			}
		});
	}

	@Override
	protected void updateItem(final VisBEvent visBEvent, final boolean empty){
		super.updateItem(visBEvent, empty);
		this.visBEvent = visBEvent;
		if(visBEvent != null){
			lbID.setText(visBEvent.getId());
			lbEvent.setText(i18n.translate("visb.event.event", visBEvent.getEvent()));
			lbPredicates.setText(i18n.translate("visb.event.predicates", visBEvent.getPredicates().toString()));
			this.setGraphic(this.eventBox);
			this.setText("");
		} else {
			this.lbID.setText("");
			this.lbEvent.setText("");
			this.lbPredicates.setText("");
			this.setGraphic(this.eventBox);
			this.setText("");
		}
	}
}
