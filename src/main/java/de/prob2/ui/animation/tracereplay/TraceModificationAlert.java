package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.animator.domainobjects.AnimationMatrixEntry;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.statespace.OperationInfo;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

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


		selectedTrace = traceModificationChecker.traceChecker.getTraceModifier().getLastChange();
	}


	private VBox setTypeIIConflicts(){

		Map<String, Map<String, String>> resultTypeII = traceModificationChecker.traceChecker.getDeltaFinder().getResultTypeII();
		Map<String, OperationInfo> operationInfoMap = traceModificationChecker.traceChecker.getOldOperationInfos();
		Map<String, String> globalVars = traceModificationChecker.traceChecker.getDeltaFinder().getResultTypeIIInit();

		VBox result = new VBox();

		{
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


			Map<String, String> partner = resultTypeII.get(entry.getKey());

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





}
