/*
 * Created on Feb 12, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Variable;
import org.reactome.pathway.factorgraph.IPACalculator;
import org.reactome.r3.util.MathUtilities;

/**
 * This class is used to model summarized analysis results for one FactorGraph.
 * @author gwu
 *
 */
public class PathwayResultSummary {
    private Long dbId;
    private String pathwayName;
    private double averageUpIPAs;
    private double averageDownIPAs;
    private double combinedPValue;
    private double combinedPValueFDR;
    private double minPValue;
    private double minPValueFDR;
    // The following properties are used for summarizing
    private Double pvalueCutoff;
    private Double IPACutoff;
    private int upOutputs;
    private int downOutputs;
    
    /**
     * Default constructor.
     */
    public PathwayResultSummary() {
    }

    public Double getPvalueCutoff() {
        return pvalueCutoff;
    }

    public void setPvalueCutoff(Double pvalueCutoff) {
        this.pvalueCutoff = pvalueCutoff;
    }

    public Double getIPACutoff() {
        return IPACutoff;
    }

    public void setIPACutoff(Double iPACutoff) {
        IPACutoff = iPACutoff;
    }

    public int getUpOutputs() {
        return upOutputs;
    }

    public int getDownOutputs() {
        return downOutputs;
    }

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    public String getPathwayName() {
        return pathwayName;
    }

    public void setPathwayName(String pathwayName) {
        this.pathwayName = pathwayName;
    }

    public double getAverageUpIPAs() {
        return averageUpIPAs;
    }

    public void setAverageUpIPAs(double averageUpIPAs) {
        this.averageUpIPAs = averageUpIPAs;
    }

    public double getAverageDownIPAs() {
        return averageDownIPAs;
    }

    public void setAverageDownIPAs(double averageDownIPAs) {
        this.averageDownIPAs = averageDownIPAs;
    }

    public double getCombinedPValue() {
        return combinedPValue;
    }

    public void setCombinedPValue(double combinedPValue) {
        this.combinedPValue = combinedPValue;
    }

    public double getCombinedPValueFDR() {
        return combinedPValueFDR;
    }

    public void setCombinedPValueFDR(double combinedPValueFDR) {
        this.combinedPValueFDR = combinedPValueFDR;
    }
    
    public void setCombinedPValueFDR(Double combinedPValueFDR) {
        this.combinedPValueFDR = combinedPValueFDR;
    }

    public double getMinPValue() {
        return minPValue;
    }

    public void setMinPValue(double minPValue) {
        this.minPValue = minPValue;
    }

    public double getMinPValueFDR() {
        return minPValueFDR;
    }

    public void setMinPValueFDR(double minPValueFDR) {
        this.minPValueFDR = minPValueFDR;
    }
    
    public void setMinPValueFDR(Double minPValueFDR) {
        this.minPValueFDR = minPValueFDR;
    }
    
    /**
     * Set the inference results for this object. The passed object will be parsed to
     * get the needed propeties for this object.
     * @param fgResults
     */
    public void setResults(FactorGraphInferenceResults fgResults,
                           Set<Variable> outputVars) throws MathException {
        FactorGraph fg = fgResults.getFactorGraph();
        String name = fg.getName();
        // Did some parsing to get ID and name
        int index = name.indexOf(":");
        int index1 = name.indexOf("]");
        String dbId = name.substring(index + 1, index1).trim();
        setDbId(new Long(dbId));
        String pathwayName = name.substring(index1 + 1).trim();
        setPathwayName(pathwayName);
        List<VariableInferenceResults> varResults = fgResults.getVariableInferenceResults(outputVars);
        parseResults(varResults,
                     outputVars, 
                     fgResults.getSampleToType());
    }
    
    public void parseResults(List<VariableInferenceResults> varResults,
                             Set<Variable> outputVars,
                             Map<String, String> sampleToType) throws MathException {
        List<Double> realIPAs = new ArrayList<Double>(); // In two cases, this is used as the first case
        List<Double> randomIPAs = new ArrayList<Double>(); // In two cases, this is used as the second case
        List<List<Double>> allRealIPAs = new ArrayList<List<Double>>();
        MannWhitneyUTest uTest = new MannWhitneyUTest();
        List<Double> pvalues = new ArrayList<Double>();
        Map<String, Set<String>> typeToSamples = null;
        if (sampleToType != null && sampleToType.size() > 0)
            typeToSamples = PlugInUtilities.getTypeToSamples(sampleToType);
        // Used for some stats
        SummaryStatistics downStats = new SummaryStatistics();
        SummaryStatistics upStats = new SummaryStatistics();
        upOutputs = 0;
        downOutputs = 0;
        for (VariableInferenceResults varResult : varResults) {
            if (!outputVars.contains(varResult.getVariable()))
                continue; // Make sure it counts for outputs only
            realIPAs.clear();
            randomIPAs.clear();
            if (typeToSamples == null)
                calculateIPAForOverview(realIPAs,
                                        randomIPAs, 
                                        varResult);
            else
                calculateTwoCasesIPAForOverview(realIPAs,
                                                randomIPAs,
                                                varResult,
                                                typeToSamples);
            if (realIPAs.size() == 0 || randomIPAs.size() == 0)
                continue; // Skip it
            double realMean = MathUtilities.calculateMean(realIPAs);
            double randomMean = MathUtilities.calculateMean(randomIPAs);
            double diff = realMean - randomMean;
            if (diff < 0.0d)
                downStats.addValue(diff);
            else if (diff > 0.0d)
                upStats.addValue(diff);
            double pvalue = uTest.mannWhitneyUTest(PlugInUtilities.convertDoubleListToArray(realIPAs),
                                                   PlugInUtilities.convertDoubleListToArray(randomIPAs));
            pvalues.add(pvalue);
            allRealIPAs.add(realIPAs);
            if (pvalueCutoff != null && IPACutoff != null) {
                if (Math.abs(diff) >= IPACutoff && pvalue <= pvalueCutoff) {
                    if (diff < 0.0d)
                        downOutputs ++;
                    else
                        upOutputs ++;
                }
            }
        }
        setAverageDownIPAs(downStats.getMean());
        setAverageUpIPAs(upStats.getMean());
        setMinPValue(pvalues);
        setCombinedPValue(pvalues, allRealIPAs);
    }
    
    private void setCombinedPValue(List<Double> pvalues,
                                   List<List<Double>> allIPAs) throws MathException {
        double combined = PlugInUtilities.calculateCombinedPValue(pvalues, allIPAs);
        setCombinedPValue(combined);
    }
    
    private void setMinPValue(List<Double> pvalue) {
        double minP = Double.MAX_VALUE;
        for (Double p : pvalue)
            minP = Math.min(p, minP);
        setMinPValue(minP);
    }

    private void calculateIPAForOverview(List<Double> realIPAs,
                                         List<Double> randomIPAs,
                                         VariableInferenceResults varResult) {
        Map<String, List<Double>> sampleToRealProbs = varResult.getPosteriorValues();
        for (List<Double> probs : sampleToRealProbs.values()) {
            double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), probs);
            realIPAs.add(ipa);
        }
        Map<String, List<Double>> sampleToRandomProbs = varResult.getRandomPosteriorValues();
        for (List<Double> probs : sampleToRandomProbs.values()) {
            double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), probs);
            randomIPAs.add(ipa);
        }
    }
    
    private void calculateTwoCasesIPAForOverview(List<Double> realIPAs,
                                                 List<Double> randomIPAs,
                                                 VariableInferenceResults varResult,
                                                 Map<String, Set<String>> typeToSamples) {
        List<String> types = new ArrayList<String>(typeToSamples.keySet());
        // Do a sort so that the results are the same as in the table
        Collections.sort(types);
        // The first type is used as real. No order is needed
        Map<String, List<Double>> sampleToRealProbs = varResult.getResultsForSamples(typeToSamples.get(types.get(0)));
        for (List<Double> probs : sampleToRealProbs.values()) {
            double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), probs);
            realIPAs.add(ipa);
        }
        // The second is used as random.
        Map<String, List<Double>> sampleToRandomProbs = varResult.getResultsForSamples(typeToSamples.get(types.get(1)));
        for (List<Double> probs : sampleToRandomProbs.values()) {
            double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), probs);
            randomIPAs.add(ipa);
        }
    }
    
}
