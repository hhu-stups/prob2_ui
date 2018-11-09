package de.prob2.ui.visualisation.magiclayout;

import java.util.List;

import javafx.scene.Node;

public interface MagicGraphI {
	
	List<MagicShape> getPossibleShapes();
	
	Node generateMagicGraph(List<MagicNodes> nodes, List<MagicEdges> edges);
}	
