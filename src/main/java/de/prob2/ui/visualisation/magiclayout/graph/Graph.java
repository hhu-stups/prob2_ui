package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.layout.Pane;

public class Graph extends Pane {
	
	private List<Vertex> vertices = new ArrayList<>();
	private List<Edge> edges = new ArrayList<>();

	public Graph() {}
	
	public void addVertex(Vertex vertex) {
		vertices.add(vertex);
		this.getChildren().add(vertex);
	}

	public void addEdge(String sourceVertexId, String targetVertexId, String caption) {
		Vertex source = (Vertex) this.lookup("#" + sourceVertexId);
		Vertex target = (Vertex) this.lookup("#" + targetVertexId);
		
		Edge edge = new Edge(source, target, caption);
		
		edges.add(edge);
		this.getChildren().add(edge);
	}
}
