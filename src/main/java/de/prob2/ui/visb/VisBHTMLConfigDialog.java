package de.prob2.ui.visb;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.*;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Singleton
@FXMLInjected
public final class VisBHTMLConfigDialog extends Stage {
	@FXML
	private Button btCreateExport;
	@FXML
	private CheckBox showConstants;
	@FXML
	private CheckBox showEvents;
	@FXML
	private CheckBox showHeader;
	@FXML
	private CheckBox showSets;
	@FXML
	private CheckBox showSource;
	@FXML
	private CheckBox showVariables;
	@FXML
	private CheckBox showVersionInfo;
	@FXML
	private Label modeLabel;

	private boolean onlyCurrentState;
	private final I18n i18n;
	private final VisBView visBView;

	@Inject
	public VisBHTMLConfigDialog(I18n i18n, StageManager stageManager, VisBView visBView) {
		super();
		this.i18n = i18n;
		this.visBView = visBView;
		stageManager.loadFXML(this, "visb_html_config_view.fxml");
	}

	@FXML
	public void initialize() {
		initModality(Modality.APPLICATION_MODAL);
		initialiseForOptions(false);
		btCreateExport.setOnAction(e -> {
			visBView.performHtmlExport(this.onlyCurrentState, getOptionsFromSelection());
			this.close();
		});
	}

	void initialiseForOptions(final boolean onlyCurrentState) {
		this.modeLabel.setText(i18n.translate(onlyCurrentState ? "visb.menu.file.export.html.customConfiguration.dialog.currentStateMode"
				: "visb.menu.file.export.html.customConfiguration.dialog.historyMode"));
		VisBExportOptions options = onlyCurrentState ? VisBExportOptions.DEFAULT_STATES : VisBExportOptions.DEFAULT_HISTORY;
		this.showConstants.setSelected(options.isShowConstants());
		this.showEvents.setSelected(options.isShowEvents());
		this.showHeader.setSelected(options.isShowHeader());
		this.showSets.setSelected(options.isShowSets());
		this.showSource.setSelected(options.isShowSource());
		this.showVariables.setSelected(options.isShowVariables());
		this.showVersionInfo.setSelected(options.isShowVersionInfo());
		this.onlyCurrentState = onlyCurrentState;
	}

	private VisBExportOptions getOptionsFromSelection() {
		return VisBExportOptions.DEFAULT
				.withShowConstants(showConstants.isSelected())
				.withShowEvents(showEvents.isSelected())
				.withShowHeader(showHeader.isSelected())
				.withShowSets(showSets.isSelected())
				.withShowSource(showSource.isSelected())
				.withShowVariables(showVariables.isSelected())
				.withShowVersionInfo(showVersionInfo.isSelected());
	}
}
