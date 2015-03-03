/*
 * Created on Mar 17, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableRowSorter;

import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.GraphEditorSelectionModel;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.Renderable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.service.NetworkModulePanel;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Variable;
import org.reactome.pathway.factorgraph.IPACalculator;
import org.reactome.r3.util.MathUtilities;

/**
 * This panel is used to list IPA values for a selected factor graph.
 * @author gwu
 *
 */
public class IPAValueTablePane extends NetworkModulePanel {
    // Cache a map from CyNode to Variable for very quick access
    private Map<CyNode, Variable> nodeToVar;
    // Used to draw
    protected PlotTablePanel contentPane;
    // For some reason, a single selection fire too many selection event.
    // Use this member variable to block multiple handling of the same
    // selection event.
    private List<CyNode> preSelectedNodes;
    // Keep this registration so that it can be unregister if this panel is closed
    private ServiceRegistration currentViewRegistration;
    // Inference results for a selected FactorGraph
    protected FactorGraphInferenceResults fgInfResults;
    // So that it can be unregister
    private ServiceRegistration graphSelectionRegistration;
    
    /**
     * In order to show title, have to set the title in the constructor.
     */
    public IPAValueTablePane(String title) {
        super(title);
        hideOtherNodesBox.setVisible(false);
        nodeToVar = new HashMap<CyNode, Variable>();
        modifyContentPane();
        // Add the following event listener in order to support multiple network views
        SetCurrentNetworkViewListener listener = new SetCurrentNetworkViewListener() {
            
            @Override
            public void handleEvent(SetCurrentNetworkViewEvent e) {
                CyNetworkView networkView = e.getNetworkView();
                setNetworkView(networkView);
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        currentViewRegistration = context.registerService(SetCurrentNetworkViewListener.class.getName(),
                                                          listener,
                                                          null);
        GraphEditorActionListener graphListener = new GraphEditorActionListener() {
            
            @Override
            public void graphEditorAction(GraphEditorActionEvent e) {
                if (e.getID() == ActionType.SELECTION && (e.getSource() instanceof GraphEditorSelectionModel)) {
                    GraphEditorSelectionModel model = (GraphEditorSelectionModel) e.getSource();
                    handleGraphEditorSelection(model.getSelection());
                }
            }
        };
        graphSelectionRegistration = context.registerService(GraphEditorActionListener.class.getName(),
                                                             graphListener,
                                                             null);
    }
    
    @Override
    public void close() {
        if (currentViewRegistration != null) {
            // Unregister it so that this object can be GC.
            currentViewRegistration.unregister();
        }
        if (graphSelectionRegistration != null)
            graphSelectionRegistration.unregister();
        super.close();
    }

    protected void modifyContentPane() {
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        // Add a label
        JLabel ipaLabel = new JLabel("Note: IPA stands for \"Integrated Pathway Activity\" (click for details).");
        ipaLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ipaLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                String url = "http://bioinformatics.oxfordjournals.org/content/26/12/i237.full";
                PlugInUtilities.openURL(url);
            }
            
        });
        controlToolBar.add(ipaLabel);
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        addTablePlotPane();
    }

    protected void addTablePlotPane() {
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
        final boolean hidePValues = tableModel.getHideFDRs();
        String text = null;
        if (hidePValues)
            text = "Show Columns for pValues/FDRs";
        else
            text = "Hide Columns for pValues/FDRs";
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableModel.setHideFDRs(!hidePValues);
                contentPane.setFDRAxisVisible(hidePValues);
            }
        });
        popupMenu.add(item);
        popupMenu.show(contentTable, 
                       e.getX(), 
                       e.getY());
    }

    private void initNodeToVarMap() {
        nodeToVar.clear();
        if (view == null)
            return;
        FactorGraph fg = FactorGraphRegistry.getRegistry().getFactorGraph(view.getModel());
        if (fg != null) {
            Map<String, Variable> labelToVar = new HashMap<String, Variable>();
            for (Variable var : fg.getVariables()) {
                labelToVar.put(var.getId(), 
                               var); // PGMVariable's label has been saved as name.
            }
            // Do a simple mapping
            TableHelper tableHelper = new TableHelper();
            for (CyNode node : view.getModel().getNodeList()) {
                String label = tableHelper.getStoredNodeAttribute(view.getModel(),
                                                                  node, 
                                                                  "name", 
                                                                  String.class);
                Variable var = labelToVar.get(label);
                if (var != null)
                    nodeToVar.put(node, var);
            }
        }
    }
    
    public void setInferenceResults(FactorGraphInferenceResults fgResults) {
        // Get a list of samples from posteriors from all variables
        Set<String> sampleSet = null;
        if (fgResults != null) {// If a pathway view is selected, network view will be null.
            sampleSet = fgResults.getSamples();
            this.fgInfResults = fgResults;
        }
        if (sampleSet == null)
            sampleSet = new HashSet<String>(); // To avoid a null exception
        List<String> sampleList = new ArrayList<String>(sampleSet);
        IPAValueTableModel model = (IPAValueTableModel) contentPane.getTableModel();
        model.setSamples(sampleList);
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
        // Get the selected Variables
        List<Variable> selectedVars = new ArrayList<Variable>();
        if (selectedNodes != null && selectedNodes.size() > 0) {
            for (CyNode node : selectedNodes) {
                Variable var = nodeToVar.get(node);
                if (var != null) {
                    selectedVars.add(var);
                }
            }
        }
        setVariables(selectedVars);
    }
    
    protected void handleGraphEditorSelection(List<?> selection) {
        // Selection should be processed via another route
        if (view != null && view.getModel() != null && view.getModel().getDefaultNodeTable() != null)
            return; // To be handled by handleEvent() method.
        if (fgInfResults == null)
            return;
        FactorGraph fg = fgInfResults.getFactorGraph();
        List<Variable> selectedVars = new ArrayList<Variable>();
        if (selection != null && selection.size() > 0) {
            Map<String, Variable> dbIdToVar = new HashMap<String, Variable>();
            for (Variable var : fg.getVariables()) {
                String dbId = var.getProperty(ReactomeJavaConstants.DB_ID);
                if (dbId == null)
                    continue;
                dbIdToVar.put(dbId, var);
            }
            for (Object obj : selection) {
                Renderable r = (Renderable) obj;
                Variable var = dbIdToVar.get(r.getReactomeId() + "");
                if (var != null)
                    selectedVars.add(var);
            }
        }
        setVariables(selectedVars);
    }
    
    /**
     * Display results for a list of Variables.
     * @param variables
     */
    public void setVariables(List<Variable> variables) {
        List<VariableInferenceResults> varResults = new ArrayList<VariableInferenceResults>();
        if (fgInfResults != null) {
            for (Variable var : variables) {
                VariableInferenceResults varResult = fgInfResults.getVariableInferenceResults(var);
                if (varResult != null)
                    varResults.add(varResult);
            }
            Collections.sort(varResults, new Comparator<VariableInferenceResults>() {
                public int compare(VariableInferenceResults varResults1, VariableInferenceResults varResults2) {
                    return varResults1.getVariable().getName().compareTo(varResults2.getVariable().getName());
                }
            });
        }
        IPAValueTableModel model = (IPAValueTableModel) contentPane.getTableModel();
        model.setVarResults(varResults);
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
                if (fgInfResults.getSampleToType() != null && column == 1)
                    return super.getComparator(1);
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
    protected void doTableSelection(ListSelectionEvent e) {
        // Do nothing for the super class.
    }
    
    protected class IPAValueTableModel extends NetworkModuleTableModel {
        private String[] originalHeaders = new String[]{"Sample", "Select Nodes to View"};
        // Cache the list of variables for different view
        protected List<VariableInferenceResults> varResults;
        // A flag to indicate if p-values should be displayed
        // Default is hide for a simply drawing
        private boolean hideFDRs = true;
        
        public IPAValueTableModel() {
            columnHeaders = originalHeaders; // Just some test data
            tableData = new ArrayList<String[]>();
        }
        
        public void setSamples(List<String> samples) {
            Collections.sort(samples);
            tableData.clear();
            Map<String, String> sampleToType = fgInfResults.getSampleToType();
            if (sampleToType != null && sampleToType.size() > 0) {
                // Add a new type in the header
                originalHeaders = new String[]{"Sample", "Type", "Select Nodes to View"};
            }
            else {
                // Reset back to the default
                originalHeaders = new String[]{"Sample", "Select Nodes to View"};
            }
            for (String sample : samples) {
                String[] values = null;
                if (sampleToType != null && sampleToType.size() > 0) 
                    values = new String[]{sample, sampleToType.get(sample), ""};
                else
                    values = new String[]{sample, ""};
                tableData.add(values);
            }
            // Before firing table structure change, have to make sure columnHeaders are correct
            columnHeaders = originalHeaders; // Re-assign in case originalHeaders has been changed.
            fireTableStructureChanged();
        }
        
        public void setHideFDRs(boolean hidePValues) {
            this.hideFDRs = hidePValues;
            resetData();
        }
        
        public boolean getHideFDRs() {
            return this.hideFDRs;
        }
        
        public void setVarResults(List<VariableInferenceResults> varResults) {
            this.varResults = varResults;
            if (varResults != null) {
                Collections.sort(varResults, new Comparator<VariableInferenceResults>() {
                    public int compare(VariableInferenceResults varResults1,
                                       VariableInferenceResults varResults2) {
                        String name1 = varResults1.getVariable().getName();
                        String name2 = varResults2.getVariable().getName();
                        return name1.compareTo(name2);
                    }
                });
            }
            resetData();
        }
        
        protected void resetDataWithPValues(List<String> sampleList) {
            if (fgInfResults.isUsedForTwoCases())
                throw new IllegalStateException("This operation is not supported when two-cases analysis is performed!");
            columnHeaders = new String[varResults.size() * 3 + 1];
            columnHeaders[0] = "Sample";
            for (int i = 0; i < varResults.size(); i++) {
                String label = varResults.get(i).getVariable().getName();
                columnHeaders[3 * i + 1] = label;
                columnHeaders[3 * i + 2] = label + PlotTablePanel.P_VALUE_COL_NAME_AFFIX;
                columnHeaders[3 * i + 3] = label + PlotTablePanel.FDR_COL_NAME_AFFIX;
            }
            // In order to calculate p-values
            Map<Variable, List<Double>> varToRandomIPAs = generateRandomIPAs(varResults);
            for (int i = 0; i < sampleList.size(); i++) {
                String[] rowData = new String[varResults.size() * 3 + 1];
                rowData[0] = sampleList.get(i);
                for (int j = 0; j < varResults.size(); j++) {
                    VariableInferenceResults varResult = varResults.get(j);
                    Map<String, List<Double>> posteriors = varResult.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(),
                                                            postProbs);
                    rowData[3 * j + 1] = PlugInUtilities.formatProbability(ipa);
                    List<Double> randomIPAs = varToRandomIPAs.get(varResult.getVariable());
                    double pvalue = calculatePValue(ipa, randomIPAs);
                    rowData[3 * j + 2] = pvalue + "";
                }
                tableData.add(rowData);
            }
            int totalPermutation = varResults.get(0).getRandomPosteriorValues().size();
            // Add FDR values
            for (int j = 0; j < varResults.size(); j++) {
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
                // Replace p-values with FDRs
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
        
        protected void resetDataWithoutPValues(List<String> sampleList) {
            int dataIndex = 0;
            Map<String, String> sampleToType = fgInfResults.getSampleToType();
            if (sampleToType == null || sampleToType.size() == 0) {
                columnHeaders = new String[varResults.size() + 1];
                dataIndex = 1;
            }
            else {
                columnHeaders = new String[varResults.size() + 2];
                columnHeaders[1] = "Type";
                dataIndex = 2;
            }
            columnHeaders[0] = "Sample";
            for (int i = 0; i < varResults.size(); i++) {
                String name = varResults.get(i).getVariable().getName();
                columnHeaders[i + dataIndex] = name;
            }
            for (int i = 0; i < sampleList.size(); i++) {
                String[] rowData = new String[columnHeaders.length];
                rowData[0] = sampleList.get(i);
                if (sampleToType != null)
                    rowData[1] = sampleToType.get(sampleList.get(i));
                for (int j = 0; j < varResults.size(); j++) {
                    VariableInferenceResults varResult = varResults.get(j);
                    Map<String, List<Double>> posteriors = varResult.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(),
                                                            postProbs);
                    rowData[j + dataIndex] = PlugInUtilities.formatProbability(ipa);
                }
                tableData.add(rowData);
            }
        }
        
        protected void resetData() {
            if (varResults == null || varResults.size() == 0) {
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
            Set<String> samples = new HashSet<String>();
            for (VariableInferenceResults varResults : varResults) {
                samples.addAll(varResults.getPosteriorValues().keySet());
            }
            List<String> sampleList = new ArrayList<String>(samples);
            Collections.sort(sampleList);
            tableData.clear();
            
            if (hideFDRs)
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
        protected double calculatePValue(double value, List<Double> randomValues) {
            if (value == 0.0d)
                return 1.0; // Always
            if (value > 0.0d) {
                return calculatePValueRightTail(value, randomValues);
            }
            else {
                return calculatePValueLeftTail(value, randomValues);
            }
        }
        
        private double calculatePValueRightTail(double value, List<Double> randomValues) {
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
                return 1.0d;
            // Move the count one position ahead
            return (double) (randomValues.size() - index - 1) / randomValues.size();
        }
        
        private double calculatePValueLeftTail(double value, List<Double> randomValues) {
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
                return 1.0;
            return (double) index / randomValues.size();
        }
        
        private Map<Variable, List<Double>> generateRandomIPAs(List<VariableInferenceResults> varResults) {
            Map<Variable, List<Double>> varToRandomIPAs = new HashMap<Variable, List<Double>>();
            for (VariableInferenceResults varResult : varResults) {
                List<Double> ipas = new ArrayList<Double>();
                varToRandomIPAs.put(varResult.getVariable(),
                                    ipas);
                Map<String, List<Double>> randomPosts = varResult.getRandomPosteriorValues();
                for (String sample : randomPosts.keySet()) {
                    double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(),
                                                            randomPosts.get(sample));
                    ipas.add(ipa);
                }
                Collections.sort(ipas);
            }
            return varToRandomIPAs;
        }
        
    }
    
}
