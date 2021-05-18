package de.prob2.ui.project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.prob.json.JacksonManager;
import de.prob.json.JsonConversionException;

class ProjectJsonContext extends JacksonManager.Context<Project> {

	private Path location;

	ProjectJsonContext(final ObjectMapper objectMapper) {
		super(objectMapper, Project.class, Project.FILE_TYPE, Project.CURRENT_FORMAT_VERSION);
	}
	
	@Override
	public boolean shouldAcceptOldMetadata() {
		return true;
	}
	
	private static ObjectNode checkObject(final JsonNode node) {
		if (!(node instanceof ObjectNode)) {
			throw new JsonConversionException("Expected an ObjectNode, not " + (node == null ? "(missing field)" : node.getClass().getSimpleName()));
		}
		return (ObjectNode)node;
	}
	
	private static ArrayNode checkArray(final JsonNode node) {
		if (!(node instanceof ArrayNode)) {
			throw new JsonConversionException("Expected an ArrayNode, not " + (node == null ? "(missing field)" : node.getClass().getSimpleName()));
		}
		return (ArrayNode)node;
	}
	
	private static String checkText(final JsonNode node) {
		if (!(node instanceof TextNode)) {
			throw new JsonConversionException("Expected a TextNode, not " + (node == null ? "(missing field)" : node.getClass().getSimpleName()));
		}
		return Objects.requireNonNull(node.textValue());
	}
	
	private static void updateV0CheckableItem(final ObjectNode checkableItem) {
		// Some old items might have "selected" missing or under the previous name "shouldExecute".
		if (!checkableItem.has("selected")) {
			if (checkableItem.has("shouldExecute")) {
				checkableItem.set("selected", checkableItem.get("shouldExecute"));
			} else {
				checkableItem.put("selected", true);
			}
		}
		checkableItem.remove("shouldExecute");
	}
	
	private static void updateV0SymbolicCheckingItem(final ObjectNode symbolicCheckingItem) {
		// The names of some symbolic checking types have changed in the past and need to be updated.
		if (symbolicCheckingItem.get("type").isNull()) {
			// If a project was loaded after a type renaming,
			// and the UI version in question did not support file format versioning/conversion,
			// the type will be null when the project is saved again.
			// In this case, the type needs to be restored from the code string.
			final String code = checkText(symbolicCheckingItem.get("code"));
			if ("CHECK_ASSERTIONS".equals(code)) {
				symbolicCheckingItem.put("type", "CHECK_STATIC_ASSERTIONS");
			}
		}
	}
	
	private static void updateV0TestCaseItem(final ObjectNode testCaseItem) {
		if (!testCaseItem.has("additionalInformation")) {
			testCaseItem.set("additionalInformation", testCaseItem.objectNode());
		}
		final ObjectNode additionalInformation = checkObject(testCaseItem.get("additionalInformation"));
		final String type = checkText(testCaseItem.get("type"));
		final String code = checkText(testCaseItem.get("code"));
		// Old UI versions did not have the additionalInformation field
		// and instead stored additional information in the code string.
		// The additionalInformation field may also have been deleted
		// when test case generation was separated from symbolic animation
		// (see moveV0TestCaseSymbolicAnimationItems for details).
		// In either of these cases, additionalInformation will be missing/empty,
		// and its contents need to be extracted from the code string.
		final String[] splitOnSlash = code.replace(" ", "").split("/");
		final String[] splitFirstOnColon = splitOnSlash[0].split(":");
		if ("MCDC".equals(type)) {
			if (!additionalInformation.has("level")) {
				if (!"MCDC".equals(splitFirstOnColon[0])) {
					throw new JsonConversionException("First part of MCDC item code string does not contain level: " + splitOnSlash[0]);
				}
				additionalInformation.put("level", Integer.parseInt(splitFirstOnColon[1]));
			}
		} else if ("COVERED_OPERATIONS".equals(type)) {
			if (!additionalInformation.has("operations")) {
				if (!"OPERATION".equals(splitFirstOnColon[0])) {
					throw new JsonConversionException("First part of covered operations item code string does not contain operations: " + splitOnSlash[0]);
				}
				final String[] operationNames = splitFirstOnColon[1].split(",");
				final ArrayNode operationNamesArrayNode = additionalInformation.arrayNode(operationNames.length);
				for (final String operationName : operationNames) {
					operationNamesArrayNode.add(operationName);
				}
				additionalInformation.set("operations", operationNamesArrayNode);
			}
		}
		if (!testCaseItem.has("maxDepth")) {
			// Test case items moved from symbolic animation items may have the maxDepth field missing.
			// In this case, the depth value needs to be extracted from the code string.
			final String[] depthSplitOnColon = splitOnSlash[1].split(":");
			if (!"DEPTH".equals(depthSplitOnColon[0])) {
				throw new JsonConversionException("Second part of test case item code string does not contain depth: " + splitOnSlash[1]);
			}
			testCaseItem.put("maxDepth", Integer.parseInt(depthSplitOnColon[1]));
		}
	}
	
	private static void moveV0TestCaseSymbolicAnimationItems(final ArrayNode symbolicAnimationFormulas, final ArrayNode testCases) {
		// Test case generation was previously part of symbolic animation,
		// but has now been moved into its own checking category.
		// Projects from older versions may still contain symbolic animation items for test case generation,
		// which need to be converted to proper test case generation items.
		for (final Iterator<JsonNode> it = symbolicAnimationFormulas.iterator(); it.hasNext();) {
			final ObjectNode symbolicAnimationFormula = checkObject(it.next());
			final String testCaseGenerationType;
			final JsonNode typeNode = symbolicAnimationFormula.get("type");
			if (typeNode.isNull()) {
				// If a project contains symbolic animation items for test case generation,
				// and it is loaded and re-saved by a newer version that has separated test case generation (but no file format versioning/conversion),
				// the symbolic animation items will have their type silently set to null,
				// because the corresponding enum items have been removed.
				// In this case, the type needs to be restored from the code string.
				final String code = checkText(symbolicAnimationFormula.get("code"));
				if (code.startsWith("MCDC")) {
					testCaseGenerationType = "MCDC";
				} else if (code.startsWith("OPERATION")) {
					testCaseGenerationType = "COVERED_OPERATIONS";
				} else {
					testCaseGenerationType = null;
				}
			} else {
				final String typeName = checkText(typeNode);
				if ("MCDC".equals(typeName)) {
					testCaseGenerationType = "MCDC";
				} else if ("COVERED_OPERATIONS".equals(typeName)) {
					testCaseGenerationType = "COVERED_OPERATIONS";
				} else {
					testCaseGenerationType = null;
				}
			}
			if (testCaseGenerationType != null) {
				// If this item is for test case generation, move it into the list of test case items.
				it.remove();
				testCases.add(symbolicAnimationFormula);
				// Update/fix the type, as determined above.
				symbolicAnimationFormula.put("type", testCaseGenerationType);
				// In symbolic animation items, the maxDepth value was stored in additionalInformation.
				// In test case items, it is stored as a regular field.
				// If additionalInformation is missing,
				// the maxDepth value needs to be extracted from the code field instead.
				// That case is handled in updateV0TestCaseItem.
				final JsonNode additionalInformationNode = symbolicAnimationFormula.get("additionalInformation");
				if (additionalInformationNode != null && additionalInformationNode.isObject()) {
					final ObjectNode additionalInformation = (ObjectNode)additionalInformationNode;
					final JsonNode maxDepthNode = additionalInformation.remove("maxDepth");
					if (maxDepthNode != null) {
						symbolicAnimationFormula.set("maxDepth", maxDepthNode);
					}
				}
			}
		}
	}
	
	private static void updateV0ModelcheckingItem(final ObjectNode modelcheckingItem) {
		// This information is no longer stored in the project file
		// and is instead derived from the options list.
		modelcheckingItem.remove("strategy");
		modelcheckingItem.remove("description");
		
		// Old modelcheckingItems might not have "shouldExecute" yet.
		if (!modelcheckingItem.has("shouldExecute")) {
			modelcheckingItem.put("shouldExecute", true);
		}
	}
	
	/**
	 * <p>Convert a serialied File or Path object to a plain string value if necessary/possible.</p>
	 * <p>
	 * Some old UI versions would serialize a File or Path object as a JSON object with a single field "path".
	 * In later versions this was changed so that they are serialized as plain strings instead.
	 * This method tries to convert paths from the old format.
	 * </p>
	 * 
	 * @param path the path value to be converted
	 * @return the path, converted if necessary
	 */
	private static JsonNode convertV0Path(final JsonNode path) {
		if (path.isObject()) {
			final JsonNode actualPath = path.get("path");
			if (actualPath != null && actualPath.isTextual()) {
				return actualPath;
			} else {
				return path;
			}
		} else {
			return path;
		}
	}
	
	private static void updateV0Machine(final ObjectNode machine) {
		machine.set("location", convertV0Path(machine.get("location")));
		// The machine type is no longer stored explicitly in the project file
		// and is derived from the machine file extension instead.
		machine.remove("type");
		
		// The last used preference was previously stored in "lastUsed" as a full Preference object.
		// This has been replaced by "lastUsedPreferenceName",
		// which stores just the name and not the full map of preference values.
		if (!machine.has("lastUsedPreferenceName")) {
			final String lastUsedPreferenceName;
			if (machine.has("lastUsed")) {
				lastUsedPreferenceName = checkText(checkObject(machine.get("lastUsed")).get("name"));
			} else {
				lastUsedPreferenceName = "default";
			}
			machine.put("lastUsedPreferenceName", lastUsedPreferenceName);
		}
		machine.remove("lastUsed");
		
		for (final String checkableItemFieldName : new String[] {"ltlFormulas", "ltlPatterns", "symbolicCheckingFormulas", "symbolicAnimationFormulas", "testCases"}) {
			if (!machine.has(checkableItemFieldName)) {
				machine.set(checkableItemFieldName, machine.arrayNode());
			}
			checkArray(machine.get(checkableItemFieldName)).forEach(checkableItemNode ->
				ProjectJsonContext.updateV0CheckableItem(checkObject(checkableItemNode))
			);
		}
		checkArray(machine.get("symbolicCheckingFormulas")).forEach(symbolicCheckingItemNode ->
			updateV0SymbolicCheckingItem(checkObject(symbolicCheckingItemNode))
		);
		final ArrayNode testCases = checkArray(machine.get("testCases"));
		final ArrayNode symbolicAnimationFormulas = checkArray(machine.get("symbolicAnimationFormulas"));
		moveV0TestCaseSymbolicAnimationItems(symbolicAnimationFormulas, testCases);
		testCases.forEach(testCaseItemNode ->
			ProjectJsonContext.updateV0TestCaseItem(checkObject(testCaseItemNode))
		);
		if (!machine.has("traces")) {
			machine.set("traces", machine.arrayNode());
		}
		final ArrayNode traces = checkArray(machine.get("traces"));
		for (int i = 0; i < traces.size(); i++) {
			traces.set(i, convertV0Path(traces.get(i)));
		}
		
		// Some very old projects might not have the "modelcheckingItems" list yet.
		if (!machine.has("modelcheckingItems")) {
			machine.set("modelcheckingItems", machine.arrayNode());
		}
		checkArray(machine.get("modelcheckingItems")).forEach(modelcheckingItemNode ->
			updateV0ModelcheckingItem(checkObject(modelcheckingItemNode))
		);
	}
	
	private static void updateV1Machine(final ObjectNode machine) {
		checkArray(machine.get("testCases")).forEach(testCaseItemNode -> {
			final ObjectNode additionalInformation = checkObject(checkObject(testCaseItemNode).get("additionalInformation"));
			// These additionalInformation entries were previously saved in the project file.
			// However, the code that would read these values could only execute after re-running the test case generation,
			// which would overwrite these entries with newly generated values.
			// This means that these saved values were never actually used
			// and do not need to be stored in the project file.
			// These entries have now been replaced with regular @JsonIgnore fields.
			additionalInformation.remove("traceInformation");
			additionalInformation.remove("uncoveredOperations");
		});
		
		// The items of all of these lists have a field called checked,
		// which was previously saved in the project file,
		// but ignored when loading the project.
		// The checked field is now marked as @JsonIgnore and no longer saved in the project file.
		for (final String fieldName : new String[] {"modelcheckingItems", "ltlFormulas", "ltlPatterns", "symbolicCheckingFormulas", "symbolicAnimationFormulas", "testCases"}) {
			checkArray(machine.get(fieldName)).forEach(node ->
				checkObject(node).remove("checked")
			);
		}
	}

	private static void updateV2Machine(final ObjectNode machine) {
		checkArray(machine.get("modelcheckingItems")).forEach(modelCheckingItemNode -> {
			final ObjectNode modelCheckingItem = checkObject(modelCheckingItemNode);
			if (!modelCheckingItem.has("nodesLimit")) {
				modelCheckingItem.put("nodesLimit", "-");
			}
		});
	}

	private static void updateV3Machine(final ObjectNode machine) {
		checkArray(machine.get("modelcheckingItems")).forEach(modelCheckingItemNode -> {
			final ObjectNode modelCheckingItem = checkObject(modelCheckingItemNode);
			if (!modelCheckingItem.has("timeLimit")) {
				modelCheckingItem.put("timeLimit", "-");
			}
		});
	}

	private static void updateV4Machine(final ObjectNode machine) {
		checkArray(machine.get("symbolicCheckingFormulas")).forEach(symbolicCheckingItemNode -> {
			final ObjectNode symbolicCheckingItem = checkObject(symbolicCheckingItemNode);
			final String oldCheckingType = checkText(symbolicCheckingItem.get("type"));
			if ("IC3".equals(oldCheckingType) || "TINDUCTION".equals(oldCheckingType) || "KINDUCTION".equals(oldCheckingType) || "BMC".equals(oldCheckingType)) {
				assert checkText(symbolicCheckingItem.get("name")).equals(oldCheckingType);
				assert checkText(symbolicCheckingItem.get("code")).equals(oldCheckingType);
				symbolicCheckingItem.put("type", "SYMBOLIC_MODEL_CHECK");
			}
		});
	}

	private static void updateV5Machine(final ObjectNode machine) {
		if(!machine.has("visBVisualisation")) {
			machine.put("visBVisualisation", "");
		}
	}

	private static void updateV6Machine(final ObjectNode machine) {
		final String visBLocation = checkText(machine.get("visBVisualisation"));
		if(visBLocation.isEmpty()) {
			machine.set("visBVisualisation", machine.nullNode());
		}
	}

	private static void updateV7Machine(final ObjectNode machine, final Path projectLocation) {
		final JsonNode visBLocationNode = machine.get("visBVisualisation");
		if(!visBLocationNode.isNull()) {
			Path visBLocation = Paths.get(checkText(visBLocationNode));
			if(visBLocation.isAbsolute()) {
				Path newVisBLocationPath = projectLocation.getParent().relativize(visBLocation);
				machine.put("visBVisualisation", newVisBLocationPath.toString());
			}
		}
	}

	private static void updateV8Machine(final ObjectNode machine) {
		final JsonNode visBLocationNode = machine.get("visBVisualisation");
		if(!visBLocationNode.isNull()) {
			Path visBLocation = Paths.get(checkText(visBLocationNode).replaceAll("\\\\", "/"));
			machine.put("visBVisualisation", visBLocation.toString());
		}
		Path location = Paths.get(checkText(machine.get("location")).replaceAll("\\\\", "/"));
		machine.put("location", location.toString());
		final ArrayNode newTraceArray = machine.arrayNode();
		for(JsonNode traceNode : checkArray(machine.get("traces"))) {
			Path traceLocation = Paths.get(checkText(traceNode).replaceAll("\\\\", "/"));
			newTraceArray.add(traceLocation.toString());
		}
		machine.set("traces", newTraceArray);
	}

	private static void updateV9Machine(final ObjectNode machine) {
		machine.set("simulationItems", machine.arrayNode());
	}

	private static void updateV10Machine(final ObjectNode machine) {
		checkArray(machine.get("modelcheckingItems")).forEach(modelCheckingItemNode -> {
			final ObjectNode modelCheckingItem = checkObject(modelCheckingItemNode);
			if (!modelCheckingItem.has("goal")) {
				modelCheckingItem.put("goal", "-");
			}
		});
	}

	private static void updateV11Machine(final ObjectNode machine) {
		if(!machine.has("simulation")) {
			machine.set("simulation", machine.nullNode());
		}
	}

	private static void updateV12Machine(final ObjectNode machine) {
		for (final String checkableItemFieldName : new String[] {"symbolicCheckingFormulas", "symbolicAnimationFormulas", "testCases"}) {
			checkArray(machine.get(checkableItemFieldName)).forEach(checkableItemNode ->
				checkObject(checkableItemNode).remove("description")
			);
		}
	}

	private static void updateV13Machine(final ObjectNode machine) {
		for (final String checkableItemFieldName : new String[] {"ltlFormulas", "symbolicCheckingFormulas", "symbolicAnimationFormulas", "testCases"}) {
			checkArray(machine.get(checkableItemFieldName)).forEach(checkableItemNode ->
				checkObject(checkableItemNode).remove("name")
			);
		}
	}

	private static void updateV14Machine(final ObjectNode machine) {
		checkArray(machine.get("testCases")).forEach(checkableItemNode ->
			checkObject(checkableItemNode).remove("code")
		);
	}

	private static void updateV15Machine(final ObjectNode machine) {
		checkArray(machine.get("testCases")).forEach(testCaseNode -> {
			final ObjectNode testCase = checkObject(testCaseNode);
			// Move all fields from additionalInformation subobject into main test case object.
			final ObjectNode additionalInformation = checkObject(testCase.get("additionalInformation"));
			testCase.remove("additionalInformation");
			testCase.setAll(additionalInformation);
		});
	}
	
	@Override
	public ObjectNode convertOldData(final ObjectNode oldObject, final int oldVersion) {
		if (oldVersion <= 0) {
			if (!oldObject.has("name")) {
				oldObject.put("name", "");
			}
			if (!oldObject.has("description")) {
				oldObject.put("description", "");
			}
			if (!oldObject.has("machines")) {
				oldObject.set("machines", oldObject.arrayNode());
			}
			if (!oldObject.has("preferences")) {
				oldObject.set("preferences", oldObject.arrayNode());
			}
		}
		
		checkArray(oldObject.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			if (oldVersion <= 0) {
				updateV0Machine(machine);
			}
			if (oldVersion <= 1) {
				updateV1Machine(machine);
			}
			if (oldVersion <= 2) {
				updateV2Machine(machine);
			}
			if (oldVersion <= 3) {
				updateV3Machine(machine);
			}
			if (oldVersion <= 4) {
				updateV4Machine(machine);
			}
			if (oldVersion <= 5) {
				updateV5Machine(machine);
			}
			if (oldVersion <= 6) {
				updateV6Machine(machine);
			}
			if (oldVersion <= 7) {
				updateV7Machine(machine, this.location);
			}
			if (oldVersion <= 8) {
				updateV8Machine(machine);
			}
			if (oldVersion <= 9) {
				updateV9Machine(machine);
			}
			if (oldVersion <= 10) {
				updateV10Machine(machine);
			}
			if (oldVersion <= 11) {
				updateV11Machine(machine);
			}
			if (oldVersion <= 12) {
				updateV12Machine(machine);
			}
			if (oldVersion <= 13) {
				updateV13Machine(machine);
			}
			if (oldVersion <= 14) {
				updateV14Machine(machine);
			}
			if (oldVersion <= 15) {
				updateV15Machine(machine);
			}
		});
		
		return oldObject;
	}

	public void setProjectLocation(Path location) {
		this.location = location;
	}
}
