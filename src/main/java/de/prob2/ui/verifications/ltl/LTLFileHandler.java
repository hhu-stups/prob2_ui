package de.prob2.ui.verifications.ltl;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob2.ui.internal.AbstractFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.json.JsonMetadata;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

import javafx.stage.FileChooser.ExtensionFilter;

public class LTLFileHandler extends AbstractFileHandler<LTLData> {
	public static final String LTL_FILE_EXTENSION = "ltl";
	public static final String LTL_FILE_PATTERN = "*." + LTL_FILE_EXTENSION;

	@Inject
	public LTLFileHandler(JsonManager<LTLData> jsonManager, CurrentProject currentProject, StageManager stageManager, ResourceBundle bundle) {
		super(currentProject, stageManager, bundle, jsonManager);
		jsonManager.initContext(new JsonManager.Context<>(LTLData.class, "LTL", 1));
	}
	
	public void save() {
		Machine machine = currentProject.getCurrentMachine();
		File file = showSaveDialog(bundle.getString("verifications.ltl.ltlView.fileChooser.saveLTL.title"),
				currentProject.getLocation().toFile(), 
				machine.getName() + "." + LTL_FILE_EXTENSION,
				new ExtensionFilter(
					String.format(bundle.getString("common.fileChooser.fileTypes.ltl"), LTL_FILE_PATTERN),
					LTL_FILE_PATTERN
				));
		List<LTLFormulaItem> formulas = machine.getLTLFormulas().stream()
				.filter(LTLFormulaItem::selected)
				.collect(Collectors.toList());
		List<LTLPatternItem> patterns = machine.getLTLPatterns().stream()
				.filter(LTLPatternItem::selected)
				.collect(Collectors.toList());
		writeToFile(file, new LTLData(formulas, patterns), false, JsonMetadata.USER_CREATOR);
	}


	protected boolean isValidData(LTLData data) {
		return data.getFormulas() != null && data.getPatterns() != null;
	}

}
