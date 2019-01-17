package de.prob2.ui.verifications.ltl;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.inject.Inject;

import de.prob2.ui.internal.AbstractFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.LoggerFactory;

public class LTLFileHandler extends AbstractFileHandler<LTLData> {

	@Inject
	public LTLFileHandler(Gson gson, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle, VersionInfo versionInfo) {
		super(gson, currentProject, stageManager, bundle, versionInfo, LTLData.class);
		this.LOGGER = LoggerFactory.getLogger(LTLFileHandler.class);
		this.FILE_ENDING = "*.ltl";
	}
	
	public void save() {
		Machine machine = currentProject.getCurrentMachine();
		File file = showSaveDialog(bundle.getString("verifications.ltl.ltlView.fileChooser.saveLTL.title"),
				currentProject.getLocation().toFile(), 
				machine.getName() + FILE_ENDING.substring(1),
				new ExtensionFilter(
						String.format(bundle.getString("common.fileChooser.fileTypes.ltl"), FILE_ENDING),
						FILE_ENDING));
		List<LTLFormulaItem> formulas = machine.getLTLFormulas().stream()
				.filter(LTLFormulaItem::selected)
				.collect(Collectors.toList());
		List<LTLPatternItem> patterns = machine.getLTLPatterns().stream()
				.filter(LTLPatternItem::selected)
				.collect(Collectors.toList());
		writeToFile(file, new LTLData(formulas, patterns), false);
	}


	protected boolean isValidData(LTLData data) {
		return data.getFormulas() != null && data.getPatterns() != null;
	}

}
