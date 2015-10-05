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
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.InferenceCannotConvergeException;
import org.reactome.factorgraph.LoopyBeliefPropagation;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.VariableAssignment;
import org.reactome.factorgraph.common.DataType;
import org.reactome.factorgraph.common.EmpiricalFactorHandler;
import org.reactome.factorgraph.common.ObservationFactorHandler;
import org.reactome.factorgraph.common.ObservationFileLoader;
import org.reactome.factorgraph.common.ObservationHelper;
import org.reactome.factorgraph.common.ObservationRandomizer;
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
    private final String NON_MUTATION_DATA_KEY = "Non_" + DataType.Mutation;
    private List<DataDescriptor> data;
    private LoopyBeliefPropagation lbp;
    private PGMType pgmType;
    private int numberOfPermutation;
    
    /**
     * Default constructor.
     */
    public PGMImpactAnalysisTask() {
    }
    
    public int getNumberOfPermutation() {
        return numberOfPermutation;
    }

    public void setNumberOfPermutation(int numberOfPermutation) {
        this.numberOfPermutation = numberOfPermutation;
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
    
    private Map<Variable, Double> runPosteriorInference(Observation<Number> observation,
                                                        ObservationHelper observationHelper,
                                                        Map<String, Map<Variable, double[]>> dataTypeToVarToPrior,
                                                        Set<Variable> fiGeneVariables) throws InferenceCannotConvergeException {
        // Split the observation into multiple ones according to data types
        List<Observation<Number>> splitedObservations = observationHelper.splitObservationIntoMutationAndNonMutation(observation);
        Map<Variable, Double> varToScore = new HashMap<Variable, Double>();
        for (Observation<Number> dataObs : splitedObservations) {
            lbp.setObservation(dataObs);
            //        lbp.setUpdateViaFactors(true);
            lbp.runInference();
            String dataType = null;
            if (isMutationObservation(dataObs))
                dataType = DataType.Mutation.toString();
            else
                dataType = NON_MUTATION_DATA_KEY;
            Map<Variable, double[]> varToPrior = dataTypeToVarToPrior.get(dataType);
            for (Variable var : fiGeneVariables) {
             // There is no need to check this. This has been checked during generation of fiGeneVariables.
//                if (var instanceof ContinuousVariable)
//                    continue;
//                String name = var.getName();
//                if (name.contains("_"))
//                    continue;
                double[] belief = var.getBelief();
                double[] prior = varToPrior.get(var);
                double logRatio = Math.log10(belief[1] * prior[0] / (belief[0] * prior[1]));
                Double oldValue = varToScore.get(var);
                if (oldValue == null)
                    varToScore.put(var, logRatio);
                else
                    varToScore.put(var, oldValue + logRatio);
            }
        }
        return varToScore;
    }
    
    private Map<String, Map<Variable, double[]>> runPriorInference(List<Observation<Number>> observations,
                                                                   ObservationHelper helper,
                                                                   Set<Variable> fiGeneVariables) throws InferenceCannotConvergeException {
        // We want to perform impact inference based two data types: mutation and non-mutation
        Map<String, Map<Variable, double[]>> dataTypeToVarToPrior = new HashMap<String, Map<Variable,double[]>>();
        Observation<Number> baseObs = helper.createBaseObservation(observations,
                                                                   null,
                                                                   0);
        List<Observation<Number>> splitedBasedObservations = helper.splitObservationIntoMutationAndNonMutation(baseObs);
        for (Observation<Number> obs : splitedBasedObservations) {
            Map<Variable, double[]> varToPrior = new HashMap<Variable, double[]>();
            String dataType = null;
            if (isMutationObservation(obs))
                dataType = DataType.Mutation.toString();
            else
                dataType = NON_MUTATION_DATA_KEY;
            dataTypeToVarToPrior.put(dataType, varToPrior);
            
            lbp.setObservation(baseObs);
            lbp.runInference();
            for (Variable var : fiGeneVariables) {
                // There is no need to check this. This has been checked during generation of fiGeneVariables.
//                if (var instanceof ContinuousVariable)
//                    continue; // Don't support this query.
                double[] belief = var.getBelief();
                double[] prior = new double[belief.length];
                System.arraycopy(belief, 0, prior, 0, belief.length);
                varToPrior.put(var, prior);
            }
        }
        return dataTypeToVarToPrior;
    }
    
    /**
     * Assume the passed Observation has been split into mutation and non-mutation. Otherwise,
     * the implementation for quick check is not right in this method.
     * @param obs
     * @return
     */
    private boolean isMutationObservation(Observation<Number> obs) {
        // Just pick one VariableAssignement
        VariableAssignment<Number> varAssgn = obs.getVariableAssignments().iterator().next();
        Variable var = varAssgn.getVariable();
        if (var.getName().endsWith("_" + DataType.Mutation))
            return true;
        return false;
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
        FactorGraph fg = null;
        FIPGMConstructor constructor = null;
        try {
            fetchFIs(); // Need to get all FIs to construct the FI pgm model.
            constructor = getPGMConstructor();
            progPane.setText("Constructing the PGM...");
            fg = constructor.constructFactorGraph(pgmType);
        }
        catch(Exception e) { // Stop if any exception is thrown.
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot build a graphical model from the Reactome FI network:\n" + e.getMessage(),
                                          "Error in Constructing Model",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
            return;
        }
        if (fg == null || constructor == null) {
            frame.getGlassPane().setVisible(false);
            return;
        }
        progPane.setText("Generating random observations...");
        List<Observation<Number>> observations = constructor.getObservationLoader().getObservations();
        // Generate random observations
        ObservationRandomizer randomizer = new ObservationRandomizer();
        randomizer.setNumberOfPermutation(numberOfPermutation);
        List<Observation<Number>> randomObservations = randomizer.createRandomObservations(observations);
        
        lbp.setFactorGraph(fg);
        Set<Variable> fiGeneVariables = getFIGeneVariables(fg);
        ObservationHelper helper = new ObservationHelper();
        Map<String, Map<Variable, double[]>> dataTypeToVarToPrior;
        long time1 = System.currentTimeMillis();
        try {
            progPane.setText("Running prior inference...");
            dataTypeToVarToPrior = runPriorInference(observations, 
                                                     helper, 
                                                     fiGeneVariables);
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
        
        // A list of observations that cannot converge
        List<String> notConvergedObs = new ArrayList<String>();
        Map<String, Map<Variable, Double>> sampleToVarToResult = runPosteriorInferences(observations, 
                                                                                        fiGeneVariables,
                                                                                        helper, 
                                                                                        dataTypeToVarToPrior,
                                                                                        runningTime,
                                                                                        notConvergedObs,
                                                                                        progPane);
        progPane.setText("Running random samples...");
        List<String> randomNotConvergedObs = new ArrayList<>();
        Map<String, Map<Variable, Double>> randomSampleToVarToResult = runPosteriorInferences(randomObservations, 
                                                                                        fiGeneVariables,
                                                                                        helper, 
                                                                                        dataTypeToVarToPrior,
                                                                                        runningTime,
                                                                                        notConvergedObs,
                                                                                        progPane);
        if (notConvergedObs.size() > 0) {
            showNoConvergeInfo(notConvergedObs,
                               randomNotConvergedObs);
        }
        showResults(sampleToVarToResult,
                    randomSampleToVarToResult,
                    progPane);
        frame.getGlassPane().setVisible(false);
    }

    private Map<String, Map<Variable, Double>> runPosteriorInferences(List<Observation<Number>> observations,
                                                                      Set<Variable> fiGeneVariables,
                                                                      ObservationHelper helper,
                                                                      Map<String, Map<Variable, double[]>> dataTypeToVarToPrior,
                                                                      double runningTime,
                                                                      List<String> notConvergedObs,
                                                                      ProgressPane progPane) {
        progPane.setMaximum(observations.size());
        progPane.setMinimum(0);
        progPane.setValue(0);
        progPane.setIndeterminate(false);
        int count = 0;
        long time1, time2;
        Map<String, Map<Variable, Double>> sampleToVarToResult = new HashMap<>();
        for (Observation<Number> observation : observations) {
            showInferenceText(count, observations.size(), runningTime, observation, progPane);
            time1 = System.currentTimeMillis(); // get a better time
            progPane.setValue(++ count);
            Map<Variable, Double> varToResult = null;
            try {
                varToResult = runPosteriorInference(observation, 
                                                    helper,
                                                    dataTypeToVarToPrior,
                                                    fiGeneVariables);
            }
            catch(InferenceCannotConvergeException e) {
                notConvergedObs.add(observation.getName());
                continue; // Just escape it
            }
            time2 = System.currentTimeMillis();
            System.out.println("Time for " + observation.getName() + ": " + (time2 - time1) / 1000.0d + " seconds");
            // Average the original runningTime to get a better estimation
            runningTime = ((time2 - time1) + runningTime) / 2.0d;
            sampleToVarToResult.put(observation.getName(), varToResult);
            if (sampleToVarToResult.size() > 3)
                break; // This is test code
        }
        return sampleToVarToResult;
    }

    private void showNoConvergeInfo(List<String> notConvergedObs,
                                    List<String> randomNotConveregedSamples) {
        // Show a warning message
        StringBuilder builder = new StringBuilder();
        if (randomNotConveregedSamples.size() > 0) {
            builder.append("The inference for " + randomNotConveregedSamples + " random samples cannot converge. ");
        }
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
    
    /**
     * Show inference results
     * @param sampleToVarToResult
     */
    private void showResults(Map<String, Map<Variable, Double>> sampleToVarToResult,
                             Map<String, Map<Variable, Double>> randomSampleToVarToResult,
                             ProgressPane progressPane) {
        progressPane.setText("Selecting genes...");
        progressPane.setIndeterminate(true);
        PGMImpactAnalysisResultDialog dialog = new PGMImpactAnalysisResultDialog();
        dialog.setSampleResults(sampleToVarToResult,
                                randomSampleToVarToResult);
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
    
    private void fetchFIs() throws Exception {
        FINetworkService networkService = FIPlugInHelper.getHelper().getNetworkService();
        Set<String> fis = networkService.queryAllFIs();
        FIPGMConfiguration config = PlugInObjectManager.getManager().getFIPGMConfig();
        config.setFIs(fis);
    }
    
    private Set<Variable> getFIGeneVariables(FactorGraph fg) {
        Set<Variable> variables = new HashSet<Variable>();
        Set<String> fiGenes = getFIGenes();
        for (Variable var : fg.getVariables()) {
            if (var instanceof ContinuousVariable)
                continue;
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
                    handler = new CyMutationEmpiricalFactorHandler();
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
