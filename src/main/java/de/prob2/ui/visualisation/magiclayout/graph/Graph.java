package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.Pane;

public class Graph extends Pane {

	private List<Vertex> vertices = new ArrayList<>();
	private List<Edge> edges = new ArrayList<>();

	public Graph() {
	}

	public void addVertex(Vertex vertex) {
		vertices.add(vertex);
		this.getChildren().add(vertex);

		double x = Math.random() * 750;
		double y = Math.random() * 500;
		
		while (vertexAt(x, y, vertex.getWidth(), vertex.getHeight())) {
			x = Math.random() * 750;
			y = Math.random() * 500;
		}
		vertex.relocate(x, y);
	}

	private boolean vertexAt(double x, double y, double offsetX, double offsetY) {
		for (Vertex vertex : vertices) {
			if (x > (vertex.getLeftX() - offsetX) 
					&& x < vertex.getRightX()
					&& y > (vertex.getTopY() - offsetY) 
					&& y < vertex.getBottomY()) {
				return true;
			}
		}
		return false;
	}

	public void addEdge(String sourceVertexId, String targetVertexId, String caption) {
		Vertex source = (Vertex) this.lookup("#" + sourceVertexId);
		Vertex target = (Vertex) this.lookup("#" + targetVertexId);

		Edge edge = new Edge(source, target, caption);

		edges.add(edge);
		this.getChildren().add(edge);
	}
}
