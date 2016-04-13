/*
 * Created on Dec 14, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.pgm.GeneLevelSelectionHandler;
import org.reactome.cytoscape.pgm.SampleListComponent;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Variable;

/**
 * Customized for showing single sample information for a chosen sample.
 * @author gwu
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FIPGMSampleListComponent extends SampleListComponent {
    private CyNetworkView networkView;
    // The source generating selection
    private SelectionMediator selectionMediator;
    private Selectable infTableHandler;
    private Selectable obsTableHandler;
    private Selectable networkHandler;
    // Just cache a copy for helping
    private TableHelper tableHelper;
    
    /**
     * Default constructor.
     */
    public FIPGMSampleListComponent() {
        SetCurrentNetworkViewListener currentViewListener = new SetCurrentNetworkViewListener() {
            @Override
            public void handleEvent(SetCurrentNetworkViewEvent arg0) {
                setNetworkView(arg0.getNetworkView());
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        //TODO: Should this service be removed when this component is closed?
        context.registerService(SetCurrentNetworkViewListener.class.getName(),
                                currentViewListener,
                                null);
        RowsSetListener listener = new RowsSetListener() {
            
            @Override
            public void handleEvent(RowsSetEvent e) {
                handleNetworkSelection(e);
            }
        };
        context.registerService(RowsSetListener.class.getName(),
                                listener,
                                null);
        // For selection synchronization
        selectionMediator = new SelectionMediator();
        Selectable selectable = new InferenceTableSelectionHandler();
        selectionMediator.addSelectable(selectable);
        infTableHandler = selectable;
        selectable = new ObservationTableSelectionHandler();
        selectionMediator.addSelectable(selectable);
        obsTableHandler = selectable;
        selectable = new NetworkViewSelectionHandler();
        selectionMediator.addSelectable(selectable);
        networkHandler = selectable;
        // Modify the text a little bit
        highlightViewBtn.setText("Highlight network for sample");
    }
    
    @Override
    protected void enableViewSelectionSyn() {
    }

    @Override
    protected void registerRadioButton() {
        PlugInObjectManager.getManager().registerRadioButton("HighlightNetwork",
                                                             highlightViewBtn);
    }
    
    @Override
    protected void handleViewBtnSelectionEvent() {
        FIPGMResults results = FIPGMResults.getResults();
        Set<Variable> geneVars = getDisplayedVariables();
        Map<String, Map<Variable, Double>> sampleToVarToScore = results.getSampleToVarToScore(geneVars);
        double[] minMaxScore = getMinMaxScore(sampleToVarToScore);
        FIPGMImpactVisualStyle style = new FIPGMImpactVisualStyle();
        style.setMinMaxGeneValues(minMaxScore);
        // Need to recreate visual style so that node sizes can be reset.
        style.setVisualStyle(networkView, true);
        highlightViewFromSample();
    }
    
    private double[] getMinMaxScore(Map<String, Map<Variable, Double>> sampleToVarToScore) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Map<Variable, Double> varToScore : sampleToVarToScore.values()) {
            for (Double score : varToScore.values()) {
                if (score > max)
                    max = score;
                if (score < min)
                    min = score;
            }
        }
        return new double[]{min, max};
    }
    
    private Set<Variable> getDisplayedVariables() {
        FIPGMResults results = FIPGMResults.getResults();
        if (results == null || networkView == null || networkView.getModel() == null)
            return new HashSet<>();
        Set<String> genes = PlugInUtilities.getDisplayedGenesInNetwork(networkView.getModel());
        Map<String, Variable> nameToVar = results.getNameToVariable();
        Set<Variable> geneVars = new HashSet<>();
        for (String gene : genes) {
            Variable var = nameToVar.get(gene);
            if (var != null)
                geneVars.add(nameToVar.get(gene));
        }
        return geneVars;
    }
    
    @Override
    protected void highlightViewFromSample() {
        if (!highlightViewBtn.isSelected() || networkView == null || networkView.getModel() == null)
            return;
        if (tableHelper == null)
            tableHelper = new TableHelper();
        FIPGMSampleInferenceModel model = (FIPGMSampleInferenceModel) inferenceTable.getModel();
        Map<String, Double> geneToScore = model.getGeneToScore();
        tableHelper.storeNodeAttributesByName(networkView.getModel(),
                                              FIVisualStyle.GENE_VALUE_ATT,
                                              geneToScore);
        networkView.updateView();
    }

    private void handleNetworkSelection(RowsSetEvent e) {
        if(!e.containsColumn(CyNetwork.SELECTED))
            return;
        if (networkView == null || networkView.getModel() == null)
            return;
        selectionMediator.fireSelectionEvent(networkHandler);
    }
    
    private void selectTableRows(Set<String> selectedGenes,
                                 JTable table,
                                 boolean isForObservation) {
        table.clearSelection();
        if (selectedGenes != null && selectedGenes.size() > 0) {
            int firstIndex = -1;
            ListSelectionModel selectionModel = table.getSelectionModel();
            selectionModel.setValueIsAdjusting(true);
            for (int i = 0; i < table.getRowCount(); i++) {
                String name = (String) table.getValueAt(i, 0);
                if (isForObservation) {
                    int index = name.indexOf("_");
                    name = name.substring(0, index);
                }
                if (selectedGenes.contains(name)) {
                    selectionModel.addSelectionInterval(i, i);
                    if (firstIndex == -1)
                        firstIndex = i;
                }
            }
            selectionModel.setValueIsAdjusting(false);
            if (firstIndex > -1) {
                Rectangle rect = table.getCellRect(firstIndex, 0, false);
                table.scrollRectToVisible(rect);
            }
        }
    }
    
    private void setNetworkView(CyNetworkView networkView) {
        if (this.networkView == networkView)
            return;
        this.networkView = networkView;
        if (networkView == null || networkView.getModel() == null)
            return;
        String sample = (String) sampleBox.getSelectedItem();
        SampleTableModel model = (SampleTableModel) inferenceTable.getModel();
        model.setSample(sample);
        model = (SampleTableModel) observationTable.getModel();
        model.setSample(sample);
    }
    
    public void showResults() {
        if (FIPGMResults.getResults() == null)
            return; // Nothing to be displayed.
        showSamples();
        // Show detailed information for the selected network
        setNetworkView(PlugInUtilities.getCurrentNetworkView());
    }
    
    private void showSamples() {
        if(FIPGMResults.getResults().getSampleToVarToScore() == null)
            return;
        List<String> samples = new ArrayList<String>(FIPGMResults.getResults().getSampleToVarToScore().keySet());
        Collections.sort(samples);
        DefaultComboBoxModel<String> sampleModel = (DefaultComboBoxModel<String>) sampleBox.getModel();
        sampleModel.removeAllElements();
        for (String sample : samples)
            sampleModel.addElement(sample);
        sampleBox.setSelectedIndex(0);
    }
    
    @Override
    protected void handleInferenceTableSelection() {
        if (blockRowSelectionSync)
            return;
        selectionMediator.fireSelectionEvent(infTableHandler);
    }
    
    @Override
    protected void handleObservationTableSelection() {
        if (blockRowSelectionSync)
            return;
        selectionMediator.fireSelectionEvent(obsTableHandler);
        PlugInObjectManager.getManager().getObservationVarSelectionMediator().fireSelectionEvent(observationVarSelectionHandler);
    }

    private Set<String> getTableSelectedGenes(JTable table, boolean isForObservation) {
        Set<String> selectedGenes = new HashSet<>();
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows != null && selectedRows.length > 0) {
            for (int selectedRow : selectedRows) {
                String name = (String) table.getValueAt(selectedRow, 0);
                if (isForObservation) {
                    int index = name.indexOf("_");
                    name = name.substring(0, index);
                }
                selectedGenes.add(name);
            }
        }
        return selectedGenes;
    }

    @Override
    protected SampleTableModel createInferenceTableModel() {
        return new FIPGMSampleInferenceModel();
    }
    
    @Override
    protected SampleTableModel createObservationTableModel() {
        return new FIPGMSampleObservationModel();
    }

    private class FIPGMSampleObservationModel extends ObservationTableModel {
        
        public FIPGMSampleObservationModel() {
        }

        @Override
        protected void resetData() {
            data.clear();
            if (sample == null || networkView == null || networkView.getModel() == null)
                return;
            Set<String> genes = PlugInUtilities.getDisplayedGenesInNetwork(networkView.getModel());
            if (genes == null || genes.size() == 0)
                return;
            FIPGMResults results = FIPGMResults.getResults();
            resetData(results.getObservations(),
                      results.getRandomObservations(),
                      genes);
        }
    }

    private class FIPGMSampleInferenceModel extends SampleTableModel {
        
        public FIPGMSampleInferenceModel() {
        }
        
        public Map<String, Double> getGeneToScore() {
            Map<String, Double> geneToScore = new HashMap<>();
            if (data != null) {
                for (List<Object> row : data) {
                    String gene = (String) row.get(0);
                    Double score = (Double) row.get(1);
                    geneToScore.put(gene, score);
                }
            }
            return geneToScore;
        }
        
        @Override
        protected void resetData() {
            data.clear();
            if (sample == null)
                return;
            Set<Variable> geneVars = getDisplayedVariables();
            if (geneVars == null || geneVars.size() == 0)
                return;
            FIPGMResults results = FIPGMResults.getResults();
            Map<String, Map<Variable, Double>> sampleToVarToScore = results.getSampleToVarToScore(geneVars);
            Map<Variable, Double> varToScore = sampleToVarToScore.get(sample);
            // In order to calculate p-values
            Map<Variable, List<Double>> varToRandomScores = results.getRandomScores(geneVars);
            for (Variable var : varToScore.keySet()) {
                Double score = varToScore.get(var);
                List<Double> randomScores = varToRandomScores.get(var);
                Double pvalue = PlugInUtilities.calculateNominalPValue(score, randomScores, "right"); // Should be positive always
                List<Object> row = new ArrayList<>();
                data.add(row);
                row.add(var.getName());
                row.add(score);
                row.add(pvalue);
            }
            int permutationSize = varToRandomScores.values().iterator().next().size();
            calculateFDRs(permutationSize);
        }
    }
    
    private class InferenceTableSelectionHandler implements Selectable {

        @Override
        public void setSelection(List selection) {
            selectTableRows(new HashSet<String>(selection), 
                            inferenceTable,
                            false);
        }

        @Override
        public List getSelection() {
            Set<String> selectedGenes = getTableSelectedGenes(inferenceTable, false);
            return new ArrayList<String>(selectedGenes);
        }
        
    }
    
    private class ObservationTableSelectionHandler implements Selectable {
        
        @Override
        public void setSelection(List selection) {
            selectTableRows(new HashSet<String>(selection), 
                            observationTable,
                            true);
        }

        @Override
        public List getSelection() {
            Set<String> selectedGenes = getTableSelectedGenes(observationTable, 
                                                              true);
            return new ArrayList<String>(selectedGenes);
        }
        
    }
    
    private class NetworkViewSelectionHandler implements Selectable {
        
        @Override
        public void setSelection(List selection) {
            TableHelper tableHelper = new TableHelper();
            tableHelper.selectNodes(networkView, 
                                    "name",
                                    new HashSet<String>(selection));
        }

        @Override
        public List getSelection() {
            Set<String> selectedGenes = PlugInUtilities.getSelectedGenesInNetwork(networkView.getModel());
            return new ArrayList<>(selectedGenes);
        }
    }
    
}
