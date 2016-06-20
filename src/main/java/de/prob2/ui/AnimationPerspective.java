package de.prob2.ui;

import java.net.URL;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.scripting.Api;
import de.prob2.ui.dotty.DottyView;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.operations.OperationsView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.paint.Color;

public class AnimationPerspective implements Initializable {

	@SuppressWarnings("unused")
	private final Api api;

	@SuppressWarnings("unused")
	private FXMLLoader loader;

	@FXML
	private Accordion small;

	private OperationsView opsController;

	private HistoryView historyController;
	
	private DottyView dottyController;

	@Inject
	public AnimationPerspective(Api api, FXMLLoader loader, OperationsView opsController,
			HistoryView historyController, DottyView dottyController) {
		this.api = api;
		this.loader = loader;
		this.opsController = opsController;
		this.historyController = historyController;
		this.dottyController = dottyController;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		small.getPanes().clear();
		small.getPanes().addAll(opsController, historyController, dottyController);

	}

}
