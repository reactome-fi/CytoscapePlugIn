/*
 * Created on Jan 27, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.math.MathException;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.gk.render.Renderable;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Variable;
import org.reactome.pathway.factorgraph.IPACalculator;
import org.reactome.r3.util.InteractionUtilities;
import org.reactome.r3.util.MathUtilities;

/**
 * @author gwu
 *
 */
public class IPAPathwaySummaryPane extends IPAValueTablePane {
    
    private DefaultBoxAndWhiskerCategoryDataset dataset;
    private CategoryPlot plot;
    private ChartPanel chartPanel;
    private JTable tTestResultTable;
    // For node selection sync between table and network view
    private boolean isFromTable;
    private boolean isFromNetwork;
    // For combined p-value
    private JLabel combinedPValueLabel;
    // Cache calculated IPA values for sorting purpose
    private Map<String, List<Double>> realSampleToIPAs;
    private Map<String, List<Double>> randomSampleToIPAs;
    // Used for selecting a node
    private CyNetworkView networkView;
    private ServiceRegistration networkSelectionRegistration;
    // For showing a summarized result
    private JLabel outputResultLabel;
    
    /**
     * @param title
     */
    public IPAPathwaySummaryPane(String title) {
        super(title);
    }        
    
    @Override
    public void setNetworkView(CyNetworkView networkView) {
        this.networkView = networkView;
    }
    
    @Override
    protected void modifyContentPane() {
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        // Add a label
        outputResultLabel = new JLabel("Total checked outputs:");
        controlToolBar.add(outputResultLabel);
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        
        addContentPane();
        
        installListeners();
    }

    private void addContentPane() {
        JPanel boxPlotPane = createBoxPlotPane();
        JPanel ttestResultPane = createTTestResultTable();
        JPanel combinedPValuePane = createCombinedPValuePane();
        
        JPanel lowerPane = new JPanel();
        lowerPane.setLayout(new BorderLayout());
        lowerPane.add(ttestResultPane, BorderLayout.CENTER);
        lowerPane.add(combinedPValuePane, BorderLayout.SOUTH);
        
        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                        boxPlotPane,
                                        lowerPane);
        jsp.setDividerLocation(0.50d);
        jsp.setDividerLocation(150); // Give the plot 150 px initially
        
        // Add a JSplitPane for the table and a new graph pane to display graphs
        for (int i = 0; i < getComponentCount(); i++) {
            Component comp = getComponent(i);
            if (comp instanceof JScrollPane) {
                remove(comp);
                break;
            }
        }
        add(jsp, BorderLayout.CENTER);
        
        realSampleToIPAs = new HashMap<String, List<Double>>();
        randomSampleToIPAs = new HashMap<String, List<Double>>();
    }
    
    private void installListeners() {
        tTestResultTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
            
            @Override
            public void sorterChanged(RowSorterEvent e) {
                rePlotData();
            }
        });
        // Use this simple method to make sure marker is synchronized between two views.
        TableAndPlotActionSynchronizer tpSyncHelper = new TableAndPlotActionSynchronizer(tTestResultTable, chartPanel);
        
        tTestResultTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                doTableSelection(e);
            }
        });
        
        // Synchronize selection from network to pathway overview
        RowsSetListener selectionListener = new RowsSetListener() {
            @Override
            public void handleEvent(RowsSetEvent event) {
                if (!event.containsColumn(CyNetwork.SELECTED) || 
                        networkView == null ||
                        networkView.getModel() == null || 
                        networkView.getModel().getDefaultEdgeTable() == null ||
                        networkView.getModel().getDefaultNodeTable() == null) {
                    return;
                }
                List<CyNode> nodes = CyTableUtil.getNodesInState(networkView.getModel(),
                                                                 CyNetwork.SELECTED,
                                                                 true);
                handleNetworkSelection(nodes);
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        networkSelectionRegistration = context.registerService(RowsSetListener.class.getName(),
                                                               selectionListener, 
                                                               null);
    }
    
    @Override
    public void close() {
        // Unregistered registered service for easy GC.
        if (networkSelectionRegistration != null) {
            networkSelectionRegistration.unregister();
            networkSelectionRegistration = null; 
        }
        super.close();
    }

    private void handleNetworkSelection(List<CyNode> selectedNodes) {
        if (isFromTable)
            return;
        isFromNetwork = true;
        Set<String> rowKeys = new HashSet<String>();
        TableHelper helper = new TableHelper();
        for (CyNode node : selectedNodes) {
            String label = helper.getStoredNodeAttribute(networkView.getModel(),
                                                         node,
                                                         "SourceIds",
                                                         String.class);
            rowKeys.add(label);
        }
        selectRows(rowKeys);
        isFromNetwork = false;
    }

    private void selectRows(Set<String> rowKeys) {
        tTestResultTable.clearSelection();
        if (rowKeys.size() > 0) {
            // Find the row index in the table model
            TableModel model = tTestResultTable.getModel();
            int selected = -1;
            for (int i = 0; i < model.getRowCount(); i++) {
                String tmp = (String) model.getValueAt(i, 0);
                if (rowKeys.contains(tmp)) {
                    int viewIndex = tTestResultTable.convertRowIndexToView(i);
                    tTestResultTable.getSelectionModel().addSelectionInterval(viewIndex, viewIndex);
                    if (selected == -1)
                        selected = viewIndex;
                }
            }
            if (selected > -1) {
                Rectangle rect = tTestResultTable.getCellRect(selected, 0, false);
                tTestResultTable.scrollRectToVisible(rect);
            }
        }
    }
    
    @Override
    protected void handleGraphEditorSelection(List<?> selection) {
        if (isFromTable)
            return;
        isFromNetwork = true; // Just borrow this flag
        Set<String> rowKeys = new HashSet<String>();
        for (Object obj : selection) {
            Renderable r = (Renderable) obj;
            if (r.getReactomeId() != null)
                rowKeys.add(r.getReactomeId() + "");
        }
        selectRows(rowKeys);
        isFromNetwork = false;
    }

    @Override
    protected void doTableSelection(ListSelectionEvent e) {
        if (isFromNetwork)
            return;
        isFromTable = true;
        // Get the selected variable labels
        Set<String> sourceIdsForSelection = new HashSet<String>();
        TTestTableModel model = (TTestTableModel) tTestResultTable.getModel();
        if (tTestResultTable.getSelectedRowCount() > 0) {
            for (int row : tTestResultTable.getSelectedRows()) {
                int modelIndex = tTestResultTable.convertRowIndexToModel(row);
                String sourceId = (String) model.getValueAt(modelIndex, 0);
                sourceIdsForSelection.add(sourceId);
            }
        }
        if (networkView != null) {
            // Clear all selection
            TableHelper tableHelper = new TableHelper();
            CyNetwork network = networkView.getModel();
            int totalSelected = 0;
            for (View<CyNode> nodeView : networkView.getNodeViews()) {
                CyNode node = nodeView.getModel();
                Long nodeSUID = node.getSUID();
                String nodeLabel = tableHelper.getStoredNodeAttribute(network,
                                                                      node, 
                                                                      "SourceIds", 
                                                                      String.class);
                boolean isSelected = sourceIdsForSelection.contains(nodeLabel);
                if (isSelected)
                    totalSelected ++;
                tableHelper.setNodeSelected(network, 
                                            node,
                                            isSelected);
            }
            PlugInUtilities.zoomToSelected(networkView,
                                           totalSelected);
            networkView.updateView();
        }
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        try {
            ServiceReference[] references = context.getServiceReferences(PropertyChangeListener.class.getName(), 
                                                                         "(target=" + getClass().getSimpleName() + ")");
            if (references != null) {
                PropertyChangeEvent event = new PropertyChangeEvent(tTestResultTable,
                                                                    "tableSelection",
                                                                    tTestResultTable.getSelectedRows(),
                                                                    null);
                for (ServiceReference reference : references) {
                    PropertyChangeListener l = (PropertyChangeListener) context.getService(reference);
                    l.propertyChange(event);
                    context.ungetService(reference);
                }
            }
        }
        catch (InvalidSyntaxException e1) {
            e1.printStackTrace();
        }
        isFromTable = false;
    }
    
    private void rePlotData() {
        dataset.clear();
        TTestTableModel tableModel = (TTestTableModel) tTestResultTable.getModel();
        // In two cases, these values will be set dynamically. So we have to
        // get them in the table.
        String realLabel = tableModel.getColumnName(2);
        String randomLabel = tableModel.getColumnName(3);
        for (int i = 0; i < tTestResultTable.getRowCount(); i++) {
            int index = tTestResultTable.convertRowIndexToModel(i);
            String varLabel = (String) tableModel.getValueAt(index, 0);
            List<Double> realIPAs = realSampleToIPAs.get(varLabel);
            dataset.add(realIPAs, realLabel, varLabel);
            List<Double> randomIPAs = randomSampleToIPAs.get(varLabel);
            dataset.add(randomIPAs, randomLabel, varLabel);
        }
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
    }
    
    private JPanel createTTestResultTable() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        JLabel label = new JLabel("Mean Value Comparison Results");
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.BOLD));
        // Want to make sure label is in the middle, so
        // add a panel
        JPanel labelPane = new JPanel();
        labelPane.add(label);
        panel.add(labelPane, BorderLayout.NORTH);
        
        TTestTableModel model = new TTestTableModel();
        tTestResultTable = new JTable(model);
        // Need to add a row sorter
        TableRowSorter<TTestTableModel> sorter = new TableRowSorter<TTestTableModel>(model) {

            @Override
            public Comparator<?> getComparator(int column) {
                if (column == 1) // Just use the String comparator for name
                    return super.getComparator(0);
                Comparator<String> rtn = new Comparator<String>() {
                    public int compare(String var1, String var2) {
                        Double value1 = new Double(var1);
                        Double value2 = new Double(var2);
                        return value1.compareTo(value2);
                    }
                };
                return rtn;
            }
        };
        tTestResultTable.setRowSorter(sorter);
        
        panel.add(new JScrollPane(tTestResultTable), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCombinedPValuePane() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        JLabel titleLabel = new JLabel("Combined p-value using an extension of Fisher's method (click to see the reference): ");
        titleLabel.setToolTipText("Click to view the reference");
        titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combinedPValueLabel = new JLabel("1.0");
        titleLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                String url = "http://www.sciencedirect.com/science/article/pii/S0167715202003103";
                PlugInUtilities.openURL(url);
            }
            
        });
        panel.add(titleLabel);
        panel.add(combinedPValueLabel);
        
        return panel;
    }
    
    private JPanel createBoxPlotPane() {
        dataset = new DefaultBoxAndWhiskerCategoryDataset();
        // Want to control data update by this object self to avoid
        // conflict exception.
        dataset.setNotify(false);
        CategoryAxis xAxis = new CategoryAxis("Variable");
        NumberAxis yAxis = new NumberAxis("IPA");
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        // We want to show the variable label
        BoxAndWhiskerToolTipGenerator tooltipGenerator = new BoxAndWhiskerToolTipGenerator() {

            @Override
            public String generateToolTip(CategoryDataset dataset,
                                          int row,
                                          int column) {
                Object variable = dataset.getColumnKey(column);
                String rtn = super.generateToolTip(dataset, row, column);
                return "Variable: " + variable + " " + rtn;
            }
        };
        renderer.setBaseToolTipGenerator(tooltipGenerator);
        plot = new CategoryPlot(dataset,
                                xAxis, 
                                yAxis,
                                renderer);
        Font font = getFont();
        font = font.deriveFont(Font.BOLD);
        JFreeChart chart = new JFreeChart("Boxplot for Integrated Pathway Activities (IPAs) of Outputs", 
                                          font,
                                          plot, 
                                          true);
        chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEtchedBorder());
        return chartPanel;
    }
    
    private void setOverview(List<VariableInferenceResults> varResults,
                             Set<Variable> outputVars,
                             Map<String, String> sampleToType) {
        StringBuilder builder = new StringBuilder();
        int size = 0;
        if (varResults != null)
            size = varResults.size();
        builder.append("Total checked outputs: " + varResults.size());
        if (size == 0) {
            outputResultLabel.setText(builder.toString());
            return; 
        }
        double pvalueCutoff = 0.01d;
        double ipaDiffCutoff = 0.30d; // 2 fold difference
        try {
            generateOverview(varResults, 
                             outputVars,
                             pvalueCutoff,
                             ipaDiffCutoff, 
                             sampleToType,
                             builder);
            outputResultLabel.setText(builder.toString());
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in generating details: " + e,
                                          "Error in Generating Details",
                                          JOptionPane.ERROR_MESSAGE);
            outputResultLabel.setText(builder.toString());
        }
    }
    
    private void generateOverview(List<VariableInferenceResults> varResults,
                                  Set<Variable> outputVars,
                                  double pvalueCutoff,
                                  double ipaDiffCutoff,
                                  Map<String, String> sampleToType,
                                  StringBuilder builder) throws Exception {
        // Do a test
        int negPerturbedOutputs = 0;
        int posPerturbedOutputs = 0;
        List<Double> realIPAs = new ArrayList<Double>();
        List<Double> randomIPAs = new ArrayList<Double>();
        List<List<Double>> allRealIPAs = new ArrayList<List<Double>>();
        MannWhitneyUTest uTest = new MannWhitneyUTest();
        List<Double> pvalues = new ArrayList<Double>();
        boolean hasData = false;
        Map<String, Set<String>> typeToSamples = null;
        if (sampleToType != null)
            typeToSamples = getTypeToSamples(sampleToType);
        for (VariableInferenceResults varResult : varResults) {
            if (!outputVars.contains(varResult.getVariable()))
                continue; // Make sure it counts for outputs only
            realIPAs.clear();
            randomIPAs.clear();
            if (typeToSamples == null)
                calculateIPAForOverview(realIPAs, randomIPAs, varResult);
            else
                calculateTwoCasesIPAForOverview(realIPAs,
                                                randomIPAs,
                                                varResult,
                                                typeToSamples);
            if (realIPAs.size() == 0 || randomIPAs.size() == 0)
                continue;
            hasData = true;
            double realMean = MathUtilities.calculateMean(realIPAs);
            double randomMean = MathUtilities.calculateMean(randomIPAs);
            double meanDiff = realMean - randomMean;
            if (Math.abs(meanDiff) < ipaDiffCutoff)
                continue;
            double pvalue = uTest.mannWhitneyUTest(PlugInUtilities.convertDoubleListToArray(realIPAs),
                                                   PlugInUtilities.convertDoubleListToArray(randomIPAs));
            if (pvalue < pvalueCutoff) {
                if (meanDiff < 0.0d)
                    negPerturbedOutputs ++;
                else if (meanDiff > 0.0d)
                    posPerturbedOutputs ++;
            }
        }
        if (!hasData) {
            builder.append(" (No inference results are available.) ");
            return;
        }
        builder.append(" (").append(negPerturbedOutputs).append(" down perturbed, ");
        builder.append(posPerturbedOutputs).append(" up perturbed, based on pvalue < ");
        builder.append(pvalueCutoff).append(" and IPA mean diff > ");
        builder.append(ipaDiffCutoff).append(".)  ");
        return;
    }

    private void calculateIPAForOverview(List<Double> realIPAs,
                                         List<Double> randomIPAs,
                                         VariableInferenceResults varResult) {
        Map<String, List<Double>> sampleToRealProbs = varResult.getPosteriorValues();
        for (List<Double> probs : sampleToRealProbs.values()) {
            double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), probs);
            realIPAs.add(ipa);
        }
        Map<String, List<Double>> sampleToRandomProbs = varResult.getRandomPosteriorValues();
        for (List<Double> probs : sampleToRandomProbs.values()) {
            double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), probs);
            randomIPAs.add(ipa);
        }
    }
    
    private void calculateTwoCasesIPAForOverview(List<Double> realIPAs,
                                                 List<Double> randomIPAs,
                                                 VariableInferenceResults varResult,
                                                 Map<String, Set<String>> typeToSamples) {
        List<String> types = new ArrayList<String>(typeToSamples.keySet());
        // The first type is used as real. No order is needed
        Map<String, List<Double>> sampleToRealProbs = grepVarResultsForSamples(varResult, typeToSamples.get(types.get(0)));
        for (List<Double> probs : sampleToRealProbs.values()) {
            double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), probs);
            realIPAs.add(ipa);
        }
        // The second is used as random.
        Map<String, List<Double>> sampleToRandomProbs = grepVarResultsForSamples(varResult, typeToSamples.get(types.get(1)));
        for (List<Double> probs : sampleToRandomProbs.values()) {
            double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(), probs);
            randomIPAs.add(ipa);
        }
    }
    
    /**
     * Get the map from Reactome ids to IPA diffs.
     * @return
     */
    public Map<String, Double> getReactomeIdToIPADiff() {
        TTestTableModel tableModel = (TTestTableModel) tTestResultTable.getModel();
        Map<String, Double> idToDiff = new HashMap<String, Double>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String id = (String) tableModel.getValueAt(i, 0); // The first value
            String diff = (String) tableModel.getValueAt(i, 4);
            idToDiff.put(id, new Double(diff));
        }
        return idToDiff;
    }
    
    public void setVariableResults(List<VariableInferenceResults> varResults,
                                   Set<Variable> outputVars,
                                   Map<String, String> sampleToType) throws MathException {
        // Do a sort
        List<VariableInferenceResults> sortedResults = new ArrayList<VariableInferenceResults>(varResults);
        Collections.sort(sortedResults, new Comparator<VariableInferenceResults>() {
            public int compare(VariableInferenceResults varResults1,
                               VariableInferenceResults varResults2) {
                return varResults1.getVariable().getName().compareTo(varResults2.getVariable().getName());
            }
        });
        dataset.clear();
        realSampleToIPAs.clear();
        randomSampleToIPAs.clear();
        TTestTableModel tableModel = (TTestTableModel) tTestResultTable.getModel();
        tableModel.reset(); // Reset the original data if any.
        List<Double> pvalues = new ArrayList<Double>();
        if (sampleToType == null) {
            parseResults(sortedResults, 
                         tableModel,
                         pvalues);
        }
        else {
            parseTwoCasesResults(sortedResults,
                                 tableModel,
                                 pvalues,
                                 sampleToType);
        }
        // The following code is used to control performance:
        // 16 is arbitrary
        CategoryAxis axis = plot.getDomainAxis();
        if (varResults.size() > PlugInUtilities.PLOT_CATEGORY_AXIX_LABEL_CUT_OFF) {
            axis.setTickLabelsVisible(false);
            axis.setTickMarksVisible(false);
        }
        else {
            axis.setTickLabelsVisible(true);
            axis.setTickMarksVisible(true);
        }
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
        // Make a copy to avoid modifying by the called method
        tableModel.calculateFDRs(new ArrayList<Double>(pvalues));
        tableModel.fireTableStructureChanged();
        setCombinedPValue(pvalues);
        // Set overview
        setOverview(varResults,
                    outputVars,
                    sampleToType);
    }
    
    private void parseTwoCasesResults(List<VariableInferenceResults> sortedResults,
                                      TTestTableModel tableModel,
                                      List<Double> pvalues,
                                      Map<String, String> sampleToType) throws MathException {
        // Get two types
        Set<String> types = new HashSet<String>(sampleToType.values());
        if (types.size() != 2) {
            JOptionPane.showMessageDialog(this,
                                          "The number of sample types in the data set is not 2: " +
                                           InteractionUtilities.joinStringElements(", ", types),
                                          "Wrong Sample Types",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Do a sort
        List<String> sortedTypes = new ArrayList<String>(types);
        Collections.sort(sortedTypes);
        tableModel.setSampleTypes(sortedTypes);
        Map<String, Set<String>> typeToSamples = getTypeToSamples(sampleToType);
        for (VariableInferenceResults varResults : sortedResults) {
            List<Double> ipas0 = null;
            List<Double> ipas1 = null;
            for (String type : sortedTypes) {
                Set<String> samples = typeToSamples.get(type);
                Map<String, List<Double>> sampleToProbs = grepVarResultsForSamples(varResults, samples);
                List<Double> ipas = addValueToDataset(sampleToProbs, 
                                                      type,
                                                      varResults);
                if (ipas0 == null)
                    ipas0 = ipas;
                else
                    ipas1 = ipas;
            }
            double pvalue = tableModel.addRow(ipas0, 
                                              ipas1,
                                              varResults.getVariable().getName(),
                                              varResults.getVariable().getCustomizedInfo());
            pvalues.add(pvalue);
            // In this two cases analysis, we assume the first type is real and the second random.
            realSampleToIPAs.put(varResults.getVariable().getCustomizedInfo(), ipas0);
            randomSampleToIPAs.put(varResults.getVariable().getCustomizedInfo(), ipas1);
        }
    }

    private Map<String, Set<String>> getTypeToSamples(Map<String, String> sampleToType) {
        // Do a reverse map
        Map<String, Set<String>> typeToSamples = new HashMap<String, Set<String>>();
        for (String sample : sampleToType.keySet()) {
            String type = sampleToType.get(sample);
            InteractionUtilities.addElementToSet(typeToSamples, type, sample);
        }
        return typeToSamples;
    }
    
    private Map<String, List<Double>> grepVarResultsForSamples(VariableInferenceResults varResults,
                                                               Set<String> samples) {
        Map<String, List<Double>> sampleToResults = new HashMap<String, List<Double>>();
        Map<String, List<Double>> sampleToProbs = varResults.getPosteriorValues();
        for (String sample : sampleToProbs.keySet()) {
            if (samples.contains(sample)) {
                List<Double> probs = sampleToProbs.get(sample);
                sampleToResults.put(sample, probs);
            }
        }
        return sampleToResults;
    }

    private void parseResults(List<VariableInferenceResults> sortedResults,
                              TTestTableModel tableModel,
                              List<Double> pvalues) throws MathException {
        for (VariableInferenceResults varResults : sortedResults) {
            Map<String, List<Double>> sampleToProbs = varResults.getPosteriorValues();
            List<Double> realIPAs = addValueToDataset(sampleToProbs, 
                                                      "Real Samples",
                                                      varResults);
            List<Double> randomIPAs = addValueToDataset(varResults.getRandomPosteriorValues(),
                                                        "Random Samples",
                                                        varResults);
            double pvalue = tableModel.addRow(realIPAs, 
                                              randomIPAs,
                                              varResults.getVariable().getName(),
                                              varResults.getVariable().getCustomizedInfo());
            pvalues.add(pvalue);
            realSampleToIPAs.put(varResults.getVariable().getCustomizedInfo(), realIPAs);
            randomSampleToIPAs.put(varResults.getVariable().getCustomizedInfo(), randomIPAs);
        }
    }
    
    /**
     * Calculate combined p-value from a list of p-values using Fisher's method and display
     * it in a label.
     * @param pvalues
     * @throws MathException
     */
    private void setCombinedPValue(List<Double> pvalues) throws MathException {
//        double combinedPValue = MathUtilities.combinePValuesWithFisherMethod(pvalues);
        // Since it is pretty sure, variables are dependent in pathway, use another method
        // to combine p-values
        PValueCombiner combiner = new PValueCombiner();
        double combinedPValue = combiner.combinePValue(new ArrayList<List<Double>>(realSampleToIPAs.values()),
                                                        pvalues);
        combinedPValueLabel.setText(PlugInUtilities.formatProbability(combinedPValue));
    }
    
    private List<Double> addValueToDataset(Map<String, List<Double>> sampleToProbs,
                                           String label,
                                           VariableInferenceResults varResults) {
        List<Double> ipas = new ArrayList<Double>();
        for (List<Double> probs : sampleToProbs.values()) {
            double ipa = IPACalculator.calculateIPA(varResults.getPriorValues(), probs);
            ipas.add(ipa);
        }
        dataset.add(ipas, 
                    label,
                    varResults.getVariable().getCustomizedInfo());
        return ipas;
    }
    
    class TTestTableModel extends AbstractTableModel {
        private List<String> colHeaders;
        private List<String[]> data;
        
        public TTestTableModel() {
            String[] headers = new String[]{
                    "DB_ID",
                    "Name",
                    "RealMean",
                    "RandomMean",
                    "MeanDiff",
//                    "t-statistic",
                    "p-value",
                    "FDR"
            };
            colHeaders = Arrays.asList(headers);
            data = new ArrayList<String[]>();
        }
        
        /**
         * Clear up the displayed data if any.
         */
        public void reset() {
            data.clear();
            fireTableDataChanged();
        }
        
        public void setSampleTypes(List<String> types) {
            if (types.size() < 2)
                return; // Cannot use it
            colHeaders.set(2, types.get(0));
            colHeaders.set(3, types.get(1));
        }
        
        /**
         * Add a new column to the table model. P-value will be returned from this method.
         * @param realIPAs
         * @param randomIPAs
         * @param varLabel it should the DB_ID for a Reactome PhysicalEntity instance.
         * @return
         * @throws MathException
         */
        public double addRow(List<Double> realIPAs,
                             List<Double> randomIPAs,
                             String varName,
                             String varLabel) throws MathException {
            double realMean = MathUtilities.calculateMean(realIPAs);
            double randomMean = MathUtilities.calculateMean(randomIPAs);
            double diff = realMean - randomMean;
            // Need a double array
            double[] realArray = new double[realIPAs.size()];
            for (int i = 0; i < realIPAs.size(); i++)
                realArray[i] = realIPAs.get(i);
            double[] randomArray = new double[randomIPAs.size()];
            for (int i = 0; i < randomIPAs.size(); i++)
                randomArray[i] = randomIPAs.get(i);
//            double t = TestUtils.t(realArray,
//                                   randomArray);
//            double pvalue = TestUtils.tTest(realArray,
//                                            randomArray);
            double pvalue = new MannWhitneyUTest().mannWhitneyUTest(realArray, randomArray);
            
            String[] row = new String[colHeaders.size()];
            row[0] = varLabel;
            row[1] = varName;
            row[2] = PlugInUtilities.formatProbability(realMean);
            row[3] = PlugInUtilities.formatProbability(randomMean);
            row[4] = PlugInUtilities.formatProbability(diff);
//            row[4] = PlugInUtilities.formatProbability(t);
            row[5] = PlugInUtilities.formatProbability(pvalue);
            
            data.add(row);
            
            return pvalue;
        }
        
        /**
         * A method to calculate FDRs. The order in the passed List should be the same
         * as p-values stored in the data object. Otherwise, the calculated FDRs assignment
         * will be wrong.
         * @param pvalues
         */
        void calculateFDRs(List<Double> pvalues) {
            if (data.size() != pvalues.size())
                throw new IllegalArgumentException("Passed pvalues list has different size to the stored table data.");
            List<String[]> pvalueSortedList = new ArrayList<String[]>(data);
            final int fdrIndex = 6;
            // Just copy pvalues into rowdata for the time being
            for (int i = 0; i < pvalueSortedList.size(); i++) {
                Double pvalue = pvalues.get(i);
                String[] rowData = pvalueSortedList.get(i);
                rowData[fdrIndex] = pvalue + "";
            }
            Collections.sort(pvalueSortedList, new Comparator<String[]>() {
                public int compare(String[] row1, String[] row2) {
                    Double pvalue1 = new Double(row1[fdrIndex]);
                    Double pvalue2 = new Double(row2[fdrIndex]);
                    return pvalue1.compareTo(pvalue2);
                }
            });
            // pvalues will be sorted 
            List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
            // Modify pvalues into FDRs for the last column. Since the same String[] objects are
            // used in the sorted list and the original data, there is no need to do anything for
            // table display purpose.
            for (int i = 0; i < pvalueSortedList.size(); i++) {
                String[] rowData = pvalueSortedList.get(i);
                rowData[fdrIndex] = PlugInUtilities.formatProbability(fdrs.get(i));
            }
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return colHeaders.size(); 
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String[] row = data.get(rowIndex);
            return row[columnIndex];
        }

        @Override
        public String getColumnName(int column) {
            return colHeaders.get(column);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }
    
}
