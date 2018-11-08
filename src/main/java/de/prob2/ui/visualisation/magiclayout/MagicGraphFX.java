package de.prob2.ui.visualisation.magiclayout;

import java.util.Arrays;
import java.util.List;

import de.prob2.ui.visualisation.magiclayout.graph.Graph;
import javafx.scene.Node;

public class MagicGraphFX implements MagicGraphI {
	
	@Override
	public List<MagicShape> getPossibleShapes() {
		MagicShape shapes[] = new MagicShape[]{MagicShape.RECTANGLE, MagicShape.CIRCLE, MagicShape.ELLIPSE};
		return Arrays.asList(shapes);
	}
	
	@Override
	public Node generateMagicGraph() {
		return new Graph();
	}

}
