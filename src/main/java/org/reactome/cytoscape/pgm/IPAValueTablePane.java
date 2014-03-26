/*
 * Created on Mar 17, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.table.TableRowSorter;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.view.model.CyNetworkView;
import org.reactome.cytoscape.service.NetworkModulePanel;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pgm.InferenceResults;
import org.reactome.pgm.PGMFactorGraph;
import org.reactome.pgm.PGMVariable;

/**
 * This panel is used to list IPA values for a selected factor graph.
 * @author gwu
 *
 */
public class IPAValueTablePane extends NetworkModulePanel {
    // Cache a map from CyNode to PGMVariable for a very quick access
    private Map<CyNode, PGMVariable> nodeToVar;
    // Used to draw
    private PlotTablePanel contentPane;
    // For some reason, a single selection fire too many selection event.
    // Use this member variable to block multiple handling of the same
    // selection event.
    private List<CyNode> preSelectedNodes;
    
    /**
     * In order to show title, have to set the title in the constructor.
     */
    public IPAValueTablePane(String title) {
        super(title);
        hideOtherNodesBox.setVisible(false);
        nodeToVar = new HashMap<CyNode, PGMVariable>();
        modifyContentPane();
    }
    
    private void modifyContentPane() {
        // Add a JSplitPane for the table and a new graph pane to display graphs
        for (int i = 0; i < getComponentCount(); i++) {
            Component comp = getComponent(i);
            if (comp instanceof JScrollPane) {
                remove(comp);
                break;
            }
        }
        contentPane = new PlotTablePanel("IPA");
        contentPane.setTable(contentTable);
        add(contentPane, BorderLayout.CENTER);
    }
    
    @Override
    public void setNetworkView(CyNetworkView view) {
        super.setNetworkView(view);
        initNodeToVarMap();
    }
    
    private void initNodeToVarMap() {
        nodeToVar.clear();
        PGMFactorGraph fg = NetworkToFactorGraphMap.getMap().get(view.getModel());
        if (fg != null) {
            Map<String, PGMVariable> labelToVar = new HashMap<String, PGMVariable>();
            for (PGMVariable var : fg.getVariables()) {
                labelToVar.put(var.getLabel(), var);
            }
            // Do a simple mapping
            TableHelper tableHelper = new TableHelper();
            for (CyNode node : view.getModel().getNodeList()) {
                String label = tableHelper.getStoredNodeAttribute(view.getModel(),
                                                                  node, 
                                                                  "nodeLabel", 
                                                                  String.class);
                PGMVariable var = labelToVar.get(label);
                if (var != null)
                    nodeToVar.put(node, var);
            }
        }
    }

    @Override
    public void handleEvent(RowsSetEvent event) {
        if (!event.containsColumn(CyNetwork.SELECTED)) {
            return;
        }
        // This method may be called during a network destroy when its default node table has been
        // destroyed. The default table is used in the selection.
        if (view == null || view.getModel() == null || view.getModel().getDefaultNodeTable() == null)
            return;
        CyNetwork network = view.getModel();
        List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network,
                                                                 CyNetwork.SELECTED,
                                                                 true);
        if (selectedNodes.equals(preSelectedNodes))
            return;
        preSelectedNodes = selectedNodes;
        List<PGMVariable> variables = new ArrayList<PGMVariable>();
        if (selectedNodes != null && selectedNodes.size() > 0) {
            for (CyNode node : selectedNodes) {
                PGMVariable var = nodeToVar.get(node);
                if (var != null)
                    variables.add(var);
            }
        }
        Collections.sort(variables, new Comparator<PGMVariable>() {
            public int compare(PGMVariable var1, PGMVariable var2) {
                return var1.getLabel().compareTo(var2.getLabel());
            }
        });
        IPAValueTableModel model = (IPAValueTableModel) contentPane.getTableModel();
        model.setVariables(variables);
    }

    public void setResultsList(List<InferenceResults> resultsList) {
        IPAValueTableModel model = (IPAValueTableModel) contentPane.getTableModel();
        model.setResultsList(resultsList);
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.NetworkModulePanel#createTableModel()
     */
    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new IPAValueTableModel();
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
    protected void doTableSelection() {
        // Do nothing for the super class.
    }

    private class IPAValueTableModel extends NetworkModuleTableModel {
        private final String[] ORIGINAL_HEADERS = new String[]{"Sample", "Select Nodes to View"};
        // Cache the whole list in order for showing selected variables
        private List<InferenceResults> resultsList;
        
        public IPAValueTableModel() {
            columnHeaders = ORIGINAL_HEADERS; // Just some test data
            tableData = new ArrayList<String[]>();
        }
        
        public void setResultsList(List<InferenceResults> resultsList1) {
            this.resultsList = new ArrayList<InferenceResults>(resultsList1);
            Collections.sort(this.resultsList, new Comparator<InferenceResults>() {
                public int compare(InferenceResults results1, InferenceResults results2) {
                    String sample1 = results1.getSample();
                    String sample2 = results2.getSample();
                    // Make sure an InferenceResults having no sample assigned is the first one always.
                    if (sample1 == null)
                        return -1;
                    if (sample2 == null)
                        return 1;
                    return sample1.compareTo(sample2);
                }
            });
            
            tableData.clear();
            for (InferenceResults results : this.resultsList) {
                if (results.getSample() == null)
                    continue;
                String[] values = new String[]{results.getSample(),
                                               ""};
                tableData.add(values);
            }
            fireTableStructureChanged();
        }
        
        public void setVariables(List<PGMVariable> variables) {
            if (variables == null || variables.size() == 0) {
                columnHeaders = ORIGINAL_HEADERS;
                // Refresh the tableData
                for (String[] values : tableData) {
                    for (int i = 1; i < values.length; i++)
                        values[i] = "";
                }
                fireTableStructureChanged();
                return;
            }
            columnHeaders = new String[variables.size() + 1];
            columnHeaders[0] = "Sample";
            for (int i = 0; i < variables.size(); i++) 
                columnHeaders[i + 1] = variables.get(i).getLabel();
            // Fill up the data
            // The first one should be the prior probability
            InferenceResults prior = resultsList.get(0);
            Map<String, List<Double>> priorProbs = prior.getResults();
            tableData.clear();
            for (int i = 1; i < resultsList.size(); i++) {
                InferenceResults posterior = resultsList.get(i);
                Map<String, List<Double>> postProbs = posterior.getResults();
                String[] rowData = new String[variables.size() + 1];
                rowData[0] = posterior.getSample();
                for (int j = 0; j < variables.size(); j++) {
                    double ipa = calculateIPA(variables.get(j),
                                              priorProbs,
                                              postProbs);
                    rowData[j + 1] = PlugInUtilities.formatProbability(ipa);
                }
                tableData.add(rowData);
            }
            fireTableStructureChanged();
        }
        
        private double calculateIPA(PGMVariable variable,
                                    Map<String, List<Double>> varToPriorProbs,
                                    Map<String, List<Double>> varToPostProbs) {
            List<Double> priorProbs = varToPriorProbs.get(variable.getId());
            List<Double> postProbs = varToPostProbs.get(variable.getId());
            if (priorProbs == null || postProbs == null || priorProbs.size() < 3 || postProbs.size() < 3)
                return 0.0d;
            List<Double> ratios = new ArrayList<Double>(3);
            for (int i = 0; i < 3; i++) {
                double ratio = calculateLogRatio(priorProbs.get(i),
                                                 postProbs.get(i));
                ratios.add(ratio);
            }
            return calculateIPA(ratios);
        }
        
        private double calculateIPA(List<Double> ratios) {
            double down = ratios.get(0);
            double normal = ratios.get(1);
            double up = ratios.get(2);
            if (up > down && up > normal)
                return up;
            if (down > up && down > normal)
                return -down;
            return 0;
        }
        
        private double calculateLogRatio(double priorValue,
                                         double postValue) {
            double value = Math.log10(postValue / (1.0d - postValue) * (1.0d - priorValue) / priorValue);
            return value;
        }
        
    }
    
}
