package de.prob2.ui.simulation.diagram;

public class DiagramNode {
    String id;
    String colour; 
    String label;
    String style; 

    public DiagramNode(String id, String colour, String label, String style) {
        this.id = id;
        this.colour = colour;
        this.label = label;
        this.style = style;
    }

    public String getId() {
        return id;
    }

    public String getColour() {
        return colour;
    }

    public String getLabel() {
        return label;
    }

    public String getStyle() {
        return style;
    }
}
