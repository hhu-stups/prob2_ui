package de.prob2.ui.simulation.diagram;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

public class DiagramGenerator {

    private final StageManager stageManager;

    private final FileChooserManager fileChooserManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final Injector injector;

    private final I18n i18n;

	private DiagramStage diaStage; 

    private RealTimeSimulator realTimeSimulator;

    
	@Inject
    public DiagramGenerator(StageManager stageManager, FileChooserManager fileChooserManager,
            CurrentProject currentProject, CurrentTrace currentTrace, Injector injector, I18n i18n, RealTimeSimulator realTimeSimulator) {

                
        this.stageManager = stageManager;
        this.fileChooserManager = fileChooserManager;
        this.currentProject = currentProject;
        this.currentTrace = currentTrace;
        this.injector = injector;
        this.i18n = i18n;
        this.realTimeSimulator = realTimeSimulator;
		this.realTimeSimulator.setDiagramGenerator(this);
    }


    //Initializes Velocity engine for diagram generation
	private VelocityContext velocityInit(){
		Properties props = new Properties();
		VelocityContext nodeContext = new VelocityContext();
		props.setProperty("resource.loader", "class");
		props.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init(props);
		return nodeContext;
		
	}

	
	public void generateDiagram(){
		//Initialisation of Velocity engine
		VelocityContext nodeContext = velocityInit();
		Template nodes = Velocity.getTemplate("/de/prob2/ui/simulation/velocity/nodes_template.vm");
		StringWriter sw = new StringWriter(); 
		
		//Nodes and edges are collected and put into velocity context
		nodeContext.put("nodes", collectNodes(false));
		nodeContext.put("activations", collectEdges());
		nodes.merge(nodeContext, sw);
		String nodesString = sw.toString();


		//To be enhanced with pop up window that contains the diagram

		printSimulationDiagramm(nodesString);
	}

	
	public void generateComplexDiagram(){
		//init velocity
		VelocityContext nodeContext = velocityInit();
		Template nodes = Velocity.getTemplate("/de/prob2/ui/simulation/velocity/complex_template.vm");
		StringWriter sw = new StringWriter(); 

		//Nodes and edges are collected and put into velocity context
		nodeContext.put("nodes", collectComplexNodes());
		nodeContext.put("activations", collectEdges());
		nodes.merge(nodeContext, sw);
		String nodesString = sw.toString();


		//To be replaced with pop up window that contains the diagram
		printSimulationDiagramm(nodesString);
	}

	public void generateLiveDiagram(){
		//Initialisation of Velocity engine
		VelocityContext nodeContext = velocityInit();
		Template nodes = Velocity.getTemplate("/de/prob2/ui/simulation/velocity/nodes_template.vm");
		StringWriter sw = new StringWriter(); 
		
		//Nodes and edges are collected and put into velocity context
		nodeContext.put("nodes", collectNodes(true));
		nodeContext.put("activations", collectEdges());
		nodes.merge(nodeContext, sw);
		String nodesString = sw.toString();


		//To be enhanced with pop up window that contains the diagram

		printSimulationDiagramm(nodesString);

	}

	

	//Method that collects all nodes for simple activation diagram
	private List<DiagramNode> collectNodes(boolean showCurrent){
		//init of Configs for Simple nodes
		SimulationModelConfiguration config = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		List<ActivationConfiguration> activations = config.getActivationConfigurations();
		List<UIListenerConfiguration> listeners = config.getUiListenerConfigurations();
		List<DiagramNode> diaNode = new ArrayList<DiagramNode>();
		ActivationOperationConfiguration opConfig;
		
	
		//Adding nodes to context
		for (ActivationConfiguration activation : activations) {
			if (activation.getClass().equals(ActivationChoiceConfiguration.class)) {
				diaNode.add(new DiagramNode(activation.getId(),"red",activation.getId(), "diamond"));

			} else {
				opConfig = (ActivationOperationConfiguration)activation;
				String eventColour = "white";
				String opColour = "yellow";
				if (showCurrent && !realTimeSimulator.getConfigurationToActivation().get(opConfig.getId()).isEmpty()) {
					opColour = "blue";
					eventColour = "blue";
				}
				diaNode.add(new DiagramNode(opConfig.getOpName()+"_event", eventColour, opConfig.getOpName(), "ellipse"));

				if(!activation.getId().equals("$initialise_machine")){
					diaNode.add(new DiagramNode(activation.getId(), opColour, activation.getId(),"diamond"));
				}
			}
			System.out.println(activation.getClass().getName());
		}
		for(UIListenerConfiguration listener : listeners){
			boolean listenerinit = true;
			if (listenerinit) {
				diaNode.add(new DiagramNode("User", "white", "User", "ellipse"));
				listenerinit = false;
			}
			diaNode.add(new DiagramNode(listener.getEvent(),"white",listener.getEvent(),"ellipse"));
		}
		return diaNode;
	}

		//collects Nodes for complex activation Diagram
		private List<DiagramNode> collectComplexNodes(){
			SimulationModelConfiguration config = (SimulationModelConfiguration) realTimeSimulator.getConfig();
			List<ActivationConfiguration> activations = config.getActivationConfigurations();
			List<UIListenerConfiguration> listeners = config.getUiListenerConfigurations();
			List<DiagramNode> diaNode = new ArrayList<DiagramNode>();
			ActivationOperationConfiguration opConfig;

			//Adding nodes to context
			for (ActivationConfiguration activation : activations) {
				if (activation.getClass().equals(ActivationChoiceConfiguration.class)) {
					diaNode.add(new DiagramNode(activation.getId(),"red",activation.getId(), "diamond"));
	
				} else {
					opConfig = (ActivationOperationConfiguration)activation;
					diaNode.add(new DiagramNode(opConfig.getOpName()+"_event","white",opConfig.getOpName(), "ellipse"));
					
	
					if(!activation.getId().equals("$initialise_machine")){
						diaNode.add(new ComplexNode(activation.getId(),
							"yellow",
							opConfig.getId(), 
							"ellipse",
							opConfig.getActivationKind().getName(),
							opConfig.getAdditionalGuards(),
							opConfig.getPriority()));
					}
				}
			}
			for(UIListenerConfiguration listener : listeners){
				boolean listenerinit = true;
				if (listenerinit) {
					diaNode.add(new DiagramNode("User", "white", "User", "ellipse"));
					listenerinit = false;
				}
				diaNode.add(new ComplexListener(listener.getEvent(), "white", listener.getEvent(), "ellipse", listener.getPredicate()));
			}
			return diaNode;
		}

	//Method that collects all relevant edges between Nodes of the Activation diagramms
	private List<DiagramEdge> collectEdges(){

		SimulationModelConfiguration config = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		List<ActivationConfiguration> activations = config.getActivationConfigurations();
		List<UIListenerConfiguration> listeners = config.getUiListenerConfigurations();
		List<DiagramEdge> activating = new ArrayList<DiagramEdge>();
		ActivationChoiceConfiguration choiceConfig; 
		ActivationOperationConfiguration opConfig;
		DiagramEdge edge = new DiagramEdge("", null, null, ""); 

		for (ActivationConfiguration activation : activations) {
			if (activation.getClass().equals(ActivationChoiceConfiguration.class)) {
				choiceConfig = (ActivationChoiceConfiguration)activation;
				edge = new DiagramEdge(choiceConfig.getId(), choiceConfig.getActivations().keySet().stream().toList(), choiceConfig.getActivations().values().stream().toList(), "dotted");
				activating.add(edge);
			} else {
				opConfig = (ActivationOperationConfiguration)activation;
				if(!activation.getId().equals("$initialise_machine")){
				edge = new DiagramEdge(opConfig.getId(), List.of(opConfig.getOpName()+"_event"), List.of(opConfig.getAfter()), "");
				activating.add(edge);
				}
				edge = new DiagramEdge(opConfig.getOpName()+"_event", opConfig.getActivating(), opConfig.getActivating().stream().map(n -> "Activating").toList(), "");
				boolean isPresent = false;
				for (DiagramEdge compareEdge : activating) {
					if (compareEdge.getFrom().equals(edge.getFrom())) {
						isPresent = true;
					}
				}
				if (!isPresent) {
					activating.add(edge);
				}
			}
		}
		//Same as above but for listeners
		for(UIListenerConfiguration listener : listeners){
			activating.add(new DiagramEdge("User", List.of(listener.getEvent()), List.of("Interaction"), ""));
			edge = new DiagramEdge(listener.getEvent(), listener.getActivating(), listener.getActivating().stream().map(n -> "Activating").toList(), "");
			activating.add(edge);
		}

		return activating;
	}



	private void printSimulationDiagramm(String nodesString){
		//Getting Activation data; Only used for console information
		SimulationModelConfiguration config = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		System.out.println("ACTIVATIONS:");
		System.out.println(config.getActivationConfigurations());
		System.out.println("LISTENERS:");
		System.out.println(config.getUiListenerConfigurations());
		System.out.println("DOT: \n" + nodesString);

		makeDiagramStage(nodesString);
	}
	

	private void makeDiagramStage(String nodesString){
		//DiagramStage diagramStage = new DiagramStage(stageManager, currentProject, currentTrace, injector, i18n, nodesString, fileChooserManager);
		//diagramStage.show();
		diaStage = new DiagramStage(stageManager, currentProject, currentTrace, injector, i18n, nodesString, fileChooserManager);
		diaStage.show();
	}

	public void updateGraph(){
		
		diaStage.updateGraph("");
	}
}
