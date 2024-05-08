package de.prob2.ui.project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import de.prob.json.JacksonManager;
import de.prob.json.JsonConversionException;

class ProjectJsonContext extends JacksonManager.Context<Project> {
	// From VOParser.scc:
	// identifier_literal = (letter | underscore) (letter | underscore | digit | dot | comma)*;
	private static final Pattern VALIDATION_TASK_ID_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_.,]*$");

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

	private static void updateV16Machine(final ObjectNode machine) {
		if(!machine.has("requirements")) {
			machine.set("requirements", machine.arrayNode());
		}
	}

	private static void updateV17Machine(final ObjectNode machine) {
		final ArrayNode symbolicCheckingFormulas = checkArray(machine.get("symbolicCheckingFormulas"));
		final ArrayNode symbolicAnimationFormulas = checkArray(machine.get("symbolicAnimationFormulas"));

		// In the past, some symbolic item types were moved back and forth
		// between symbolic checking and animation.
		// If a formula with one of these types was created before the type was moved,
		// the formula might still be saved under the wrong category.
		// This doesn't cause an error on loading or saving,
		// because the two symbolic categories use the same base class and data format.
		// The error is only detected once the user tries to check/execute the formula.
		// To fix this, search both lists for formulas of types that have been moved
		// and move the formulas into the correct list.

		for (final Iterator<JsonNode> it = symbolicCheckingFormulas.iterator(); it.hasNext();) {
			final ObjectNode formula = checkObject(it.next());
			final String type = checkText(formula.get("type"));
			if ("SEQUENCE".equals(type) || "FIND_VALID_STATE".equals(type)) {
				symbolicAnimationFormulas.add(formula);
				it.remove();
			}
		}

		for (final Iterator<JsonNode> it = symbolicAnimationFormulas.iterator(); it.hasNext();) {
			final ObjectNode formula = checkObject(it.next());
			final String type = checkText(formula.get("type"));
			if ("DEADLOCK".equals(type) || "FIND_REDUNDANT_INVARIANTS".equals(type)) {
				symbolicCheckingFormulas.add(formula);
				it.remove();
			}
		}
	}

	private static void updateV18Machine(final ObjectNode machine) {
		checkArray(machine.get("simulationItems")).forEach(simulationItemNode -> {
			final ObjectNode simulationItem = checkObject(simulationItemNode);
			final ObjectNode additionalInformation = checkObject(simulationItem.get("information"));
			if (!additionalInformation.has("MAX_STEPS_BEFORE_PROPERTY")) {
				additionalInformation.put("MAX_STEPS_BEFORE_PROPERTY", 0);
			}
		});
	}

	private static void updateV19Machine(final ObjectNode machine) {
		if(!machine.has("historyChartItems")) {
			machine.set("historyChartItems", machine.arrayNode());
		}
	}

	private static void updateV20Machine(final ObjectNode machine) {
		if(!machine.has("validationObligations")) {
			machine.set("validationObligations", machine.arrayNode());
		}
		if(!machine.has("validationTasks")) {
			machine.set("validationTasks", machine.arrayNode());
		}
	}

	private static void updateV21Machine(final ObjectNode machine) {
		if(machine.has("validationObligations")) {
			machine.remove("validationObligations");
		}
	}

	private static void updateV22Project(final ObjectNode project) {
		ArrayNode machines = checkArray(project.get("machines"));
		ArrayNode projectRequirements = project.arrayNode();
		machines.forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			if(!machine.has("requirements")) {
				machine.set("requirements", machine.arrayNode());
			}
			ArrayNode requirements = checkArray(machine.get("requirements"));
			projectRequirements.addAll(requirements);
			machine.remove("requirements");
			ArrayNode machineVOs = project.arrayNode();
			requirements.forEach(requirementNode -> {
				final ObjectNode requirement = checkObject(requirementNode);
				ArrayNode validationObligations = checkArray(requirement.get("validationObligations"));
				machineVOs.addAll(validationObligations);
				requirement.remove("validationObligations");
			});
			if (!machine.has("validationObligations")) {
				machine.set("validationObligations", machineVOs);
			}
		});
		if (!project.has("requirements")) {
			project.set("requirements", projectRequirements);
		}
	}

	private static void updateV23Machine(final ObjectNode machine) {
		ArrayNode simulations = machine.arrayNode();
		if(machine.has("simulation")) {
			JsonNode jsonNode = machine.get("simulation");
			machine.remove("simulation");
			if(!jsonNode.isNull()) {
				String simulation = checkText(jsonNode);
				if(!simulation.isEmpty()) {
					simulations.add(simulation);
				}
			}
		}
		machine.set("simulations", simulations);
	}

	private static void updateV24Machine(final ObjectNode machine) {
		ArrayNode simulations = machine.arrayNode();
		if(machine.has("simulations")) {
			ArrayNode arrayNode = checkArray(machine.get("simulations"));
			int i = 0;
			for(JsonNode jsonNode : arrayNode) {
				ObjectNode objectNode = machine.objectNode();
				objectNode.set("path", jsonNode);
				if(i == 0) {
					// Adapt simulation items from machine
					// In earlier versions of machine, there was only one SimB file
					objectNode.set("simulationItems", checkArray(machine.get("simulationItems")));
				} else {
					objectNode.set("simulationItems", machine.arrayNode());
				}
				simulations.add(objectNode);
				i++;
			}
		}
		if(machine.has("simulationItems")) {
			machine.remove("simulationItems");
		}
		machine.set("simulations", simulations);
	}

	private static void updateV25Machine(final ObjectNode machine, final Path projectLocation) {
		if(machine.has("simulations")) {
			ArrayNode simulations = checkArray(machine.get("simulations"));
			for(JsonNode simulation : simulations) {
				ObjectNode simulationObject = checkObject(simulation);
				if(!simulationObject.isNull()) {
					Path simBLocation = Paths.get(checkText(simulationObject.get("path")));
					if(simBLocation.isAbsolute()) {
						Path newSimBLocation = projectLocation.getParent().relativize(simBLocation);
						simulationObject.remove("path");
						simulationObject.put("path", newSimBLocation.toString());
					}
				}
			}
		}
	}

	private static void updateV26Machine(final ObjectNode machine) {
		final ArrayNode traceObjects = machine.arrayNode();
		for (final JsonNode traceLocationNode : checkArray(machine.get("traces"))) {
			final ObjectNode traceObject = traceObjects.addObject();
			traceObject.set("location", traceLocationNode);
			traceObject.put("selected", true);
		}
		machine.set("traces", traceObjects);
	}

	private static void updateV27Machine(final ObjectNode machine) {
		final ArrayNode validationTasksArray = checkArray(machine.get("validationTasks"));

		final Map<String, String> idByParams = new HashMap<>();
		for (final JsonNode taskNode : validationTasksArray) {
			final ObjectNode task = checkObject(taskNode);
			final String id = checkText(task.get("id"));
			final String parameters = checkText(task.get("parameters"));
			idByParams.put(parameters, id);
		}

		final ArrayNode modelcheckingItems = checkArray(machine.get("modelcheckingItems"));
		for (final JsonNode itemNode : modelcheckingItems) {
			final ObjectNode item = checkObject(itemNode);
			final ObjectNode options = checkObject(item.get("options"));
			final ArrayNode prologOptions = checkArray(options.get("options"));
			final StringJoiner parameters = new StringJoiner(", ");
			for (final JsonNode option : prologOptions) {
				parameters.add(checkText(option));
			}
			item.put("id", idByParams.get(parameters.toString()));
		}

		final ArrayNode ltlFormulas = checkArray(machine.get("ltlFormulas"));
		for (final JsonNode formulaNode : ltlFormulas) {
			final ObjectNode formula = checkObject(formulaNode);
			final String code = checkText(formula.get("code"));
			formula.put("id", idByParams.get(code));
		}

		final ArrayNode symbolicCheckingFormulas = checkArray(machine.get("symbolicCheckingFormulas"));
		for (final JsonNode formulaNode : symbolicCheckingFormulas) {
			final ObjectNode formula = checkObject(formulaNode);
			final String type = checkText(formula.get("type"));
			final String code = checkText(formula.get("code"));
			final String parameters;
			if ("INVARIANT".equals(type)) {
				parameters = String.format(Locale.ROOT, "%s(%s)", type, code.isEmpty() ? "all" : code);
			} else if ("DEADLOCK".equals(type)) {
				parameters = String.format(Locale.ROOT, "%s(%s)", type, code);
			} else if ("SYMBOLIC_MODEL_CHECK".equals(type)) {
				parameters = code;
			} else {
				parameters = type;
			}
			formula.put("id", idByParams.get(parameters));
		}

		final ArrayNode traces = checkArray(machine.get("traces"));
		for (final JsonNode traceNode : traces) {
			final ObjectNode trace = checkObject(traceNode);
			final String location = checkText(trace.get("location"));
			final String[] split = location.split("/");
			final String fileName = split[split.length - 1];
			trace.put("id", idByParams.get(fileName));
		}

		final ArrayNode simulations = checkArray(machine.get("simulations"));
		for (final JsonNode simulationNode : simulations) {
			final ObjectNode simulation = checkObject(simulationNode);
			for (final JsonNode simulationItemNode : checkArray(simulation.get("simulationItems"))) {
				final ObjectNode simulationItem = checkObject(simulationItemNode);
				final ObjectNode information = checkObject(simulationItem.get("information"));
				final StringJoiner configuration = new StringJoiner(",\n");
				information.fields().forEachRemaining(entry ->
					configuration.add(String.format(Locale.ROOT, "%s : %s", entry.getKey(), entry.getValue().asText()))
				);
				simulationItem.put("id", idByParams.get(configuration.toString()));
			}
		}

		machine.remove("validationTasks");
	}

	private static void updateV28Machine(ObjectNode machine) {
		checkArray(machine.get("modelcheckingItems")).forEach(modelCheckingItemNode -> {
			ObjectNode modelCheckingItem = checkObject(modelCheckingItemNode);
			for (String key : new String[] {"nodesLimit", "timeLimit"}) {
				if (modelCheckingItem.has(key)) {
					String content = checkText(modelCheckingItem.get(key));
					if ("-".equals(content)) {
						modelCheckingItem.remove(key);
					} else {
						modelCheckingItem.put(key, Integer.parseInt(content));
					}
				}
			}

			for (String key : new String[] {"goal"}) {
				if (modelCheckingItem.has(key)) {
					String content = checkText(modelCheckingItem.get(key));
					if ("-".equals(content)) {
						modelCheckingItem.remove(key);
					}
				}
			}

			ObjectNode options = checkObject(modelCheckingItem.get("options"));
			ArrayNode prologOptions = checkArray(options.get("options"));
			for (Iterator<JsonNode> it = prologOptions.iterator(); it.hasNext();) {
				if ("INSPECT_EXISTING_NODES".equals(it.next().textValue())) {
					it.remove();
				}
			}
			modelCheckingItem.set("options", prologOptions);
		});
	}

	private static void updateV29Machine(ObjectNode machine) {
		checkArray(machine.get("modelcheckingItems")).forEach(modelCheckingItemNode -> {
			ObjectNode modelCheckingItem = checkObject(modelCheckingItemNode);
			ArrayNode options = checkArray(modelCheckingItem.get("options"));
			boolean breadthFirst = false;
			boolean depthFirst = false;
			boolean ignoreOtherErrors = false;
			for (Iterator<JsonNode> it = options.iterator(); it.hasNext();) {
				final String text = it.next().textValue();
				if ("BREADTH_FIRST_SEARCH".equals(text)) {
					breadthFirst = true;
					it.remove();
				} else if ("DEPTH_FIRST_SEARCH".equals(text)) {
					depthFirst = true;
					it.remove();
				} else if ("FIND_OTHER_ERRORS".equals(text)) {
					ignoreOtherErrors = true;
					it.remove();
				}
			}
			if (ignoreOtherErrors) {
				options.add("IGNORE_OTHER_ERRORS");
			}
			final String searchStrategy;
			if (breadthFirst) {
				searchStrategy = "BREADTH_FIRST";
			} else if (depthFirst) {
				searchStrategy = "DEPTH_FIRST";
			} else {
				searchStrategy = "MIXED_BF_DF";
			}
			modelCheckingItem.put("searchStrategy", searchStrategy);
		});
	}

	private static void updateV30Project(final ObjectNode project) {
		final ArrayNode requirements = checkArray(project.get("requirements"));
		final Map<String, ArrayNode> vosByRequirement = new HashMap<>();
		requirements.forEach(requirementNode -> {
			final ObjectNode requirement = checkObject(requirementNode);
			final ArrayNode vosForRequirement = requirement.putArray("validationObligations");
			vosByRequirement.put(checkText(requirement.get("name")), vosForRequirement);
		});

		final ArrayNode machines = checkArray(project.get("machines"));
		machines.forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			final String machineName = checkText(machine.get("name"));
			final ArrayNode vosForMachine = checkArray(machine.remove("validationObligations"));
			final Map<String, String> expressionsByRequirement = new HashMap<>();
			vosForMachine.forEach(voNode -> {
				final ObjectNode vo = checkObject(voNode);
				String expression = checkText(vo.get("expression"));
				final boolean needsParentheses = !VALIDATION_TASK_ID_PATTERN.matcher(expression).matches();

				final String requirementName = checkText(vo.get("requirement"));
				if (expressionsByRequirement.containsKey(requirementName)) {
					// If there were multiple validation obligations for the same machine and requirement,
					// join them using a conjunction.
					// To avoid operator precedence issues,
					// add parentheses around everything that's not just a simple task ID.
					if (needsParentheses) {
						expression = "(" + expression + ")";
					}
					expression = expressionsByRequirement.get(requirementName) + " & " + expression;
				}
				expressionsByRequirement.put(requirementName, expression);
			});

			expressionsByRequirement.forEach((requirementName, expression) -> {
				final ArrayNode vosForRequirement = vosByRequirement.get(requirementName);
				final ObjectNode vo = vosForRequirement.objectNode();
				vo.put("machine", machineName);
				vo.put("expression", expression);
				vosForRequirement.add(vo);
			});
		});
	}


	private static void updateV31Machine(ObjectNode machine) {
		final ArrayNode ltlFormulas = checkArray(machine.get("ltlFormulas"));
		for (final JsonNode formulaNode : ltlFormulas) {
			final ObjectNode formula = checkObject(formulaNode);
			formula.put("expectedResult", "true");
		}
	}

	private static void updateV32Machine(final ObjectNode machine) {
		if(!machine.has("proofObligationItems")) {
			machine.set("proofObligationItems", machine.arrayNode());
		}
	}

	private static void updateV33Machine(final ObjectNode machine) {
		if(!machine.has("dotVisualizationItems")) {
			machine.set("dotVisualizationItems", machine.objectNode());
		}
		if(!machine.has("tableVisualizationItems")) {
			machine.set("tableVisualizationItems", machine.objectNode());
		}
	}

	private static void updateV34Machine(final ObjectNode machine) {
		final ArrayNode proofObligations = checkArray(machine.get("proofObligationItems"));
		for (final Iterator<JsonNode> iterator = proofObligations.iterator(); iterator.hasNext();) {
			final JsonNode poNode = iterator.next();
			final ObjectNode po = checkObject(poNode);
			po.remove("selected");
			po.remove("description");
			if (poNode.get("id").isNull()) {
				iterator.remove();
			}
		}
	}

	private static void updateV35Machine(final ObjectNode machine) {
		checkArray(machine.get("ltlPatterns")).forEach(patternNode ->
				checkObject(patternNode).remove("selected")
		);

		for (final String key : new String[] {"dotVisualizationItems", "tableVisualizationItems"}) {
			final ObjectNode itemsByType = checkObject(machine.get(key));
			itemsByType.fields().forEachRemaining(e -> {
				for (final JsonNode itemNode : checkArray(e.getValue())) {
					final ObjectNode item = checkObject(itemNode);
					item.remove("selected");
				}
			});
		}
	}

	private static void updateV36Machine(final ObjectNode machine) {
		JsonNode ltlFormulas = machine.get("ltlFormulas");
		machine.set("temporalFormulas", ltlFormulas);
		machine.remove("ltlFormulas");
		checkArray(machine.get("temporalFormulas")).forEach(temporalNode ->
				checkObject(temporalNode).put("type", "LTL")
		);
	}

	private static void updateV37Machine(final ObjectNode machine) {
		checkArray(machine.get("temporalFormulas")).forEach(
			temporalNode -> checkObject(temporalNode).put("stateLimit", -1)
		);
	}

	private static void updateV38Machine(final ObjectNode machine) {
		checkArray(machine.get("temporalFormulas")).forEach(
			node -> checkObject(node).put("taskType", "TEMPORAL")
		);
		checkArray(machine.get("symbolicCheckingFormulas")).forEach(
			node -> checkObject(node).put("taskType", "SYMBOLIC")
		);
		checkArray(machine.get("traces")).forEach(
			node -> checkObject(node).put("taskType", "REPLAY_TRACE")
		);
		checkArray(machine.get("modelcheckingItems")).forEach(
			node -> checkObject(node).put("taskType", "MODEL_CHECKING")
		);
		// POs are saved via SavedProofObligationItem
		checkArray(machine.get("simulations")).forEach(
			modelNode -> checkArray(checkObject(modelNode).get("simulationItems"))
				.forEach(node -> checkObject(node).put("taskType", "SIMULATION"))
		);
		checkObject(machine.get("dotVisualizationItems")).forEach(
			listNode -> checkArray(listNode)
				.forEach(node -> checkObject(node).put("taskType", "DYNAMIC_FORMULA"))
		);
		checkObject(machine.get("tableVisualizationItems")).forEach(
			listNode -> checkArray(listNode)
				.forEach(node -> checkObject(node).put("taskType", "DYNAMIC_FORMULA"))
		);
	}

	private static void updateV39Machine(final ObjectNode machine) {
		ArrayNode validationTasks;
		JsonNode validationTasksNode = machine.get("validationTasks");
		if (validationTasksNode == null) {
			validationTasks = machine.arrayNode();
			machine.set("validationTasks", validationTasks);
		} else {
			validationTasks = checkArray(validationTasksNode);
		}

		checkArray(machine.remove("temporalFormulas"))
			.forEach(node -> validationTasks.add(checkObject(node)));
	}

	private static void updateV40Machine(final ObjectNode machine) {
		ArrayNode validationTasks = checkArray(machine.get("validationTasks"));
		checkArray(machine.remove("symbolicCheckingFormulas"))
			.forEach(node -> validationTasks.add(checkObject(node)));
	}

	private static void updateV41Machine(final ObjectNode machine) {
		ArrayNode validationTasks = checkArray(machine.get("validationTasks"));
		checkArray(machine.remove("traces"))
			.forEach(node -> validationTasks.add(checkObject(node)));
	}

	private static void updateV42Machine(final ObjectNode machine) {
		ArrayNode validationTasks = checkArray(machine.get("validationTasks"));
		checkArray(machine.remove("modelcheckingItems"))
			.forEach(node -> validationTasks.add(checkObject(node)));
	}

	private static void updateV43Machine(final ObjectNode machine) {
		ArrayNode validationTasks = checkArray(machine.get("validationTasks"));
		checkObject(machine.remove("dotVisualizationItems")).forEach(listNode ->
			checkArray(listNode).forEach(node -> {
				ObjectNode obj = checkObject(node);
				obj.put("taskType", "DOT_FORMULA");
				validationTasks.add(obj);
			})
		);
		checkObject(machine.remove("tableVisualizationItems")).forEach(listNode ->
			checkArray(listNode).forEach(node -> {
				ObjectNode obj = checkObject(node);
				obj.put("taskType", "TABLE_FORMULA");
				validationTasks.add(obj);
			})
		);
	}

	private static void updateV44Machine(final ObjectNode machine) {
		ArrayNode validationTasks = checkArray(machine.get("validationTasks"));
		checkArray(machine.get("simulations")).forEach(modelNode -> {
			ObjectNode model = checkObject(modelNode);
			checkArray(model.remove("simulationItems")).forEach(node -> {
				ObjectNode obj = checkObject(node);
				obj.put("simulationPath", checkText(model.get("path")));
				validationTasks.add(obj);
			});
		});
	}

	private static void updateV45Machine(final ObjectNode machine) {
		ArrayNode validationTasks = checkArray(machine.get("validationTasks"));
		checkArray(machine.remove("proofObligationItems"))
			.forEach(node -> {
				ObjectNode obj = checkObject(node);
				obj.put("taskType", "PROOF_OBLIGATION");
				validationTasks.add(obj);
			});
	}

	private static void updateV46Machine(final ObjectNode machine) {
		checkArray(machine.get("validationTasks")).forEach(taskNode -> {
			ObjectNode task = checkObject(taskNode);
			String taskType = checkText(task.get("taskType"));
			if ("MODEL_CHECKING".equals(taskType)) {
				task.set("selected", task.remove("shouldExecute"));
			}
		});
	}

	private static void updateV47Machine(final ObjectNode machine) {
		ArrayNode validationTasks = checkArray(machine.get("validationTasks"));
		validationTasks.forEach(taskNode -> {
			ObjectNode task = checkObject(taskNode);
			String taskType = checkText(task.get("taskType"));
			if ("TEMPORAL".equals(taskType)) {
				task.replace("taskType", task.remove("type"));
			}
		});
	}

	private static void updateV48Machine(final ObjectNode machine) {
		ArrayNode validationTasks = checkArray(machine.get("validationTasks"));
		validationTasks.forEach(taskNode -> {
			ObjectNode task = checkObject(taskNode);
			String taskType = checkText(task.get("taskType"));
			if ("SYMBOLIC".equals(taskType)) {
				String symbolicType = checkText(task.remove("type"));
				String code = checkText(task.remove("code"));
				String newTaskType = switch (symbolicType) {
					case "INVARIANT" -> {
						task.put("operationName", code.isEmpty() ? null : code);
						yield "CBC_INVARIANT_PRESERVATION_CHECKING";
					}
					case "DEADLOCK" -> {
						task.put("predicate", code);
						yield "CBC_DEADLOCK_FREEDOM_CHECKING";
					}
					case "CHECK_REFINEMENT" -> "CBC_REFINEMENT_CHECKING";
					case "CHECK_STATIC_ASSERTIONS" -> "CBC_STATIC_ASSERTION_CHECKING";
					case "CHECK_DYNAMIC_ASSERTIONS" -> "CBC_DYNAMIC_ASSERTION_CHECKING";
					case "CHECK_WELL_DEFINEDNESS" -> "WELL_DEFINEDNESS_CHECKING";
					case "FIND_REDUNDANT_INVARIANTS" -> "CBC_FIND_REDUNDANT_INVARIANTS";
					case "SYMBOLIC_MODEL_CHECK" -> {
						task.put("algorithm", code);
						yield "SYMBOLIC_MODEL_CHECKING";
					}
					default -> throw new JsonConversionException("Invalid symbolic checking type: " + symbolicType);
				};
				task.put("taskType", newTaskType);
			}
		});
	}

	private static void updateV49Machine(final ObjectNode machine) {
		ArrayNode validationTasks = checkArray(machine.get("validationTasks"));
		ArrayNode symbolicAnimationFormulas = checkArray(machine.remove("symbolicAnimationFormulas"));
		symbolicAnimationFormulas.forEach(taskNode -> {
			ObjectNode task = checkObject(taskNode);
			String symbolicType = checkText(task.remove("type"));
			String code = checkText(task.remove("code"));
			String newTaskType = switch (symbolicType) {
				case "SEQUENCE" -> {
					String[] operationNames = code.replace(" ", "").split(";");
					ArrayNode operationNamesNode = task.putArray("operationNames");
					for (String operationName : operationNames) {
						operationNamesNode.add(operationName);
					}
					yield "CBC_FIND_SEQUENCE";
				}
				case "FIND_VALID_STATE" -> {
					task.put("predicate", code);
					yield "FIND_VALID_STATE";
				}
				default -> throw new JsonConversionException("Invalid symbolic animation type: " + symbolicType);
			};
			task.put("taskType", newTaskType);
			validationTasks.add(task);
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
			if (oldVersion <= 16) {
				updateV16Machine(machine);
			}
			if (oldVersion <= 17) {
				updateV17Machine(machine);
			}
			if (oldVersion <= 18) {
				updateV18Machine(machine);
			}
			if (oldVersion <= 19) {
				updateV19Machine(machine);
			}
			if (oldVersion <= 20) {
				updateV20Machine(machine);
			}
			if (oldVersion <= 21) {
				updateV21Machine(machine);
			}
			if (oldVersion <= 22) {
				updateV22Project(oldObject);
			}
			if (oldVersion <= 23) {
				updateV23Machine(machine);
			}
			if (oldVersion <= 24) {
				updateV24Machine(machine);
			}
			if (oldVersion <= 25) {
				updateV25Machine(machine, this.location);
			}
			if (oldVersion <= 26) {
				updateV26Machine(machine);
			}
			if (oldVersion <= 27) {
				updateV27Machine(machine);
			}
			if (oldVersion <= 28) {
				updateV28Machine(machine);
			}
			if (oldVersion <= 29) {
				updateV29Machine(machine);
			}
		});

		if (oldVersion <= 30) {
			updateV30Project(oldObject);
		}

		checkArray(oldObject.get("machines")).forEach(machineNode -> {
			final ObjectNode machine = checkObject(machineNode);
			if (oldVersion <= 31) {
				updateV31Machine(machine);
			}
			if (oldVersion <= 32) {
				updateV32Machine(machine);
			}
			if (oldVersion <= 33) {
				updateV33Machine(machine);
			}
			if (oldVersion <= 34) {
				updateV34Machine(machine);
			}
			if (oldVersion <= 35) {
				updateV35Machine(machine);
			}
			if (oldVersion <= 36) {
				updateV36Machine(machine);
			}
			if (oldVersion <= 37) {
				updateV37Machine(machine);
			}
			if (oldVersion <= 38) {
				updateV38Machine(machine);
			}
			if (oldVersion <= 39) {
				updateV39Machine(machine);
			}
			if (oldVersion <= 40) {
				updateV40Machine(machine);
			}
			if (oldVersion <= 41) {
				updateV41Machine(machine);
			}
			if (oldVersion <= 42) {
				updateV42Machine(machine);
			}
			if (oldVersion <= 43) {
				updateV43Machine(machine);
			}
			if (oldVersion <= 44) {
				updateV44Machine(machine);
			}
			if (oldVersion <= 45) {
				updateV45Machine(machine);
			}
			if (oldVersion <= 46) {
				updateV46Machine(machine);
			}
			if (oldVersion <= 47) {
				updateV47Machine(machine);
			}
			if (oldVersion <= 48) {
				updateV48Machine(machine);
			}
			if (oldVersion <= 49) {
				updateV49Machine(machine);
			}
		});

		return oldObject;
	}

	public void setProjectLocation(Path location) {
		this.location = location;
	}
}
