package de.prob2.ui.verifications.po;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.unicode.UnicodeTranslator;
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

	private record Identifier(String id, IEvalElement type) {
		String prettyType() {
			return UnicodeTranslator.toUnicode(type.getPrettyPrint());
		}
	}

	@FXML private TreeTableView<Object> poTreeView;
	@FXML private TreeTableColumn<Object, String> nameColumn;
	@FXML private TreeTableColumn<Object, String> descriptionColumn;
	@FXML private TreeItem<Object> rootItem;
	@FXML private ProofSequentCodeArea textArea;

	@Inject
	ProofObligationDetailsStage(StageManager stageManager) {
		super();
		stageManager.loadFXML(this, "po_details_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.poTreeView.getSelectionModel().selectedItemProperty().addListener((obs, from, to) -> {
				if (to != null && to.getValue() instanceof ProofObligationItem item) {
					this.textArea.replaceText(item.getProofObligation().getSequentPrettyPrint(true));
				} else if (to != null && to.getParent() != null && to.getParent().getValue() instanceof ProofObligationItem parent) {
					this.textArea.replaceText(parent.getProofObligation().getSequentPrettyPrint(true));
				} else {
					this.textArea.replaceText("");
				}
		});

		this.nameColumn.setCellValueFactory(features -> {
				if (features.getValue().getValue() instanceof ProofObligationItem item) {
					return new SimpleStringProperty(item.getName());
				} else if (features.getValue().getValue() instanceof Identifier id) {
					return new SimpleStringProperty(id.id());
				}
				return new SimpleStringProperty("");
		});
		this.descriptionColumn.setCellValueFactory(features -> {
				if (features.getValue().getValue() instanceof ProofObligationItem item) {
					return new SimpleStringProperty(item.getProofObligation().getDescription());
				} else if (features.getValue().getValue() instanceof Identifier id) {
					return new SimpleStringProperty(id.prettyType());
				}
				return new SimpleStringProperty("");
		});

		this.rootItem.setValue(new ProofObligationItem("(this root item should be invisible)", ""));
	}

	public void setItems(final List<ProofObligationItem> items, final ProofObligationItem select) {
		this.rootItem.getChildren().clear();
		items.forEach(i -> {
			TreeItem<Object> item = new TreeItem<>(i);
			i.getProofObligation().getIdentifiers().forEach((id, type) ->
					item.getChildren().add(new TreeItem<>(new Identifier(id, type))));
			this.rootItem.getChildren().add(item);
		});
		this.poTreeView.getSelectionModel().select(items.indexOf(select));
	}

}
