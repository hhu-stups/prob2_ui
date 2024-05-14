package de.prob2.ui.simulation.diagram;

public class ComplexListener extends DiagramNode {

    private String predicate;

    public ComplexListener(String id, String colour, String label, String style, String predicate) {
        super(id, colour, label, style);
        this.predicate = predicate;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    } 
}
