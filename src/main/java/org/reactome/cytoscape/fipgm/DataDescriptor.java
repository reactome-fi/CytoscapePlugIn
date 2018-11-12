/*
 * Created on Sep 10, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.util.List;

import org.reactome.cytoscape.fipgm.Threshold.ThresholdRelation;
import org.reactome.factorgraph.common.DataType;

/**
 * This class is used to describe a selected data for impact analysis.
 * @author gwu
 *
 */
public class DataDescriptor {
    // The name of the file containing the data
    private String fileName;
    // The data type
    private DataType dataType;
    // For distribution information
    private DataTypeDistribution distribution;
    private Threshold[] thresholds;
    private ThresholdRelation relation = ThresholdRelation.or; // As default
    
    /**
     * Default constructor.
     */
    public DataDescriptor() {
    }
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public DataTypeDistribution getDistribution() {
        return distribution;
    }

    public void setDistribution(DataTypeDistribution distribution) {
        this.distribution = distribution;
    }

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
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("fileName: " + getFileName()).append("; ");
        builder.append("dataType: " + getDataType()).append("; ");
        if (thresholds != null && thresholds.length > 0) {
            builder.append("abnormal: ");
            if (thresholds.length == 1)
                builder.append(thresholds[0].toString());
            else if (thresholds.length == 2) {
                builder.append(thresholds[0].toString() + " " + 
                               relation + " " + 
                               thresholds[1].toString());
            }
        }
        else
            builder.append(distribution == null ? "null" : distribution.toListString());
        return builder.toString();
    }

}
