package de.prob2.ui.visualisation.magiclayout;

import java.util.List;

import de.prob.statespace.State;
import javafx.scene.Node;

public interface MagicGraphI {
	
	Boolean supportsClustering();
	
	List<MagicShape> getPossibleShapes();
	
	Node generateMagicGraph(State state);

	void updateMagicGraph(State state);

	void setGraphStyle(List<MagicNodes> nodes, List<MagicEdges> edges);
}	
