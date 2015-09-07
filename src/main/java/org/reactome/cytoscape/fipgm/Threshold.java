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
    private ValueRelation valueRelation;
    private double value;
    
    /**
     * Default constructor.
     */
    public Threshold() {
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
