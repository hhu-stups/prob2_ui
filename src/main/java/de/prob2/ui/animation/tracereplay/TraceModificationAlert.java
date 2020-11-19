package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.animator.domainobjects.AnimationMatrixEntry;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.check.Delta;
import de.prob.check.tracereplay.check.TraceCheckerUtils;
import de.prob.statespace.OperationInfo;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TraceModificationAlert extends Dialog<List<PersistentTrace>> {


	private final ResourceBundle resourceBundle;
	private final StageManager stageManager;
	private final TraceModificationChecker traceModificationChecker;
	List<PersistentTransition> selectedTrace;


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


	public TraceModificationAlert(final Injector injector, final StageManager stageManager, TraceModificationChecker traceModificationChecker){
		this.resourceBundle = injector.getInstance(ResourceBundle.class);
		this.stageManager = stageManager;
		this.traceModificationChecker = traceModificationChecker;
		stageManager.loadFXML(this, "prototypeAlertView.fxml");

		ButtonType buttonTypeI = new ButtonType(resourceBundle.getString("traceModification.alert.button.current"), ButtonBar.ButtonData.NO);
		ButtonType buttonTypeB = new ButtonType(resourceBundle.getString("traceModification.alert.button.both"), ButtonBar.ButtonData.APPLY);
		ButtonType buttonTypeN = new ButtonType(resourceBundle.getString("traceModification.alert.button.new"), ButtonBar.ButtonData.YES);

		this.setTitle(resourceBundle.getString("traceModification.alert.header.title"));
		this.setHeaderText(resourceBundle.getString("traceModification.alert.header.text"));
		this.getDialogPane().getButtonTypes().addAll(buttonTypeI, buttonTypeB, buttonTypeN);




		this.setResultConverter(param -> {

			if(param.getButtonData() == ButtonBar.ButtonData.NO){
				return Collections.singletonList(traceModificationChecker.traceChecker.getTrace());
			}else {
				if(param.getButtonData() == ButtonBar.ButtonData.APPLY){
					return  Arrays.asList(traceModificationChecker.traceChecker.getTrace(),
							new PersistentTrace(traceModificationChecker.traceChecker.getTrace().getDescription(), selectedTrace));
				}else {
					if(param.getButtonData() == ButtonBar.ButtonData.YES){
						return Collections.singletonList(new PersistentTrace(traceModificationChecker.traceChecker.getTrace().getDescription(), selectedTrace));
					}else {
						return Collections.singletonList(traceModificationChecker.traceChecker.getTrace());
					}
				}
			}
		});
	}


	@FXML
	private void initialize(){
		stageManager.register(this);

		VBox accordion = setTypeIIConflicts();
		typeII.setContent(accordion);
		typeII.textProperty().set(resourceBundle.getString("traceModification.alert.typeII") + " (" + accordion.getChildren().size()+ ")");
		accordion.prefHeightProperty().bindBidirectional(typeII.prefHeightProperty());
		accordion.prefWidthProperty().bindBidirectional(typeII.prefWidthProperty());

		VBox accordion2 = setTypeIIAmbiguousConflicts();
		typeIIPer.setContent(accordion2);
		typeIIPer.textProperty().set(resourceBundle.getString("traceModification.alert.typeIIAmbiguous" ));

		selectedTrace = traceModificationChecker.traceChecker.getTraceModifier().getLastChange();
	}


	private VBox setTypeIIConflicts(){

		Map<String, Map<String, String>> resultTypeII = traceModificationChecker.traceChecker.getDeltaFinder().getResultTypeII();
		Map<String, OperationInfo> operationInfoMap = traceModificationChecker.traceChecker.getOldOperationInfos();
		Map<String, String> globalVars = traceModificationChecker.traceChecker.getDeltaFinder().getResultTypeIIInit();

		VBox result = new VBox();

		if(traceModificationChecker.traceChecker.getTypeFinder().getInitIsTypeIorIICandidate()
			&&!traceModificationChecker.traceChecker.getDeltaFinder().getResultTypeIIInit().isEmpty()){
			int row = 0;
			GridPane gridPane = new GridPane();


			Label empty = new Label();
			Label newL = new Label(resourceBundle.getString("traceModification.alert.current"));
			Label oldL = new Label(resourceBundle.getString("traceModification.alert.old"));


			row = registerRow(gridPane, empty, oldL, newL, row);


			registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.name")), prepareColumn(new ArrayList<>(globalVars.keySet())),
					prepareColumn(new ArrayList<>(globalVars.values())), row);

			TitledPane titledPane = new TitledPane(resourceBundle.getString("traceModification.alert.init"), gridPane);


			result.getChildren().add(titledPane);
		}


		for(Map.Entry<String, Map<String, String>> entry : resultTypeII.entrySet()){
			int row = 0;
			GridPane gridPane = new GridPane();



			Label empty = new Label();
			Label newL = new Label(resourceBundle.getString("traceModification.alert.current"));
			Label oldL = new Label(resourceBundle.getString("traceModification.alert.old"));


			row = registerRow(gridPane, empty, oldL, newL, row);


			Label operations = new Label(resourceBundle.getString("traceModification.alert.name"));


			VBox oldB = prepareColumn(Collections.singletonList(entry.getKey()));
			VBox newB = prepareColumn(Collections.singletonList(entry.getValue().get(entry.getKey())));
			row = registerRow(gridPane, operations, oldB, newB, row);


			OperationInfo operationInfo = operationInfoMap.get(entry.getKey());
			List<String> variables = Stream.of(operationInfo.getReadVariables(), operationInfo.getNonDetWrittenVariables(), operationInfo.getWrittenVariables())
					.flatMap(Collection::stream).distinct().collect(Collectors.toList());

			Map<String, String> partner = resultTypeII.get(entry.getKey());

			if(!variables.isEmpty()){
				row = registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.variables")), prepareColumn(variables),
						prepareColumn(retractEntries(variables,partner)), row);
			}

			if(!operationInfo.getParameterNames().isEmpty()){
				row = registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.input")),
						prepareColumn(operationInfo.getParameterNames()),
						prepareColumn(retractEntries(operationInfo.getParameterNames(),partner)), row);
			}

			if(!operationInfo.getParameterNames().isEmpty()){
				registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.output")),
						prepareColumn(operationInfo.getOutputParameterNames()),
						prepareColumn(retractEntries(operationInfo.getOutputParameterNames(),partner)), row);
			}


			TitledPane titledPane = new TitledPane(entry.getKey() + "<->" + entry.getValue().get(entry.getKey()), gridPane);


			result.getChildren().add(titledPane);


		}

			return result;
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

	List<String> retractEntries(List<String> entries, Map<String, String> goal){
		return entries.stream().map(goal::get).collect(Collectors.toList());
	}


	VBox prepareColumn(List<String> stuff){

		List<Label> oldStuffFX = stuff.stream().map(Label::new).collect(Collectors.toList());


		Node[] elements = new Node[oldStuffFX.size()];
		oldStuffFX.toArray(elements);

		return new VBox(elements);
	}


	VBox prepareFlexibleColumn(List<StringProperty> stuff){
		List<Label> labels = stuff.stream().map(stringProperty -> {
			Label result = new Label();
			result.textProperty().bindBidirectional(stringProperty);
			return result;
		}).collect(Collectors.toList());

		Node[] elements = new Node[labels.size()];
		labels.toArray(elements);

		return new VBox(elements);
	}


	VBox prepareFlexibleColumn2(Map<String, StringProperty> stuff){
		List<Label> labels = stuff.values().stream().map(stringProperty -> {
			Label result = new Label();
			result.textProperty().bindBidirectional(stringProperty);
			return result;
		}).collect(Collectors.toList());

		Node[] elements = new Node[labels.size()];
		labels.toArray(elements);

		return new VBox(elements);
	}


	public VBox setTypeIIAmbiguousConflicts() {


		Map<String, List<Delta>> resultTypeII = traceModificationChecker.traceChecker.getTypeIICandidates();
		Map<Pair<String, String>, Delta> changeToDelta = resultTypeII.entrySet().stream().flatMap(entry -> entry.getValue().stream()
						.map(delta -> new Pair<>(new Pair<>(delta.getOriginalName(), delta.getDeltaName()), delta)))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		Map<String, OperationInfo> operationInfoMap = traceModificationChecker.traceChecker.getOldOperationInfos();

		Map<Set<Delta>, List<PersistentTransition>> changelogPhase2II = traceModificationChecker.traceChecker.getTraceModifier().getChangelogPhase2II();

		final Set<Pair<String, String>> selected = new HashSet<>();


		VBox result = new VBox();


		for(Map.Entry<String, List<Delta>> entry : resultTypeII.entrySet()){

			StringProperty title = new SimpleStringProperty("Title");

			final Map<String, StringProperty> mapVariables = new HashMap<>();
			final Map<String, StringProperty> mapInput = new HashMap<>();
			final Map<String, StringProperty> mapOutput = new HashMap<>();

			int row = 0;
			GridPane gridPane = new GridPane();

			Label empty = new Label();
			Label newL = new Label(resourceBundle.getString("traceModification.alert.current"));
			Label oldL = new Label(resourceBundle.getString("traceModification.alert.old"));


			row = registerRow(gridPane, empty, oldL, newL, row);


			Label name = new Label(resourceBundle.getString("traceModification.alert.name"));


			VBox newB = new VBox(); // prepareColumn(Collections.singletonList(entry.getKey()));
			ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList(entry.getValue().stream().map(Delta::getDeltaName).collect(Collectors.toList())));
			choiceBox.getSelectionModel().selectFirst();
			selected.add(new Pair<>(entry.getKey(), choiceBox.getValue()));
			newB.getChildren().add(choiceBox);
			VBox oldB = new VBox(new Label(entry.getKey()));
			row = registerRow(gridPane, name, oldB, newB, row);


			OperationInfo operationInfo = operationInfoMap.get(entry.getKey());
			List<String> variables = Stream.of(operationInfo.getReadVariables(), operationInfo.getNonDetWrittenVariables(), operationInfo.getWrittenVariables())
					.flatMap(Collection::stream).distinct().collect(Collectors.toList());



			if(!variables.isEmpty()){
				mapVariables.putAll(
						TraceCheckerUtils.zip(variables, variables.stream().map(SimpleStringProperty::new).collect(Collectors.toList())));

				row = registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.variables")),
						prepareColumn(variables), prepareFlexibleColumn2(mapVariables), row);
			}


			if(!operationInfo.getParameterNames().isEmpty()){

				List<String> inputValues = (List<String>) entry.getValue().get(0).getInputParameters().values();
				List<String> inputKeys = new ArrayList<>(entry.getValue().get(0).getInputParameters().keySet());

				mapInput.putAll(TraceCheckerUtils.zip(inputKeys,
						inputValues.stream().map(SimpleStringProperty::new).collect(Collectors.toList())));

				row = registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.input")),
						prepareColumn(operationInfo.getParameterNames()),
						prepareFlexibleColumn2(mapInput), row);
			}

			if(!operationInfo.getParameterNames().isEmpty()){

				List<String> outputValues = (List<String>) entry.getValue().get(0).getOutputParameters().values();
				List<String> outputKeys = new ArrayList<>(entry.getValue().get(0).getOutputParameters().keySet());

				mapOutput.putAll(TraceCheckerUtils.zip(outputKeys,
						outputValues.stream().map(SimpleStringProperty::new).collect(Collectors.toList())));

				row = registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.output")),
						prepareColumn(operationInfo.getParameterNames()),
						prepareFlexibleColumn2(mapInput), row);
			}



			choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
				selected.remove(new Pair<>(entry.getKey(), oldValue));
				selected.add(new Pair<>(entry.getKey(), newValue));

				selectedTrace = changelogPhase2II.get(selected.stream().map(changeToDelta::get).collect(Collectors.toSet()));

				Pair<String, String> key = new Pair<>(entry.getKey(), newValue);


				title.set(entry.getKey() + "<->" + newValue);

				changeToDelta.get(key).getVariables().forEach((key1, value) -> {
					mapVariables.get(key1).set(value);
				});

				changeToDelta.get(key).getInputParameters().forEach((key1, value) -> {
					mapInput.get(key1).set(value);
				});

				changeToDelta.get(key).getOutputParameters().forEach((key1, value) -> {
					mapOutput.get(key1).set(value);

				});

			});


			TitledPane titledPane = new TitledPane("", gridPane);
			titledPane.textProperty().bindBidirectional(title);

			result.getChildren().add(titledPane);

		}

		return result;

	}



	public int flexibleRow(List<Delta> newValues, Map<String, StringProperty> map, GridPane gridPane, int row, String key){
		List<String> outputValues = (List<String>) newValues.get(0).getOutputParameters().values();
		List<String> outputKeys = new ArrayList<>(newValues.get(0).getOutputParameters().keySet());

		map.putAll(TraceCheckerUtils.zip(outputKeys,
				outputValues.stream().map(SimpleStringProperty::new).collect(Collectors.toList())));

		return registerRow(gridPane, new Label(resourceBundle.getString(key)),
				prepareColumn(outputKeys),
				prepareFlexibleColumn2(map), row);
	}

	public VBox setTypeIVInitialisation(){
		Map<String, Map<String, String>> resultTypeII = traceModificationChecker.traceChecker.getDeltaFinder().getResultTypeII();
		Map<String, OperationInfo> operationInfoMap = traceModificationChecker.traceChecker.getOldOperationInfos();
		Map<String, String> globalVars = traceModificationChecker.traceChecker.getDeltaFinder().getResultTypeIIInit();

		VBox result = new VBox();

		if(!traceModificationChecker.traceChecker.getTypeFinder().getInitIsTypeIorIICandidate() &&
				!traceModificationChecker.traceChecker.getDeltaFinder().getResultTypeIIInit().isEmpty()){
			int row = 0;
			GridPane gridPane = new GridPane();


			Label empty = new Label();
			Label newL = new Label(resourceBundle.getString("traceModification.alert.current"));
			Label oldL = new Label(resourceBundle.getString("traceModification.alert.old"));


			row = registerRow(gridPane, empty, oldL, newL, row);


			registerRow(gridPane, new Label(resourceBundle.getString("traceModification.alert.name")), prepareColumn(new ArrayList<>(globalVars.keySet())),
					prepareColumn(new ArrayList<>(globalVars.values())), row);

			TitledPane titledPane = new TitledPane(resourceBundle.getString("traceModification.alert.init"), gridPane);


			result.getChildren().add(titledPane);
		}

		return result;

	}






}
