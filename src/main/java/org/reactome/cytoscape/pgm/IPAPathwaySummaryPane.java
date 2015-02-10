/*
 * Created on Jan 27, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

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
import org.gk.util.DialogControlPane;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.service.TTestTableModel;
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
    
    // Used to display the values. The main component
    private TTestTablePlotPane<Variable> tablePlotPane;
    // For node selection sync between table and network view
    private boolean isFromTable;
    private boolean isFromNetwork;
    // Used for selecting a node
    private CyNetworkView networkView;
    private ServiceRegistration networkSelectionRegistration;
    // For showing a summarized result
    private JLabel outputResultLabel;
    // Add a button to recheck outputs
    private JButton recheckOutuptBtn;
    // Cache these values for rechecking
    private List<VariableInferenceResults> varResults;
    private Set<Variable> outputVars;
    private Map<String, String> sampleToType;
    
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
        recheckOutuptBtn = new JButton("Reset Cutoffs");
        recheckOutuptBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                recheckOutputs();
            }
        });
        controlToolBar.add(recheckOutuptBtn);
        controlToolBar.add(closeBtn);
        
        addContentPane();
        
        installListeners();
    }
    
    public void hideControlToolBar() {
        controlToolBar.setVisible(false);
    }
    
    private void recheckOutputs() {
        RecheckOutputsDialog dialog = new RecheckOutputsDialog();
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setModal(true);
        dialog.setVisible(true);
        if (dialog.isOkClicked) {
            String pvalueCutoffText = dialog.pvalueCutoffTF.getText().trim();
            String ipaDiffCutoffText = dialog.ipaCutoffTF.getText().trim();
            setOverview(new Double(pvalueCutoffText), 
                        new Double(ipaDiffCutoffText));
        }
    }

    private void addContentPane() {
        tablePlotPane = new TTestTablePlotPane<Variable>() {

            @Override
            protected String[] getAnnotations(Variable key) {
                return new String[]{getVariableKey(key), key.getName()};
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
        tablePlotPane.setCombinedPValueTitle("Combined p-value for outputs using an extension of Fisher's method (click to see the reference): ");
        add(tablePlotPane, BorderLayout.CENTER);
    }
    
    private void installListeners() {
        tablePlotPane.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
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
        JTable tTestResultTable = tablePlotPane.getTable();
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
        JTable tTestResultTable = tablePlotPane.getTable();
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
    
    private void setOverview(double pvalueCutoff,
                             double ipaDiffCutoff) {
        StringBuilder builder = new StringBuilder();
        int size = 0;
        if (varResults != null)
            size = varResults.size();
        builder.append("Total checked outputs: " + outputVars.size());
        if (size == 0) {
            outputResultLabel.setText(builder.toString());
            return; 
        }
        try {
            generateOverview(pvalueCutoff,
                             ipaDiffCutoff, 
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
    
    private void generateOverview(double pvalueCutoff,
                                  double ipaDiffCutoff,
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
        // Do a sort so that the results are the same as in the table
        Collections.sort(types);
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
        TTestTableModel tableModel = (TTestTableModel) tablePlotPane.getTable().getModel();
        Map<String, Double> idToDiff = new HashMap<String, Double>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String id = (String) tableModel.getValueAt(i, 0); // The first value
            String diff = (String) tableModel.getValueAt(i, 4);
            try {
                Double value = new Double(diff); // Just in case the diff is not a number (aka NaN).
                idToDiff.put(id, value);
            }
            catch(NumberFormatException e) {}
        }
        return idToDiff;
    }
    
    public void setVariableResults(List<VariableInferenceResults> varResults,
                                   Set<Variable> outputVars, // Used to calculate combined p-value and overview.
                                   Map<String, String> sampleToType) throws MathException {
        // Cache these values for recheck
        this.varResults = varResults;
        this.outputVars = outputVars;
        this.sampleToType = sampleToType;
        if (sampleToType == null) {
            parseResults(varResults);
        }
        else {
            parseTwoCasesResults(varResults,
                                 sampleToType);
        }
        // Set overview
        // Use these default values
        double pvalueCutoff = 0.01d;
        double ipaDiffCutoff = 0.30d; // 2 fold difference
        setOverview(pvalueCutoff,
                    ipaDiffCutoff);
    }
    
    private void parseTwoCasesResults(List<VariableInferenceResults> sortedResults,
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
        Map<String, Set<String>> typeToSamples = getTypeToSamples(sampleToType);
        Map<Variable, List<Double>> varToIPAs1 = new HashMap<Variable, List<Double>>();
        Map<Variable, List<Double>> varToIPAs2 = new HashMap<Variable, List<Double>>();
        for (VariableInferenceResults varResults : sortedResults) {
            List<Double> ipas1 = null;
            List<Double> ipas2 = null;
            for (String type : sortedTypes) {
                Set<String> samples = typeToSamples.get(type);
                Map<String, List<Double>> sampleToProbs = grepVarResultsForSamples(varResults, samples);
                List<Double> ipas = calculateIPAs(varResults.getPriorValues(),
                                                  sampleToProbs);
                if (ipas1 == null)
                    ipas1 = ipas;
                else
                    ipas2 = ipas;
            }
            varToIPAs1.put(varResults.getVariable(), ipas1);
            varToIPAs2.put(varResults.getVariable(), ipas2);
        }
        tablePlotPane.setDisplayValues(sortedTypes.get(0),
                                       varToIPAs1, 
                                       sortedTypes.get(1), 
                                       varToIPAs2);
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

    private void parseResults(List<VariableInferenceResults> sortedResults) throws MathException {
        Map<Variable, List<Double>> varToRealIPAs = new HashMap<Variable, List<Double>>();
        Map<Variable, List<Double>> varToRandomIPAs = new HashMap<Variable, List<Double>>();
        for (VariableInferenceResults varResults : sortedResults) {
            List<Double> realIPAs = calculateIPAs(varResults.getPriorValues(),
                                                  varResults.getPosteriorValues());
            varToRealIPAs.put(varResults.getVariable(), realIPAs);
            List<Double> randomIPAs = calculateIPAs(varResults.getPriorValues(),
                                                    varResults.getRandomPosteriorValues());
            varToRandomIPAs.put(varResults.getVariable(), randomIPAs);
        }
        tablePlotPane.setDisplayValues("Real Samples",
                                       varToRealIPAs,
                                       "Random Samples",
                                       varToRandomIPAs);
    }
    
    private String getVariableKey(Variable var) {
        if (var.getCustomizedInfo() != null)
            return var.getCustomizedInfo();
        return var.getName();
    }
    
    private List<Double> calculateIPAs(List<Double> priorValues,
                                       Map<String, List<Double>> sampleToProbs) {
        List<Double> ipas = new ArrayList<Double>();
        for (List<Double> probs : sampleToProbs.values()) {
            double ipa = IPACalculator.calculateIPA(priorValues, probs);
            ipas.add(ipa);
        }
        return ipas;
    }
    
    private class RecheckOutputsDialog extends JDialog {
        private DialogControlPane controlPane;
        private JTextField pvalueCutoffTF;
        private JTextField ipaCutoffTF;
        private boolean isOkClicked;
        
        public RecheckOutputsDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        private void init() {
            setTitle("Recheck Outputs");
            JPanel contentPane = new JPanel();
            contentPane.setLayout(new GridBagLayout());
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            JLabel label = new JLabel("<html><u><b>Enter Cutoffs for Rechecing Outputs</b></u></html>");
            constraints.gridwidth = 2;
            contentPane.add(label, constraints);
            
            label = new JLabel("P-value Cutoff:");
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            contentPane.add(label, constraints);
            pvalueCutoffTF = new JTextField();
            pvalueCutoffTF.setColumns(4);;
            constraints.gridx = 1;
            contentPane.add(pvalueCutoffTF, constraints);
            
            label = new JLabel("IPA MeanDiff Cutoff:");
            constraints.gridy = 2;
            constraints.gridwidth = 1;
            constraints.gridx = 0;
            contentPane.add(label, constraints);
            ipaCutoffTF = new JTextField();
            ipaCutoffTF.setColumns(4);;
            constraints.gridx = 1;
            contentPane.add(ipaCutoffTF, constraints);
            
            controlPane = new DialogControlPane();
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!validateValues())
                        return;
                    isOkClicked = true;
                    dispose();
                }
            });
            
            getContentPane().add(contentPane, BorderLayout.CENTER);
            getContentPane().add(controlPane, BorderLayout.SOUTH);
        }
        
        private boolean validateValues() {
            String text = pvalueCutoffTF.getText().trim();
            boolean rtn = validateNumber(text, "P-value");
            if (!rtn)
                return false;
            text = ipaCutoffTF.getText().trim();
            rtn = validateNumber(text, "IPA meanDiff");
            if (!rtn)
                return false;
            return true;
        }

        private boolean validateNumber(String text,
                                       String type) {
            if (text.length() == 0) {
                JOptionPane.showMessageDialog(this,
                                              type + " cutoff should not be empty!", 
                                              "Empty Cutoff",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            try {
                Double value = new Double(text);
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                                              type + " cutoff should be a number!", 
                                              "Wrong P-value Cutoff",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        
    }
}
