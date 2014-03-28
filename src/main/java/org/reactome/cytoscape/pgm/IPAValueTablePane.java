/*
 * Created on Mar 17, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
        contentPane = new PlotTablePanel("IPA", true);
        contentPane.setTable(contentTable);
        add(contentPane, BorderLayout.CENTER);
    }
    
    @Override
    public void setNetworkView(CyNetworkView view) {
        super.setNetworkView(view);
        initNodeToVarMap();
    }
    
    
    @Override
    protected void doContentTablePopup(MouseEvent e) {
        JPopupMenu popupMenu = createExportAnnotationPopup();
        final IPAValueTableModel tableModel = (IPAValueTableModel) contentPane.getTableModel();
        final boolean hidePValues = tableModel.hidePValues;
        String text = null;
        if (hidePValues)
            text = "Show Column(s) for PValues";
        else
            text = "Hide Column(s) for PValues";
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableModel.setHidePValues(!hidePValues);
                contentPane.setPValueAxisVisible(hidePValues);
            }
        });
        popupMenu.add(item);
        popupMenu.show(contentTable, 
                       e.getX(), 
                       e.getY());
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

    public void setSamples(List<String> samples) {
        IPAValueTableModel model = (IPAValueTableModel) contentPane.getTableModel();
        model.setSamples(samples);
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
        // Cache the list of variables for different view
        private List<PGMVariable> variables;
        // A flag to indicate if p-values should be displayed
        private boolean hidePValues;
        
        public IPAValueTableModel() {
            columnHeaders = ORIGINAL_HEADERS; // Just some test data
            tableData = new ArrayList<String[]>();
        }
        
        public void setSamples(List<String> samples) {
            Collections.sort(samples);
            tableData.clear();
            for (String sample : samples) {
                String[] values = new String[]{sample,
                                               ""};
                tableData.add(values);
            }
            fireTableStructureChanged();
        }
        
        public void setHidePValues(boolean hidePValues) {
            this.hidePValues = hidePValues;
            resetData();
        }
        
        public void setVariables(List<PGMVariable> variables) {
            this.variables = variables;
            if (variables != null) {
                Collections.sort(variables, new Comparator<PGMVariable>() {
                    public int compare(PGMVariable var1, PGMVariable var2) {
                        return var1.getLabel().compareTo(var2.getLabel());
                    }
                });
            }
            resetData();
        }
        
        private void resetDataWithPValues(List<String> sampleList) {
            columnHeaders = new String[variables.size() * 2 + 1];
            columnHeaders[0] = "Sample";
            for (int i = 0; i < variables.size(); i++) {
                String label = variables.get(i).getLabel();
                columnHeaders[2 * i + 1] = label;
                columnHeaders[2 * i + 2] = label + "(pvalue)";
            }
            // In order to caclualte p-values
            Map<PGMVariable, List<Double>> varToRandomIPAs = generateRandomIPAs(variables);
            for (int i = 0; i < sampleList.size(); i++) {
                String[] rowData = new String[variables.size() * 2 + 1];
                rowData[0] = sampleList.get(i);
                for (int j = 0; j < variables.size(); j++) {
                    PGMVariable var = variables.get(j);
                    Map<String, List<Double>> posteriors = var.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = calculateIPA(var.getValues(),
                                              postProbs);
                    rowData[2 * j + 1] = PlugInUtilities.formatProbability(ipa);
                    List<Double> randomIPAs = varToRandomIPAs.get(var);
                    String pvalue = calculatePValue(ipa, randomIPAs);
                    rowData[2 * j + 2] = pvalue;
                }
                tableData.add(rowData);
            }
        }
        
        private void resetDataWithoutPValues(List<String> sampleList) {
            columnHeaders = new String[variables.size() + 1];
            columnHeaders[0] = "Sample";
            for (int i = 0; i < variables.size(); i++) {
                String label = variables.get(i).getLabel();
                columnHeaders[i + 1] = label;
            }
            for (int i = 0; i < sampleList.size(); i++) {
                String[] rowData = new String[variables.size() + 1];
                rowData[0] = sampleList.get(i);
                for (int j = 0; j < variables.size(); j++) {
                    PGMVariable var = variables.get(j);
                    Map<String, List<Double>> posteriors = var.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = calculateIPA(var.getValues(),
                                              postProbs);
                    rowData[j + 1] = PlugInUtilities.formatProbability(ipa);
                }
                tableData.add(rowData);
            }
        }
        
        private void resetData() {
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
            // Get a list of all samples
            Set<String> samples = new HashSet<String>();
            for (PGMVariable var : variables) {
                samples.addAll(var.getPosteriorValues().keySet());
            }
            List<String> sampleList = new ArrayList<String>(samples);
            Collections.sort(sampleList);
            tableData.clear();
            
            if (hidePValues)
                resetDataWithoutPValues(sampleList);
            else
                resetDataWithPValues(sampleList);
            
            fireTableStructureChanged();
        }
        
        /**
         * Split the random values into two parts: one for positive and another for negative.
         * P-values should be calculated based on these two parts. In other words, this should
         * be a two-tailed test.
         * @param value
         * @param randomValues
         * @return
         */
        private String calculatePValue(double value, List<Double> randomValues) {
            if (value == 0.0d)
                return "1.0"; // Always
            if (value > 0.0d) {
                return calculatePValueRightTail(value, randomValues);
            }
            else {
                return calculatePValueLeftTail(value, randomValues);
            }
        }
        
        private String calculatePValueRightTail(double value, List<Double> randomValues) {
            // Values in copy should be sorted already.
            int index = -1;
            for (int i = randomValues.size() - 1; i >= 0; i--) {
                if (randomValues.get(i) < value) {
                    index = i;
                    break;
                }
            }
            // In order to plot and sort, use 0.0 for "<"
//            if (index == randomValues.size() - 1)
//                return "<" + (1.0d / randomValues.size());
            if (index == -1)
                return "1.0";
            // Move the count one position ahead
            return (double) (randomValues.size() - index - 1) / randomValues.size() + "";
        }
        
        private String calculatePValueLeftTail(double value, List<Double> randomValues) {
            // Values in copy should be sorted already.
            int index = -1;
            for (int i = 0; i < randomValues.size(); i++) {
                if (randomValues.get(i) > value) {
                    index = i;
                    break;
                }
            }
            // In order to plot and sort, use 0.0 for "<"
//            if (index == 0)
//                return "<" + (1.0d / randomValues.size());
            if (index == -1)
                return "1.0";
            return (double) index / randomValues.size() + "";
        }
        
        private Map<PGMVariable, List<Double>> generateRandomIPAs(List<PGMVariable> variables) {
            Map<PGMVariable, List<Double>> varToRandomIPAs = new HashMap<PGMVariable, List<Double>>();
            for (PGMVariable var : variables) {
                List<Double> ipas = new ArrayList<Double>();
                varToRandomIPAs.put(var, ipas);
                Map<String, List<Double>> randomPosts = var.getRandomPosteriorValues();
                for (String sample : randomPosts.keySet()) {
                    double ipa = calculateIPA(var.getValues(),
                                              randomPosts.get(sample));
                    ipas.add(ipa);
                }
                Collections.sort(ipas);
            }
            return varToRandomIPAs;
        }
        
        private double calculateIPA(List<Double> priorProbs,
                                    List<Double> postProbs) {
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
