/*
 * Created on Oct 19, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.util.swing.FileUtil;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * @author gwu
 *
 */
public class PGMImpactResultLoadTask extends PGMImpactAnalysisTask {
    private boolean usedToShowResults;
    
    /**
     * Default constructor.
     */
    public PGMImpactResultLoadTask() {
        needToAskSaveResults = false;
    }

    public boolean isUsedToShowResults() {
        return usedToShowResults;
    }

    public void setUsedToShowResults(boolean usedToShowResults) {
        this.usedToShowResults = usedToShowResults;
    }

    @Override
    protected void doAnalysis() {
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        ProgressPane progPane = new ProgressPane();
        frame.setGlassPane(progPane);
        progPane.setTitle("FI PGM Impact Analysis");
        progPane.setText("Loading analysis results...");
        progPane.setIndeterminate(true);
        progPane.setSize(400, 200);
        progPane.setVisible(true);
        File file = null;
        if (!usedToShowResults) {
            // Get a file
            file = PlugInUtilities.getAnalysisFile("Open Analysis Results",
                                                   FileUtil.LOAD);
            if (file == null) {
                frame.getGlassPane().setVisible(false);
                return;
            }
        }
        try {
            FIPGMResults results = FIPGMResults.getResults();
            if (file != null) {
                results.loadResults(file);
                // We need to fetch FIs first
                progPane.setText("Fetch FIs...");
                fetchFIs();
            }
            showResults(results.getSampleToVarToScore(),
                        results.getRandomSampleToVarToScore(),
                        progPane,
                        frame);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(frame,
                                          "Cannot open inferece results: " + e,
                                          "Error in Opening Results", 
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            frame.getGlassPane().setVisible(false);
        }
    }
    
}
