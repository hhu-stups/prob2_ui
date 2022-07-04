package de.prob2.ui.vomanager;

import com.google.inject.Singleton;
import de.prob2.ui.internal.FXMLInjected;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

@FXMLInjected
@Singleton
public class VOHistoryTable<T> extends Stage {

	@FXML
	private  TableView<ValidationObligation> table;

	@FXML
	private  TableColumn<ValidationObligation,String> name;

	@FXML
	private  TableColumn<ValidationObligation,String>  requirement;

	@FXML
	private  TableColumn<ValidationObligation,String>  expression;

	private final ValidationObligation vo;

	public VOHistoryTable(ValidationObligation vo) {
		this.vo = vo;
	}

	@FXML
	public void initialize() {
		initTable();
	}

	private void initTable(){
		table.getItems().addAll(vo.getPreviousVersions());
	}

}
