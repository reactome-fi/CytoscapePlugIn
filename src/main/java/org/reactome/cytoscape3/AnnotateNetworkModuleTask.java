package org.reactome.cytoscape3;

import java.util.List;
import java.util.Map;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.gk.util.ProgressPane;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotateNetworkModuleTask extends AbstractTask {
    private final Logger logger = LoggerFactory.getLogger(AnnotateNetworkModuleTask.class);
    protected CyNetworkView view;
    protected String type;
    // A flag to indicate if an interactive GUI is needed
    private boolean avoidGUIs;
    // Indicate if it is done
    protected boolean isAborted;
    
    public AnnotateNetworkModuleTask(CyNetworkView view, String type) {
        this.view = view;
        this.type = type;
    }

    public void setAvoidGUIs(boolean avoidGUIs) {
        this.avoidGUIs = avoidGUIs;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Annotate Network Modules");
        taskMonitor.setStatusMessage("Annotating network modules...");
        taskMonitor.setProgress(0.0d);
        annotateNetworkModules();
        taskMonitor.setProgress(1.0d);
    }
    
    public void annotateNetworkModules() {
        NetworkModuleHelper helper = new NetworkModuleHelper();
        helper.setAvoidGUIs(avoidGUIs);
        Map<String, Integer> nodeToModule = helper.extractNodeToModule(view);
        if (nodeToModule == null || nodeToModule.isEmpty()) {
            isAborted = true;
            return;
        }
        ProgressPane progPane = new ProgressPane();
        progPane.setIndeterminate(true);
        progPane.setText("Annotating modules...");
        PlugInObjectManager.getManager().getCytoscapeDesktop().setGlassPane(progPane);
        PlugInObjectManager.getManager().getCytoscapeDesktop().getGlassPane().setVisible(true);
        try {
            RESTFulFIService fiService = new RESTFulFIService(view);
            List<ModuleGeneSetAnnotation> annotations = fiService.annotateNetworkModules(nodeToModule, type);
            ResultDisplayHelper.getHelper().displayModuleAnnotations(annotations, view, type, true);
        }
        catch (Exception e) {
            if (avoidGUIs) {
                logger.error("Error in annotating modules: " + e.getMessage(), e);
                isAborted = true;
            }
            else {
                PlugInUtilities.showErrorMessage("Error in Annotating Modules", "Please see the logs for details.");
                e.printStackTrace();
            }
        }
        progPane.setIndeterminate(false);
        PlugInObjectManager.getManager().getCytoscapeDesktop().getGlassPane().setVisible(false);
    }

}
