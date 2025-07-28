package de.prob2.ui.verifications.modelchecking;

import java.util.Set;

import com.google.inject.Inject;

import de.prob.check.LTSminModelCheckingOptions;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.TranslatableAdapter;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;

@FXMLInjected
public class LTSminModelCheckingTab extends Tab {

	@FXML
	private ChoiceBox<LTSminModelCheckingOptions.Backend> selectBackend;
	@FXML
	private RadioButton findDeadlocks;
	@FXML
	private RadioButton findInvViolations;
	@FXML
	private CheckBox usePOR;

	private final I18n i18n;

	private LTSminModelCheckingItem result;

	@Inject
	private LTSminModelCheckingTab(StageManager stageManager, I18n i18n) {
		this.i18n = i18n;

		this.result = null;

		stageManager.loadFXML(this, "ltsmin_modelchecking_tab.fxml");
	}

	@FXML
	private void initialize() {
		this.selectBackend.getItems().setAll(
				LTSminModelCheckingOptions.Backend.SEQUENTIAL,
				LTSminModelCheckingOptions.Backend.SYMBOLIC
		);
		this.selectBackend.setConverter(i18n.translateConverter(TranslatableAdapter.adapter(LTSminModelCheckingTab::getBackendNameKey)));

		this.setDefaultData();
	}

	public static String getBackendNameKey(LTSminModelCheckingOptions.Backend backend) {
		return switch (backend) {
			case SEQUENTIAL -> "verifications.modelchecking.modelcheckingStage.ltsminTab.backend.sequential";
			case SYMBOLIC -> "verifications.modelchecking.modelcheckingStage.ltsminTab.backend.symbolic";
			default -> null;
		};
	}

	private Set<LTSminModelCheckingOptions.Option> getOptions() {
		boolean invariant = findInvViolations.isSelected();
		return LTSminModelCheckingOptions.DEFAULT
				       .backend(selectBackend.getValue())
				       .checkInvariantViolations(invariant)
				       .checkDeadlocks(!invariant)
				       .partialOrderReduction(usePOR.isSelected())
				       .getPrologOptions();
	}

	ModelCheckingItem startModelCheck(String id) {
		return new LTSminModelCheckingItem(id, this.selectBackend.getValue(), this.getOptions());
	}

	private void setDefaultData() {
		LTSminModelCheckingOptions def = LTSminModelCheckingOptions.DEFAULT;
		this.setData(new LTSminModelCheckingItem(null, def.backend(), def.getPrologOptions()));
	}

	public void setData(final LTSminModelCheckingItem item) {
		this.result = item;
		LTSminModelCheckingOptions opts = item.getFullOptions();
		selectBackend.setValue(opts.backend());
		boolean invariant = opts.checkInvariantViolations();
		// either invariant violations or deadlocks, never both
		findInvViolations.setSelected(invariant);
		findDeadlocks.setSelected(!invariant);
		usePOR.setSelected(opts.partialOrderReduction());
	}
}
