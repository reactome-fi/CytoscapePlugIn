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
    
    /**
     * Default constructor.
     */
    public PGMImpactResultLoadTask() {
        needToAskSaveResults = false;
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
        // Get a file
        File file = PlugInUtilities.getAnalysisFile("Open Analysis Results",
                                                    FileUtil.LOAD);
        if (file == null) {
            frame.getGlassPane().setVisible(false);
            return;
        }
        try {
            FIPGMResults results = FIPGMResults.getResults();
            results.loadResults(file);
            // We need to fetch FIs first
            progPane.setText("Fetch FIs...");
            fetchFIs();
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
