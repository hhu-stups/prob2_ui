package de.prob2.ui.visualisation.magiclayout;

import java.util.List;

import de.prob.statespace.State;
import javafx.scene.Node;

public interface MagicGraphI {
	
	Boolean supportsClustering();
	
	List<MagicLayout> getSupportedLayouts();
	
	List<MagicShape> getSupportedShapes();
	
	List<MagicLineType> getSupportedLineTypes();
	
	List<MagicLineWidth> getSupportedLineWidths(); 
	
	Node generateMagicGraph(State state, MagicLayout magicLayout);

	void updateMagicGraph(State state);

	void setGraphStyle(List<MagicNodegroup> nodes, List<MagicEdgegroup> edges);

}