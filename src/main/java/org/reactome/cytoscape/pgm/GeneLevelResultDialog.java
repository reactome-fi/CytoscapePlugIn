/*
 * Created on Feb 9, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.apache.commons.math.MathException;
import org.gk.render.RenderablePathway;
import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.common.DataType;

/**
 * This customized JDialog is used to show the observation data for a displayed pathway diagram.
 * @author gwu
 *
 */
public class GeneLevelResultDialog extends JDialog {
    // Used to display results
    private IPAPathwaySummaryPane summaryPane;
    
    /**
     * Default constructor.
     */
    public GeneLevelResultDialog() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    private void init() {
        summaryPane = new IPAPathwaySummaryPane("Observation");
        summaryPane.hideControlToolBar();
        summaryPane.setBorder(BorderFactory.createEtchedBorder());
        getContentPane().add(summaryPane, BorderLayout.CENTER);
        
        DialogControlPane controlPane = new DialogControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        controlPane.getCancelBtn().setVisible(false);
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        setLocationRelativeTo(getOwner());
    }
    
    
    public boolean showResultsForDiagram(RenderablePathway diagram,
                                         Set<String> genes) {
        FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(diagram);
        if (fgResults == null) {
            JOptionPane.showMessageDialog(this,
                                          "No inference results are available for this pathway. Please run inference first.",
                                          "No Results",
                                          JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        List<VariableInferenceResults> varResults = new ArrayList<VariableInferenceResults>();
        Set<Variable> variables = new HashSet<Variable>();
        Map<Variable, VariableInferenceResults> varToResults = fgResults.getVarToResults();
        for (Variable var : varToResults.keySet()) {
            if (var.getName().endsWith(DataType.mRNA_EXP.toString()) || var.getName().endsWith(DataType.CNV.toString())) {
                if (genes == null) { // Use all
                    variables.add(var);
                    varResults.add(varToResults.get(var));
                }
                else { // Need to check if the gene is in the list
                    int index = var.getName().indexOf("_"); // Should not use lastIndex since mRNA_Exp
                    String gene = var.getName().substring(0, index);
                    if (genes.contains(gene)) {
                        variables.add(var);
                        varResults.add(varToResults.get(var));
                    }
                }
            }
        }
        if (variables.size() == 0) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot find any results for selected ." + (genes == null ? "diagram." : "entity."),
                                          "Empty Results",
                                          JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        Map<String, String> sampleToType = fgResults.getSampleToType();
        try {
            summaryPane.setVariableResults(varResults, variables, sampleToType);
        }
        catch(MathException e) {
            JOptionPane.showMessageDialog(this,
                                          "Error in result display: " + e,
                                          "Error in Result Display",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    /**
     * Show the gene level results for the passed pathway diagram.
     * @param diagram
     * @return
     */
    public boolean showResultsForDiagram(RenderablePathway diagram) {
        return showResultsForDiagram(diagram, null);
    }
    
}
