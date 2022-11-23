package de.prob2.ui.verifications.po;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.model.eventb.EventBModel;
import de.prob.model.eventb.ProofObligation;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

import java.util.List;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class ProofObligationView extends AnchorPane {

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	@FXML
	private TableView<ProofObligationItem> tvProofObligations;

	@FXML
	private TableColumn<ProofObligationItem, Checked> poStatusColumn;
	@FXML
	private TableColumn<ProofObligationItem, String> poIdColumn;
	@FXML
	private TableColumn<ProofObligationItem, String> poColumn;
	@FXML
	private TableColumn<ProofObligationItem, String> poDescriptionColumn;

	@Inject
	private ProofObligationView(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace) {
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this, "po_view.fxml");
	}

	@FXML
	public void initialize() {
		poStatusColumn.setCellFactory(col -> new CheckedCell<>());
		poStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		poIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		poColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		poDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		currentTrace.addListener((observable, from, to) -> {
			tvProofObligations.getItems().clear();
			AbstractModel model = currentTrace.getModel();
			if(model == null) {
				return;
			}
			if (model instanceof EventBModel) {
				if(((EventBModel) model).getTopLevelMachine() == null) {
					return;
				}
				List<ProofObligation> pos = ((EventBModel) model).getTopLevelMachine().getProofs();
				List<ProofObligationItem> poItems = pos.stream().map(ProofObligationItem::new).collect(Collectors.toList());
				tvProofObligations.getItems().addAll(poItems);
			}
		});
	}
}
