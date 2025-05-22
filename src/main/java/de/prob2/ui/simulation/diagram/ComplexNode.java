package de.prob2.ui.simulation.diagram;

public class ComplexNode extends DiagramNode{
    String activationKind;
    String additionalGuards; 
    int priority; 

    public ComplexNode(String id, String colour, String label, String style, String activationKind, String additionalGuards, int priority){
        super(id, colour, label, style);
        this.activationKind = activationKind;
        this.additionalGuards = additionalGuards;
        this.priority=priority;
    }

    public String getActivationKind() {
        return activationKind;
    }

    public void setActivationKind(String activationKind) {
        this.activationKind = activationKind;
    }

    public String getAdditionalGuards() {
        return additionalGuards;
    }

    public void setAdditionalGuards(String additionalGuards) {
        this.additionalGuards = additionalGuards;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    
}
