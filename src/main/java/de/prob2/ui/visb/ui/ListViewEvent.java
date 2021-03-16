package de.prob2.ui.visb.ui;


import com.google.inject.Injector;
import de.prob.animator.domainobjects.VisBEvent;
import de.prob.animator.domainobjects.VisBHover;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.visb.VisBStage;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

import static de.prob2.ui.internal.JavascriptFunctionInvoker.buildInvocation;
import static de.prob2.ui.internal.JavascriptFunctionInvoker.wrapAsString;

public class ListViewEvent extends ListCell<VisBEvent> {
	@FXML
	private VBox eventBox;
	@FXML
	private Label lbID;
	@FXML
	private Label lbEvent;
	@FXML
	private Label lbPredicates;

	private VisBEvent visBEvent;

	private final ResourceBundle bundle;

	private final Injector injector;

	public ListViewEvent(final StageManager stageManager, final ResourceBundle bundle, final Injector injector) {
		stageManager.loadFXML(this,"list_view_event.fxml");
		this.visBEvent = null;
		this.bundle = bundle;
		this.injector = injector;
	}

	@FXML
	public void initialize(){
		this.setText("");
		this.setGraphic(this.eventBox);
		this.getStyleClass().add("visb-item");
		this.hoverProperty().addListener((observable, from, to) -> {
			if(visBEvent != null) {
				String id = visBEvent.getId();
				for (VisBHover hover : visBEvent.getHovers()) {
					String invocation = buildInvocation("changeAttribute", wrapAsString(hover.getHoverID()), wrapAsString(hover.getHoverAttr()), to ? wrapAsString(hover.getHoverEnterVal()) : wrapAsString(hover.getHoverLeaveVal()));
					injector.getInstance(VisBStage.class).runScript(invocation);
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
			lbEvent.setText(String.format(bundle.getString("visb.event.event"), visBEvent.getEvent()));
			lbPredicates.setText(String.format(bundle.getString("visb.event.predicates"), visBEvent.getPredicates().toString()));
			this.setGraphic(this.eventBox);
			this.setText("");
		} else {
			clear();
		}
	}

	public void clear() {
		this.lbID.setText("");
		this.lbEvent.setText("");
		this.lbPredicates.setText("");
		this.setGraphic(this.eventBox);
		this.setText("");
	}

}
