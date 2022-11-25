package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;

@FXMLInjected
public class ReplayedTraceTable extends TableView<ReplayedTraceRow> {

	final TableColumn<ReplayedTraceRow, Integer> step;
	final TableColumn<ReplayedTraceRow, String> fileTransition;
	final TableColumn<ReplayedTraceRow, String> replayedTransition;

	@Inject
	public ReplayedTraceTable(I18n i18n) {
		super();

		step = new TableColumn<>("%Step");
		step.setCellValueFactory(new PropertyValueFactory<>("step"));
		fileTransition = new TableColumn<>("%File Transition");
		fileTransition.setCellValueFactory(new PropertyValueFactory<>("fileTransition"));
		replayedTransition = new TableColumn<>("%Replayed Transition");
		replayedTransition.setCellValueFactory(new PropertyValueFactory<>("replayedTransition"));

		getColumns().setAll(Arrays.asList(step, fileTransition, replayedTransition));
	}
}
