/*
 * Created on Jan 23, 2017
 *
 */
package org.reactome.cytoscape.service;

import javax.swing.JPanel;

import org.cytoscape.work.AbstractTask;

/**
 * @author gwu
 *
 */
public abstract class AbstractPathwayEnrichmentAnalysisTask extends AbstractTask {
    
    /**
     * Default constructor.
     */
    public AbstractPathwayEnrichmentAnalysisTask() {
    }
    
    public abstract void setGeneList(String geneList);
    
    public abstract void setEventPane(JPanel treePane);
    
    /**
     * Use a glass panel based way to do progress monitoring to avoid part text problem
     * in the event tree.
     * NOTE: This doesn't help at all!
     * @param progressPane
     * @throws Exception
     */
    public abstract void doEnrichmentAnalysis();
    
}
