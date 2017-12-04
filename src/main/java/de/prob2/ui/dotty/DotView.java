package de.prob2.ui.dotty;


import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

@Singleton
public class DotView extends Stage {
	
	@FXML
	private WebView dotView;
	
	@FXML
	private ScrollPane pane;
	
	private double oldMousePositionX = -1;
	private double oldMousePositionY = -1;
	private double dragFactor = 1;
	
	@Inject
	public DotView(final StageManager stageManager) {
		stageManager.loadFXML(this, "dot_view.fxml");
		dotView.setContextMenuEnabled(false);
		dotView.setOnMouseMoved(e-> {
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();
		});
		
		dotView.setOnMouseDragged(e-> {
			pane.setHvalue(pane.getHvalue() + (-e.getSceneX() + oldMousePositionX)/(pane.getWidth() * dragFactor));
			pane.setVvalue(pane.getVvalue() + (-e.getSceneY() + oldMousePositionY)/(pane.getHeight() * dragFactor));
			oldMousePositionX = e.getSceneX();
			oldMousePositionY = e.getSceneY();

		});
		dotView.setOnMouseClicked(e-> {
			if(e.getClickCount() < 2) {
				return;
			}
			if(e.getButton() == MouseButton.SECONDARY) {
				dotView.setZoom(dotView.getZoom() * 0.5);
				dragFactor *= 0.8;
			} else {
				dotView.setZoom(dotView.getZoom() * 2);
				dragFactor *= 1.3;
			}
			pane.setHvalue(e.getX() / pane.getWidth());
			pane.setVvalue(e.getY() / pane.getHeight());
		});
	}
	
	@FXML
	public void initialize() {
		dotView.getEngine().load(this.getClass().getResource("GCD.svg").toExternalForm());
	}

}
