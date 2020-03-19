package de.prob2.ui.verifications.ltl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.AbstractFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.json.JsonMetadata;
import de.prob2.ui.json.ObjectWithMetadata;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import de.prob2.ui.verifications.ltl.patterns.LTLPatternItem;

import javafx.stage.FileChooser;

public class LTLFileHandler extends AbstractFileHandler<LTLData> {
	public static final String LTL_FILE_EXTENSION = "ltl";
	public static final String LTL_FILE_PATTERN = "*." + LTL_FILE_EXTENSION;

	@Inject
	public LTLFileHandler(JsonManager<LTLData> jsonManager, CurrentProject currentProject, StageManager stageManager, FileChooserManager fileChooserManager, ResourceBundle bundle) {
		super(currentProject, stageManager, fileChooserManager, bundle, jsonManager);
		jsonManager.initContext(new JsonManager.Context<LTLData>(LTLData.class, "LTL", 1) {
			@Override
			public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
				if (oldMetadata.getFileType() == null) {
					assert oldMetadata.getFormatVersion() == 0;
					for (final String fieldName : new String[] {"formulas", "patterns"}) {
						if (!oldObject.has(fieldName)) {
							throw new JsonParseException("Not a valid LTL file - missing required field " + fieldName);
						}
					}
				}
				return new ObjectWithMetadata<>(oldObject, oldMetadata);
			}
		});
	}
	
	public LTLData load(Path path) throws IOException {
		return this.jsonManager.readFromFile(path).getObject();
	}
	
	public void save() {
		Machine machine = currentProject.getCurrentMachine();
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("verifications.ltl.ltlView.fileChooser.saveLTL.title"));
		fileChooser.setInitialFileName(machine.getName() + "." + LTL_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
			String.format(bundle.getString("common.fileChooser.fileTypes.ltl"), LTL_FILE_PATTERN),
			LTL_FILE_PATTERN
		));
		final Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.LTL, stageManager.getCurrent());
		if (path != null) {
			List<LTLFormulaItem> formulas = machine.getLTLFormulas().stream()
				.filter(LTLFormulaItem::selected)
				.collect(Collectors.toList());
			List<LTLPatternItem> patterns = machine.getLTLPatterns().stream()
				.filter(LTLPatternItem::selected)
				.collect(Collectors.toList());
			try {
				writeToFile(path.toFile(), new LTLData(formulas, patterns), false, JsonMetadata.USER_CREATOR);
			} catch (IOException e) {
				stageManager.makeExceptionAlert(e, "verifications.ltl.ltlView.saveLTL.error").showAndWait();
			}
		}
	}
}
