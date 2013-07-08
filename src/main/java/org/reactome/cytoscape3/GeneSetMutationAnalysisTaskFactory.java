package org.reactome.cytoscape3;

import java.io.File;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;



public class GeneSetMutationAnalysisTaskFactory extends AbstractTaskFactory
{
    private TaskMonitor tm;
    private String format;
    private File file;
    private boolean chooseHomoGenes;
    private CySwingApplication desktopApp;
    private int sampleCutoffValue;
    private boolean useLinkers;
    private boolean showUnlinked;
    private boolean showUnlinkedEnabled;
    private boolean fetchFIAnnotations;
    private CyNetworkFactory networkFactory;
    private CyNetworkViewFactory viewFactory;
    private CyNetworkViewManager viewManager;
    private CyNetworkManager netManager;


    public GeneSetMutationAnalysisTaskFactory(CySwingApplication desktopApp,
	    String format, File file, boolean chooseHomoGenes, boolean useLinkers,
	    boolean showUnlinked, boolean showUnlinkedEnabled, boolean fetchFIAnnotations,
	    int sampleCutoffValue, CyNetworkFactory networkFactory,
	    CyNetworkManager netManager,
	    CyNetworkViewFactory viewFactory,
	    CyNetworkViewManager viewManager)
    {
	this.desktopApp = desktopApp;

	this.format = format;
	this.file = file;
	this.chooseHomoGenes = chooseHomoGenes;
	this.useLinkers = useLinkers;
	this.showUnlinked = showUnlinked;
	this.showUnlinkedEnabled = showUnlinkedEnabled;
	this.fetchFIAnnotations = fetchFIAnnotations;
	this.sampleCutoffValue = sampleCutoffValue;
	this.networkFactory = networkFactory;
	this.netManager = netManager;
	this.viewFactory = viewFactory;
	this.viewManager = viewManager;
    }


    @Override
    public TaskIterator createTaskIterator()
    {
	return new TaskIterator(new GeneSetMutationAnalysisTask(desktopApp,
	    	format, file, chooseHomoGenes,
	    	useLinkers, sampleCutoffValue,
	        showUnlinked,
	        showUnlinkedEnabled,
	        fetchFIAnnotations,
	    	networkFactory,
	    	netManager,
	    	viewFactory,
	    	viewManager));
    }}
