package de.prob2.ui.dynamic;

import java.util.List;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableVisualizationCommand;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.util.Builder;

@FXMLInjected
public class DynamicTableView extends Pane implements Builder<DynamicTableView> {

	@FXML
	private TableView<ObservableList<String>> tableView;

	@Inject
	public DynamicTableView(StageManager stageManager) {
		stageManager.loadFXML(this, "table_view.fxml");
	}

	@Override
	public DynamicTableView build() {
		return this;
	}

	@FXML
	private void initialize() {
	}

	@FXML
	private void save() {
		// TODO
	}

	void clearContent() {
		this.tableView.setVisible(false);
		this.tableView.getColumns().clear();
		this.tableView.getItems().clear();
	}

	void visualize(TableVisualizationCommand command, List<IEvalElement> args) {
		throw new UnsupportedOperationException("Table Visualization NYI"); // TODO
	}
}
