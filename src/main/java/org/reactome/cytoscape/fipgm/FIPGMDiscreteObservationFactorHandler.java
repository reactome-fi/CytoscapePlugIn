/*
 * Created on Sep 15, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import org.junit.Test;
import org.reactome.cytoscape.fipgm.Threshold.ThresholdRelation;
import org.reactome.cytoscape.fipgm.Threshold.ValueRelation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.VariableAssignment;
import org.reactome.factorgraph.common.DataType;
import org.reactome.factorgraph.common.DiscreteObservationFactorhandler;

/**
 * A customized DiscreteObservationFactorHandler to be used in this package.
 * @author gwu
 *
 */
public class FIPGMDiscreteObservationFactorHandler extends DiscreteObservationFactorhandler {
    // To be used for discretizing
    private Threshold[] thresholds;
    private ThresholdRelation relation = ThresholdRelation.or; // As default
    
    /**
     * Default constructor.
     */
    public FIPGMDiscreteObservationFactorHandler() {
    }
    
    @Override
    public VariableAssignment<Number> parseValue(Double value, 
                                                 DataType dataType,
                                                 Variable variable) {
        Integer assignment = getAssignment(value,
                                           variable.getStates()); // We use only one value here.
        VariableAssignment<Number> varAssgn = new VariableAssignment<Number>();
        varAssgn.setVariable(variable);
        varAssgn.setAssignment(assignment);
        return varAssgn;
    }
    
    /**
     * Support only two states.
     * @param value
     * @param state
     * @return
     */
    private int getAssignment(Double value,
                              int state) {
        if (state != 2)
            throw new IllegalArgumentException("The variable state should be 2 only!");
        int rtn = 0;
        boolean abnormal = true;
        if (relation == ThresholdRelation.and || relation == null) {
            for (Threshold t : thresholds) {
                if (t.getValueRelation() == ValueRelation.less)
                    abnormal &= (value < t.getValue());
                else
                    abnormal &= (value > t.getValue());
            }
        }
        else if (relation == ThresholdRelation.or) {
            abnormal = false;
            for (Threshold t : thresholds) {
                if (t.getValueRelation() == ValueRelation.less)
                    abnormal |= (value < t.getValue());
                else
                    abnormal |= (value > t.getValue());
            }
        }
        if (abnormal)
            rtn = 1;
        return rtn; // Default should be 0, which is normal
    }
    
    @Test
    public void testGetAssignment() {
        thresholds = new Threshold[1];
        Threshold t = new Threshold();
        t.setValue(0.0d);
        t.setValueRelation(ValueRelation.less);
        thresholds[0] = t;
        double value = -2.0d;
        System.out.println(value + " -> " + getAssignment(value, 2));
        value = 2.0d;
        System.out.println(value + " -> " + getAssignment(value, 2));
        Threshold t1 = new Threshold();
        t1.setValue(1.0d);
        thresholds = new Threshold[2];
        t.setValue(-1.0d);
        thresholds[0] = t;
        thresholds[1] = t1;
        relation = ThresholdRelation.or;
        value = -2.0d;
        System.out.println(value + " -> " + getAssignment(value, 2));
        value = 2.0d;
        System.out.println(value + " -> " + getAssignment(value, 2));
        relation = ThresholdRelation.and;
        t.setValueRelation(ValueRelation.greater);
        t1.setValueRelation(ValueRelation.less);
        value = -2.0d;
        System.out.println(value + " -> " + getAssignment(value, 2));
        value = 2.0d;
        System.out.println(value + " -> " + getAssignment(value, 2));
        value = 0.5d;
        System.out.println(value + " -> " + getAssignment(value, 2));
    }

    public Threshold[] getThresholds() {
        return thresholds;
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
    
}
