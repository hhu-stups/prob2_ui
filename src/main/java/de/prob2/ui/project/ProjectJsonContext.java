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
import de.prob2.ui.animation.symbolic.testcasegeneration.TestCaseGenerationType;
import de.prob2.ui.project.preferences.Preference;
import de.prob2.ui.symbolic.SymbolicExecutionType;

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
		if (!checkableItem.has("selected")) {
			checkableItem.put("selected", true);
		}
	}
	
	private static void updateV0SymbolicCheckingItem(final ObjectNode symbolicCheckingItem) {
		// The names of some symbolic checking types have changed in the past and need to be updated.
		SymbolicExecutionType newType = null;
		if (symbolicCheckingItem.get("type").isNull()) {
			// If a project was loaded after a type renaming,
			// and the UI version in question did not support file format versioning/conversion,
			// the type will be null when the project is saved again.
			// In this case, the type needs to be restored from the code string.
			final String code = checkText(symbolicCheckingItem.get("code"));
			if ("CHECK_ASSERTIONS".equals(code)) {
				newType = SymbolicExecutionType.CHECK_STATIC_ASSERTIONS;
			}
		}
		
		if (newType != null) {
			symbolicCheckingItem.put("type", newType.name());
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
		if (TestCaseGenerationType.MCDC.name().equals(type)) {
			if (!additionalInformation.has("level")) {
				if (!"MCDC".equals(splitFirstOnColon[0])) {
					throw new JsonConversionException("First part of MCDC item code string does not contain level: " + splitOnSlash[0]);
				}
				additionalInformation.put("level", Integer.parseInt(splitFirstOnColon[1]));
			}
		} else if (TestCaseGenerationType.COVERED_OPERATIONS.name().equals(type)) {
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
			final TestCaseGenerationType testCaseGenerationType;
			final JsonNode typeNode = symbolicAnimationFormula.get("type");
			if (typeNode.isNull()) {
				// If a project contains symbolic animation items for test case generation,
				// and it is loaded and re-saved by a newer version that has separated test case generation (but no file format versioning/conversion),
				// the symbolic animation items will have their type silently set to null,
				// because the corresponding enum items have been removed.
				// In this case, the type needs to be restored from the code string.
				final String code = checkText(symbolicAnimationFormula.get("code"));
				if (code.startsWith("MCDC")) {
					testCaseGenerationType = TestCaseGenerationType.MCDC;
				} else if (code.startsWith("OPERATION")) {
					testCaseGenerationType = TestCaseGenerationType.COVERED_OPERATIONS;
				} else {
					testCaseGenerationType = null;
				}
			} else {
				final String typeName = checkText(typeNode);
				if ("MCDC".equals(typeName)) {
					testCaseGenerationType = TestCaseGenerationType.MCDC;
				} else if ("COVERED_OPERATIONS".equals(typeName)) {
					testCaseGenerationType = TestCaseGenerationType.COVERED_OPERATIONS;
				} else {
					testCaseGenerationType = null;
				}
			}
			if (testCaseGenerationType != null) {
				// If this item is for test case generation, move it into the list of test case items.
				it.remove();
				testCases.add(symbolicAnimationFormula);
				// Update/fix the type, as determined above.
				symbolicAnimationFormula.put("type", testCaseGenerationType.name());
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
	
	private static void updateV0Machine(final ObjectNode machine) {
		if (!machine.has("lastUsedPreferenceName")) {
			final String lastUsedPreferenceName;
			if (machine.has("lastUsed")) {
				lastUsedPreferenceName = checkText(checkObject(machine.get("lastUsed")).get("name"));
			} else {
				lastUsedPreferenceName = Preference.DEFAULT.getName();
			}
			machine.put("lastUsedPreferenceName", lastUsedPreferenceName);
		}
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
		if (!machine.has("modelcheckingItems")) {
			machine.set("modelcheckingItems", machine.arrayNode());
		}
	}
	
	private static void updateV0Project(final ObjectNode project) {
		if (!project.has("name")) {
			project.put("name", "");
		}
		if (!project.has("description")) {
			project.put("description", "");
		}
		if (!project.has("machines")) {
			project.set("machines", project.arrayNode());
		}
		checkArray(project.get("machines")).forEach(machineNode ->
			updateV0Machine(checkObject(machineNode))
		);
		if (!project.has("preferences")) {
			project.set("preferences", project.arrayNode());
		}
	}
	
	private static void updateV1Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
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
		});
	}

	private static void updateV2Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			checkArray(machine.get("modelcheckingItems")).forEach(modelCheckingItemNode -> {
				final ObjectNode modelCheckingItem = checkObject(modelCheckingItemNode);
				if (!modelCheckingItem.has("nodesLimit")) {
					modelCheckingItem.put("nodesLimit", "-");
				}
			});
		});
	}

	private static void updateV3Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			checkArray(machine.get("modelcheckingItems")).forEach(modelCheckingItemNode -> {
				final ObjectNode modelCheckingItem = checkObject(modelCheckingItemNode);
				if (!modelCheckingItem.has("timeLimit")) {
					modelCheckingItem.put("timeLimit", "-");
				}
			});
		});
	}

	private static void updateV4Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = (ObjectNode)machineNode;
			checkArray(machine.get("symbolicCheckingFormulas")).forEach(symbolicCheckingItemNode -> {
				final ObjectNode symbolicCheckingItem = checkObject(symbolicCheckingItemNode);
				final String oldCheckingType = checkText(symbolicCheckingItem.get("type"));
				if ("IC3".equals(oldCheckingType) || "TINDUCTION".equals(oldCheckingType) || "KINDUCTION".equals(oldCheckingType) || "BMC".equals(oldCheckingType)) {
					assert checkText(symbolicCheckingItem.get("name")).equals(oldCheckingType);
					assert checkText(symbolicCheckingItem.get("code")).equals(oldCheckingType);
					symbolicCheckingItem.put("type", "SYMBOLIC_MODEL_CHECK");
				}
			});
		});
	}

	private static void updateV5Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			if(!machine.has("visBVisualisation")) {
				machine.put("visBVisualisation", "");
			}
		});
	}

	private static void updateV6Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			final String visBLocation = checkText(machine.get("visBVisualisation"));
			if(visBLocation.isEmpty()) {
				machine.set("visBVisualisation", machine.nullNode());
			}
		});
	}

	private static void updateV7Project(final ObjectNode project, final Path location) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			final JsonNode visBLocationNode = machine.get("visBVisualisation");
			if(!visBLocationNode.isNull()) {
				Path visBLocation = Paths.get(checkText(visBLocationNode));
				if(visBLocation.isAbsolute()) {
					Path newVisBLocationPath = location.getParent().relativize(visBLocation);
					machine.put("visBVisualisation", newVisBLocationPath.toString());
				}
			}
		});
	}

	private static void updateV8Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
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
		});
	}

	private static void updateV9Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			machine.set("simulationItems", machine.arrayNode());
		});
	}

	private static void updateV10Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			checkArray(machine.get("modelcheckingItems")).forEach(modelCheckingItemNode -> {
				final ObjectNode modelCheckingItem = checkObject(modelCheckingItemNode);
				if (!modelCheckingItem.has("goal")) {
					modelCheckingItem.put("goal", "-");
				}
			});
		});
	}

	private static void updateV11Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			if(!machine.has("simulation")) {
				machine.set("simulation", machine.nullNode());
			}
		});
	}

	private static void updateV12Project(final ObjectNode project) {
		checkArray(project.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			for (final String checkableItemFieldName : new String[] {"symbolicCheckingFormulas", "symbolicAnimationFormulas", "testCases"}) {
				checkArray(machine.get(checkableItemFieldName)).forEach(checkableItemNode ->
					checkObject(checkableItemNode).remove("description")
				);
			}
		});
	}
	
	@Override
	public ObjectNode convertOldData(final ObjectNode oldObject, final int oldVersion) {
		if (oldVersion <= 0) {
			updateV0Project(oldObject);
		}
		if (oldVersion <= 1) {
			updateV1Project(oldObject);
		}
		if (oldVersion <= 2) {
			updateV2Project(oldObject);
		}
		if (oldVersion <= 3) {
			updateV3Project(oldObject);
		}
		if (oldVersion <= 4) {
			updateV4Project(oldObject);
		}
		if (oldVersion <= 5) {
			updateV5Project(oldObject);
		}
		if (oldVersion <= 6) {
			updateV6Project(oldObject);
		}
		if (oldVersion <= 7) {
			updateV7Project(oldObject, location);
		}
		if (oldVersion <= 8) {
			updateV8Project(oldObject);
		}
		if (oldVersion <= 9) {
			updateV9Project(oldObject);
		}
		if (oldVersion <= 10) {
			updateV10Project(oldObject);
		}
		if (oldVersion <= 11) {
			updateV11Project(oldObject);
		}
		if (oldVersion <= 12) {
			updateV12Project(oldObject);
		}
		return oldObject;
	}

	public void setProjectLocation(Path location) {
		this.location = location;
	}
}
