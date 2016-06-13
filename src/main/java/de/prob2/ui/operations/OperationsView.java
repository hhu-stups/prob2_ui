package de.prob2.ui.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.inject.Inject;

import de.prob.Main;
import de.prob.model.eventb.Event;
import de.prob.model.eventb.EventParameter;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Machine;
import de.prob.model.representation.ModelElementList;
import de.prob.scripting.Api;
import de.prob.statespace.Animations;
import de.prob.statespace.ITraceChangesListener;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;

public class OperationsView extends TitledPane implements ITraceChangesListener {


	@FXML 	private ListView<Operation> opsListView;
	@FXML	private Button backButton;
	@FXML	private Button forwardButton;
	@FXML	private Button searchButton;
	@FXML	private Button sortButton;
	@FXML	private ToggleButton disabledOpsToggle;	
	@FXML	private TextField filterEvents;
	@FXML   private TextField randomText;
	@FXML   private MenuItem oneRandomEvent;
	@FXML   private MenuItem fiveRandomEvents;
	@FXML   private MenuItem tenRandomEvents;
	@FXML   private Button randomEventsButton;

	private Trace currentTrace;
	private AbstractModel currentModel;
	private List<String> opNames = new ArrayList<String>();;
	private Map<String, List<String>> opToParams = new HashMap<String, List<String>>();
	private List<Operation> events = new ArrayList<Operation>();
	private boolean showNotEnabled = true;
	private String filter = "";
	Comparator<Operation> sorter = new ModelOrder(new ArrayList<String>());
	private Animations animations;
	
	@Inject
	public OperationsView(Animations ANIMATIONS, FXMLLoader loader) {
		this.animations = ANIMATIONS;
		animations.registerAnimationChangeListener(this);
		
		try {
			loader.setLocation(getClass().getResource("ops_view.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	

	@FXML
	public void initialize(){
		opsListView.setCellFactory(new TransitionTransformer());
		
		opsListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Operation>() {
		    @Override
		    public void changed(ObservableValue<? extends Operation> observable, Operation oldValue, Operation newValue) {
		    	if(newValue != null && newValue.isEnabled()) {
		    		animations.traceChange(currentTrace.add(newValue.id));
		    		System.out.println("Selected item: " + newValue);
		    	}
		    }
		});
	}
	

	@FXML
	private void handleDisabledOpsToggle() {
		if (disabledOpsToggle.isSelected()) {
			disabledOpsToggle.getStyleClass().add("eyeclosed");
			showNotEnabled = false;
			animations.traceChange(currentTrace);
			
		} else {
			disabledOpsToggle.getStyleClass().remove("eyeclosed");
			showNotEnabled = true;
			animations.traceChange(currentTrace);
		}
	}
	
	@FXML
	private void handleBackButton() {
		animations.traceChange(currentTrace.back());
	}
	
	@FXML
	private void handleForwardButton() {
		animations.traceChange(currentTrace.forward());
	}
	
	@FXML
	private void handleSearchButton() {
		filter = filterEvents.getText();
		ObservableList<Operation> opsList = opsListView.getItems();
		opsList.clear();
		opsList.addAll(applyFilter(filter));
	}
	
	private List<Operation> applyFilter(final String filter) {
		List<Operation> newOps = new ArrayList<Operation>();
		for (Operation op : events) {
			if (op.name.startsWith(filter)) {
				newOps.add(op);
			}
		}
		return newOps;
	}
	
	@FXML
	private void handleSortButton() {
		String oldMode = getSortMode();
		if ("normal".equals(oldMode)) {
			sorter = new AtoZ();
			sortButton.getStyleClass().remove("aToZ");
			sortButton.getStyleClass().remove("sort_by_order");
			sortButton.getStyleClass().add("zToA");
		} else if ("aToZ".equals(oldMode)) {
			sorter = new ZtoA();
			sortButton.getStyleClass().remove("aToZ");
			sortButton.getStyleClass().remove("zToA");
			sortButton.getStyleClass().add("sort_by_order");
		} else if ("zToA".equals(oldMode)) {
			sorter = new ModelOrder(opNames);
			sortButton.getStyleClass().remove("zToA");
			sortButton.getStyleClass().remove("sort_by_order");
			sortButton.getStyleClass().add("aToZ");
		}
		Collections.sort(events, sorter);
		ObservableList<Operation> opsList = opsListView.getItems();
		opsList.clear();
		opsList.addAll(applyFilter(filter));
	}
	
	public String getSortMode() {
		if (sorter instanceof ModelOrder) {
			return "normal";
		}
		if (sorter instanceof AtoZ) {
			return "aToZ";
		}
		if (sorter instanceof ZtoA) {
			return "zToA";
		}
		return "other";
	}
	

	@FXML
	public void random(ActionEvent event) {
		if(event.getSource().equals(oneRandomEvent)) {
			animations.traceChange(currentTrace.randomAnimation(1));
		} else if(event.getSource().equals(fiveRandomEvents)) {
			animations.traceChange(currentTrace.randomAnimation(5));
		} else if(event.getSource().equals(tenRandomEvents)) {
			animations.traceChange(currentTrace.randomAnimation(10));
		} else if(event.getSource().equals(randomEventsButton) || event.getSource().equals(randomText)) {
			try {
				int steps = Integer.parseInt(randomText.getText());
				animations.traceChange(currentTrace.randomAnimation(steps));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public void changed(List<Trace> t) {
		if (t == null || t.isEmpty()) return;
		Trace trace = t.get(0);
		if (trace == null) {
			currentTrace = null;
			currentModel = null;
			opNames = new ArrayList<String>();
			if (sorter instanceof ModelOrder) {
				sorter = new ModelOrder(opNames);
			}
		}

		if (trace.getModel() != currentModel) {
			updateModel(trace);
		}
		currentTrace = trace;
		events = new ArrayList<Operation>();
		Set<Transition> operations = currentTrace.getNextTransitions(true);
		Set<String> notEnabled = new HashSet<String>(opNames);
		Set<String> withTimeout = currentTrace.getCurrentState().getTransitionsWithTimeout();
		for(Transition transition: operations){
			String id = transition.getId();
			String name = extractPrettyName(transition.getName());
			notEnabled.remove(name);
			List<String> params = transition.getParams();
			Operation operation = new Operation(id, name, params, true, withTimeout.contains(name));
			events.add(operation);
		}
		if (showNotEnabled) {
			for (String s : notEnabled) {
				if (!s.equals("INITIALISATION")) {
					events.add(new Operation(s, s, opToParams.get(s), false, withTimeout.contains(s)));
				}
			}
		}
		try {
			Collections.sort(events, sorter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		backButton.setDisable(!currentTrace.canGoBack());
		forwardButton.setDisable(!currentTrace.canGoForward());
		
		Platform.runLater(() -> {
			ObservableList<Operation> opsList = opsListView.getItems();
			opsList.clear();
			opsList.addAll(applyFilter(filter));
		});
		
	}

	private void updateModel(final Trace trace) {
		currentModel = trace.getModel();
		AbstractElement mainComponent = trace.getStateSpace().getMainComponent();
		opNames = new ArrayList<String>();
		opToParams = new HashMap<String, List<String>>();
		if (mainComponent instanceof Machine) {
			ModelElementList<BEvent> events = mainComponent.getChildrenOfType(BEvent.class);
			for (BEvent e : events) {
				opNames.add(e.getName());

				List<String> paramList = new ArrayList<String>();
				if (e instanceof Event) {
					for (EventParameter eParam : ((Event) e).getParameters()) {
						paramList.add(eParam.getName());
					}
				} else if (e instanceof de.prob.model.classicalb.Operation) {
					paramList.addAll(((de.prob.model.classicalb.Operation) e)
							.getParameters());
				}
				opToParams.put(e.getName(), paramList);
			}
		}
		if (sorter instanceof ModelOrder) {
			sorter = new ModelOrder(opNames);
		}
	}

	private String extractPrettyName(final String name) {
		if ("$setup_constants".equals(name)) {
			return "SETUP_CONSTANTS";
		}
		if ("$initialise_machine".equals(name)) {
			return "INITIALISATION";
		}
		return name;
	}
	
	@Override
	public void removed(List<UUID> t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void animatorStatus(Set<UUID> busy) {
		// TODO Auto-generated method stub
		
	}
	
	private class EventComparator {

		private String stripString(final String param) {
			return param.replaceAll("\\{", "").replaceAll("\\}", "");
		}

		public int compareParams(final List<String> params1,
				final List<String> params2) {
			for (int i = 0; i < params1.size(); i++) {
				String p1 = stripString(params1.get(i));
				String p2 = stripString(params2.get(i));
				if (p1.compareTo(p2) != 0) {
					return p1.compareTo(p2);
				}

			}
			return 0;
		}
	}

	private class ModelOrder extends EventComparator implements	Comparator<Operation> {

		private final List<String> ops;

		public ModelOrder(final List<String> ops) {
			this.ops = ops;
		}

		@Override
		public int compare(final Operation o1, final Operation o2) {
			if (ops.contains(o1.name) && ops.contains(o2.name)
					&& ops.indexOf(o1.name) == ops.indexOf(o2.name)) {
				return compareParams(o1.params, o2.params);
			}
			return ops.indexOf(o1.name) - ops.indexOf(o2.name);
		}
	}
	
	private class AtoZ extends EventComparator implements Comparator<Operation> {

		@Override
		public int compare(final Operation o1, final Operation o2) {
			if (o1.name.compareTo(o2.name) == 0) {
				return compareParams(o1.params, o2.params);
			}
			return o1.name.compareTo(o2.name);
		}

	}

	private class ZtoA extends EventComparator implements Comparator<Operation> {

		@Override
		public int compare(final Operation o1, final Operation o2) {
			if (o1.name.compareTo(o2.name) == 0) {
				return compareParams(o1.params, o2.params);
			}
			return -1 * o1.name.compareTo(o2.name);
		}

	}
	
}
