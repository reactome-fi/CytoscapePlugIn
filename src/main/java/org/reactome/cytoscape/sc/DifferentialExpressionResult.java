package org.reactome.cytoscape.sc;

import java.util.List;

/**
 * This class is used to model the results produced from differential gene expression analysis.
 * @author wug
 *
 */
public class DifferentialExpressionResult {
    private List<Double> scores;
    private List<String> names;
    private List<Double> logFoldChanges;
    private List<Double> pvals;
    private List<Double> pvalsAdj;
    
    public DifferentialExpressionResult() {
    }

    public List<Double> getScores() {
        return scores;
    }

    public void setScores(List<Double> scores) {
        this.scores = scores;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public List<Double> getLogFoldChanges() {
        return logFoldChanges;
    }

    public void setLogFoldChanges(List<Double> logFoldChanges) {
        this.logFoldChanges = logFoldChanges;
    }

    public List<Double> getPvals() {
        return pvals;
    }

    public void setPvals(List<Double> pvals) {
        this.pvals = pvals;
    }

    public List<Double> getPvalsAdj() {
        return pvalsAdj;
    }

    public void setPvalsAdj(List<Double> pvalsAdj) {
        this.pvalsAdj = pvalsAdj;
    }

}
