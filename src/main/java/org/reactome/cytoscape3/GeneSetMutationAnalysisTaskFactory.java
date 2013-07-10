package org.reactome.cytoscape3;

import java.io.File;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
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
    private VisualMappingManager visMapManager;
    private CyLayoutAlgorithmManager layoutManager;
    private VisualStyleFactory visStyleFactory;
    private VisualMappingFunctionFactory visMapFuncFactoryP;
    private VisualMappingFunctionFactory visMapFuncFactoryC;
    private VisualMappingFunctionFactory visMapFuncFactoryD;
    private TaskManager taskManager;


    public GeneSetMutationAnalysisTaskFactory(CySwingApplication desktopApp,
	    String format, File file, boolean chooseHomoGenes, boolean useLinkers,
	    boolean showUnlinked, boolean showUnlinkedEnabled, boolean fetchFIAnnotations,
	    int sampleCutoffValue, CyNetworkFactory networkFactory,
	    CyNetworkManager netManager,
	    CyNetworkViewFactory viewFactory,
	    CyNetworkViewManager viewManager,
	    CyLayoutAlgorithmManager layoutManager,
        VisualMappingManager visMapManager,
        VisualStyleFactory visStyleFactory,
        VisualMappingFunctionFactory visMapFuncFactoryC,
        VisualMappingFunctionFactory visMapFuncFactoryD,
        VisualMappingFunctionFactory visMapFuncFactoryP,
        TaskManager taskManager)
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
	this.visMapManager = visMapManager;
    this.layoutManager = layoutManager;
    this.visStyleFactory = visStyleFactory;
    this.visMapFuncFactoryP = visMapFuncFactoryP;
    this.visMapFuncFactoryC = visMapFuncFactoryC;
    this.visMapFuncFactoryD = visMapFuncFactoryD;
    this.taskManager = taskManager;
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
	    	viewManager, layoutManager, visMapManager,
	    	visStyleFactory, visMapFuncFactoryC, visMapFuncFactoryD, visMapFuncFactoryP, taskManager));
    }}
