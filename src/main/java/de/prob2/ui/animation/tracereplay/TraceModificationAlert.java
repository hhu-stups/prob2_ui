package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.check.*;
import de.prob.statespace.OperationInfo;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

public class TraceModificationAlert extends Dialog<List<PersistentTrace>> {

	private final ResourceBundle resourceBundle;
	private final StageManager stageManager;
	private final TraceModificationChecker traceModificationChecker;
	ObservableSet<RenamingDelta> selectedRenamingDelta = FXCollections.observableSet();
	ObservableMap<String, Map<TraceExplorer.MappingNames, Map<String, String>>> selectedMapping = FXCollections.observableHashMap();
	Set<String> typeIVCandidates;

	@FXML
	private TextFlow textFlow;

	@FXML
	private TitledPane typeII;

	@FXML
	private TitledPane typeIIPer;

	@FXML
	private TitledPane typeIII;

	@FXML
	private TitledPane typeIV;

	@FXML
	private Accordion toplevelAccordion;

	@FXML
	private BorderPane border;


	public TraceModificationAlert(final Injector injector, final StageManager stageManager, TraceModificationChecker traceModificationChecker, PersistentTrace oldTrace){
		this.resourceBundle = injector.getInstance(ResourceBundle.class);
		this.stageManager = stageManager;
		this.traceModificationChecker = traceModificationChecker;
		this.typeIVCandidates = traceModificationChecker.traceChecker.getTypeFinder().getTypeIV();
		stageManager.loadFXML(this, "prototypeAlertView.fxml");

		ButtonType buttonTypeI = new ButtonType(resourceBundle.getString("traceModification.alert.button.current"), ButtonBar.ButtonData.NO);
		ButtonType buttonTypeB = new ButtonType(resourceBundle.getString("traceModification.alert.button.both"), ButtonBar.ButtonData.APPLY);
		ButtonType buttonTypeN = new ButtonType(resourceBundle.getString("traceModification.alert.button.new"), ButtonBar.ButtonData.YES);

		this.setTitle(resourceBundle.getString("traceModification.alert.header.title"));
		this.setHeaderText(resourceBundle.getString("traceModification.alert.header.text"));
		this.getDialogPane().getButtonTypes().addAll(buttonTypeI, buttonTypeB, buttonTypeN);

		this.setResultConverter(param -> {
			Set<RenamingDelta> renamingDeltaHelper = new HashSet<>(selectedRenamingDelta);
			HashMap<String, Map<TraceExplorer.MappingNames, Map<String, String>>> mappingHelper = new HashMap<>(selectedMapping);
			List<PersistentTransition> selectedTrace = traceModificationChecker.traceChecker.getTraceModifier()
					.getChangelogPhase3II().get(renamingDeltaHelper).get(mappingHelper).stream()
					.flatMap(delta -> delta.getNewTransitions().stream()).collect(toList());

			if(param.getButtonData() == ButtonBar.ButtonData.NO){
				return singletonList(oldTrace);
			}else {
				if(param.getButtonData() == ButtonBar.ButtonData.APPLY){
					return  Arrays.asList(oldTrace, new PersistentTrace(oldTrace.getDescription(), selectedTrace));
				}else {
					if(param.getButtonData() == ButtonBar.ButtonData.YES){
						return singletonList(new PersistentTrace(oldTrace.getDescription(), selectedTrace));
					}else {
						return singletonList(oldTrace);
					}
				}
			}
		});
	}


	@FXML
	private void initialize(){
		stageManager.register(this);

		RenamingAnalyzerInterface renamingAnalyzer = traceModificationChecker.traceChecker.getRenamingAnalyzer();
		TraceModifier traceModifier = traceModificationChecker.traceChecker.getTraceModifier();

		VBox accordion = setTypeIIConflicts(renamingAnalyzer.getResultTypeIIAsDeltaList(), renamingAnalyzer.getResultTypeIIInit(), traceModificationChecker.traceChecker.getOldOperationInfos());
		typeII.setContent(accordion);
		typeII.textProperty().set(resourceBundle.getString("traceModification.alert.typeII") + " (" + traceModifier.getSizeTypeDetII()+ ")");
		accordion.prefHeightProperty().bindBidirectional(typeII.prefHeightProperty());
		accordion.prefWidthProperty().bindBidirectional(typeII.prefWidthProperty());
		if(!traceModifier.typeIIDetDirty()){
			typeII.setCollapsible(false);
		}

		VBox accordion2 = setTypeIIAmbiguousConflicts(renamingAnalyzer.getResultTypeIIWithCandidates());
		typeIIPer.setContent(accordion2);
		typeIIPer.textProperty().set(resourceBundle.getString("traceModification.alert.typeIIAmbiguous" ) + " (" + traceModifier.getSizeTypeNonDetII() + ")");
		if(!traceModifier.typeIINonDetDirty()){
			typeIIPer.setCollapsible(false);
		}

		VBox accordion3 = setTypeIIIConflicts(traceModifier.getChangelogPhase3II(),
				traceModificationChecker.traceChecker.getOldOperationInfos(),
				traceModificationChecker.traceChecker.getNewOperationInfos());

		typeIII.setContent(accordion3);
		typeIII.textProperty().set(resourceBundle.getString("traceModification.alert.typeIII") + " (" + traceModifier.getSizeTypeIII() + ")");

		if(!traceModifier.typeIIIDirty()){
			typeIII.setCollapsible(false);
		}


		VBox accordion4 = createTypeIVMapping(traceModifier.getChangelogPhase4(), traceModifier.getChangelogPhase3II());

		typeIV.setContent(accordion4);
		typeIV.textProperty().set(resourceBundle.getString("traceModification.alert.typeIV") + " (" + traceModifier.getSizeTypeIV() + ")");

		if(!traceModifier.typeIVDirty()){
			typeIV.setCollapsible(false);
		}


	}


	private VBox setTypeIIConflicts(List<RenamingDelta> resultTypeII, Map<String, String> resultInit,
									Map<String, OperationInfo> operationInfoMap){


		VBox result = new VBox();

		if(!resultInit.isEmpty()){
			GridPane gridPane = new GridPane();


			Label empty = new Label();
			Label newL = new Label(resourceBundle.getString("traceModification.alert.new"));
			Label oldL = new Label(resourceBundle.getString("traceModification.alert.old"));


			registerRow(gridPane, empty, oldL, newL, 0);


			registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.name")), prepareColumn(new ArrayList<>(resultInit.keySet())),
					prepareColumn(new ArrayList<>(resultInit.values())), 1);

			TitledPane titledPane = new TitledPane(resourceBundle.getString("traceModification.alert.init"), gridPane);


			result.getChildren().add(titledPane);
		}


		generateLabelView(resultTypeII, operationInfoMap, result);

		return result;
	}


	public void generateLabelView(List<RenamingDelta> resultTypeII, Map<String, OperationInfo> operationInfoMap, VBox result){


		for(RenamingDelta entry : resultTypeII){
			int row = 0;
			GridPane gridPane = new GridPane();


			Label empty = new Label();
			Label newL = new Label(resourceBundle.getString("traceModification.alert.new"));
			Label oldL = new Label(resourceBundle.getString("traceModification.alert.old"));


			row = registerRow(gridPane, empty, oldL, newL, row);


			Label operations = new Label(resourceBundle.getString("traceModification.alert.name"));


			VBox oldB = prepareColumn(singletonList(entry.getOriginalName()));
			VBox newB = prepareColumn(singletonList(entry.getDeltaName()));
			row = registerRow(gridPane, operations, oldB, newB, row);


			OperationInfo operationInfo = operationInfoMap.get(entry.getOriginalName());


			if(!operationInfo.getAllVariables().isEmpty()){
				List<String> partner = operationInfo.getAllVariables().stream().map(innerEntry -> entry.getVariables().get(innerEntry)).collect(toList());
				row = registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.variables")), prepareColumn(operationInfo.getAllVariables()),
						prepareColumn(partner), row);
			}

			if(!operationInfo.getParameterNames().isEmpty()){
				List<String> partner = operationInfo.getParameterNames().stream().map(innerEntry -> entry.getInputParameters().get(innerEntry)).collect(toList());
				row = registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.input")),
						prepareColumn(operationInfo.getParameterNames()),
						prepareColumn(partner), row);
			}

			if(!operationInfo.getParameterNames().isEmpty()){
				List<String> partner = operationInfo.getOutputParameterNames().stream().map(innerEntry -> entry.getOutputParameters().get(innerEntry)).collect(toList());
				registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.output")),
						prepareColumn(operationInfo.getOutputParameterNames()),
						prepareColumn(partner), row);
			}

			TitledPane titledPane = new TitledPane(entry.getOriginalName() + "<->" + entry.getDeltaName(), gridPane);

			result.getChildren().add(titledPane);
		}

	}


	int registerRow(GridPane gridPane, Node label, Node oldB, Node newB, int row){
		gridPane.add(label, 0, row);
		gridPane.add(oldB, 1, row);
		gridPane.add(newB, 2,row);
		GridPane.setHgrow(label, Priority.ALWAYS);
		GridPane.setHgrow(oldB, Priority.ALWAYS);
		GridPane.setHgrow(newB, Priority.ALWAYS);
		return row+1;
	}



	List<SimpleStringProperty> retractEntries2(List<String> entries, Map<String, SimpleStringProperty> goal){
		return entries.stream().map(goal::get).collect(toList());
	}


	VBox prepareColumn2(List<SimpleStringProperty> stuff){

		List<Label> oldStuffFX = stuff.stream().map(entry -> {
			if(entry != null)
			{
				Label result = new Label(entry.getName());
				result.textProperty().bindBidirectional(entry);
				return result;
			}
			else{
				return new Label("???");
			}
		}).collect(toList());


		Node[] elements = new Node[oldStuffFX.size()];
		oldStuffFX.toArray(elements);

		return new VBox(elements);
	}


	VBox prepareColumn(List<String> stuff){

		List<Label> oldStuffFX = stuff.stream().map(Label::new).collect(toList());


		Node[] elements = new Node[oldStuffFX.size()];
		oldStuffFX.toArray(elements);

		return new VBox(elements);
	}


	VBox prepareFlexibleColumn2(Map<String, StringProperty> stuff){
		List<Label> labels = stuff.values().stream().map(stringProperty -> {
			Label result = new Label();
			result.textProperty().bindBidirectional(stringProperty);
			return result;
		}).collect(toList());

		Node[] elements = new Node[labels.size()];
		labels.toArray(elements);

		return new VBox(elements);
	}


	public VBox setTypeIIAmbiguousConflicts(Map<String, List<RenamingDelta>> typeIIWithCandidates) {


		Map<Pair<String, String>, RenamingDelta> changeToDelta = typeIIWithCandidates.entrySet().stream().flatMap(entry -> entry.getValue().stream()
						.map(delta -> new Pair<>(new Pair<>(delta.getOriginalName(), delta.getDeltaName()), delta)))
				.collect(toMap(Pair::getKey, Pair::getValue));

		Map<String, OperationInfo> operationInfoMap = traceModificationChecker.traceChecker.getOldOperationInfos();

		final Set<Pair<String, String>> selected = new HashSet<>();

		VBox result = new VBox();

		for(Map.Entry<String, List<RenamingDelta>> entry : typeIIWithCandidates.entrySet()){

			StringProperty title = new SimpleStringProperty("Title");

			final Map<String, StringProperty> mapVariables = new HashMap<>();
			final Map<String, StringProperty> mapInput = new HashMap<>();
			final Map<String, StringProperty> mapOutput = new HashMap<>();

			int row = 0;
			GridPane gridPane = new GridPane();

			Label empty = new Label();
			Label newL = new Label(resourceBundle.getString("traceModification.alert.new"));
			Label oldL = new Label(resourceBundle.getString("traceModification.alert.old"));


			row = registerRow(gridPane, empty, oldL, newL, row);


			Label name = new Label(resourceBundle.getString("traceModification.alert.name"));


			VBox newB = new VBox(); // prepareColumn(Collections.singletonList(entry.getKey()));
			ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList(entry.getValue().stream().map(RenamingDelta::getDeltaName).collect(toList())));
			selected.add(new Pair<>(entry.getKey(), choiceBox.getValue()));
			newB.getChildren().add(choiceBox);
			VBox oldB = new VBox(new Label(entry.getKey()));
			row = registerRow(gridPane, name, oldB, newB, row);


			OperationInfo operationInfo = operationInfoMap.get(entry.getKey());


			if(!operationInfo.getAllVariables().isEmpty()){
				row = flexibleRow(entry.getValue().get(0).getVariables(), mapVariables,
						gridPane, row, resourceBundle.getString("traceModification.alert.variables"));

			}


			if(!operationInfo.getParameterNames().isEmpty()){
				row = flexibleRow(entry.getValue().get(0).getInputParameters(), mapInput,
						gridPane, row, resourceBundle.getString("traceModification.alert.input"));

			}

			if(!operationInfo.getOutputParameterNames().isEmpty()){
				flexibleRow(entry.getValue().get(0).getOutputParameters(), mapOutput,
						gridPane, row, resourceBundle.getString("traceModification.alert.output"));
			}



			choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
				selected.remove(new Pair<>(entry.getKey(), oldValue));
				selected.add(new Pair<>(entry.getKey(), newValue));


				Pair<String, String> key = new Pair<>(entry.getKey(), newValue);


				title.set(entry.getKey() + "<->" + newValue);

				changeToDelta.get(key).getVariables().forEach((key1, value) -> mapVariables.get(key1).set(value));

				changeToDelta.get(key).getInputParameters().forEach((key1, value) -> mapInput.get(key1).set(value));

				changeToDelta.get(key).getOutputParameters().forEach((key1, value) -> mapOutput.get(key1).set(value));

				selectedRenamingDelta.clear();
				selectedRenamingDelta.addAll(selected.stream().map(changeToDelta::get).collect(Collectors.toSet()));

			});

			choiceBox.getSelectionModel().selectFirst();
			Set<RenamingDelta> selectedRenamingDeltaPre = selected.stream().map(changeToDelta::get).collect(Collectors.toSet());
			selectedRenamingDelta.addAll(selectedRenamingDeltaPre);

			TitledPane titledPane = new TitledPane("", gridPane);
			titledPane.textProperty().bindBidirectional(title);

			result.getChildren().add(titledPane);

		}

		return result;

	}


	public int flexibleRow(Map<String, String> newValues, Map<String, StringProperty> map, GridPane gridPane, int row, String label){
		List<String> outputValues = new ArrayList<>(newValues.values());
		List<String> outputKeys = new ArrayList<>(newValues.keySet());

		map.putAll(TraceCheckerUtils.zip(outputKeys,
				outputValues.stream().map(SimpleStringProperty::new).collect(toList())));

		return registerRow(gridPane, new Label(label),
				prepareColumn(outputKeys),
				prepareFlexibleColumn2(map), row);
	}


	public VBox setTypeIIIConflicts(Map<Set<RenamingDelta>, Map<Map<String, Map<TraceExplorer.MappingNames, Map<String, String>>>, List<PersistenceDelta>>> resultTypeIIIWithTransitions,
									Map<String, OperationInfo> oldInfo, Map<String, OperationInfo> newInfo){


		if(resultTypeIIIWithTransitions.isEmpty()) return new VBox();


		Set<Map<String, Map<TraceExplorer.MappingNames, Map<String, String>>>> typeIIIRefined = resultTypeIIIWithTransitions.entrySet().stream().findFirst().get().getValue().keySet();


		if(typeIIIRefined.isEmpty()) return new VBox();

		Map<String, Map<TraceExplorer.MappingNames, Map<String, String>>> sample = typeIIIRefined.stream().findFirst().get();

		if(sample.isEmpty()) return new VBox();

		VBox result = new VBox();

		Map<String, Map<String, Map<TraceExplorer.MappingNames, Map<String, String>>>> namesToOptions =new HashMap<>();

		int i = 1;
		for(Map<String, Map<TraceExplorer.MappingNames, Map<String, String>>> entry : typeIIIRefined){
			namesToOptions.put("Option " + i, entry);
			i++;
		}

		ChoiceBox<String> options = new ChoiceBox<>(FXCollections.observableArrayList(namesToOptions.keySet()));

		result.getChildren().add(options);

		VBox inner = new VBox();
		createVBox(sample, oldInfo, newInfo, inner);
		result.getChildren().add(inner);

		options.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

			Map<String, Map<TraceExplorer.MappingNames, Map<String, String>>> newMapping = namesToOptions.get(newValue);

			createVBox(newMapping, oldInfo, newInfo, inner);

			selectedMapping.clear();
			selectedMapping.putAll(newMapping);


		});

		options.getSelectionModel().selectFirst();
		selectedMapping.putAll(namesToOptions.get(options.getValue()));

		return result;
	}



	public void createVBox(Map<String, Map<TraceExplorer.MappingNames, Map<String, String>>> resultTypeIII,
						   Map<String, OperationInfo> oldOperations,
						   Map<String, OperationInfo> newOperations, VBox result){

		result.getChildren().clear();
		for(String operationMapping : resultTypeIII.keySet()){
			OperationInfo oldOp = oldOperations.get(operationMapping);
			OperationInfo newOp = newOperations.get(operationMapping);
			result.getChildren().add(createGridPane(resultTypeIII.get(operationMapping), oldOp, newOp, operationMapping));
		}

		//Again Pass by reference
	}

	public TitledPane createGridPane(Map<TraceExplorer.MappingNames, Map<String, String>> mappings,
									 OperationInfo oldIds,
									 OperationInfo newIds,
									 String name){

		GridPane gridPane = new GridPane();

		Map<TraceExplorer.MappingNames, List<String>> oldSplitted = TraceExplorer.fillMapping(oldIds);
		Map<TraceExplorer.MappingNames, List<String>> newSplitted = TraceExplorer.fillMapping(newIds);

		int row = 0;

		Label empty = new Label();
		Label newL = new Label(resourceBundle.getString("traceModification.alert.new"));
		Label oldL = new Label(resourceBundle.getString("traceModification.alert.old"));
		row = registerRow(gridPane, empty, oldL, newL, row);

		Map<TraceExplorer.MappingNames, String> mappingsNamesToBundleNames = new HashMap<>();
		mappingsNamesToBundleNames.put(TraceExplorer.MappingNames.VARIABLES_MODIFIED, resourceBundle.getString("traceModification.alert.variables.modified"));
		mappingsNamesToBundleNames.put(TraceExplorer.MappingNames.VARIABLES_READ, resourceBundle.getString("traceModification.alert.variables.read"));
		mappingsNamesToBundleNames.put(TraceExplorer.MappingNames.INPUT_PARAMETERS, resourceBundle.getString("traceModification.alert.input"));
		mappingsNamesToBundleNames.put(TraceExplorer.MappingNames.OUTPUT_PARAMETERS, resourceBundle.getString("traceModification.alert.output"));


		for(Map.Entry<TraceExplorer.MappingNames, Map<String, String>> entry : mappings.entrySet()){
				List<String> oldVars = oldSplitted.get(entry.getKey());
				List<String> newVars = newSplitted.get(entry.getKey());
				row = fillGridPane(entry.getValue(), oldVars, newVars, mappingsNamesToBundleNames.get(entry.getKey()),row, gridPane);
		}

		return new TitledPane(name, gridPane);
	}


	public int fillGridPane(Map<String, String> mappings, List<String> oldIds , List<String> newIds, String labelTitle, int row, GridPane gridPane){

		Map<String, String> cleanedMapping = mappings.entrySet()
				.stream()
				.filter(entry -> !entry.getKey().equals(entry.getValue()))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));


		Set<String> voidCards = oldIds.stream()
				.filter(entry -> !mappings.containsKey(entry))
				.collect(Collectors.toSet());
		Set<String> dummyVoidCards = IntStream.range(0, voidCards.size()).mapToObj(element -> "void").collect(Collectors.toSet());

		Set<String> wildCards = newIds.stream()
				.filter(entry -> !mappings.containsValue(entry))
				.collect(Collectors.toSet());
		Set<String> dummyWildCards = IntStream.range(0, wildCards.size()).mapToObj(element -> "???").collect(Collectors.toSet());


		Map<String, String> mappingKeysVar =
				cleanedMapping.entrySet()
				.stream()
				.filter(element -> oldIds.contains(element.getKey()))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));


		List<Label> oldLabels = createLabel(Stream.of(voidCards, dummyWildCards, mappingKeysVar.keySet()).flatMap(Collection::stream).collect(toList()));
		List<Label> newLabels = createLabel(Stream.of(dummyVoidCards, wildCards, mappingKeysVar.values()).flatMap(Collection::stream).collect(toList()));

		if(oldLabels.size()!= 0)
		{
			VBox oldBox = new VBox();
			oldBox.getChildren().addAll(oldLabels);

			VBox newBox = new VBox();
			newBox.getChildren().addAll(newLabels);

			registerRow(gridPane, new Label(labelTitle), oldBox, newBox, row);
		}

		row++;

		return row;
		//GridPane is pass by reference...
	}

	public VBox createTypeIVMapping(Map<Set<RenamingDelta>, Map<Map<String, Map<TraceExplorer.MappingNames, Map<String, String>>>, Map<String, TraceAnalyser.AnalyserResult>>> typeIVResults,
									Map<Set<RenamingDelta>, Map<Map<String, Map<TraceExplorer.MappingNames, Map<String, String>>>, List<PersistenceDelta>>> resultTypeIIIWithTransitions){

		VBox result = new VBox();

		Set<RenamingDelta> accessCollection = new HashSet<>(selectedRenamingDelta);
		HashMap<String, Map<TraceExplorer.MappingNames, Map<String, String>>> accessMap = new HashMap<>(selectedMapping);

		Map<String, TraceAnalyser.AnalyserResult> typeIVMap = typeIVResults.get(accessCollection).get(accessMap);
		List<PersistenceDelta> typeIIIMap = resultTypeIIIWithTransitions.get(accessCollection).get(accessMap);

		result.getChildren().add(createTypeIVView(typeIVMap, typeIIIMap));

		selectedRenamingDelta.addListener((SetChangeListener<RenamingDelta>) change -> {
			if(change.wasAdded()){
				Set<RenamingDelta> accessCollectionHelper = new HashSet<>(change.getSet());
				Map<String, TraceAnalyser.AnalyserResult> currentlySelectedTypeIV = typeIVResults.get(accessCollectionHelper).get(selectedMapping);
				List<PersistenceDelta> selectedTrace = resultTypeIIIWithTransitions.get(accessCollectionHelper).get(selectedMapping);

				result.getChildren().clear();
				result.getChildren().add(createTypeIVView(currentlySelectedTypeIV, selectedTrace));
			}
		});

		selectedMapping.addListener((MapChangeListener<String, Map<TraceExplorer.MappingNames,Map<String, String>>>) change -> {
			if(change.wasAdded()){
				HashMap<String, Map<TraceExplorer.MappingNames, Map<String, String>>> accessMapHelper = new HashMap<>(change.getMap());

				Map<String, TraceAnalyser.AnalyserResult> currentlySelectedTypeIV = typeIVResults.get(selectedRenamingDelta).get(accessMapHelper);
				List<PersistenceDelta> selectedTrace = resultTypeIIIWithTransitions.get(selectedRenamingDelta).get(accessMapHelper);

				result.getChildren().clear();
				result.getChildren().add(createTypeIVView(currentlySelectedTypeIV, selectedTrace));
			}
		});




		return result;
	}

	private static GridPane createTypeIVView(Map<String, TraceAnalyser.AnalyserResult> currentlySelectedTypeIV, List<PersistenceDelta> selectedTrace){
		GridPane inner = new GridPane();

		ColumnConstraints column = new ColumnConstraints();
		column.setPercentWidth(30);
		inner.getColumnConstraints().add(column);

		ColumnConstraints column2 = new ColumnConstraints();
		column2.setPercentWidth(40);
		inner.getColumnConstraints().add(column2);

		ColumnConstraints column3 = new ColumnConstraints();
		column3.setPercentWidth(30);
		inner.getColumnConstraints().add(column3);


		int row = 0;
		for(String candidate : currentlySelectedTypeIV.keySet()){
			switch (currentlySelectedTypeIV.get(candidate)){
				case Straight:
					Label left = new Label(candidate);
					inner.add(left, 0, row);
					String partnerStraight = TraceAnalyser.calculateStraight(singleton(candidate), selectedTrace).get(candidate);
					Label rightStraight = new Label(partnerStraight);
					inner.add(new Label("is mapped to"), 1, row);
					inner.add(rightStraight, 2, row);
					break;
				case Mixed:
					Label leftMixed = new Label(candidate);
					inner.add(leftMixed, 0, row);
					Label rightMixed = new Label("different operations were treated\n as intermediate operation");
					inner.add(new Label("is mapped to different operations"), 1, row);
					inner.add(rightMixed, 2, row);
					break;
				case Removed:
					Label leftRemoved = new Label(candidate);
					inner.add(leftRemoved, 0, row);
					Label rightRemoved = new Label("This operation was removed - the trace is no longer replayable");
					inner.add(rightRemoved, 1, row);
					break;
				case Intermediate:
					Label leftIntermediate = new Label(candidate);
					inner.add(leftIntermediate, 0, row);
					List<String> partnerIntermediate = TraceAnalyser.calculateIntermediate(singleton(candidate), selectedTrace).get(candidate);
					inner.add(new Label("the operation has a new operation executed before"), 1, row);
					Label rightIntermediate1 = new Label(partnerIntermediate.get(0));
					Label rightIntermediate2 = new Label(partnerIntermediate.get(1));
					VBox intermediate = new VBox();
					intermediate.getChildren().add(rightIntermediate1);
					intermediate.getChildren().add(rightIntermediate2);
					inner.add(intermediate, 2, row);
					break;
				case MixedNames:
					Label leftMixedNames = new Label(candidate);
					inner.add(leftMixedNames,  0, row);
					Label rightMixedNames = new Label("The operation was substituted by different other operations");
					inner.add(rightMixedNames, 1, row);
					List<String> partnerIntermediate2 = TraceAnalyser.calculateIntermediate(singleton(candidate), selectedTrace).get(candidate);

					Label rightMixedNames1 = new Label(partnerIntermediate2.get(0));
					Label rightMixedNames2 = new Label(partnerIntermediate2.get(1)); //toDO WRONG111111
					VBox mixedNames = new VBox();
					mixedNames.getChildren().add(rightMixedNames1);
					mixedNames.getChildren().add(rightMixedNames2);
					inner.add(mixedNames, 2, row);
					break;
			}

			row++;
		}
		return inner;
	}


	private static List<Label> createLabel(Collection<String> collection){
		return collection.stream().map(Label::new).collect(toList());
	}






}
