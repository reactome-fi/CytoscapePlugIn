/*
 * Created on Apr 1, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistribution;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.reactome.r3.util.MathUtilities;

/**
 * This method is used to combine p-values from a list of t-test based on an extend Fisher's method
 * reported in paper: Combing dependent P-values by Kost & McDermott (Kost & McDermott. Statistics & 
 * Probability Letters 60 (2002): 183 - 190). 
 * (also see: http://www.sciencedirect.com/science/article/pii/S0167715202003103). Though we cannot
 * assume a known variance case, however, the implementation here is still based on the first known variance
 * case (2.1).
 * @author gwu
 *
 */
public class PValueCombiner {
    
    /**
     * Default constructor.
     */
    public PValueCombiner() {
    }
    
    /**
     * Call this method to do a Kosh & McDermott's method to combine p-values into
     * one single value.
     * @param realValuesList
     * @param pvalues
     * @return
     */
    public double combinePValue(List<List<Double>> realValuesList,
                                List<Double> pvalues) throws MathException {
        // Use a copy so that a minimum pvalue can be used
        List<Double> pvaluesCopy = new ArrayList<Double>(pvalues);
        // Have to make sure there is no zero in the pvalues collection. Otherwise,
        // log will throw an exception
        for (int i = 0; i < pvaluesCopy.size(); i++) {
            Double pvalue = pvaluesCopy.get(i);
            if (pvalue.equals(0.0d)) 
                pvaluesCopy.set(i, 1.0E-16); // As the minimum value
        }
        int size = pvalues.size();
        double fisher = calculateFisherValue(pvaluesCopy);
        
        double mean = calculateMean(size);
        double var = calculateVar(realValuesList, size);
        double f = 2.0d * mean * mean / var;
        double c = var / (2.0d * mean);
        // If f is NaN (e.g. because of a list of 0.0 in the real values, which generates NaN)
        // we switch to use the old Fisher method
        if (Double.isNaN(f) || Double.isNaN(c)) {
            ChiSquaredDistribution distribution = new ChiSquaredDistributionImpl(2 * size);
            return 1.0d - distribution.cumulativeProbability(fisher);
        }
        else {
            ChiSquaredDistribution distribution = new ChiSquaredDistributionImpl(f);
            return 1.0d - distribution.cumulativeProbability(c * fisher);
        }
    }
    
    private double calculateFisherValue(List<Double> pvalues) {
        double total = 0.0d;
        for (Double pvalue : pvalues)
            total += Math.log(pvalue);
        return -2.0d * total;
    }
    
    private double calculateMean(int size) {
        return 2.0d * size;
    }
    
    private double calculateVar(List<List<Double>> valuesList,
                                int size) {
        double total = 0.0d;
        for (int i = 0; i < valuesList.size() - 1; i++) {
            List<Double> values1 = valuesList.get(i);
            for (int j = i + 1; j < valuesList.size(); j++) {
                List<Double> values2 = valuesList.get(j);
                if (values1.size() != values2.size())
                    return Double.NaN; // Cannot calculate correaltion
                double cov = calculateCovariance(values1, values2);
                total += cov;
            }
        }
        return 4.0d * size + 2 * total;
    }
    
    /**
     * This is an implementation of equation 8 in the original paper.
     * @param values1
     * @param values2
     * @return
     */
    private double calculateCovariance(List<Double> values1,
                                       List<Double> values2) {
        // Calculate correlation rho between two vectors
        PearsonsCorrelation correlation = MathUtilities.constructPearsonCorrelation(values1, values2);
        double rho = correlation.getCorrelationMatrix().getEntry(0, 1);
        double rtn = rho * (3.263 + rho * (0.710 + 0.027 * rho));
        return rtn;
    }
    
}
