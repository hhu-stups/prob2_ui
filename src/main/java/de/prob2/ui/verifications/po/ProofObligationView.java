package de.prob2.ui.verifications.po;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.model.eventb.EventBModel;
import de.prob.model.eventb.ProofObligation;
import de.prob.model.representation.AbstractModel;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class ProofObligationView extends AnchorPane {

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final I18n i18n;

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
	private ProofObligationView(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace, final I18n i18n) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.i18n = i18n;
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

		this.tvProofObligations.setRowFactory(param -> {
			final TableRow<ProofObligationItem> row = new TableRow<>();
			MenuItem editItem = new MenuItem("Edit validation task ID");
			editItem.setOnAction(event -> {
				ProofObligationItem item = row.getItem();
				final TextInputDialog dialog = new TextInputDialog(item.getId() == null ? "" : item.getId());
				stageManager.register(dialog);
				dialog.setTitle(i18n.translate("animation.tracereplay.view.contextMenu.editId"));
				dialog.setHeaderText(i18n.translate("vomanager.validationTaskId"));
				dialog.getEditor().setPromptText(i18n.translate("common.optionalPlaceholder"));
				final Optional<String> res = dialog.showAndWait();
				res.ifPresent(idText -> {
					final String id = idText.trim().isEmpty() ? null : idText;
					item.setId(id);
				});
				tvProofObligations.refresh();
			});

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(editItem)));

			return row;
		});

	}
}
