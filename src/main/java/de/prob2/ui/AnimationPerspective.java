package de.prob2.ui;

import java.net.URL;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.scripting.Api;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.states.StatesView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

public class AnimationPerspective implements Initializable {

	@SuppressWarnings("unused")
	private final Api api;

	@SuppressWarnings("unused")
	private FXMLLoader loader;
	
	@FXML
	private TitledPane ops;
	
	@FXML
	private TitledPane statesp;
	
	@FXML
	private TitledPane historyp;
	
	@FXML
	private Accordion small;

	private OperationsView opsController;

	private HistoryView historyController;
	
	
	

	@Inject
	public AnimationPerspective(Api api, FXMLLoader loader, OperationsView opsController, HistoryView historyController) {
		this.api = api;
		this.loader = loader;
		this.opsController = opsController;
		this.historyController = historyController;
		

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		Parent opsview = null;
		Parent statesview = null;
		Parent historyview = null;
//		try {
//			opsview = FXMLLoader.load(getClass().getResource("operations/ops_view.fxml"));
//			statesview = FXMLLoader.load(getClass().getResource("states/states_view.fxml"));
//			historyview = FXMLLoader.load(getClass().getResource("history/history_view.fxml"));
//		} catch (IOException e) {
//			throw new IllegalStateException(e);
//		}

		ops = (TitledPane) opsview;
		statesp = (TitledPane) statesview;
		historyp = (TitledPane) historyview;
		
		
		
		small.getPanes().clear();
		small.getPanes().addAll(opsController, historyController);
		
		
		
	}

}
