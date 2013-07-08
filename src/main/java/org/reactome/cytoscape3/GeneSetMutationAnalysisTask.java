package org.reactome.cytoscape3;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.reactome.cancer.CancerAnalysisUtilitites;
import org.reactome.cancer.MATFileLoader;



import org.reactome.r3.util.FileUtility;
import org.reactome.r3.util.InteractionUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;






public class GeneSetMutationAnalysisTask extends AbstractTask
{
    private CySwingApplication desktopApp;
    private TaskMonitor tm;
    private String format;
    private File file;
    private boolean chooseHomoGenes;
    private boolean useLinkers;
    private boolean showUnlinked;
    private boolean showUnlinkedEnabled;
    private boolean fetchFIAnnotations;
    private int sampleCutoffValue;
    private CyNetworkFactory networkFactory;
    private CyNetworkViewFactory viewFactory;
    private CyNetworkViewManager viewManager;
    private CyNetworkManager netManager;
    public GeneSetMutationAnalysisTask(CySwingApplication desktopApp,
	    	String format, File file, boolean chooseHomoGenes,
	    	boolean useLinkers, int sampleCutoffValue,
	        boolean showUnlinked,
	        boolean showUnlinkedEnabled,
	        boolean fetchFIAnnotations,
	    	CyNetworkFactory networkFactory,
	    	CyNetworkManager netManager,
	    	CyNetworkViewFactory viewFactory,
	    	CyNetworkViewManager viewManager)
    {
	this.desktopApp = desktopApp;
	this.networkFactory = networkFactory;
	this.netManager = netManager;
	this.chooseHomoGenes = chooseHomoGenes;
	this.useLinkers = useLinkers;
	this.showUnlinked = showUnlinked;
	this.format = format;
	this.file = file;
	this.sampleCutoffValue = sampleCutoffValue;
	this.showUnlinkedEnabled = showUnlinkedEnabled;
	this.viewFactory = viewFactory;
	this.viewManager = viewManager;
	this.fetchFIAnnotations = fetchFIAnnotations;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception
    {
	desktopApp.getJFrame().getGlassPane().setVisible(true);
	taskMonitor.setTitle("Gene Set / Mutation Analysis");
	taskMonitor.setStatusMessage("Loading file...");
	try
	{
	    Map<String, Integer> geneToSampleNumber = null;
            Map<String, String> geneToSampleString = null;
            Map<String, Set<String>> sampleToGenes = null;
            Set<String> selectedGenes = null;

            if (format.equals("MAF")){
                
        	sampleToGenes = new MATFileLoader().loadSampleToGenes(file.getAbsolutePath(), 
                                                                              chooseHomoGenes);
        	selectedGenes = CancerAnalysisUtilitites.selectGenesInSamples(sampleCutoffValue, 
                        sampleToGenes);
            }
            else if (format.equals("GeneSample")) {
                geneToSampleNumber = new HashMap<String, Integer>();
                geneToSampleString = new HashMap<String, String>();
                loadGeneSampleFile(file,
                                   geneToSampleNumber,
                                   geneToSampleString);
                selectedGenes = selectGenesBasedOnSampleCutoff(geneToSampleNumber,
                                                               sampleCutoffValue);
            }
            else if (format.equals("GeneSet")) {
                selectedGenes = loadGeneSetFile(file);
            }
            //Check if it is possible to construct the network
            //given the sample size.
            if (useLinkers)
            {
        	taskMonitor.setStatusMessage("Checking FI Network size...");
        	//FINetworkService fiService = PlugInScopeObjectManager.getManager().getNetworkService();
        	//Integer cutoff = fiService.getNetworkBuildSizeCutoff();
        	Integer cutoff = 1000;
        	if (cutoff != null && selectedGenes.size() >= cutoff) {
                    JOptionPane.showMessageDialog(desktopApp.getJFrame(), 
                                                  "The size of the gene set is too big. Linker genes should not be used!\n" +
                                                  "Please try again without using linker genes.",
                                                  "Error in Building Network",
                                                  JOptionPane.ERROR_MESSAGE);
                    desktopApp.getJFrame().getGlassPane().setVisible(false);
                    return;
                }
            }

             CytoPanel controlPane = desktopApp.getCytoPanel(CytoPanelName.WEST);
             int selectedIndex = controlPane.getSelectedIndex();
             taskMonitor.setStatusMessage("Constructing FI Network...");
            CyNetwork network = constructFINetwork(selectedGenes,
                    file.getName());
            if (network == null){
        	JOptionPane.showMessageDialog(desktopApp.getJFrame(),
                        "Cannot find any functional interaction among provided genes.\n" +
                        "No network can be constructed.\n" +
                        "Note: only human gene names are supported.",
                        "Empty Network",
                        JOptionPane.INFORMATION_MESSAGE);
        	desktopApp.getJFrame().getGlassPane().setVisible(false);
        	return;
            }
            controlPane.setSelectedIndex(selectedIndex);
            if (sampleToGenes != null)
            {
        	geneToSampleNumber = new HashMap<String, Integer>();
        	geneToSampleString = new HashMap<String, String>();
        	Map<String, Set<String>> geneToSamples = InteractionUtilities.switchKeyValues(sampleToGenes);
                geneToSamples.keySet().retainAll(selectedGenes);
                for (String gene : geneToSamples.keySet())
                {
                    Set<String> samples = geneToSamples.get(gene);
                    geneToSampleNumber.put(gene, samples.size());
                    geneToSampleString.put(gene, InteractionUtilities.joinStringElements(";", samples));
                }
            }
            taskMonitor.setStatusMessage("Formatting network attributes...");
            CyTableManager tableManager = new CyTableManager();
            //Collection<CyNetworkView> views = viewManager.getNetworkViews(network);
            CyNetworkView view = viewFactory.createNetworkView(network);
            if (geneToSampleNumber != null)
        	tableManager.loadNodeAttributes(view, "SampleNumber", geneToSampleNumber);
            if (geneToSampleString != null)
        	tableManager.loadNodeAttributes(view, "Samples", geneToSampleNumber);
            //Check if linker genes are to be used.
            if (useLinkers)
            {
                taskMonitor.setStatusMessage("Fetching linker genes...");
                Map<Long, Boolean> geneToIsLinker = new HashMap<Long, Boolean>();
                for (Iterator<?> it = network.getNodeList().iterator(); it.hasNext();)
                {
                    CyNode node = (CyNode) it.next();
                    Long suid = node.getSUID();
                    geneToIsLinker.put(suid, !selectedGenes.contains(suid));
                }
                tableManager.loadNodeAttributes(view, "isLinker", geneToSampleNumber);
        	
        	if (fetchFIAnnotations)
        	{
        	    taskMonitor.setStatusMessage("Fetching FI annotations...");
        	    new FIAnnotationHelper().annotateFIs(view, new RESTFulFIService(),
        		    tableManager);
        	}
            }
	}
	catch (Exception e){
	    JOptionPane.showMessageDialog(desktopApp.getJFrame(),
                    "Error in Loading File: " + e.getMessage(),
                    "Error in Loading",
                    JOptionPane.ERROR_MESSAGE);
	    desktopApp.getJFrame().getGlassPane().setVisible(false);
	}
	desktopApp.getJFrame().getGlassPane().setVisible(false);
    }
    
private void loadGeneSampleFile(File file,
            Map<String, Integer> geneToSampleNumber,
            Map<String, String> geneToSampleString) throws IOException {
    FileUtility fu = new FileUtility();
    fu.setInput(file.getAbsolutePath());
    // The first line should be a header line
    String line = fu.readLine();
    // Check if the format is correct
    String[] tokens = line.split("\t");
    if (tokens.length != 2 &&
	    tokens.length != 3) {
	throw new IllegalArgumentException("Wrong file format. Gene/sample number format should have two or\n" +
	                           "three columns: gene, sample number and an optional sample column.");
    }
    while ((line = fu.readLine()) != null) {
	tokens = line.split("\t");
	geneToSampleNumber.put(tokens[0],
		new Integer(tokens[1]));
	if (tokens.length > 2)
	    geneToSampleString.put(tokens[0],
		    tokens[2]);
    }
    fu.close();
	}
private Set<String> selectGenesBasedOnSampleCutoff(Map<String, Integer> geneToSampleNumber,
		int sampleCutoff) {
    Set<String> selectedGenes = new HashSet<String>();
    for (String gene : geneToSampleNumber.keySet()) {
	Integer number = geneToSampleNumber.get(gene);
	if (number >= sampleCutoff)
	    selectedGenes.add(gene);
    }
    return selectedGenes;
	}
private Set<String> loadGeneSetFile(File file) throws IOException
	{
    	Set<String> genes = new HashSet<String>();
    	FileUtility fu = new FileUtility();
    	fu.setInput(file.getAbsolutePath());
    	String line = fu.readLine();
    	String[] tokens = line.split("\t");
    	if (tokens.length != 1) {
    	    throw new IllegalArgumentException("Wrong file format.\nGeneset format should have only one column and have no header.");
    	}
    	genes.add(line.trim());
    	while ((line = fu.readLine()) != null)
    	    genes.add(line.trim());
    	fu.close();
    	return genes;
	}
private CyNetwork constructFINetwork(Set<String> selectedGenes, String title) throws Exception
{
 // Check if a local service should be used
    FINetworkService fiService = PlugInScopeObjectManager.getManager().getNetworkService();
    Set<String> fis = fiService.buildFINetwork(selectedGenes, 
            useLinkers);
    CyNetwork network = null;
    if (fis != null && fis.size() > 0)
    {
        
        CyNetworkGenerator generator = new CyNetworkGenerator(networkFactory);
        // Check if any unlinked nodes should be added
        if (showUnlinkedEnabled && showUnlinked)
            network = generator.constructFINetwork(selectedGenes, fis, title);
        else
            network = generator.constructFINetwork(fis, title);
      }
    netManager.addNetwork(network);
    System.out.println(network);
    CyNetworkView view = viewFactory.createNetworkView(network);
    viewManager.addNetworkView(view);
    System.out.println("Done.");
    CyTableManager manager = new CyTableManager();
    //manager.storeDataSetType(network, "Data Set");
    return network;
}
}
