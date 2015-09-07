/*
 * Created on Sep 7, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.util.List;

import org.reactome.cytoscape.fipgm.Threshold.ThresholdRelation;


/**
 * @author gwu
 *
 */
public enum DataTypeDistribution {
    
    Discrete,
    Empirical,
    Direct;
    
    private Threshold[] thresholds;
    private ThresholdRelation relation = ThresholdRelation.or; // As default
    
    public Threshold[] getThresholds() {
        return this.thresholds;
    }
    
    public void setThresholds(List<Threshold> list) {
        if (list == null || list.size() == 0)
            thresholds = null;
        else {
            thresholds = new Threshold[list.size()];
            for (int i = 0; i < list.size(); i++)
                thresholds[i] = list.get(i);
        }
    }
    
    public void setThresholds(Threshold[] thresholds) {
        this.thresholds = thresholds;
    }
    
    public ThresholdRelation getRelation() {
        return relation;
    }

    public void setRelation(ThresholdRelation relation) {
        this.relation = relation;
    }

    /**
     * Text to be displayed in a list-style GUI (e.g. JComboBox or JList).
     * @return
     */
    public String toListString() {
        switch (this) {
            case Discrete : return "Discrete based on thresholds";
            case Empirical : return "Use empirical distribution";
            case Direct : return "Use values as beliefs";
        }
       return "";
    }
    
    @Override
    public String toString() {
        if (this == Discrete) {
            if (thresholds == null || thresholds.length > 2)
                return super.toString();
            if (thresholds.length == 1)
                return thresholds[0].toString();
            if (thresholds.length == 2) {
                return thresholds[0] +  " " + relation + " " + thresholds[1];
            }
        }
        return super.toString();
    }
    
    /**
     * Provide more detailed information about a selected DataTypeDistribution.
     * @return
     */
    public String toNoteString() {
        if (this == Discrete) {
            if (thresholds == null)
                return toListString();
            if (thresholds.length == 1)
                return "Values " + thresholds[0] + " as abnormal";
            if (thresholds.length == 2) {
                return "Values " + thresholds[0] +  " " + relation + " values " + thresholds[1] + " as abnormal";
            }
            return toListString(); // Don't support more than two threshold values
        }
        return toListString();
    }
    
}
