package de.prob2.ui.project;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.prob.json.JsonManager;
import de.prob.json.JsonMetadata;
import de.prob.json.ObjectWithMetadata;
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationType;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.symbolic.SymbolicExecutionType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

class ProjectJsonContext extends JsonManager.Context<Project> {

	private Path location;

	ProjectJsonContext(final Gson gson) {
		super(gson, Project.class, "Project", 8);
	}
	
	private static void updateV0CheckableItem(final JsonObject checkableItem) {
		if (!checkableItem.has("selected")) {
			checkableItem.addProperty("selected", true);
		}
	}
	
	private static void updateV0SymbolicCheckingItem(final JsonObject symbolicCheckingItem) {
		// The names of some symbolic checking types have changed in the past and need to be updated.
		SymbolicExecutionType newType = null;
		if (symbolicCheckingItem.get("type").isJsonNull()) {
			// If a project was loaded after a type renaming,
			// and the UI version in question did not support file format versioning/conversion,
			// the type will be null when the project is saved again.
			// In this case, the type needs to be restored from the code string.
			final String code = symbolicCheckingItem.get("code").getAsString();
			if ("CHECK_ASSERTIONS".equals(code)) {
				newType = SymbolicExecutionType.CHECK_STATIC_ASSERTIONS;
			}
		}
		
		if (newType != null) {
			symbolicCheckingItem.addProperty("type", newType.name());
		}
	}
	
	private static void updateV0TestCaseItem(final JsonObject testCaseItem) {
		if (!testCaseItem.has("additionalInformation")) {
			testCaseItem.add("additionalInformation", new JsonObject());
		}
		final JsonObject additionalInformation = testCaseItem.getAsJsonObject("additionalInformation");
		final String type = testCaseItem.get("type").getAsString();
		final String code = testCaseItem.get("code").getAsString();
		// Old UI versions did not have the additionalInformation field
		// and instead stored additional information in the code string.
		// The additionalInformation field may also have been deleted
		// when test case generation was separated from symbolic animation
		// (see moveV0TestCaseSymbolicAnimationItems for details).
		// In either of these cases, additionalInformation will be missing/empty,
		// and its contents need to be extracted from the code string.
		final String[] splitOnSlash = code.replace(" ", "").split("/");
		final String[] splitFirstOnColon = splitOnSlash[0].split(":");
		if (TestCaseGenerationType.MCDC.name().equals(type)) {
			if (!additionalInformation.has("level")) {
				if (!"MCDC".equals(splitFirstOnColon[0])) {
					throw new JsonParseException("First part of MCDC item code string does not contain level: " + splitOnSlash[0]);
				}
				additionalInformation.addProperty("level", Integer.parseInt(splitFirstOnColon[1]));
			}
		} else if (TestCaseGenerationType.COVERED_OPERATIONS.name().equals(type)) {
			if (!additionalInformation.has("operations")) {
				if (!"OPERATION".equals(splitFirstOnColon[0])) {
					throw new JsonParseException("First part of covered operations item code string does not contain operations: " + splitOnSlash[0]);
				}
				final String[] operationNames = splitFirstOnColon[1].split(",");
				final JsonArray operationNamesJsonArray = new JsonArray(operationNames.length);
				for (final String operationName : operationNames) {
					operationNamesJsonArray.add(operationName);
				}
				additionalInformation.add("operations", operationNamesJsonArray);
			}
		}
		if (!testCaseItem.has("maxDepth")) {
			// Test case items moved from symbolic animation items may have the maxDepth field missing.
			// In this case, the depth value needs to be extracted from the code string.
			final String[] depthSplitOnColon = splitOnSlash[1].split(":");
			if (!"DEPTH".equals(depthSplitOnColon[0])) {
				throw new JsonParseException("Second part of test case item code string does not contain depth: " + splitOnSlash[1]);
			}
			testCaseItem.addProperty("maxDepth", Integer.parseInt(depthSplitOnColon[1]));
		}
	}
	
	private static void moveV0TestCaseSymbolicAnimationItems(final JsonArray symbolicAnimationFormulas, final JsonArray testCases) {
		// Test case generation was previously part of symbolic animation,
		// but has now been moved into its own checking category.
		// Projects from older versions may still contain symbolic animation items for test case generation,
		// which need to be converted to proper test case generation items.
		for (final Iterator<JsonElement> it = symbolicAnimationFormulas.iterator(); it.hasNext();) {
			final JsonObject symbolicAnimationFormula = it.next().getAsJsonObject();
			final TestCaseGenerationType testCaseGenerationType;
			final JsonElement typeElement = symbolicAnimationFormula.get("type");
			if (typeElement.isJsonNull()) {
				// If a project contains symbolic animation items for test case generation,
				// and it is loaded and re-saved by a newer version that has separated test case generation (but no file format versioning/conversion),
				// the symbolic animation items will have their type silently set to null,
				// because the corresponding enum items have been removed.
				// In this case, the type needs to be restored from the code string.
				final String code = symbolicAnimationFormula.get("code").getAsString();
				if (code.startsWith("MCDC")) {
					testCaseGenerationType = TestCaseGenerationType.MCDC;
				} else if (code.startsWith("OPERATION")) {
					testCaseGenerationType = TestCaseGenerationType.COVERED_OPERATIONS;
				} else {
					testCaseGenerationType = null;
				}
			} else if ("MCDC".equals(typeElement.getAsString())) {
				testCaseGenerationType = TestCaseGenerationType.MCDC;
			} else if ("COVERED_OPERATIONS".equals(typeElement.getAsString())) {
				testCaseGenerationType = TestCaseGenerationType.COVERED_OPERATIONS;
			} else {
				testCaseGenerationType = null;
			}
			if (testCaseGenerationType != null) {
				// If this item is for test case generation, move it into the list of test case items.
				it.remove();
				testCases.add(symbolicAnimationFormula);
				// Update/fix the type, as determined above.
				symbolicAnimationFormula.addProperty("type", testCaseGenerationType.name());
				// In symbolic animation items, the maxDepth value was stored in additionalInformation.
				// In test case items, it is stored as a regular field.
				// If additionalInformation is missing,
				// the maxDepth value needs to be extracted from the code field instead.
				// That case is handled in updateV0TestCaseItem.
				final JsonElement additionalInformationElement = symbolicAnimationFormula.get("additionalInformation");
				if (additionalInformationElement != null && additionalInformationElement.isJsonObject()) {
					symbolicAnimationFormula.add("maxDepth", additionalInformationElement.getAsJsonObject().remove("maxDepth"));
				}
			}
		}
	}
	
	private static void updateV0Machine(final JsonObject machine) {
		if (!machine.has("lastUsedPreferenceName")) {
			final String lastUsedPreferenceName;
			if (machine.has("lastUsed")) {
				lastUsedPreferenceName = machine.getAsJsonObject("lastUsed").get("name").getAsString();
			} else {
				lastUsedPreferenceName = Preference.DEFAULT.getName();
			}
			machine.addProperty("lastUsedPreferenceName", lastUsedPreferenceName);
		}
		for (final String checkableItemFieldName : new String[] {"ltlFormulas", "ltlPatterns", "symbolicCheckingFormulas", "symbolicAnimationFormulas", "testCases"}) {
			if (!machine.has(checkableItemFieldName)) {
				machine.add(checkableItemFieldName, new JsonArray());
			}
			machine.getAsJsonArray(checkableItemFieldName).forEach(checkableItemElement ->
				ProjectJsonContext.updateV0CheckableItem(checkableItemElement.getAsJsonObject())
			);
		}
		machine.getAsJsonArray("symbolicCheckingFormulas").forEach(symbolicCheckingItemElement ->
			updateV0SymbolicCheckingItem(symbolicCheckingItemElement.getAsJsonObject())
		);
		final JsonArray testCases = machine.getAsJsonArray("testCases");
		final JsonArray symbolicAnimationFormulas = machine.getAsJsonArray("symbolicAnimationFormulas");
		moveV0TestCaseSymbolicAnimationItems(symbolicAnimationFormulas, testCases);
		testCases.forEach(testCaseItemElement ->
			ProjectJsonContext.updateV0TestCaseItem(testCaseItemElement.getAsJsonObject())
		);
		if (!machine.has("traces")) {
			machine.add("traces", new JsonArray());
		}
		if (!machine.has("modelcheckingItems")) {
			machine.add("modelcheckingItems", new JsonArray());
		}
	}
	
	private static void updateV0Project(final JsonObject project) {
		if (!project.has("name")) {
			project.addProperty("name", "");
		}
		if (!project.has("description")) {
			project.addProperty("description", "");
		}
		if (!project.has("machines")) {
			project.add("machines", new JsonArray());
		}
		project.getAsJsonArray("machines").forEach(machineElement ->
			updateV0Machine(machineElement.getAsJsonObject())
		);
		if (!project.has("preferences")) {
			project.add("preferences", new JsonArray());
		}
	}
	
	private static void updateV1Project(final JsonObject project) {
		project.getAsJsonArray("machines").forEach(machineElement -> {
			final JsonObject machine = machineElement.getAsJsonObject();
			machine.getAsJsonArray("testCases").forEach(testCaseItemElement -> {
				final JsonObject additionalInformation = testCaseItemElement.getAsJsonObject().getAsJsonObject("additionalInformation");
				// These additionalInformation entries were previously saved in the project file.
				// However, the code that would read these values could only execute after re-running the test case generation,
				// which would overwrite these entries with newly generated values.
				// This means that these saved values were never actually used
				// and do not need to be stored in the project file.
				// These entries have now been replaced with regular transient fields.
				additionalInformation.remove("traceInformation");
				additionalInformation.remove("uncoveredOperations");
			});
			
			// The items of all of these lists have a field called checked,
			// which was previously saved in the project file,
			// but ignored when loading the project.
			// The checked field is now transient and no longer saved in the project file.
			for (final String fieldName : new String[] {"modelcheckingItems", "ltlFormulas", "ltlPatterns", "symbolicCheckingFormulas", "symbolicAnimationFormulas", "testCases"}) {
				machine.getAsJsonArray(fieldName).forEach(element ->
					element.getAsJsonObject().remove("checked")
				);
			}
		});
	}

	private static void updateV2Project(final JsonObject project) {
		project.getAsJsonArray("machines").forEach(machineElement -> {
			final JsonObject machine = machineElement.getAsJsonObject();
			machine.getAsJsonArray("modelcheckingItems").forEach(modelCheckingItemElement -> {
				if (!modelCheckingItemElement.getAsJsonObject().has("nodesLimit")) {
					modelCheckingItemElement.getAsJsonObject().addProperty("nodesLimit", "-");
				}
			});
		});
	}

	private static void updateV3Project(final JsonObject project) {
		project.getAsJsonArray("machines").forEach(machineElement -> {
			final JsonObject machine = machineElement.getAsJsonObject();
			machine.getAsJsonArray("modelcheckingItems").forEach(modelCheckingItemElement -> {
				if (!modelCheckingItemElement.getAsJsonObject().has("timeLimit")) {
					modelCheckingItemElement.getAsJsonObject().addProperty("timeLimit", "-");
				}
			});
		});
	}

	private static void updateV4Project(final JsonObject project) {
		project.getAsJsonArray("machines").forEach(machineElement -> {
			final JsonObject machine = machineElement.getAsJsonObject();
			machine.getAsJsonArray("symbolicCheckingFormulas").forEach(symbolicCheckingItemElement -> {
				final JsonObject symbolicCheckingItem = symbolicCheckingItemElement.getAsJsonObject();
				final String oldCheckingType = symbolicCheckingItem.get("type").getAsString();
				if ("IC3".equals(oldCheckingType) || "TINDUCTION".equals(oldCheckingType) || "KINDUCTION".equals(oldCheckingType) || "BMC".equals(oldCheckingType)) {
					assert symbolicCheckingItem.get("name").getAsString().equals(oldCheckingType);
					assert symbolicCheckingItem.get("code").getAsString().equals(oldCheckingType);
					symbolicCheckingItem.addProperty("type", "SYMBOLIC_MODEL_CHECK");
				}
				if (!symbolicCheckingItemElement.getAsJsonObject().has("timeLimit")) {
					symbolicCheckingItemElement.getAsJsonObject().addProperty("timeLimit", "-");
				}
			});
		});
	}

	private static void updateV5Project(final JsonObject project) {
		project.getAsJsonArray("machines").forEach(machineElement -> {
			final JsonObject machine = machineElement.getAsJsonObject();
			if(!machine.has("visBVisualisation")) {
				machine.addProperty("visBVisualisation", "");
			}
		});
	}

	private static void updateV6Project(final JsonObject project) {
		project.getAsJsonArray("machines").forEach(machineElement -> {
			final JsonObject machine = machineElement.getAsJsonObject();
			String visBLocation = machine.get("visBVisualisation").getAsString();
			if(visBLocation.isEmpty()) {
				String nullString = null;
				machine.remove("visBVisualisation");
				machine.addProperty("visBVisualisation", nullString);
			}
		});
	}

	private static void updateV7Project(final JsonObject project, final Path location) {
		project.getAsJsonArray("machines").forEach(machineElement -> {
			final JsonObject machine = machineElement.getAsJsonObject();
			JsonElement visBLocationElement = machine.get("visBVisualisation");
			if(!visBLocationElement.isJsonNull()) {
				Path visBLocation = Paths.get(visBLocationElement.getAsString());
				if(visBLocation.isAbsolute()) {
					machine.remove("visBVisualisation");
					Path newVisBLocationPath = location.getParent().relativize(visBLocation);
					machine.addProperty("visBVisualisation", newVisBLocationPath.toString());
				}
			}
		});
	}
	
	@Override
	public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
		if (oldMetadata.getFormatVersion() <= 0) {
			updateV0Project(oldObject);
		}
		if (oldMetadata.getFormatVersion() <= 1) {
			updateV1Project(oldObject);
		}
		if (oldMetadata.getFormatVersion() <= 2) {
			updateV2Project(oldObject);
		}
		if (oldMetadata.getFormatVersion() <= 3) {
			updateV3Project(oldObject);
		}
		if (oldMetadata.getFormatVersion() <= 4) {
			updateV4Project(oldObject);
		}
		if (oldMetadata.getFormatVersion() <= 5) {
			updateV5Project(oldObject);
		}
		if (oldMetadata.getFormatVersion() <= 6) {
			updateV6Project(oldObject);
		}
		if (oldMetadata.getFormatVersion() <= 7) {
			updateV7Project(oldObject, location);
		}
		return new ObjectWithMetadata<>(oldObject, oldMetadata);
	}

	public void setProjectLocation(Path location) {
		this.location = location;
	}
}
