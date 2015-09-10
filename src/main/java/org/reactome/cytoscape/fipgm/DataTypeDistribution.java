/*
 * Created on Sep 7, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.util.ArrayList;
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
     * Extract the distribution from a String generated via the toString() method.
     * @param text
     * @return
     */
    public static DataTypeDistribution extractDistribution(String text) {
        DataTypeDistribution rtn = null;
        try {
            rtn = DataTypeDistribution.valueOf(text);
        }
        catch(IllegalArgumentException e) {
            // The text cannot be matched. Use the default.
            rtn = Discrete;
        }
        return rtn;
    }
    
    /**
     * Extract threshold relation from a text generated from the toString() method.
     * @param text
     * @return
     */
    public static ThresholdRelation extractThresholdRelation(String text) {
        if (text.contains(ThresholdRelation.or.toString()))
            return ThresholdRelation.or;
        if (text.contains(ThresholdRelation.and.toString()))
            return ThresholdRelation.and;
        return null;
    }
    
    /**
     * Extract thresholds from a text generated from the toString() method.
     * @param text
     * @return
     */
    public static List<Threshold> extractThresholds(String text,
                                                    ThresholdRelation relation) {
        List<Threshold> list = new ArrayList<Threshold>();
        if (relation == null) {
            Threshold threshold = new Threshold(text);
            list.add(threshold);
        }
        else {
            // Assume there are only two thresholds for the time being
            String[] tokens = text.split(relation.toString());
            for (String token : tokens) {
                Threshold threshold = new Threshold(token.trim());
                list.add(threshold);
            }
        }
        return list;
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
