package org.reactome.cytoscape3;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.gk.util.ProgressPane;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cancer.base.MATFileLoader;
import org.reactome.cytoscape.service.FIAnalysisTask;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FINetworkService;
import org.reactome.cytoscape.service.FINetworkServiceFactory;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.TableFormatter;
import org.reactome.cytoscape.service.TableFormatterImpl;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.r3.util.FileUtility;
import org.reactome.r3.util.InteractionUtilities;

/**
 * Performs Gene Set/Mutation Analysis on a given input file and parameters
 * provided by an ActionDialog. This is not a thread-safe class, and should not
 * be cached.
 * 
 * @author Eric T. Dawson
 */
public class GeneSetMutationAnalysisTask extends FIAnalysisTask {
    private String format;
    
    private File file;
    
    // For manually entered genes
    private String enteredGenes;
    
    private boolean chooseHomoGenes;
    
    private boolean useLinkers;
    
    private boolean showUnlinked;
    
    private boolean showUnlinkedEnabled;
    
    private boolean fetchFIAnnotations;
    
    private int sampleCutoffValue;
    
    public GeneSetMutationAnalysisTask(GeneSetMutationAnalysisDialog gui) {
        
        this.chooseHomoGenes = gui.chooseHomoGenes();
        this.useLinkers = gui.useLinkers();
        this.showUnlinked = gui.getUnlinkedGeneBox().isSelected();
        this.format = gui.getFileFormat();
        this.file = gui.getSelectedFile();
        this.enteredGenes = gui.getEnteredGenes();
        this.sampleCutoffValue = gui.getSampleCutoffValue();
        this.showUnlinkedEnabled = gui.getUnlinkedGeneBox().isEnabled();
        this.fetchFIAnnotations = gui.shouldFIAnnotationsBeFetched();
    }
    
    public GeneSetMutationAnalysisTask(GeneSetMutationAnalysisOptions options) {
        this.chooseHomoGenes = options.isChooseHomoGenes();
        this.useLinkers = options.isUseLinkers();
        this.showUnlinked = options.isShowUnlinked();
        this.format = options.getFormat();
        if (options.getFile() != null && !options.getFile().trim().equals("null")) // If a file is provided, we will use the file
            this.file = new File(options.getFile());
        else
            this.enteredGenes = options.getEnteredGenes();
        this.sampleCutoffValue = options.getSampleCutoffValue();
        this.showUnlinked = options.isShowUnlinked();
        this.fetchFIAnnotations = options.isFetchFIAnnotations();
        // Set the version
        if (options.getFiVersion() != null)
            PlugInObjectManager.getManager().setFiNetworkVersion(options.getFiVersion());
    }
    
    @Override
    protected void doAnalysis() {
        ProgressPane progPane = new ProgressPane();
        progPane.setMinimum(1);
        progPane.setMaximum(100);
        progPane.setTitle("Gene Set/Mutation Analysis");
        progPane.setText("Loading gene set...");
        progPane.setValue(25);
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        frame.setGlassPane(progPane);
        frame.getGlassPane().setVisible(true);
        try {
            Map<String, Integer> geneToSampleNumber = null;
            Map<String, String> geneToSampleString = null;
            Map<String, Set<String>> sampleToGenes = null;
            Set<String> selectedGenes = null;
            
            if (format.equals("MAF")) {
                sampleToGenes = new MATFileLoader().loadSampleToGenes(file.getAbsolutePath(), chooseHomoGenes);
                selectedGenes = selectGenesInSamples(sampleCutoffValue, sampleToGenes);
            }
            else if (format.equals("GeneSample")) {
                geneToSampleNumber = new HashMap<String, Integer>();
                geneToSampleString = new HashMap<String, String>();
                loadGeneSampleFile(file, geneToSampleNumber, geneToSampleString);
                selectedGenes = selectGenesBasedOnSampleCutoff(geneToSampleNumber, sampleCutoffValue);
            }
            else if (format.equals("GeneSet")) {
                selectedGenes = loadGeneSetFile(enteredGenes, file);
            }
            // Check if it is possible to construct the network
            // given the number of selected genes when userLinker = true
            if (useLinkers) {
                progPane.setText("Checking FI Network size...");
                FIPlugInHelper r = FIPlugInHelper.getHelper();
                FINetworkService fiService = new FINetworkServiceFactory().getFINetworkService();
                Integer cutoff = fiService.getNetworkBuildSizeCutoff();
                if (cutoff != null && selectedGenes.size() >= cutoff) {
                    JOptionPane.showMessageDialog(frame,
                                                  "The size of the gene set is too big. Linker genes should not be used!\n"
                                                          + "Please try again without using linker genes.",
                                                  "Error in Building Network", JOptionPane.ERROR_MESSAGE);
                    frame.getGlassPane().setVisible(false);
                    return;
                }
            }
            
            progPane.setIndeterminate(true);
            progPane.setText("Constructing FI network...");
            CyNetwork network = constructFINetwork(selectedGenes);
            if (network == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot construct a FI network: No interaction cannot be found.",
                                              "Empty Network", JOptionPane.INFORMATION_MESSAGE);
                frame.getGlassPane().setVisible(false);
                return;
            }
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            TableFormatter tableFormatter = (TableFormatter) context.getService(tableFormatterServRef);
            tableFormatter.makeGeneSetMutationAnalysisTables(network);
            String networkName = (file == null ? "Untitled" : file.getName());
            network.getDefaultNetworkTable().getRow(network.getSUID()).set("name", networkName);
            if (network == null || network.getNodeCount() <= 0) {
                JOptionPane.showMessageDialog(frame, "Cannot find any functional interaction among provided genes.\n"
                        + "No network can be constructed.\n" + "Note: only human gene names are supported.",
                                              "Empty Network", JOptionPane.INFORMATION_MESSAGE);
                frame.getGlassPane().setVisible(false);
                return;
            }
            CyNetworkManager netManager = (CyNetworkManager) context.getService(netManagerRef);
            netManager.addNetwork(network);
            
            // Fix for broken default value persistence in CyTables
            // Should be remedied in the 3.1 api
            CyTable nodeTable = network.getDefaultNodeTable();
            for (CyNode node : network.getNodeList()) {
                Long nodeSUID = node.getSUID();
                nodeTable.getRow(nodeSUID).set("isLinker", false);
            }
            
            if (sampleToGenes != null) {
                geneToSampleNumber = new HashMap<String, Integer>();
                geneToSampleString = new HashMap<String, String>();
                Map<String, Set<String>> geneToSamples = InteractionUtilities.switchKeyValues(sampleToGenes);
                geneToSamples.keySet().retainAll(selectedGenes);
                for (String gene : geneToSamples.keySet()) {
                    Set<String> samples = geneToSamples.get(gene);
                    geneToSampleNumber.put(gene, samples.size());
                    geneToSampleString.put(gene, InteractionUtilities.joinStringElements(";", samples));
                }
            }
            progPane.setText("Loading network attributes...");
            TableHelper tableHelper = new TableHelper();
            CyNetworkViewFactory viewFactory = (CyNetworkViewFactory) context.getService(viewFactoryRef);
            CyNetworkView view = viewFactory.createNetworkView(network);
            tableHelper.storeFINetworkVersion(network, PlugInObjectManager.getManager().getFiNetworkVersion());
            tableHelper.storeDataSetType(network, TableFormatterImpl.getSampleMutationData());
            tableHelper.markAsReactomeNetwork(network);
            CyNetworkViewManager viewManager = (CyNetworkViewManager) context.getService(viewManagerRef);
            viewManager.addNetworkView(view);
            if (geneToSampleNumber != null && !geneToSampleNumber.isEmpty()) {
                tableHelper.storeNodeAttributesByName(view, "sampleNumber", geneToSampleNumber);
            }
            if (geneToSampleString != null && !geneToSampleString.isEmpty()) {
                tableHelper.storeNodeAttributesByName(view, "samples", geneToSampleString);
            }
            // Check if linker genes are to be used.
            if (useLinkers) {
                progPane.setText("Fetching linker genes...");
                Map<String, Boolean> geneToIsLinker = new HashMap<String, Boolean>();
                for (CyNode node : network.getNodeList()) {
                    Long suid = node.getSUID();
                    String nodeName = network.getDefaultNodeTable().getRow(suid).get("name", String.class);
                    geneToIsLinker.put(nodeName, !selectedGenes.contains(nodeName));
                }
                tableHelper.storeNodeAttributesByName(view, "isLinker", geneToIsLinker);
            }
            if (fetchFIAnnotations) {
                progPane.setText("Fetching FI annotations...");
                new FINetworkGenerator().annotateFIs(view);
            }
            ServiceReference visHelperRef = context.getServiceReference(FIVisualStyle.class.getName());
            if (visHelperRef != null) {
                FIVisualStyle styleHelper = (FIVisualStyle) context.getService(visHelperRef);
                styleHelper.setVisualStyle(view);
                styleHelper.doLayout();
            }

            progPane.setIndeterminate(false);
            progPane.setValue(100);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error in Loading File: " + e.getMessage(), "Error in Loading",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        frame.getGlassPane().setVisible(false);
    }
    
    private void loadGeneSampleFile(File file, Map<String, Integer> geneToSampleNumber,
                                    Map<String, String> geneToSampleString) throws IOException {
        FileUtility fu = new FileUtility();
        fu.setInput(file.getAbsolutePath());
        // The first line should be a header line
        String line = fu.readLine();
        // Check if the format is correct
        String[] tokens = line.split("\t");
        if (tokens.length != 2 && tokens.length != 3)
            throw new IllegalArgumentException("Wrong file format. Gene/sample number format should have two or\n"
                    + "three columns: gene, sample number and an optional sample column.");
        while ((line = fu.readLine()) != null) {
            tokens = line.split("\t");
            geneToSampleNumber.put(tokens[0], new Integer(tokens[1]));
            if (tokens.length > 2) {
                geneToSampleString.put(tokens[0], tokens[2]);
            }
        }
        fu.close();
    }
    
    private Set<String> selectGenesBasedOnSampleCutoff(Map<String, Integer> geneToSampleNumber, int sampleCutoff) {
        Set<String> selectedGenes = new HashSet<String>();
        for (String gene : geneToSampleNumber.keySet()) {
            Integer number = geneToSampleNumber.get(gene);
            if (number >= sampleCutoff) {
                selectedGenes.add(gene);
            }
        }
        return selectedGenes;
    }
    
    private Set<String> loadGeneSetFile(String enteredGenes, File file) throws IOException {
        Set<String> genes = new HashSet<String>();
        if (enteredGenes != null && enteredGenes.length() > 0) {
            String[] lines = enteredGenes.split("\n");
            for (String line : lines)
                genes.add(line.trim());
        }
        else if (file != null) { // Should handle file
            FileUtility fu = new FileUtility();
            fu.setInput(file.getAbsolutePath());
            String line = fu.readLine();
            String[] tokens = line.split("\t");
            if (tokens.length != 1)
                throw new IllegalArgumentException(
                        "Wrong file format.\nGeneset format should have only one column and have no header.");
            genes.add(line.trim());
            while ((line = fu.readLine()) != null) {
                genes.add(line.trim());
            }
            fu.close();
        }
        return genes;
    }
    
    private CyNetwork constructFINetwork(Set<String> selectedGenes) throws Exception {
        // Check if a local service should be used
        FINetworkService fiService = new FINetworkServiceFactory().getFINetworkService();
        Set<String> fis = fiService.buildFINetwork(selectedGenes, useLinkers);
        CyNetwork network = null;
        if (fis != null && fis.size() > 0) {
            
            FINetworkGenerator generator = new FINetworkGenerator();
            // Check if any unlinked nodes should be added
            if (showUnlinkedEnabled && showUnlinked) {
                network = generator.constructFINetwork(selectedGenes, fis);
            }
            else {
                network = generator.constructFINetwork(fis);
            }
        }
        // netManager.addNetwork(network);
        // TableHelper manager = new TableHelper();
        // manager.storeDataSetType(network, "Data Set");
        return network;
    }
    
    /**
     * This method is used to select genes in samples based on a sample cutoff value.
     * @param sampleNumber
     * @param sampleToGenes
     * @return
     */
    private Set<String> selectGenesInSamples(int sampleNumber, Map<String, Set<String>> sampleToGenes) {
        Map<String, Set<String>> geneToSamples = InteractionUtilities.switchKeyValues(sampleToGenes);
        Set<String> rtn = new HashSet<String>();
        for (String gene : geneToSamples.keySet()) {
            Set<String> samples = geneToSamples.get(gene);
            if (samples.size() >= sampleNumber)
                rtn.add(gene);
        }
        return rtn;
    }
}
