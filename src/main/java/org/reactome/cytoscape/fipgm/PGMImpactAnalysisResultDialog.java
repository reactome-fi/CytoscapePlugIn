/*
 * Created on Sep 17, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTable;

import org.apache.commons.math.MathException;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.Variable;

/**
 * A customized JDialog showing the analysis results from FI_PGM impact.
 * @author gwu
 *
 */
public class PGMImpactAnalysisResultDialog extends JDialog {
    private final int MAXIMUM_ROW_FOR_PLOT = 500;
    // A hard-coded list of genes
    private final int TOTAL_GENE = 1000;
    private boolean isOkClicked;
    // To show results by doing t-test
    private FilterableTTestTablePlotPane tTestPlotPane;
    private JTable resultTable;
    
    public PGMImpactAnalysisResultDialog() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    private void init() {
        setTitle("FI PGM Impact Analysis Results");
        tTestPlotPane = new FilterableTTestTablePlotPane();
        tTestPlotPane.setMaximumRowForPlot(MAXIMUM_ROW_FOR_PLOT);
        getContentPane().add(tTestPlotPane, BorderLayout.CENTER);
        // Want to keep these variables in the class
        resultTable = tTestPlotPane.getResultTable();
        
        DialogControlPane controlPane = new DialogControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        setSize(1000, 650);
        GKApplicationUtilities.center(this);
        
        // Add some controls
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!validateSelectedGenes())
                    return;
                dispose();
                isOkClicked = true;
            }
        });
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                isOkClicked = false;
            }
        });
    }
    
    private boolean validateSelectedGenes() {
        // Check the number of displayed genes
        int totalGenes = resultTable.getRowCount();
        if (totalGenes > TOTAL_GENE) {
            JOptionPane.showMessageDialog(this,
                                          "Please filter genes to less than " + TOTAL_GENE + ".", 
                                          "Too Many Genes",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    
    public boolean isOkClicked() {
        return this.isOkClicked;
    }
    
    public Map<String, Double> getSelectedGeneToScore() {
        Map<String, Double> geneToScore = new HashMap<>();
        for (int i = 0; i < resultTable.getRowCount(); i++) {
            String gene = (String) resultTable.getValueAt(i, 0);
            // Use the average score
            String scoreText = (String) resultTable.getValueAt(i, 1);
            Double score = new Double(scoreText);
            geneToScore.put(gene, score);
        }
        return geneToScore;
    }
    
    public void setSampleResults(Map<String, Map<Variable, Double>> sampleToVarToScore,
                                 Map<String, Map<Variable, Double>> randomSampleToVarToScore) {
        try {
            tTestPlotPane.setSampleResults(sampleToVarToScore, randomSampleToVarToScore);
        }
        catch(MathException e) {
            JOptionPane.showMessageDialog(this,
                                          "Error in displaying results: " + e,
                                          "Error in Result Display",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
}
