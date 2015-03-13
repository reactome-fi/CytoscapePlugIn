/*
 * Created on Feb 9, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;

import org.apache.commons.math.MathException;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.RenderablePathway;
import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.service.TTestTableModel;
import org.reactome.factorgraph.Variable;

/**
 * This customized JDialog is used to show the observation data for a displayed pathway diagram.
 * @author gwu
 *
 */
public class GeneLevelResultDialog extends GeneLevelDialog {
    // Used to display results
    private IPAPathwaySummaryPane summaryPane;
    
    /**
     * Default constructor.
     */
    public GeneLevelResultDialog() {
    }
    
    protected void init() {
        summaryPane = createSummaryPane();
        summaryPane.hideControlToolBar();
        summaryPane.setBorder(BorderFactory.createEtchedBorder());
        summaryPane.getTablePlotPane().setChartTitle("Boxplot for Integrated Pathway Activities (IPAs) of Gene mRNAs");
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
        addTableSelectionListener(summaryPane.getTablePlotPane().getTable());
    }

    private IPAPathwaySummaryPane createSummaryPane() {
        return new IPAPathwaySummaryPane("Observation") {

            @Override
            protected void doTableSelection(ListSelectionEvent e) {
                // Do nothing to avoid selecting in the pathway diagram.
            }

            @Override
            protected TTestTablePlotPane<Variable> createTablePlotPane() {
                TTestTablePlotPane<Variable> tablePlotPane = new TTestTablePlotPane<Variable>() {

                    @Override
                    protected String[] getAnnotations(Variable key) {
                        return new String[]{getVariableKey(key)}; // Don't want to show two columns having the same content.
                    }

                    @Override
                    protected String getKey(Variable key) {
                        return getVariableKey(key);
                    }

                    @Override
                    protected void sortValueKeys(List<Variable> list) {
                        Collections.sort(list, new Comparator<Variable>() {
                            public int compare(Variable var1, Variable var2) {
                                String key1 = getVariableKey(var1);
                                String key2 = getVariableKey(var2);
                                return key1.compareTo(key2);
                            }
                        });
                    }
                    
                };
                TTestTableModel model = (TTestTableModel) tablePlotPane.getTable().getModel();
                String[] headers = new String[]{
                        "Name",
                        "RealMean",
                        "RandomMean",
                        "MeanDiff",
                        "p-value",
                        "FDR"
                };
                model.setColHeaders(Arrays.asList(headers),
                                    1);
                return tablePlotPane;
            }

            @Override
            protected void handleGraphEditorSelection(PathwayEditor editor) {
                // Do nothing since there is no change we will get selected here.
            }
        };
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
            // For gene level results, we want to show results for mRNA since results for mRNA should
            // combine information from both mRNA_Exp and CNV
            if (var.getName().endsWith("_mRNA")) {
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
                                          "Cannot find any results for selected " + (genes == null ? "diagram." : "entity."),
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
