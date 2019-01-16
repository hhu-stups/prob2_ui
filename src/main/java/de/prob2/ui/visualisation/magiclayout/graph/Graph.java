package de.prob2.ui.visualisation.magiclayout.graph;

import de.prob2.ui.visualisation.magiclayout.graph.layout.Layout;
import de.prob2.ui.visualisation.magiclayout.graph.vertex.Vertex;
import javafx.scene.layout.Pane;

public class Graph extends Pane {

	private Model model;
	private Layout layout;

	public Graph(Model model) {
		this.model = model;
	}

	public Model getModel() {
		return model;
	}

	public void layout(Layout layout) {
		this.layout = layout;

		this.getChildren().clear();
		model.getVertices().forEach(vertex -> this.getChildren().add(vertex));
		model.getEdges().forEach(edge -> this.getChildren().add(edge));

		if (!this.getChildren().isEmpty()) {
			layout.drawGraph(this);
		}
		model.finishUpdate();
	}

	public void update() {
		model.getRemovedVertices().forEach(vertex -> this.getChildren().remove(vertex));
		model.getRemovedEdges().forEach(edge -> this.getChildren().remove(edge));

		model.getAddedVertices().forEach(vertex -> this.getChildren().add(vertex));
		model.getAddedEdges().forEach(edge -> this.getChildren().add(edge));

		if (!this.getChildren().isEmpty()) {
			layout.updateGraph(this);
		}
		model.finishUpdate();
	}

	public boolean vertexAt(double x, double y, double offsetX, double offsetY) {
		for (Vertex vertex : model.getVertices()) {
			if (x > (vertex.getLeftX() - offsetX) && x < vertex.getRightX() && y > (vertex.getTopY() - offsetY)
					&& y < vertex.getBottomY()) {
				return true;
			}
		}
		return false;
	}
}
