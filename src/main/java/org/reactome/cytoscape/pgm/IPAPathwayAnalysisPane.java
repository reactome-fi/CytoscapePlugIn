/*
 * Created on Mar 31, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.math.MathException;
import org.cytoscape.model.events.RowsSetEvent;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pgm.PGMFactorGraph;
import org.reactome.pgm.PGMVariable;
import org.reactome.r3.util.MathUtilities;

/**
 * This customized JPanel is used to show IPA pathway analysis results.
 * @author gwu
 *
 */
public class IPAPathwayAnalysisPane extends IPAValueTablePane {
    // For the whole data set label pathway analysis results
    private JLabel outputResultLabel;
    private JButton viewDetailsBtn;
    
    /**
     * Default constructor.
     */
    public IPAPathwayAnalysisPane(String title) {
        super(title);
    }

    @Override
    protected void modifyContentPane() {
        outputResultLabel = new JLabel("Total checked outputs:");
        viewDetailsBtn = new JButton("View Details");
        viewDetailsBtn.setToolTipText("Click to view details...");
        viewDetailsBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                viewTTestDetails();
            }
        });
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        controlToolBar.add(outputResultLabel);
        controlToolBar.add(viewDetailsBtn);
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        super.addTablePlotPane();
    }
    
    private void resetOverview() {
        IPAPathwayTableModel model = (IPAPathwayTableModel) contentPane.getTableModel();
        List<PGMVariable> variables = model.variables;
        StringBuilder builder = new StringBuilder();
        int size = 0;
        if (variables != null)
            size = variables.size();
        builder.append("Total checked outputs: " + variables.size());
        if (size == 0) {
            outputResultLabel.setText(builder.toString());
            viewDetailsBtn.setVisible(false); // Nothing to be viewed
            return; 
        }
        double pvalueCutoff = 0.05d;
        try {
            performTTest(variables, pvalueCutoff, builder);
            outputResultLabel.setText(builder.toString());
            viewDetailsBtn.setVisible(true);
        }
        catch(MathException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in doing t-test: " + e,
                                          "Error in T-Test",
                                          JOptionPane.ERROR_MESSAGE);
            outputResultLabel.setText(builder.toString());
            viewDetailsBtn.setVisible(false);
        }
    }
    
    private void performTTest(List<PGMVariable> variables,
                              double pvalueCutoff,
                              StringBuilder builder) throws MathException {
        // Do a test
        int negPerturbedOutputs = 0;
        int posPerturbedOutputs = 0;
        List<Double> realIPAs = new ArrayList<Double>();
        List<Double> randomIPAs = new ArrayList<Double>();
        List<Double> pvalues = new ArrayList<Double>();
        List<List<Double>> allRealIPAs = new ArrayList<List<Double>>();
        for (PGMVariable var : variables) {
            realIPAs.clear();
            randomIPAs.clear();
            Map<String, List<Double>> realProbs = var.getPosteriorValues();
            for (List<Double> probs : realProbs.values()) {
                double ipa = PlugInUtilities.calculateIPA(var.getValues(), probs);
                realIPAs.add(ipa);
            }
            Map<String, List<Double>> randomProbs = var.getRandomPosteriorValues();
            for (List<Double> probs : randomProbs.values()) {
                double ipa = PlugInUtilities.calculateIPA(var.getValues(), probs);
                randomIPAs.add(ipa);
            }
            Double pvalue = MathUtilities.calculateTTest(realIPAs, 
                                                         randomIPAs);
            if (pvalue < pvalueCutoff) {
                double realMean = MathUtilities.calculateMean(realIPAs);
                double randomMean = MathUtilities.calculateMean(randomIPAs);
                if (realMean < randomMean)
                    negPerturbedOutputs ++;
                else if (realMean > randomMean)
                    posPerturbedOutputs ++;
            }
            pvalues.add(pvalue);
            allRealIPAs.add(new ArrayList<Double>(realIPAs));
        }
        builder.append(" (").append(negPerturbedOutputs).append(" down perturbed, ");
        builder.append(posPerturbedOutputs).append(" up perturbed. Combined p-values: ");
        PValueCombiner combiner = new PValueCombiner();
        double combinedPValue = combiner.combinePValue(allRealIPAs, pvalues);
        builder.append(PlugInUtilities.formatProbability(combinedPValue)).append(")  ");
    }
    
    private void viewTTestDetails() {
        TTestDetailDialog dialog = new TTestDetailDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
        try {
            IPAPathwayTableModel model = (IPAPathwayTableModel) contentPane.getTableModel();
            dialog.setVariables(model.variables);
            dialog.setNetworkView(view);
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setSize(800, 600);
            // Make this dialog modaless so that the user can interact with 
            // other GUIs still.
            dialog.setVisible(true);
        }
        catch(MathException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in viewing t-test details: " + e.getMessage(),
                                          "Error in T-Test",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new IPAPathwayTableModel();
    }
    
    /**
     * Do nothing since this is a summary result panel, which is not
     * related to any node selection.
     */
    @Override
    public void handleEvent(RowsSetEvent event) {
    }

    public void setFactorGraph(PGMFactorGraph fg) {
        // Want to get a list of variables having output roles since
        // we want to focus on outputs for pathway perturbation study
        List<PGMVariable> outputs = new ArrayList<PGMVariable>();
        for (PGMVariable var : fg.getVariables()) {
            if (var.getRoles() == null || !var.getRoles().contains(PGMVariable.VariableRole.OUTPUT))
                continue;
            outputs.add(var);
        }
        IPAPathwayTableModel model = (IPAPathwayTableModel) contentPane.getTableModel();
        model.setVariables(outputs);
        resetOverview();
    }

    private class IPAPathwayTableModel extends IPAValueTableModel {
        private String np = "DownPerturbation";
        private String pp = "UpPerturbation";
        private String[] columns_with_pvalues = new String[] {
                "Sample",
                np,
                np + PlotTablePanel.P_VALUE_COL_NAME_AFFIX,
                np + PlotTablePanel.FDR_COL_NAME_AFFIX,
                pp,
                pp + PlotTablePanel.P_VALUE_COL_NAME_AFFIX,
                pp + PlotTablePanel.FDR_COL_NAME_AFFIX
        };
        private String[] colums_without_pvalues = new String[] {
                "Sample",
                np,
                pp
        };
        
        public IPAPathwayTableModel() {
            columnHeaders = columns_with_pvalues;
        }
        
        private List<List<Double>> generateRandomPerturbations() {
            List<Double> ipas = new ArrayList<Double>();
            Map<String, Double> sampleToNegative = new HashMap<String, Double>();
            Map<String, Double> sampleToPositive = new HashMap<String, Double>();
            for (PGMVariable var : variables) {
                Map<String, List<Double>> sampleToProbs = var.getRandomPosteriorValues();
                for (String sample : sampleToProbs.keySet()) {
                    List<Double> probs = sampleToProbs.get(sample);
                    double ipa = PlugInUtilities.calculateIPA(var.getValues(), probs);
                    if (ipa < 0.0d) {
                        Double value = sampleToNegative.get(sample);
                        if (value == null)
                            sampleToNegative.put(sample, ipa);
                        else
                            sampleToNegative.put(sample, value + ipa);
                    }
                    else if (ipa > 0.0d) {
                        Double value = sampleToPositive.get(sample);
                        if (value == null)
                            sampleToPositive.put(sample, ipa);
                        else
                            sampleToPositive.put(sample, value + ipa);
                    }
                }
            }
            List<Double> negValues = new ArrayList<Double>(sampleToNegative.values());
            Collections.sort(negValues);
            List<Double> posValues = new ArrayList<Double>(sampleToPositive.values());
            Collections.sort(posValues);
            List<List<Double>> rtn = new ArrayList<List<Double>>();
            rtn.add(negValues);
            rtn.add(posValues);
            return rtn;
        }

        @Override
        protected void resetDataWithPValues(List<String> sampleList) {
            columnHeaders = columns_with_pvalues;
            // In order to calculate p-values
            List<List<Double>> randomPerturbs = generateRandomPerturbations();
            for (int i = 0; i < sampleList.size(); i++) {
                String[] rowData = new String[variables.size() * 3 + 1];
                rowData[0] = sampleList.get(i);
                double negative = 0.0d;
                double positive = 0.0d;
                for (int j = 0; j < variables.size(); j++) {
                    PGMVariable var = variables.get(j);
                    Map<String, List<Double>> posteriors = var.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = PlugInUtilities.calculateIPA(var.getValues(), postProbs);
                    if (ipa < 0.0d)
                        negative += ipa;
                    else if (ipa > 0.0d)
                        positive += ipa;
                }
                rowData[1] = PlugInUtilities.formatProbability(negative);
                double pvalue = calculatePValue(negative, randomPerturbs.get(0));
                rowData[2] = PlugInUtilities.formatProbability(pvalue);
                rowData[4] = PlugInUtilities.formatProbability(positive);
                pvalue = calculatePValue(positive, randomPerturbs.get(1));
                rowData[5] = PlugInUtilities.formatProbability(pvalue);
                tableData.add(rowData);
            }
            int totalPermutation = variables.get(0).getRandomPosteriorValues().size();
            // Add FDR values
            int[] indices = new int[]{2, 5};
            for (int index : indices) {
                List<Double> pvalues = new ArrayList<Double>();
                // Sort the rows based on p-values
                final int index1 = index;
                Collections.sort(tableData, new Comparator<String[]>() {
                    public int compare(String[] row1, String[] row2) {
                        Double pvalue1 = new Double(row1[index1]);
                        Double pvalue2 = new Double(row2[index1]);   
                        return pvalue1.compareTo(pvalue2);
                    }
                });
                for (int i = 0; i < tableData.size(); i++) {
                    String[] row = tableData.get(i);
                    Double pvalue = new Double(row[index]);
                    if (pvalue.equals(0.0d)) 
                        pvalue = 1.0d / (totalPermutation + 1); // Use the closest double value for a conservative calculation
                    pvalues.add(pvalue);
                }
                List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
                // Replace p-values with FDRs
                for (int i = 0; i < tableData.size(); i++) {
                    String[] row = tableData.get(i);
                    row[index + 1] = String.format("%.3f", fdrs.get(i));
                }
            }
            // Need to sort the table back as the original
            Collections.sort(tableData, new Comparator<String[]>() {
                public int compare(String[] row1, String[] row2) {
                    return row1[0].compareTo(row2[0]);
                }
            });
        }

        @Override
        protected void resetDataWithoutPValues(List<String> sampleList) {
            columnHeaders = colums_without_pvalues;
            for (int i = 0; i < sampleList.size(); i++) {
                String[] rowData = new String[variables.size() + 1];
                rowData[0] = sampleList.get(i);
                double negativePerturbations = 0.0d;
                double positivePerturbations = 0.0d;
                for (int j = 0; j < variables.size(); j++) {
                    PGMVariable var = variables.get(j);
                    Map<String, List<Double>> posteriors = var.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = PlugInUtilities.calculateIPA(var.getValues(), postProbs);
                    if (ipa < 0.0d)
                        negativePerturbations += ipa;
                    else if (ipa > 0.0d)
                        positivePerturbations += ipa;
                }
                rowData[1] = PlugInUtilities.formatProbability(negativePerturbations);
                rowData[2] = PlugInUtilities.formatProbability(positivePerturbations);
                tableData.add(rowData);
            }
        }
    }
}
