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
    public static final String TITLE = "Impact Sample Values";
    
    /**
     * @param title
     */
    public ImpactSampleValueTablePane() {
    }
    
    @Override
    protected void addTablePlotPane() {
        super.addTablePlotPane();
        adjustGUIs();
    }
    
    @Override
    public String getTitle() {
        return TITLE;
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
        TableRowSorter<NetworkModuleTableModel> sorter = new TableRowSorter<NetworkModuleTableModel>(model);
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
        Map<String, Variable> nameToVar = results.getNameToVariable();
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
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            // The first column is the sample name
            if (columnIndex == 0)
                return String.class;
            return Double.class;
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
                tableData.clear();
                // Show samples
                List<String> sampleList = new ArrayList<>();
                if (FIPGMResults.getResults() != null && FIPGMResults.getResults().getSampleToVarToScore() != null)
                    sampleList.addAll(FIPGMResults.getResults().getSampleToVarToScore().keySet());
                Collections.sort(sampleList);
                for (int i = 0; i < sampleList.size(); i++) {
                    String sample = sampleList.get(i);
                    Object[] row = new Object[columnHeaders.length];
                    row[0] = sample;
                    for (int j = 1; j < row.length; j++)
                        row[j] = null;
                    tableData.add(row);
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
            Map<Variable, List<Double>> varToRandomScores = FIPGMResults.getResults().getRandomScores(variables);
            // Set scores and p-values
            for (int i = 0; i < sampleList.size(); i++) {
                Object[] rowData = new Object[variables.size() * 3 + 1];
                String sample = sampleList.get(i);
                rowData[0] = sample;
                Map<Variable, Double> varToScore = FIPGMResults.getResults().getSampleToVarToScore().get(sample);
                for (int j = 0; j < variables.size(); j++) {
                    Variable variable = variables.get(j);
                    Double score = varToScore.get(variable);
                    rowData[3 * j + 1] = score;
                    List<Double> randomScores = varToRandomScores.get(variable);
                    double pvalue = PlugInUtilities.calculateNominalPValue(score,
                                                                           randomScores,
                                                                           "right");
                    rowData[3 * j + 2] = pvalue;
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
                Collections.sort(tableData, new Comparator<Object[]>() {
                    public int compare(Object[] row1, Object[] row2) {
                        Double pvalue1 = new Double(row1[3 * index + 2].toString());
                        Double pvalue2 = new Double(row2[3 * index + 2].toString());   
                        return pvalue1.compareTo(pvalue2);
                    }
                });
                for (int i = 0; i < tableData.size(); i++) {
                    Object[] row = tableData.get(i);
                    Double pvalue = new Double(row[3 * j + 2].toString());
                    if (pvalue.equals(0.0d)) 
                        pvalue = 1.0d / (totalPermutation + 1); // Use the closest double value for a conservative calculation
                    pvalues.add(pvalue);
                }
                List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
                // Assign FDRs
                for (int i = 0; i < tableData.size(); i++) {
                    Object[] row = tableData.get(i);
                    row[3 * j + 3] = fdrs.get(i);
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
            int dataIndex = 1;
            columnHeaders = new String[variables.size() + 1];
            columnHeaders[0] = "Sample";
            for (int i = 0; i < variables.size(); i++) {
                String name = variables.get(i).getName();
                columnHeaders[i + dataIndex] = name;
            }
            for (int i = 0; i < sampleList.size(); i++) {
                Object[] rowData = new Object[columnHeaders.length];
                String sample = sampleList.get(i);
                rowData[0] = sample;
                Map<Variable, Double> varToScore = FIPGMResults.getResults().getSampleToVarToScore().get(sample);
                for (int j = 0; j < variables.size(); j++) {
                    Variable variable = variables.get(j);
                    Double score = varToScore.get(variable);
                    rowData[j + dataIndex] = score;
                }
                tableData.add(rowData);
            }
        }
    }
    
}
