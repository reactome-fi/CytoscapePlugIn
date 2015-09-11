/*
 * Created on Sep 10, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.util.List;

import org.reactome.cytoscape.service.FIAnalysisTask;
import org.reactome.factorgraph.LoopyBeliefPropagation;
import org.reactome.fi.pgm.FIPGMConstructor.PGMType;

/**
 * Customized FIAnalysisTask to perform FI PGM based impact analysis.
 * @author gwu
 *
 */
public class PGMImpactAnalysisTask extends FIAnalysisTask {
    private List<DataDescriptor> data;
    private LoopyBeliefPropagation lbp;
    private PGMType pgmType;
    
    /**
     * Default constructor.
     */
    public PGMImpactAnalysisTask() {
    }
    
    public List<DataDescriptor> getData() {
        return data;
    }

    public void setData(List<DataDescriptor> data) {
        this.data = data;
    }

    public LoopyBeliefPropagation getLbp() {
        return lbp;
    }

    public void setLbp(LoopyBeliefPropagation lbp) {
        this.lbp = lbp;
    }

    public PGMType getPGMType() {
        return pgmType;
    }

    public void setPGMType(PGMType pgmType) {
        this.pgmType = pgmType;
    }

    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.FIAnalysisTask#doAnalysis()
     */
    @Override
    protected void doAnalysis() {
        System.out.println(getClass().getName() + ": doAnalysis");
    }
    
}
