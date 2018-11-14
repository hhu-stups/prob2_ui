package de.prob2.ui.visualisation.magiclayout;

import java.util.List;

import javafx.scene.Node;

public interface MagicGraphI {
	
	List<MagicShape> getPossibleShapes();
	
	Node generateMagicGraph(List<MagicNodes> nodegroups, List<MagicEdges> edgegroups);

	void updateMagicGraph(Node graphNode, List<MagicNodes> nodegroups, List<MagicEdges> edgegroups);
}	
