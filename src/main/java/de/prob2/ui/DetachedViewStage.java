package de.prob2.ui;

import de.prob2.ui.internal.StageManager;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

final class DetachedViewStage extends Stage {
	private final Node detachedView;
	private final TitledPane sourceTitledPane;
	private final Accordion sourceAccordion;
	
	DetachedViewStage(final StageManager stageManager, final Node detachedView, final TitledPane sourceTitledPane, final Accordion sourceAccordion) {
		super();
		
		this.detachedView = detachedView;
		this.sourceTitledPane = sourceTitledPane;
		this.sourceAccordion = sourceAccordion;
		
		this.setScene(new Scene(new StackPane(detachedView)));
		String persistenceID = MainController.DETACHED_VIEW_PERSISTENCE_ID_PREFIX + this.getDetachedView().getClass().getName();
		this.setMinWidth(detachedView.minWidth(-1));
		this.setMinHeight(detachedView.minHeight(-1));
		stageManager.register(this, persistenceID);
		this.titleProperty().bind(sourceTitledPane.textProperty());
		
		// Default bounds, replaced by saved ones from the config when show() is called
		this.setWidth(this.getMinWidth());
		this.setHeight(this.getMinHeight());
		this.setX((Screen.getPrimary().getVisualBounds().getWidth()-this.getWidth())/2);
		this.setY((Screen.getPrimary().getVisualBounds().getHeight()-this.getHeight())/2);
	}
	
	Node getDetachedView() {
		return this.detachedView;
	}
	
	TitledPane getSourceTitledPane() {
		return this.sourceTitledPane;
	}
	
	Accordion getSourceAccordion() {
		return this.sourceAccordion;
	}
	
	void reattachView() {
		this.hide();
		this.getSourceAccordion().setVisible(true);
		this.getSourceAccordion().setMaxWidth(Double.POSITIVE_INFINITY);
		this.getSourceAccordion().setMaxHeight(Double.POSITIVE_INFINITY);
		if (this.getSourceAccordion().getExpandedPane() != null) {
			this.getSourceAccordion().getExpandedPane().setExpanded(false);
		}
		this.getSourceAccordion().setExpandedPane(this.getSourceTitledPane());
		this.getSourceTitledPane().setContent(this.getDetachedView());
		this.getSourceAccordion().getPanes().add(this.getSourceTitledPane());
	}
}
