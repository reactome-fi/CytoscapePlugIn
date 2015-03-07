/*
 * Created on Feb 10, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;

import org.apache.commons.math.MathException;
import org.gk.render.RenderablePathway;
import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.service.TTestTableModel;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;

/**
 * This customized JDialog is used to show observation data for a pathway diagram.
 * @author gwu
 *
 */
public class ObservationDataDialog extends GeneLevelDialog {
    private TTestTablePlotPane<String> tablePlotPane;
    // A list of genes to be displayed
    private Set<String> targetGenes;
    
    /**
     * Default constructor.
     */
    public ObservationDataDialog() {
    }
    
    protected void init() {
        setTitle("Observation Data");
        
        tablePlotPane = new TTestTablePlotPane<String>() {

            @Override
            protected String[] getAnnotations(String key) {
                return new String[]{key};
            }

            @Override
            protected void sortValueKeys(List<String> list) {
                Collections.sort(list);
            }
            
        };
        tablePlotPane.setBorder(BorderFactory.createEtchedBorder());
        // Headers for table
        TTestTableModel tableModel = (TTestTableModel) tablePlotPane.getTable().getModel();
        String[] headers = new String[]{
                "Name",
                "RealMean",
                "RandomMean",
                "MeanDiff",
                "p-value",
                "FDR"
        };
        List<String> colHeaders = Arrays.asList(headers);
        tableModel.setColHeaders(colHeaders, 1);
        // Set the lable for the y-axis
        tablePlotPane.getPlot().getRangeAxis().setAttributedLabel("Value");
        tablePlotPane.setChartTitle("Boxplot for Observation Data");
        
        getContentPane().add(tablePlotPane, BorderLayout.CENTER);
    
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.getCancelBtn().setVisible(false);
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        setLocationRelativeTo(getOwner());
        
        addTableSelectionListener(tablePlotPane.getTable());;
    }
    
    public Set<String> getTargetGenes() {
        return targetGenes;
    }

    public void setTargetGenes(Set<String> targetGenes) {
        this.targetGenes = targetGenes;
    }
    
    /**
     * Show observations data loaded for the passed RenderablePathway object.
     * @param diagram
     * @return
     */
    public boolean showResultsForDiagram(RenderablePathway diagram) {
        FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(diagram);
        if (fgResults == null) {
            JOptionPane.showMessageDialog(this,
                                          "No inference result is available for this diagram.\n" +
                                          "Please run graphical analysis first.",
                                          "Empty Results",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            if (fgResults.getSampleToType() != null && fgResults.getSampleToType().size() > 0)
                setObservations(fgResults.getObservations(), fgResults.getSampleToType());
            else
                setObservations(fgResults.getObservations(), fgResults.getRandomObservations());
            // Check if there is anything to be displayed
            if(tablePlotPane.getTable().getRowCount() == 0)
                return false; // Nothing to be displayed
        }
        catch(MathException e) {
            JOptionPane.showMessageDialog(this,
                                          "Error in displaying observations: " + e,
                                          "Error in Observation",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    /**
     * Set a list of observation to be displayed.
     * @param observations
     * @param sampleToType sample to type information. This object should not be null.
     */
    public void setObservations(List<Observation> observations,
                                Map<String, String> sampleToType) throws MathException {
        if (sampleToType == null)
            throw new IllegalArgumentException("sampleToType should not be null!");
        Map<String, Set<String>> typeToSamples = PlugInUtilities.getTypeToSamples(sampleToType);
        if (typeToSamples.keySet().size() != 2) {
            throw new IllegalArgumentException("Only two types are allowed. Types passed are: " + typeToSamples.keySet());
        }
        List<String> typeList = new ArrayList<String>(typeToSamples.keySet());
        Collections.sort(typeList);
        Map<String, List<Double>> nameToValues0 = new HashMap<String, List<Double>>();
        Map<String, List<Double>> nameToValues1 = new HashMap<String, List<Double>>();
        for (int i = 0; i < typeList.size(); i++) {
            String type = typeList.get(i);
            for (Observation obs : observations) {
                if (obs.getAnnoation() == null || !obs.getAnnoation().equals(type))
                    continue;
                Map<Variable, Integer> varToState = obs.getVariableToAssignment();
                if (i == 0)
                    addValues(nameToValues0, varToState);
                else
                    addValues(nameToValues1, varToState);
            }
        }
        tablePlotPane.setDisplayValues(typeList.get(0), 
                                       nameToValues0, 
                                       typeList.get(1), 
                                       nameToValues1);
    }
    
    /**
     * Display results between real and random values.
     * @param observations
     * @param randomObservations
     * @throws MathException
     */
    public void setObservations(List<Observation> observations,
                                List<Observation> randomObservations) throws MathException {
        Map<String, List<Double>> nameToRealValues = new HashMap<String, List<Double>>();
        for (Observation obs : observations) 
            addValues(nameToRealValues, obs.getVariableToAssignment());
        Map<String, List<Double>> nameToRandomValues = new HashMap<String, List<Double>>();
        for (Observation obs : randomObservations)
            addValues(nameToRandomValues, obs.getVariableToAssignment());
        tablePlotPane.setDisplayValues("Real Samples", 
                                       nameToRealValues,
                                       "Random Samples",
                                       nameToRandomValues);
    }
    
    private void addValues(Map<String, List<Double>> nameToValues,
                           Map<Variable, Integer> varToState) {
        for (Variable var : varToState.keySet()) {
            if (!shouldAdd(var))
                continue;
            Integer state = varToState.get(var);
            List<Double> values = nameToValues.get(var.getName());
            if (values == null) {
                values = new ArrayList<Double>();
                nameToValues.put(var.getName(), values);
            }
            values.add(new Double(state)); // Convert an Integer to a Double.
        }
    }
    
    private boolean shouldAdd(Variable var) {
        if (targetGenes == null)
            return true;
        // Check if this var is in the gene set
        String name = var.getName();
        int index = name.indexOf("_");
        String gene = name.substring(0, index);
        return targetGenes.contains(gene);
    }
    
}
