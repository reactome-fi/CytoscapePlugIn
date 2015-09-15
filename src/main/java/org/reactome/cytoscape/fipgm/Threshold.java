/*
 * Created on Sep 7, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

/**
 * This class is used to describe a threshold
 * @author gwu
 *
 */
public class Threshold {
    private ValueRelation valueRelation = ValueRelation.greater; // Default
    private double value;
    
    /**
     * Default constructor.
     */
    public Threshold() {
    }
    
    /**
     * The passed text should be generated via the toString() method.
     * @param text
     */
    public Threshold(String text) {
        this();
        parseText(text);
    }
    
    public ValueRelation getValueRelation() {
        return valueRelation;
    }

    public void setValueRelation(ValueRelation valueRelation) {
        this.valueRelation = valueRelation;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return valueRelation + "" + value;
    }
    
    private void parseText(String text) {
        // Get the relation
        ValueRelation[] rels = ValueRelation.values();
        ValueRelation rel1 = null;
        for (ValueRelation rel : rels) {
            if (text.startsWith(rel.toString())) {
                rel1 = rel;
                break;
            }
        }
        if (rel1 == null)
            return; // Nothing to be done
        // Get the value
        Double value = new Double(text.substring(rel1.toString().length()));
        setValue(value);
        setValueRelation(rel1);
    }
    
    /**
     * Relation for value
     * @author gwu
     *
     */
    public enum ValueRelation {
        less,
        greater;

        @Override
        public String toString() {
            if (this == greater)
                return ">";
            return "<";
        }
    }
 
    /**
     * Relationshipd between two thresholds.
     * @author gwu
     *
     */
    public enum ThresholdRelation { 
        or,
        and
    }
}
