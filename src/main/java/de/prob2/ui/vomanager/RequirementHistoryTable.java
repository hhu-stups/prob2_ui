package de.prob2.ui.vomanager;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class RequirementHistoryTable extends Stage {

	@FXML
	private TableView<Requirement> table;

	@FXML
	private TableColumn<Requirement,String> name;

	@FXML
	private  TableColumn<Requirement,String>  text;

	@FXML
	private  TableColumn<Requirement,String>  type;

	private final Requirement req;

	public RequirementHistoryTable(Requirement req) {
		this.req = req;
	}

	@FXML
	public void initialize() {
		initTable();
	}

	private void initTable(){
		table.getItems().addAll(req.getPreviousVersions());
	}


}
