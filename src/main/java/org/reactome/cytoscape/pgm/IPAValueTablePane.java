/*
 * Created on Mar 17, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
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
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.service.NetworkModulePanel;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.service.PlotTablePanel;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Variable;
import org.reactome.pathway.factorgraph.IPACalculator;
import org.reactome.r3.util.MathUtilities;

/**
 * This panel is used to list IPA values for a set of variables for each indivial sample in a 
 * table.
 * @author gwu
 *
 */
public class IPAValueTablePane extends NetworkModulePanel implements Selectable {
    // Cache a map from CyNode to Variable for very quick access
    protected Map<CyNode, Variable> nodeToVar;
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
    // A Label at the top showing a little bit note
    protected JLabel ipaLabel;
    protected PathwayHighlightControlPanel hiliteControlPane;
    
    /**
     * This constructor is used only for subclasses and expects subclasses
     * to handle title.
     */
    protected IPAValueTablePane() {
        this(null); 
    }
    
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
                if (e.getID() == ActionType.SELECTION && (e.getSource() instanceof PathwayEditor)) {
                    PathwayEditor editor = (PathwayEditor) e.getSource();
                    handleGraphEditorSelection(editor);
                }
            }
        };
        graphSelectionRegistration = context.registerService(GraphEditorActionListener.class.getName(),
                                                             graphListener,
                                                             null);
        synchronizeSampleSelection();
    }

    protected void synchronizeSampleSelection() {
        final SelectionMediator mediator = FactorGraphRegistry.getRegistry().getSampleSelectionMediator();
        List<?> selectables = mediator.getSelectables();
        if (selectables == null || !selectables.contains(this))
            mediator.addSelectable(this);
        contentPane.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                mediator.fireSelectionEvent(IPAValueTablePane.this);
            }
        });
    }
    
    /**
     * Select a set of CyNodes based on a set of attribute values.
     * @param networkView
     * @param nodeAttName
     * @param selectedAttValues
     */
    protected void selectNodes(CyNetworkView networkView,
                               String nodeAttName,
                               Set<String> selectedAttValues) {
        TableHelper tableHelper = new TableHelper();
        tableHelper.selectNodes(networkView, nodeAttName, selectedAttValues);
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
        ipaLabel = new JLabel("Note: IPA stands for \"Integrated Pathway Activity\" (click for details).");
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
    
    // The following two methods are used to handle sample selection for syncrhonization.
    @Override
    public void setSelection(List selection) {
        if (selection == null || selection.size() == 0)
            return;
        selectSamples(selection);
    }

    @Override
    public List getSelection() {
        return getSelectedSamples();
    }
    
    @Override
    public void setNetworkView(CyNetworkView view) {
        super.setNetworkView(view);
        initNodeToVarMap();
    }
    
    protected String[] getSamplesForComparison() {
        IPAValueTableModel tableModel = (IPAValueTableModel) contentPane.getTableModel();
        // Check if we can compare two samples
        String firstColName = tableModel.getColumnName(0);
        if (!firstColName.equals("Sample")) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot find selected samples for comparison!",
                                          "No Samples Selected",
                                          JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        JTable table = contentPane.getTable();
        // Only work for two samples
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows == null || selectedRows.length != 2) {
            JOptionPane.showMessageDialog(this,
                                          "Select two samples for comparison!",
                                          "Two Samples Needed",
                                          JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        String sample1 = (String) table.getValueAt(selectedRows[0], 0);
        String sample2 = (String) table.getValueAt(selectedRows[1], 0);
        return new String[] {sample1, sample2};
    }
    
    /**
     * Compare two selected samples.
     */
    protected void compareSamples() {
        String[] samples = getSamplesForComparison();
        if (samples == null)
            return;
        try {
            SampleComparisonPanel comparisonPane = (SampleComparisonPanel) PlugInUtilities.getCytoPanelComponent(SampleComparisonPanel.class,
                                                                                                                 CytoPanelName.SOUTH, 
                                                                                                                 SampleComparisonPanel.TITLE);
            comparisonPane.setHiliteControlPane(hiliteControlPane);
            comparisonPane.setData(samples[0], 
                                   samples[1],
                                   fgInfResults);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot perform sample comparison: " + e,
                                          "Error in Sample Comparison",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
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
        
        // Check if we can compare two samples
        String firstColName = tableModel.getColumnName(0);
        if (firstColName.equals("Sample")) {
            // Only work for two samples
            int count = contentPane.getTable().getSelectedRowCount();
            if (count == 2) {
                item = new JMenuItem("Compare Samples");
                item.addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        compareSamples();
                    }
                });
                popupMenu.add(item);
            }
        }
        
        popupMenu.show(contentTable, 
                       e.getX(), 
                       e.getY());
    }

    protected void initNodeToVarMap() {
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
    
    public void setFGInferenceResults(FactorGraphInferenceResults fgResults) {
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
    
    public FactorGraphInferenceResults getFGInferenceResults() {
        return this.fgInfResults;
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
    
    protected void handleGraphEditorSelection(PathwayEditor editor) {
        // Selection should be processed via another route
        if (view != null && view.getModel() != null && view.getModel().getDefaultNodeTable() != null)
            return; // To be handled by handleEvent() method.
        if (fgInfResults == null)
            return;
        FactorGraph fg = fgInfResults.getFactorGraph();
        RenderablePathway diagram = (RenderablePathway) editor.getRenderable();
        if (FactorGraphRegistry.getRegistry().getFactorGraph(diagram) != fg)
            return; // This is not for the selected pathway
        List<Variable> selectedVars = new ArrayList<Variable>();
        List<?> selection = editor.getSelection();
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
    
    /**
     * Select a list of samples in this table.
     * @param samples
     */
    public void selectSamples(List<String> samples) {
        JTable table = contentPane.getTable();
        // Clean it first
        table.clearSelection();
        // The first column should be the sample
        // For scroll
        int lastRow = -1;
        for (int i = 0; i < table.getRowCount(); i++) {
            Object value = table.getValueAt(i, 0);
            if (samples.contains(value)) {
                table.addRowSelectionInterval(i, i);
                lastRow = i;
            }
        }
        if (lastRow > -1) {
            // Scroll
            Rectangle rect = table.getCellRect(lastRow, 0, false);
            table.scrollRectToVisible(rect);
        }
    }
    
    /**
     * Get a list of selected samples.
     * @return
     */
    public List<String> getSelectedSamples() {
        List<String> samples = new ArrayList<>();
        JTable table = contentPane.getTable();
        int[] selectedRows = table.getSelectedRows();
        for (int row : selectedRows) {
            Object value = table.getValueAt(row, 0);
            samples.add(value.toString());
        }
        return samples;
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
        TableRowSorter<NetworkModuleTableModel> sorter = new TableRowSorter<NetworkModuleTableModel>(model);
        return sorter;
    }
    
    @Override
    protected void doTableSelection(ListSelectionEvent e) {
        // Do nothing for the super class.
    }
    
    public PathwayHighlightControlPanel getHiliteControlPane() {
        return hiliteControlPane;
    }

    public void setHiliteControlPane(PathwayHighlightControlPanel hiliteControlPane) {
        this.hiliteControlPane = hiliteControlPane;
    }

    protected class IPAValueTableModel extends NetworkModuleTableModel {
        protected String[] originalHeaders = new String[]{"Sample", "Select Nodes to View"};
        // Cache the list of variables for different view
        protected List<VariableInferenceResults> varResults;
        // A flag to indicate if p-values should be displayed
        // Default is showing assuming only one node is selected
        protected boolean hideFDRs = false;
        
        public IPAValueTableModel() {
            columnHeaders = originalHeaders; // Just some test data
            tableData = new ArrayList<Object[]>();
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            // The first column is the sample name
            if (columnIndex == 0)
                return String.class;
            if (columnIndex == 1) {
                Map<String, String> sampleToType = fgInfResults.getSampleToType();
                if (sampleToType == null || sampleToType.size() == 0)
                    return Double.class;
                else
                    return String.class;
            }
            return Double.class;
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
                Object[] values = null;
                if (sampleToType != null && sampleToType.size() > 0) 
                    values = new Object[]{sample, 
                                          sampleToType.get(sample), 
                                          null};
                else
                    values = new Object[]{sample, 
                                          null};
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
            Map<String, String> sampleToType = fgInfResults.getSampleToType();
            final int dataIndex = (sampleToType != null && sampleToType.size() > 0) ? 2 : 1;
            columnHeaders = new String[varResults.size() * 3 + dataIndex];
            columnHeaders[0] = "Sample";
            if (dataIndex == 2)
                columnHeaders[1] = "Type";
            for (int i = 0; i < varResults.size(); i++) {
                String label = varResults.get(i).getVariable().getName();
                columnHeaders[3 * i + dataIndex] = label;
                columnHeaders[3 * i + dataIndex + 1] = label + PlotTablePanel.P_VALUE_COL_NAME_AFFIX;
                columnHeaders[3 * i + dataIndex + 2] = label + PlotTablePanel.FDR_COL_NAME_AFFIX;
            }
            // In order to calculate p-values
            Map<Variable, List<Double>> varToRandomIPAs = fgInfResults.generateRandomIPAs(varResults);
            for (int i = 0; i < sampleList.size(); i++) {
                Object[] rowData = new Object[varResults.size() * 3 + dataIndex];
                rowData[0] = sampleList.get(i);
                if (dataIndex == 2) {
                    String type = sampleToType.get(rowData[0]);
                    rowData[1] = (type == null) ? "" : type;
                }
                for (int j = 0; j < varResults.size(); j++) {
                    VariableInferenceResults varResult = varResults.get(j);
                    Map<String, List<Double>> posteriors = varResult.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(),
                                                            postProbs);
                    rowData[3 * j + dataIndex] = ipa;
                    List<Double> randomIPAs = varToRandomIPAs.get(varResult.getVariable());
                    double pvalue = PlugInUtilities.calculateIPAPValue(ipa, randomIPAs);
                    rowData[3 * j + dataIndex + 1] = pvalue;
                }
                tableData.add(rowData);
            }
            int totalPermutation = varResults.get(0).getRandomPosteriorValues().size();
            // Add FDR values
            for (int j = 0; j < varResults.size(); j++) {
                List<Double> pvalues = new ArrayList<Double>();
                // Sort the rows based on p-values
                final int index = j;
                Collections.sort(tableData, new Comparator<Object[]>() {
                    public int compare(Object[] row1, Object[] row2) {
                        Double pvalue1 = (Double) row1[3 * index + 1 + dataIndex];
                        Double pvalue2 = (Double) row2[3 * index + 1 + dataIndex];   
                        return pvalue1.compareTo(pvalue2);
                    }
                });
                for (int i = 0; i < tableData.size(); i++) {
                    Object[] row = tableData.get(i);
                    Double pvalue = (Double) row[3 * j + 1 + dataIndex];
                    if (pvalue.equals(0.0d)) 
                        pvalue = 1.0d / (totalPermutation + 1); // Use the closest double value for a conservative calculation
                    pvalues.add(pvalue);
                }
                List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
                // Replace p-values with FDRs
                for (int i = 0; i < tableData.size(); i++) {
                    Object[] row = tableData.get(i);
                    row[3 * j + dataIndex + 2] = fdrs.get(i);
//                    row[3 * j + dataIndex + 2] = String.format("%.3f", fdrs.get(i));
                }
            }
            // Need to sort the table back as the original
            Collections.sort(tableData, new Comparator<Object[]>() {
                public int compare(Object[] row1, Object[] row2) {
                    return row1[0].toString().compareTo(row2[0].toString());
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
                Object[] rowData = new Object[columnHeaders.length];
                rowData[0] = sampleList.get(i);
                if (sampleToType != null && sampleToType.size() > 0)
                    rowData[1] = sampleToType.get(sampleList.get(i));
                for (int j = 0; j < varResults.size(); j++) {
                    VariableInferenceResults varResult = varResults.get(j);
                    Map<String, List<Double>> posteriors = varResult.getPosteriorValues();
                    List<Double> postProbs = posteriors.get(rowData[0]);
                    double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(),
                                                            postProbs);
                    rowData[j + dataIndex] = ipa;
                }
                tableData.add(rowData);
            }
        }
        
        protected void resetData() {
            if (varResults == null || varResults.size() == 0) {
                columnHeaders = originalHeaders;
                // Refresh the tableData
                for (Object[] values : tableData) {
                    for (int i = 1; i < values.length; i++)
                        values[i] = null;
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
        
    }
    
}
