package de.prob2.ui.machines;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.Machine;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

@Singleton
public final class MachinesView extends AnchorPane {
	@FXML
	private TableView<Machine> animationsTable;
	@FXML
	private TableColumn<Machine, String> name;
	@FXML
	private TableColumn<Machine, String> machine;
	@FXML
	private TableColumn<Machine, String> description;

	@Inject
	private MachinesView(final StageManager stageManager) {
		stageManager.loadFXML(this, "machines_view.fxml");
	}

	@FXML
	public void initialize() {
		name.setCellValueFactory(new PropertyValueFactory<>("name"));
//		machine.setCellValueFactory(new PropertyValueFactory<>("modelName"));
		description.setCellValueFactory(new PropertyValueFactory<>("description"));
	}
}
