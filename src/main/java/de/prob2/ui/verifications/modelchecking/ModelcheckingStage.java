package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

@FXMLInjected
public class ModelcheckingStage extends Stage {

	@FXML
	private TabPane modelCheckerTabs;
	@FXML
	private ProBModelCheckingTab probTab;
	@FXML
	private TLCModelCheckingTab tlcTab;
	@FXML
	private LTSminModelCheckingTab ltsminTab;

	@FXML
	private Button btStartModelCheck;

	@FXML
	private TextField idTextField;

	private ModelCheckingItem result;

	@Inject
	private ModelcheckingStage(StageManager stageManager) {
		this.result = null;
		stageManager.loadFXML(this, "modelchecking_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.initModality(Modality.APPLICATION_MODAL);

		btStartModelCheck.disableProperty().bind(
			this.modelCheckerTabs.getSelectionModel().selectedItemProperty().isEqualTo(tlcTab)
			.and(tlcTab.tlcApplicableErrorProperty().isNotNull())
		);
		this.modelCheckerTabs.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> tlcTab.checkTlcApplicable());
	}

	@FXML
	protected void startModelCheck() {
		final String id = idTextField.getText().trim().isEmpty() ? null : idTextField.getText();
		if (probTab.isSelected()) {
			this.result = probTab.startModelCheck(id);
		} else if (tlcTab.isSelected()) {
			this.result = tlcTab.startModelCheck(id);
		} else if (ltsminTab.isSelected()) {
			this.result = ltsminTab.startModelCheck(id);
		}
		this.hide();
	}

	@FXML
	private void cancel() {
		this.hide();
	}

	public ModelCheckingItem getResult() {
		return result;
	}

	public void setData(final ModelCheckingItem item) {
		if (item != null) {
			idTextField.setText(item.getId() == null ? "" : item.getId());
			switch (item) {
				case ProBModelCheckingItem probItem -> {
					probTab.setData(probItem);
					modelCheckerTabs.getSelectionModel().select(probTab);
				}
				case TLCModelCheckingItem tlcItem -> {
					tlcTab.setData(tlcItem);
					modelCheckerTabs.getSelectionModel().select(tlcTab);
				}
				case LTSminModelCheckingItem ltsminItem -> {
					ltsminTab.setData(ltsminItem);
					modelCheckerTabs.getSelectionModel().select(ltsminTab);
				}
				default -> throw new IllegalArgumentException("unknown model checking item: " + item);
			}
		}
		this.result = item;
	}
}
