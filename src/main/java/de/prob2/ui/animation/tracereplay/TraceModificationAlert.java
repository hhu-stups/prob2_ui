package de.prob2.ui.animation.tracereplay;

import com.google.inject.Injector;
import de.prob.animator.domainobjects.AnimationMatrixEntry;
import de.prob.check.tracereplay.PersistentTrace;
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

		ButtonType buttonTypeI = new ButtonType("Only use Current", ButtonBar.ButtonData.NO);
		ButtonType buttonTypeB = new ButtonType("Use Both", ButtonBar.ButtonData.APPLY);
		ButtonType buttonTypeN = new ButtonType("Only use New", ButtonBar.ButtonData.YES);

		this.setTitle("Trace Loading Conflict");
		this.setHeaderText("While loading the trace a mismatch between the contained transitions and the existing operations were identified.\n" +
				"The Mismatches are listed below. If you press 'X' only the currently loaded trace file will be used.");
		this.getDialogPane().getButtonTypes().addAll(buttonTypeI, buttonTypeB, buttonTypeN);



		this.setResultConverter(new Callback<ButtonType, List<PersistentTrace>>() {
			@Override
			public List<PersistentTrace> call(ButtonType param) {

				if(param == ButtonType.NO){
					return null;
				}
				return null;
			}
		});
	}


	@FXML
	private void initialize(){
		stageManager.register(this);

		ScrollPane scrollPane = new ScrollPane();
		GridPane gridPane = new GridPane();
		//scrollPane.prefViewportHeightProperty().bindBidirectional();





		VBox accordion = setTypeIIConflicts();
		typeII.setContent(accordion);
		accordion.prefHeightProperty().bindBidirectional(typeII.prefHeightProperty());
		accordion.prefWidthProperty().bindBidirectional(typeII.prefWidthProperty());

	}


	private VBox setTypeIIConflicts(){

		Map<String, Map<String, String>> resultTypeII = traceModificationChecker.traceChecker.getDeltaFinder().getResultTypeII();
		Map<String, OperationInfo> operationInfoMap = traceModificationChecker.traceChecker.getOldOperationInfos();

		VBox result = new VBox();



		for(Map.Entry<String, Map<String, String>> entry : resultTypeII.entrySet()){
			int row = 0;
			GridPane gridPane = new GridPane();


			Map<String, String> partner = resultTypeII.get(entry.getKey());

			Label empty = new Label();
			Label newL = new Label("Current");
			Label oldL = new Label("Old");


			row = registerRow(gridPane, empty, oldL, newL, row);


			Label operations = new Label("Name");


			VBox oldB = prepareColumn(Collections.singletonList(entry.getKey()));
			VBox newB = prepareColumn(Collections.singletonList(entry.getValue().get(entry.getKey())));
			row = registerRow(gridPane, operations, oldB, newB, row);


			OperationInfo operationInfo = operationInfoMap.get(entry.getKey());
			List<String> variables = Stream.of(operationInfo.getReadVariables(), operationInfo.getNonDetWrittenVariables(), operationInfo.getWrittenVariables())
					.flatMap(Collection::stream).distinct().collect(Collectors.toList());


			if(!variables.isEmpty()){
				row = registerRow(gridPane, new Label("Variables"), prepareColumn(variables),
						prepareColumn(retractEntries(variables,partner)), row);
			}

			if(!operationInfo.getParameterNames().isEmpty()){
				row = registerRow(gridPane, new Label("Input Parameter"),
						prepareColumn(operationInfo.getParameterNames()),
						prepareColumn(retractEntries(operationInfo.getParameterNames(),partner)), row);
			}

			if(!operationInfo.getParameterNames().isEmpty()){
				registerRow(gridPane, new Label("Output Parameter"),
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
