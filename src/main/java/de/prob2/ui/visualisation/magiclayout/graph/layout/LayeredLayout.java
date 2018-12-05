package de.prob2.ui.visualisation.magiclayout.graph.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import de.prob2.ui.visualisation.magiclayout.graph.Edge;
import de.prob2.ui.visualisation.magiclayout.graph.Graph;
import de.prob2.ui.visualisation.magiclayout.graph.Model;
import de.prob2.ui.visualisation.magiclayout.graph.Vertex;

/**
 * The implementations of a layered graph layout in this class are based on the
 * algorithms and steps described in
 * 
 * Oliver Bastert and Christian Matuszewski: “5. Layered Drawings of Digraphs.”
 * in 'Drawing Graphs: Methods and Models' by Michael Kaufmann and Dorothea
 * Wagner, Springer, 2001, pp. 87–120.
 * 
 */
public class LayeredLayout implements Layout {

	@Override
	public void drawGraph(Graph graph) {
		Set<Edge> acyclicEdges = removeCycles(graph.getModel());
		SortedMap<Integer, List<Vertex>> layers = assignLayers(graph.getModel().getVertices(), acyclicEdges);
		reduceCrossing(layers, acyclicEdges);
		
		double y = 10;
		for(Entry<Integer, List<Vertex>> layer :  layers.entrySet()) {
			double x = 10;
			double maxHeight = 0;
			for(Vertex vertex: layer.getValue()) {
				vertex.relocate(x, y);
				x += vertex.getWidth() + 10;
				if(vertex.getHeight() > maxHeight) {
					maxHeight = vertex.getHeight();
				}
			}
			y += maxHeight + 10;
		}
		
//		assignHorizontalCoordinates();
//		positionEdges();
	}

	/**
	 * Implementation based on "Algorithm 8: A Greedy Algorithm" in 'Drawing Graphs:
	 * Methods and Models'.
	 * 
	 * <p>
	 * (Oliver Bastert and Christian Matuszewski: “5. Layered Drawings of Digraphs.”
	 * in 'Drawing Graphs: Methods and Models' by Michael Kaufmann and Dorothea
	 * Wagner, Springer, 2001, p. 91.)
	 * 
	 * <p>
	 * Computes a subset of edges of the specified model, which does not contain any
	 * cycles. To not loose any information, weather two vertices are connected, the
	 * edges which would create a cycle are reversed.
	 * 
	 */
	private Set<Edge> removeCycles(Model model) {
		Set<Edge> removedEdges = new HashSet<>(model.getEdges());
		Set<Edge> acyclicEdges = new HashSet<>();

		model.getVertices().forEach(vertex -> {
			Set<Edge> incomingEdges = getIncomingEdges(vertex, model.getEdges());
			Set<Edge> outgoingEdges = getOutgoingEdges(vertex, model.getEdges());

			if (outgoingEdges.size() >= incomingEdges.size()) {
				acyclicEdges.addAll(outgoingEdges);
				removedEdges.removeAll(outgoingEdges);
			} else {
				acyclicEdges.addAll(incomingEdges);
				removedEdges.removeAll(incomingEdges);
			}
		});

		acyclicEdges.addAll(reverseEdges(removedEdges));

		return acyclicEdges;
	}

	private SortedMap<Integer, List<Vertex>> assignLayers(Set<Vertex> vertices, Set<Edge> acyclicEdges) {
		Map<Vertex, Integer> layers = new HashMap<>();

		vertices.forEach(vertex -> {
			if (!layers.containsKey(vertex)) {
				calculateLayer(vertex, layers, acyclicEdges);
			}
		});

		SortedMap<Integer, List<Vertex>> layerLists = new TreeMap<>();
		layers.forEach((vertex, layerNr) -> {
			if (layerLists.get(layerNr) == null) {
				layerLists.put(layerNr, new ArrayList<>());
			}
			layerLists.get(layerNr).add(vertex);
		});
		return layerLists;
	}

	/**
	 * Implementation based on section "5.3.2 Minimizing the Height" in 'Drawing
	 * Graphs: Methods and Models'.
	 * 
	 * <p>
	 * (Oliver Bastert and Christian Matuszewski: “5. Layered Drawings of Digraphs.”
	 * in 'Drawing Graphs: Methods and Models' by Michael Kaufmann and Dorothea
	 * Wagner, Springer, 2001, p. 98.)
	 *
	 * Recursively assigns a layer to each vertex based on its longest path to a
	 * sink. Sinks are assigned to layer 0.
	 * 
	 */
	private void calculateLayer(Vertex vertex, Map<Vertex, Integer> layers, Set<Edge> acyclicEdges) {
		if (isSink(vertex, acyclicEdges)) {
			layers.put(vertex, 0);
			return;
		}

		Integer layer = 0;
		Set<Vertex> succesors = getSuccessors(vertex, acyclicEdges);
		for (Vertex succ : succesors) {
			if (!layers.containsKey(succ)) {
				calculateLayer(succ, layers, acyclicEdges);
			}

			if (layers.get(succ) > layer) {
				layer = layers.get(succ);
			}
		}
		layers.put(vertex, layer + 1);
	}

	private void reduceCrossing(SortedMap<Integer, List<Vertex>> layers, Set<Edge> acyclicEdges) {
		int fixedLayerNr = 0;
		int permuteLayerNr = 1;

		while (layers.containsKey(permuteLayerNr)) {
			List<Vertex> fixedLayer = layers.get(fixedLayerNr);
			List<Vertex> permuteLayer = layers.get(permuteLayerNr);

			SortedMap<Double, Vertex> baryMap = new TreeMap<>();
			permuteLayer.forEach(vertex -> {
				// compute the subset of edges between the specified vertex and the fixed layer
				Set<Edge> edgesToFixedLayer = new HashSet<>();
				getOutgoingEdges(vertex, acyclicEdges).forEach(edge -> {
					if (fixedLayer.contains(edge.getTarget())) {
						edgesToFixedLayer.add(edge);
					}
				});
				getIncomingEdges(vertex, acyclicEdges).forEach(edge -> {
					if (fixedLayer.contains(edge.getSource())) {
						edgesToFixedLayer.add(edge);
					}
				});
				
				double bary = calculateBarycenter(vertex, edgesToFixedLayer, fixedLayer);
				while(baryMap.containsKey(bary)) {
					bary += 0.0001;
				}
				baryMap.put(bary, vertex);
			});
			
			List<Vertex> permutedLayer = new ArrayList<>();
			baryMap.values().forEach(vertex -> permutedLayer.add(vertex));
			
			layers.put(permuteLayerNr, permutedLayer);
			
			fixedLayerNr++;
			permuteLayerNr++;
		}
	}

	private double calculateBarycenter(Vertex vertex, Set<Edge> edgesToFixedLayer, List<Vertex> fixedLayer) {
		Set<Vertex> neighbours = getNeighbours(vertex, edgesToFixedLayer);

		double bary = 0;
		for (Vertex neighbour : neighbours) {
			bary += fixedLayer.indexOf(neighbour);
		}

		bary /= degree(vertex, edgesToFixedLayer);

		return bary;
	}

	private int degree(Vertex vertex, Set<Edge> edges) {
		return getIncomingEdges(vertex, edges).size() + getOutgoingEdges(vertex, edges).size();
	}

	private Set<Vertex> getNeighbours(Vertex vertex, Set<Edge> edges) {
		Set<Vertex> neighbours = new HashSet<>();

		edges.forEach(edge -> {
			if (edge.getSource().equals(vertex)) {
				neighbours.add(edge.getTarget());
			} else if (edge.getTarget().equals(vertex)) {
				neighbours.add(edge.getSource());
			}
		});

		return neighbours;
	}

	private boolean isSink(Vertex vertex, Set<Edge> edges) {
		return getOutgoingEdges(vertex, edges).size() == 0;
	}

	private Set<Edge> reverseEdges(Set<Edge> edges) {
		Set<Edge> reversedEdges = new HashSet<>();

		edges.forEach(edge -> {
			reversedEdges.add(new Edge(edge.getTarget(), edge.getSource(), ""));
		});

		return reversedEdges;
	}

	private Set<Edge> getIncomingEdges(Vertex vertex, Set<Edge> edges) {
		Set<Edge> incomingEdges = new HashSet<>();

		edges.forEach(edge -> {
			if (edge.getTarget().equals(vertex)) {
				incomingEdges.add(edge);
			}
		});

		return incomingEdges;
	}

	private Set<Edge> getOutgoingEdges(Vertex vertex, Set<Edge> edges) {
		Set<Edge> outgoingEdges = new HashSet<>();

		edges.forEach(edge -> {
			if (edge.getSource().equals(vertex)) {
				outgoingEdges.add(edge);
			}
		});

		return outgoingEdges;
	}

	private Set<Vertex> getSuccessors(Vertex vertex, Set<Edge> edges) {
		Set<Vertex> successors = new HashSet<>();

		edges.forEach(edge -> {
			if (edge.getSource().equals(vertex)) {
				successors.add(edge.getTarget());
			}
		});

		return successors;
	}

	@Override
	public void updateGraph(Graph graph) {
		// TODO Auto-generated method stub

	}

}
