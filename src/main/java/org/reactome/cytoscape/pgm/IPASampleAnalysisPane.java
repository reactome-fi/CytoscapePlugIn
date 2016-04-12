/*
 * Created on Mar 31, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.PathwayEditor;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Variable;
import org.reactome.pathway.factorgraph.IPACalculator;
import org.reactome.r3.util.MathUtilities;

/**
 * This customized JPanel is used to show IPA pathway analysis results for a list of samples
 * as up or down perturbation results.
 * @author gwu
 *
 */
public class IPASampleAnalysisPane extends IPAValueTablePane {

    /**
     * Default constructor.
     */
    public IPASampleAnalysisPane(String title) {
        super(title);
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
    
    /**
     * Nothing needs to be done here.
     */
    @Override
    protected void handleGraphEditorSelection(PathwayEditor editor) {
    }

    @Override
    public void setNetworkView(CyNetworkView view) {
        this.view = view;
    }

    /**
     * Call this method to show the results.
     * @param fgResults
     * @param outputVars
     */
    public void setInferenceResults(FactorGraphInferenceResults fgResults,
                                    Set<Variable> outputVars) {
        // We want to focus on outputs for pathway perturbation study
        List<VariableInferenceResults> outputVarResults = null;
        if (fgResults != null && outputVars != null) {
            outputVarResults = fgResults.getVariableInferenceResults(outputVars);
            this.fgInfResults = fgResults;
        }
        IPAPathwayTableModel model = (IPAPathwayTableModel) contentPane.getTableModel();
        model.setVarResults(outputVarResults);
    }
    
    /**
     * In order to show results correctly, call another method setInferenceResults(FactorGraphInferenceResults, Set).
     */
    @Override
    public void setFGInferenceResults(FactorGraphInferenceResults fgResults) {
        this.fgInfResults = fgResults;
    }

    public List<VariableInferenceResults> getInferenceResults() {
        IPAPathwayTableModel model = (IPAPathwayTableModel) contentPane.getTableModel();
        return model.varResults;
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
            for (VariableInferenceResults varResult : varResults) {
                Map<String, List<Double>> sampleToProbs = varResult.getRandomPosteriorValues();
                for (String sample : sampleToProbs.keySet()) {
                    List<Double> probs = sampleToProbs.get(sample);
                    double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), probs);
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
                Object[] rowData = new Object[columnHeaders.length];
                rowData[0] = sampleList.get(i);
                double negative = 0.0d;
                double positive = 0.0d;
                for (int j = 0; j < varResults.size(); j++) {
                    VariableInferenceResults varResult = varResults.get(j);
                    Map<String, List<Double>> posteriors = varResult.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), postProbs);
                    if (ipa < 0.0d)
                        negative += ipa;
                    else if (ipa > 0.0d)
                        positive += ipa;
                }
                rowData[1] = negative;
                // Need to calcaulte from the left side
                double pvalue = PlugInUtilities.calculateNominalPValue(negative,
                                                                       randomPerturbs.get(0),
                                                                       "left");
                rowData[2] = pvalue;
                rowData[4] = positive;
                // Need to calculate from the right side
                pvalue = PlugInUtilities.calculateNominalPValue(positive, 
                                                                randomPerturbs.get(1),
                                                                "right");
                rowData[5] = pvalue;
                tableData.add(rowData);
            }
            int totalPermutation = varResults.get(0).getRandomPosteriorValues().size();
            // Add FDR values
            int[] indices = new int[]{2, 5};
            for (int index : indices) {
                List<Double> pvalues = new ArrayList<Double>();
                // Sort the rows based on p-values
                final int index1 = index;
                Collections.sort(tableData, new Comparator<Object[]>() {
                    public int compare(Object[] row1, Object[] row2) {
                        Double pvalue1 = (Double)row1[index1];
                        Double pvalue2 = (Double)row2[index1];   
                        return pvalue1.compareTo(pvalue2);
                    }
                });
                for (int i = 0; i < tableData.size(); i++) {
                    Object[] row = tableData.get(i);
                    Double pvalue = (Double)row[index];
                    if (pvalue.equals(0.0d)) 
                        pvalue = 1.0d / (totalPermutation + 1); // Use the closest double value for a conservative calculation
                    pvalues.add(pvalue);
                }
                List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
                // Replace p-values with FDRs
                for (int i = 0; i < tableData.size(); i++) {
                    Object[] row = tableData.get(i);
                    row[index + 1] = fdrs.get(i);
                }
            }
            // Need to sort the table back as the original
            Collections.sort(tableData, new Comparator<Object[]>() {
                public int compare(Object[] row1, Object[] row2) {
                    return row1[0].toString().compareTo(row2[0].toString());
                }
            });
        }

        @Override
        protected void resetDataWithoutPValues(List<String> sampleList) {
            columnHeaders = colums_without_pvalues;
            for (int i = 0; i < sampleList.size(); i++) {
                Object[] rowData = new Object[columnHeaders.length];
                rowData[0] = sampleList.get(i);
                double negativePerturbations = 0.0d;
                double positivePerturbations = 0.0d;
                for (int j = 0; j < varResults.size(); j++) {
                    VariableInferenceResults varResult = varResults.get(j);
                    Map<String, List<Double>> posteriors = varResult.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), postProbs);
                    if (ipa < 0.0d)
                        negativePerturbations += ipa;
                    else if (ipa > 0.0d)
                        positivePerturbations += ipa;
                }
                rowData[1] = negativePerturbations;
                rowData[2] = positivePerturbations;
                tableData.add(rowData);
            }
        }
    }
}
