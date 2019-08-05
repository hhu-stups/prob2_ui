package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

@Singleton
public final class TraceInformationStage extends Stage {

	@FXML
	private TableView<TraceInformationItem> tvTraces;

	@FXML
	private TableColumn<TraceInformationItem, String> depth;

	@FXML
	private TableColumn<TraceInformationItem, String> transitions;

	@FXML
	private TableColumn<TraceInformationItem, Boolean> isComplete;

	@FXML
	private TableColumn<TraceInformationItem, Boolean> lastTransitionFeasible;

	private ObservableList<TraceInformationItem> items = FXCollections.observableArrayList();

	@Inject
	private TraceInformationStage(StageManager stageManager) {
		stageManager.loadFXML(this, "test_case_generation_trace_information.fxml", this.getClass().getName());
	}

	public void setItems(ObservableList<TraceInformationItem> items) {
		this.items.setAll(items);
	}

	@FXML
	public void initialize() {
		depth.setCellValueFactory(new PropertyValueFactory<>("depth"));
		transitions.setCellValueFactory(new PropertyValueFactory<>("transitions"));
		isComplete.setCellValueFactory(new PropertyValueFactory<>("complete"));
		lastTransitionFeasible.setCellValueFactory(new PropertyValueFactory<>("lastTransitionFeasible"));
		tvTraces.setItems(items);
	}

}
