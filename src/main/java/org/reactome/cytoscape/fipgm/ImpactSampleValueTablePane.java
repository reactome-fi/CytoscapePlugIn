/*
 * Created on Oct 12, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.Cursor;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.TableRowSorter;

import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.reactome.cytoscape.pgm.IPAValueTablePane;
import org.reactome.cytoscape.pgm.PlotTablePanel;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Variable;
import org.reactome.r3.util.MathUtilities;

/**
 * This panel is used to display impact analysis results for all samples for a selected gene
 * from a displayed FI network.
 * @author gwu
 *
 */
public class ImpactSampleValueTablePane extends IPAValueTablePane {
    
    /**
     * @param title
     */
    public ImpactSampleValueTablePane(String title) {
        super(title);
    }
    
    @Override
    protected void addTablePlotPane() {
        super.addTablePlotPane();
        adjustGUIs();
    }

    protected void adjustGUIs() {
        // Make modifications here for sub-classing purpose.
        // Need to change the label a little bit
        if (ipaLabel != null) {
            ipaLabel.setText("Show gene impact scores");
            // Make it not clickable
            MouseListener[] listeners = ipaLabel.getMouseListeners();
            if (listeners != null && listeners.length > 0) {
                for (MouseListener listener : listeners)
                    ipaLabel.removeMouseListener(listener);
            }
            ipaLabel.setCursor(Cursor.getDefaultCursor());
        }
        if (contentPane != null)
            contentPane.setYAxisLabel("Impact Score");
    }

    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new ImpactSampleValueTableModel();
    }
    
    @Override
    public void setVariables(List<Variable> variables) {
        ImpactSampleValueTableModel model = (ImpactSampleValueTableModel) contentPane.getTableModel();
        model.setVariables(variables);
    }
    
    @Override
    protected TableRowSorter<NetworkModuleTableModel> createTableRowSorter(NetworkModuleTableModel model) {
        TableRowSorter<NetworkModuleTableModel> sorter = new TableRowSorter<NetworkModuleTableModel>(model) {
            @Override
            public Comparator<?> getComparator(int column) {
                if (column == 0)
                    return super.getComparator(0);
                Comparator<String> comparator = new Comparator<String>() {
                    public int compare(String value1, String value2) {
                        if (value1 == null || value1.length() == 0 ||
                            value2 == null || value2.length() == 0)
                            return 0;
                        if (value1.equals("-INFINITY") || value2.equals("INFINITY"))
                            return -1;
                        if (value2.equals("-INFINITY") || value1.equals("INFINITY"))
                            return 1;
                        Double d1 = new Double(value1);
                        Double d2 = new Double(value2);
                        return d1.compareTo(d2);
                    }
                };
                return comparator;
            }
        };
        return sorter;
    }

    @Override
    protected void initNodeToVarMap() {
        nodeToVar.clear();
        if (view == null)
            return;
        // There should be only one result. Don't get it from the data model,
        // which may not be initialized.
        FIPGMResults results = FIPGMResults.getResults();
        if (results == null)
            return;
        Map<String, Variable> nameToVar = new HashMap<String, Variable>();
        // Just choose the first results
        Map<Variable, Double> varToScore = results.getSampleToVarToScore().values().iterator().next();
        for (Variable var : varToScore.keySet())
            nameToVar.put(var.getName(), var);
        Map<String, CyNode> nameToNode = new HashMap<>();
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        List<CyNode> nodes = view.getModel().getNodeList();
        if (nodes != null && nodes.size() > 0) {
            for (CyNode node : nodes) {
                String nodeName = nodeTable.getRow(node.getSUID()).get("name", String.class);
                if (nodeName != null)
                    nameToNode.put(nodeName, node);
            }
        }
        // Now create a map
        for (String name : nameToVar.keySet()) {
            Variable var = nameToVar.get(name);
            CyNode node = nameToNode.get(name);
            if (node == null)
                continue;
            nodeToVar.put(node, var);
        }
    }
    
    /**
     * A customized table model to show a list of impact analysis for one selected node (aka gene)
     * from the displayed FI network.
     * @author gwu
     *
     */
    private class ImpactSampleValueTableModel extends IPAValueTableModel {
        private List<Variable> variables; // Results to be displayed for this variable
        
        public ImpactSampleValueTableModel() {
            
        }

        /**
         * Set the variable whose values should be displayed.
         * @param gene
         */
        public void setVariables(List<Variable> vars) {
            if (variables == null)
                variables = new ArrayList<>(vars);
            else {
                variables.clear();
                variables.addAll(vars);
            }
            resetData();
        }

        @Override
        protected void resetData() {
            if (FIPGMResults.getResults() == null || variables == null || variables.size() == 0) {
                columnHeaders = originalHeaders;
                // Refresh the tableData
                for (String[] values : tableData) {
                    for (int i = 1; i < values.length; i++)
                        values[i] = "";
                }
                fireTableStructureChanged();
                return;
            }
            // Get a list of all samples
            Set<String> samples = new HashSet<String>(FIPGMResults.getResults().getSampleToVarToScore().keySet());
            List<String> sampleList = new ArrayList<String>(samples);
            Collections.sort(sampleList);
            tableData.clear();
            
            if (hideFDRs)
                resetDataWithoutPValues(sampleList);
            else
                resetDataWithPValues(sampleList);
            fireTableStructureChanged();
        }
        
        private Map<Variable, List<Double>> getRandomScores() {
            Map<Variable, List<Double>> varToRandomScores = new HashMap<>();
            FIPGMResults results = FIPGMResults.getResults();
            Map<String, Map<Variable, Double>> randomSampleToVarToScore = results.getRandomSampleToVarToScore();
            for (String sample : randomSampleToVarToScore.keySet()) {
                Map<Variable, Double> varToScore = randomSampleToVarToScore.get(sample);
                for (Variable var : variables) {
                    Double score = varToScore.get(var);
                    if (score == null)
                        continue;
                    List<Double> scores = varToRandomScores.get(var);
                    if (scores == null) {
                        scores = new ArrayList<>();
                        varToRandomScores.put(var, scores);
                    }
                    scores.add(score);
                }
            }
            return varToRandomScores;
        }

        @Override
        protected void resetDataWithPValues(List<String> sampleList) {
            columnHeaders = new String[variables.size() * 3 + 1];
            columnHeaders[0] = "Sample";
            for (int i = 0; i < variables.size(); i++) {
                String label = variables.get(i).getName();
                columnHeaders[3 * i + 1] = label;
                columnHeaders[3 * i + 2] = label + PlotTablePanel.P_VALUE_COL_NAME_AFFIX;
                columnHeaders[3 * i + 3] = label + PlotTablePanel.FDR_COL_NAME_AFFIX;
            }
            // In order to calculate p-values
            Map<Variable, List<Double>> varToRandomScores = getRandomScores();
            // Set scores and p-values
            for (int i = 0; i < sampleList.size(); i++) {
                String[] rowData = new String[variables.size() * 3 + 1];
                String sample = sampleList.get(i);
                rowData[0] = sample;
                Map<Variable, Double> varToScore = FIPGMResults.getResults().getSampleToVarToScore().get(sample);
                for (int j = 0; j < variables.size(); j++) {
                    Variable variable = variables.get(j);
                    Double score = varToScore.get(variable);
                    rowData[3 * j + 1] = PlugInUtilities.formatProbability(score);
                    List<Double> randomIPAs = varToRandomScores.get(variable);
                    double pvalue = calculatePValue(score, randomIPAs);
                    rowData[3 * j + 2] = pvalue + "";
                }
                tableData.add(rowData);
            }
            // Get the total permutation number
            int totalPermutation = FIPGMResults.getResults().getRandomObservations().size();
            // Add FDR values
            for (int j = 0; j < variables.size(); j++) {
                List<Double> pvalues = new ArrayList<Double>();
                // Sort the rows based on p-values
                final int index = j;
                Collections.sort(tableData, new Comparator<String[]>() {
                    public int compare(String[] row1, String[] row2) {
                        Double pvalue1 = new Double(row1[3 * index + 2]);
                        Double pvalue2 = new Double(row2[3 * index + 2]);   
                        return pvalue1.compareTo(pvalue2);
                    }
                });
                for (int i = 0; i < tableData.size(); i++) {
                    String[] row = tableData.get(i);
                    Double pvalue = new Double(row[3 * j + 2]);
                    if (pvalue.equals(0.0d)) 
                        pvalue = 1.0d / (totalPermutation + 1); // Use the closest double value for a conservative calculation
                    pvalues.add(pvalue);
                }
                List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
                // Assign FDRs
                for (int i = 0; i < tableData.size(); i++) {
                    String[] row = tableData.get(i);
                    row[3 * j + 3] = String.format("%.3f", fdrs.get(i));
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
            int dataIndex = 1;
            columnHeaders = new String[variables.size() + 1];
            columnHeaders[0] = "Sample";
            for (int i = 0; i < variables.size(); i++) {
                String name = variables.get(i).getName();
                columnHeaders[i + dataIndex] = name;
            }
            for (int i = 0; i < sampleList.size(); i++) {
                String[] rowData = new String[columnHeaders.length];
                String sample = sampleList.get(i);
                rowData[0] = sample;
                Map<Variable, Double> varToScore = FIPGMResults.getResults().getSampleToVarToScore().get(sample);
                for (int j = 0; j < variables.size(); j++) {
                    Variable variable = variables.get(j);
                    Double score = varToScore.get(variable);
                    rowData[j + dataIndex] = PlugInUtilities.formatProbability(score);
                }
                tableData.add(rowData);
            }
        }
    }
    
}
