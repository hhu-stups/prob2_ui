package de.prob2.ui.animation.symbolic.testcasegeneration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.util.Arrays;

@Singleton
public final class TraceInformationStage extends Stage {

	private final class TraceInformationRow extends TableRow<TraceInformationItem> {
		private TraceInformationRow() {
			super();
			this.getStyleClass().add("trace-information-cell");
			this.setOnMouseClicked(e -> {
				TraceInformationItem item = this.getItem();
				if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && item.getTrace() != null) {
					currentTrace.set(item.getTrace());
				}
			});
		}

		@Override
		protected void updateItem(TraceInformationItem item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll(Arrays.asList("replayable", "not-replayable"));
			if(item != null) {
				if (item.getTrace() == null) {
					this.getStyleClass().add("not-replayable");
				} else {
					this.getStyleClass().add("replayable");
				}
			}
		}
	}

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

	private final CurrentTrace currentTrace;

	@Inject
	private TraceInformationStage(final StageManager stageManager, final CurrentTrace currentTrace) {
		stageManager.loadFXML(this, "test_case_generation_trace_information.fxml", this.getClass().getName());
		this.currentTrace = currentTrace;
	}

	public void setItems(ObservableList<TraceInformationItem> items) {
		this.items.setAll(items);
	}

	@FXML
	public void initialize() {
		tvTraces.setRowFactory(item -> new TraceInformationRow());
		depth.setCellValueFactory(new PropertyValueFactory<>("depth"));
		transitions.setCellValueFactory(new PropertyValueFactory<>("transitions"));
		isComplete.setCellValueFactory(new PropertyValueFactory<>("complete"));
		lastTransitionFeasible.setCellValueFactory(new PropertyValueFactory<>("lastTransitionFeasible"));
		tvTraces.setItems(items);
	}

}
