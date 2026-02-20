package de.prob2.ui.verifications.po;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.stage.Stage;

import java.util.List;

@Singleton
public class ProofObligationDetailsStage extends Stage {

	@FXML private TreeTableView<ProofObligationItem> poTreeView;
	@FXML private TreeTableColumn<ProofObligationItem, String> nameColumn;
	@FXML private TreeTableColumn<ProofObligationItem, String> descriptionColumn;
	@FXML private TreeItem<ProofObligationItem> rootItem;
	@FXML private ProofSequentCodeArea textArea;

	@Inject
	ProofObligationDetailsStage(StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "po_details_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.poTreeView.getSelectionModel().selectedItemProperty().addListener((obs, from, to) ->
				this.textArea.replaceText(to == null ? "" : to.getValue().getProofObligation().getSequentPrettyPrint(true)));

		this.nameColumn.setCellValueFactory(features ->
				new SimpleStringProperty(features.getValue().getValue().getName()));
		this.descriptionColumn.setCellValueFactory(features ->
				new SimpleStringProperty(features.getValue().getValue().getProofObligation().getDescription()));

		this.rootItem.setValue(new ProofObligationItem("(this root item should be invisible)", ""));
	}

	public void setItems(final List<ProofObligationItem> items, final ProofObligationItem select) {
		this.rootItem.getChildren().clear();
		items.forEach(i -> this.rootItem.getChildren().add(new TreeItem<>(i)));
		this.poTreeView.getSelectionModel().select(items.indexOf(select));
	}

}
