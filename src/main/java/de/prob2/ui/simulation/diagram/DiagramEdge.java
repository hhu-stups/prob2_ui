package de.prob2.ui.simulation.diagram;

import java.util.List;

public class DiagramEdge {
	String from; 
	List<String> to; 
	List<String> edgeLabel; 
	String colour; 

	public DiagramEdge(String from, List<String> to, List<String> edgeLabel, String colour) {
		this.from = from;
		this.to = to;
		this.edgeLabel = edgeLabel;
		this.colour = colour;
	}

	public String getFrom() {
		return from;
	}
   
	public void setFrom(String from) {
		this.from = from;
	}
	public List<String> getTo() {
		return to;
	}
	public void setTo(List<String> to) {
		this.to = to;
	}
	public String getColour() {
		return colour;
	}
	public void setColour(String colour) {
		this.colour = colour;
	}
	public List<String> getEdgeLabel() {
		return edgeLabel;
	}
	public void setEdgeLabel(List<String> edgeLabel) {
		this.edgeLabel = edgeLabel;
	}
}
