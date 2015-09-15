/*
 * Created on Sep 10, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.reactome.cytoscape.service.FIAnalysisTask;
import org.reactome.cytoscape.service.FINetworkService;
import org.reactome.cytoscape.util.MessageDialog;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape3.FIPlugInHelper;
import org.reactome.factorgraph.ContinuousVariable;
import org.reactome.factorgraph.ContinuousVariable.DistributionType;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.InferenceCannotConvergeException;
import org.reactome.factorgraph.LoopyBeliefPropagation;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.VariableAssignment;
import org.reactome.factorgraph.common.DataType;
import org.reactome.factorgraph.common.EmpiricalFactorHandler;
import org.reactome.factorgraph.common.MutationEmpiricalFactorHandler;
import org.reactome.factorgraph.common.ObservationFactorHandler;
import org.reactome.factorgraph.common.ObservationFileLoader;
import org.reactome.factorgraph.common.ObservationHelper;
import org.reactome.fi.pgm.FIPGMConfiguration;
import org.reactome.fi.pgm.FIPGMConstructor;
import org.reactome.fi.pgm.FIPGMConstructor.PGMType;

/**
 * Customized FIAnalysisTask to perform FI PGM based impact analysis. Use this class to perform PGM-based FI network impact analysis. 
 * Many of functions implemented in this class are actually based on functions in class org.reactome.fi.pgm.FIPGMRunner.
 * @author gwu
 *
 */
public class PGMImpactAnalysisTask extends FIAnalysisTask {
    private List<DataDescriptor> data;
    private LoopyBeliefPropagation lbp;
    private PGMType pgmType;
    
    /**
     * Default constructor.
     */
    public PGMImpactAnalysisTask() {
    }
    
    public List<DataDescriptor> getData() {
        return data;
    }

    public void setData(List<DataDescriptor> data) {
        this.data = data;
    }

    public LoopyBeliefPropagation getLbp() {
        return lbp;
    }

    public void setLbp(LoopyBeliefPropagation lbp) {
        this.lbp = lbp;
    }

    public PGMType getPGMType() {
        return pgmType;
    }

    public void setPGMType(PGMType pgmType) {
        this.pgmType = pgmType;
    }
    
    private Observation<Number> createBaseObservation(List<Observation<Number>> observations) {
        Map<Variable, EmpiricalDistribution> varToDist = new HashMap<Variable, EmpiricalDistribution>();
        for (Observation<Number> observation : observations) {
            List<VariableAssignment<Number>> varAssgns = observation.getVariableAssignments();
            for (VariableAssignment<Number> varAssgn : varAssgns) {
                varToDist.put(varAssgn.getVariable(), varAssgn.getDistribution());
            }
        }
        Observation<Number> base = new Observation<Number>();
        String baseName = "Base";
        base.setName(baseName);
        for (Variable var : varToDist.keySet()) {
            VariableAssignment<Number> varAssgn = new VariableAssignment<Number>();
            varAssgn.setVariable(var);
            EmpiricalDistribution dist = varToDist.get(var);
            if (dist != null && var instanceof ContinuousVariable) {
                ContinuousVariable cVar = (ContinuousVariable) var;
                if (cVar.getDistributionType() == DistributionType.TWO_SIDED) {
                    varAssgn.setAssignment(dist.inverseCumulativeProbability(0.50d));
//                    varAssgn.setAssignment(dist.getNumericalMean());
                }
                else
                    varAssgn.setAssignment(dist.getSupportLowerBound());
            }
            else
                varAssgn.setAssignment(0);
            varAssgn.setDistribution(varToDist.get(var));
            base.addAssignment(varAssgn);
        }
        return base;
    }

    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.FIAnalysisTask#doAnalysis()
     */
    @Override
    protected void doAnalysis() {
        // Add GUIs and calculate and update approximate running time
        // Make sure all needed parameters have been set
        if (lbp == null || data == null || data.size() == 0 || pgmType == null) {
            throw new IllegalStateException("Make sure the LBP algorithm, data, and pgmType have been set.");
        }
        fetchFIs(); // Need to get all FIs to construct the FI pgm model.
        FIPGMConstructor constructor = getPGMConstructor();
        FactorGraph fg = null;
        try {
            fg = constructor.constructFactorGraph(pgmType);
        }
        catch(IOException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot build a graphical model from the Reactome FI network:\n" + e.getMessage(),
                                          "Error in Constructing Model",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Observation<Number>> observations = constructor.getObservationLoader().getObservations();
        // Filter observations to have shared data types.
        new ObservationHelper().filterObservationsToHaveSharedDataTypes(observations);
        
        lbp.setFactorGraph(fg);
        Map<Variable, double[]> varToBase = new HashMap<Variable, double[]>();
        
        Observation<Number> baseObs = createBaseObservation(observations);
        lbp.setObservation(baseObs);
        try {
            lbp.runInference();
        }
        catch(InferenceCannotConvergeException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "The inference cannot converge for the base observation.",
                                          "Inference Cannot Converge",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        Set<Variable> fiGeneVariables = getFIGeneVariables(fg);
        for (Variable var : fiGeneVariables) {
            if (var instanceof ContinuousVariable)
                continue; // Don't support this query.
            double[] belief = var.getBelief();
            double[] prior = new double[belief.length];
            System.arraycopy(belief, 0, prior, 0, belief.length);
            varToBase.put(var, prior);
        }
        
        // A list of observations that cannot converge
        List<String> notConvergedObs = new ArrayList<String>();
        for (Observation<Number> observation : observations) {
            lbp.setObservation(observation);
            try {
                lbp.runInference();
            }
            catch(InferenceCannotConvergeException e) {
                notConvergedObs.add(observation.getName());
                continue; // Just escape it
            }
            
            for (Variable var : fiGeneVariables) {
                if (var instanceof ContinuousVariable)
                    continue; // Handle discrete variables only
                double[] belief = var.getBelief();
                double[] prior = varToBase.get(var);
                double logRatio = Math.log10(belief[1] * prior[0] / (belief[0] * prior[1]));
            }
            System.out.println("Finish inference: " + observation.getName());
        }
        if (notConvergedObs.size() > 0) {
            // Show a warning message
            StringBuilder builder = new StringBuilder();
            builder.append("The inference for the following samples cannot converge:\n");
            for (String sample : notConvergedObs)
                builder.append("\t").append(sample);
            MessageDialog dialog = new MessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setText(builder.toString());
            dialog.setModal(true);
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setSize(400, 350);
            dialog.setVisible(true);
        }
    }
    
    private void fetchFIs() {
        try {
            FINetworkService networkService = FIPlugInHelper.getHelper().getNetworkService();
            Set<String> fis = networkService.queryAllFIs();
            FIPGMConfiguration config = PlugInObjectManager.getManager().getFIPGMConfig();
            config.setFIs(fis);
        }
        catch(Exception e) {
            throw new RuntimeException("Cannot load the FI network: " + e);
        }
    }
    
    private Set<Variable> getFIGeneVariables(FactorGraph fg) {
        Set<Variable> variables = new HashSet<Variable>();
        Set<String> fiGenes = getFIGenes();
        for (Variable var : variables) {
            if (fiGenes.contains(var.getName()))
                variables.add(var);
        }
        return variables;
    }
    
    private Set<String> getFIGenes() {
        return new HashSet<String>();
    }
    
    /**
     * Get a FIPGMConstructor with ObservationFactorHandler and evidence files configured.
     * @return
     */
    private FIPGMConstructor getPGMConstructor() {
        FIPGMConstructor constructor = new FIPGMConstructor();
        constructor.setEvidenceFiles(getEvidenceFiles());
        ObservationFileLoader fileLoader = constructor.getObservationLoader();
        Map<DataType, ObservationFactorHandler> dataTypeToHandler = getObservationFactorHandlers();
        if (dataTypeToHandler != null) {
            for (DataType dataType : dataTypeToHandler.keySet()) {
                ObservationFactorHandler handler = dataTypeToHandler.get(dataType);
                fileLoader.setObservationFactorHandler(dataType, 
                                                       handler);
            }
        }
        return constructor;
    }
    
    /**
     * Create ObservationFactorHandler from configured properties.
     * @return
     */
    private Map<DataType, ObservationFactorHandler> getObservationFactorHandlers() {
        Map<DataType, ObservationFactorHandler> dataTypeToHandler = new HashMap<DataType, ObservationFactorHandler>();
        if (data == null || data.size() == 0)
            return dataTypeToHandler;
        FIPGMConfiguration config = PlugInObjectManager.getManager().getFIPGMConfig();
        for (DataDescriptor desc : data) {
            ObservationFactorHandler handler = null;
            if (desc.getDistribution() == DataTypeDistribution.Discrete) {
                FIPGMDiscreteObservationFactorHandler handler1 = new FIPGMDiscreteObservationFactorHandler();
                handler1.setRelation(desc.getRelation());
                handler1.setThresholds(desc.getThresholds());
                handler = handler1;
            }
            else if (desc.getDistribution() == DataTypeDistribution.Empirical) {
                if (desc.getDataType() == DataType.Mutation) {
                    handler = new MutationEmpiricalFactorHandler();
                }
                else
                    handler = new EmpiricalFactorHandler();
            }
            // For the time being, we support two distributions only
            if (handler != null) {
                handler.setConfiguration(config);
                dataTypeToHandler.put(desc.getDataType(), handler);
            }
        }
        return dataTypeToHandler;
    }
    
    /**
     * Extract a map from DataType to its associated file name.
     * @return
     */
    private Map<DataType, String> getEvidenceFiles() {
        Map<DataType, String> typeToFile = new HashMap<DataType, String>();
        if (data != null) {
            for (DataDescriptor desc : data) {
                typeToFile.put(desc.getDataType(), desc.getFileName());
            }
        }
        return typeToFile;
    }
    
}
