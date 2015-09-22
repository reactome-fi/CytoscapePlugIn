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

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.gk.util.ProgressPane;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.service.FIAnalysisTask;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FINetworkService;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.TableHelper;
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
import org.reactome.r3.util.InteractionUtilities;

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
        // Make sure all needed parameters have been set
        if (lbp == null || data == null || data.size() == 0 || pgmType == null) {
            throw new IllegalStateException("Make sure the LBP algorithm, data, and pgmType have been set.");
        }
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        ProgressPane progPane = new ProgressPane();
        frame.setGlassPane(progPane);
        progPane.setTitle("FI PGM Impact Analysis");
        progPane.setText("Fetching the FI network...");
        progPane.setIndeterminate(true);
        progPane.setSize(400, 200);
        progPane.setVisible(true);
        fetchFIs(); // Need to get all FIs to construct the FI pgm model.
        FIPGMConstructor constructor = getPGMConstructor();
        FactorGraph fg = null;
        progPane.setText("Constructing the PGM...");
        try {
            fg = constructor.constructFactorGraph(pgmType);
        }
        catch(IOException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot build a graphical model from the Reactome FI network:\n" + e.getMessage(),
                                          "Error in Constructing Model",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
            return;
        }
        progPane.setText("Handling observations...");
        List<Observation<Number>> observations = constructor.getObservationLoader().getObservations();
        // Filter observations to have shared data types.
        new ObservationHelper().filterObservationsToHaveSharedDataTypes(observations);
        
        lbp.setFactorGraph(fg);
        Map<Variable, double[]> varToBase = new HashMap<Variable, double[]>();
        
        Observation<Number> baseObs = createBaseObservation(observations);
        lbp.setObservation(baseObs);
        progPane.setText("Running prior inference...");
        // Estimate inference time
        long time1 = System.currentTimeMillis();
        try {
            lbp.runInference();
        }
        catch(InferenceCannotConvergeException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "The inference cannot converge for the base observation.",
                                          "Inference Cannot Converge",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
            return;
        }
        long time2 = System.currentTimeMillis();
        double runningTime = (time2 - time1);
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
        progPane.setMaximum(observations.size());
        progPane.setMinimum(0);
        progPane.setValue(0);
        progPane.setIndeterminate(false);
        int count = 0;
        Map<String, Map<Variable, Double>> sampleToVarToResult = new HashMap<>();
        for (Observation<Number> observation : observations) {
            showInferenceText(count, observations.size(), runningTime, observation, progPane);
            time1 = System.currentTimeMillis(); // get a better time
            progPane.setValue(++ count);
            lbp.setObservation(observation);
            try {
                lbp.runInference();
            }
            catch(InferenceCannotConvergeException e) {
                notConvergedObs.add(observation.getName());
                continue; // Just escape it
            }
            Map<Variable, Double> varToResult = new HashMap<>();
            for (Variable var : fiGeneVariables) {
                if (var instanceof ContinuousVariable)
                    continue; // Handle discrete variables only
                double[] belief = var.getBelief();
                double[] prior = varToBase.get(var);
                double logRatio = Math.log10(belief[1] * prior[0] / (belief[0] * prior[1]));
                varToResult.put(var, logRatio);
            }
            time2 = System.currentTimeMillis();
            // Average the original runningTime to get a better estimation
            runningTime = ((time2 - time1) + runningTime) / 2.0d;
            sampleToVarToResult.put(observation.getName(), varToResult);
        }
        progPane.setText("The inference is done.");
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
        showResults(sampleToVarToResult,
                    progPane);
        frame.getGlassPane().setVisible(false);
    }
    
    /**
     * Show inference results
     * @param sampleToVarToResult
     */
    private void showResults(Map<String, Map<Variable, Double>> sampleToVarToResult,
                             ProgressPane progressPane) {
        progressPane.setText("Selecting genes...");
        progressPane.setIndeterminate(true);
        PGMImpactAnalysisResultDialog dialog = new PGMImpactAnalysisResultDialog();
        dialog.setSampleResults(sampleToVarToResult);
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked())
            return;
        Map<String, Double> geneToScore = dialog.getSelectedGeneToScore();
        constructFINetwork(geneToScore, progressPane);
    }
    
    private void constructFINetwork(Map<String, Double> geneToScore,
                                    ProgressPane progressPane) {
        try {
            progressPane.setText("Constructing FI network...");
            // Use the cached all FIs so that we don't need to fetch the server again to save
            // some time.
            Set<String> allFIs = PlugInObjectManager.getManager().getFIPGMConfig().getFIs();
            Set<String> fis = InteractionUtilities.getFIs(geneToScore.keySet(), allFIs);
            FINetworkGenerator generator = new FINetworkGenerator();
            CyNetwork network = generator.constructFINetwork(fis);
            network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", 
                                                                           "FI PGM Impact Analysis Network");
            // Register and display the network
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            CyNetworkManager netManager = (CyNetworkManager) context.getService(netManagerRef);
            netManager.addNetwork(network);
            // We want to put the scores into the table
            TableHelper tableHelper = new TableHelper();
            tableHelper.storeNodeAttributesByName(network,
                                                  FIVisualStyle.GENE_VALUE_ATT,
                                                  geneToScore);
            CyNetworkViewFactory viewFactory = (CyNetworkViewFactory) context.getService(viewFactoryRef);
            CyNetworkView view = viewFactory.createNetworkView(network);
            CyNetworkViewManager viewManager = (CyNetworkViewManager) context.getService(viewManagerRef);
            viewManager.addNetworkView(view);
            FIPGMImpactVisualStyle style = new FIPGMImpactVisualStyle();
            style.setVisualStyle(view);
            progressPane.setText("Layouting FI network...");
            style.doLayout();
        }
        catch(IOException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in constructing the FI network: " + e.getMessage(),
                                          "Error in Network Construction",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showInferenceText(int current,
                                   int size,
                                   double ms,
                                   Observation<Number> obs,
                                   ProgressPane progressPane) {
        double needTime = (size - current) * ms;
        // Change to minutes
        needTime /= (1000.0d * 60);
        int min = (int) (needTime);
        StringBuilder builder = new StringBuilder();
        builder.append("<html>Running ");
        builder.append(obs.getName());
        builder.append("...<br/>(Remaining time: ");
        if (min < 1)
            builder.append("&lt; 1 minute");
        else if (min == 1)
            builder.append("about 1 minute");
        else
            builder.append("about ").append(min).append(" minutes");
        builder.append(")</html>");
        progressPane.setText(builder.toString());
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
        for (Variable var : fg.getVariables()) {
            if (fiGenes.contains(var.getName()))
                variables.add(var);
        }
        return variables;
    }
    
    private Set<String> getFIGenes() {
        FIPGMConfiguration config = PlugInObjectManager.getManager().getFIPGMConfig();
        try {
            Set<String> fis = config.getFIs();
            return InteractionUtilities.grepIDsFromInteractions(fis);
        }
        catch(IOException e) {
            throw new IllegalStateException("Cannot get genes in the FI network.");
        }
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
