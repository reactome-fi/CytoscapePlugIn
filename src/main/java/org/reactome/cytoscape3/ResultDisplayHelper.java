package org.reactome.cytoscape3;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.view.model.CyNetworkView;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cytoscape.service.GeneSetAnnotationPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape3.HotNetAnalysisTask.HotNetModule;

public class ResultDisplayHelper
{
    private static ResultDisplayHelper helper;

    private ResultDisplayHelper()
    {
    }

    public static ResultDisplayHelper getHelper()
    {
        if (helper == null)
        {
            helper = new ResultDisplayHelper();
        }
        return helper;
    }

    /**
     * A helper method to show network modules.
     * 
     * @param nodeToCluster
     * @param view
     */
    protected void showModuleInTab(Map<String, Integer> nodeToCluster,
            Map<String, Set<String>> nodeToSampleSet, Double modularity,
            CyNetworkView view)
    {
        String title = "Network Module Browser";
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp
                .getCytoPanel(CytoPanelName.SOUTH);
        boolean found = false;
        int numComps = tableBrowserPane.getCytoPanelComponentCount();
        int componentIndex = -1;
        for (int i = 0; i < numComps; i++)
        {
            CytoPanelComponent aComp = (CytoPanelComponent) tableBrowserPane
                    .getComponentAt(i);
            if (aComp.getTitle().equalsIgnoreCase(title))
            {
                found = true;
                componentIndex = i;
                break;
            }
        }
        if (found == false)
        {
            NetworkModuleBrowser browser = new NetworkModuleBrowser();
            browser.setTitle(title);
            if (tableBrowserPane.getState() == CytoPanelState.HIDE)
            {
                tableBrowserPane.setState(CytoPanelState.DOCK);
            }
            int index = tableBrowserPane.indexOfComponent(browser);
            if (index == -1) return;
            componentIndex = index;
            tableBrowserPane.setSelectedIndex(index);
        }
        NetworkModuleBrowser moduleBrowser = (NetworkModuleBrowser) tableBrowserPane
                .getComponentAt(componentIndex);
        tableBrowserPane.setSelectedIndex(componentIndex);
        moduleBrowser.setNetworkView(view);
        moduleBrowser.showModules(nodeToCluster, nodeToSampleSet);
        moduleBrowser.showModularity(modularity);
    }

    protected void showHotnetModulesInTab(List<HotNetModule> modules,
            Map<String, Set<String>> sampleToGenes, CyNetworkView view)
    {
        String title = "HotNet Module Browser";
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp
                .getCytoPanel(CytoPanelName.SOUTH);
        boolean found = false;
        int numComps = tableBrowserPane.getCytoPanelComponentCount();
        int componentIndex = -1;
        for (int i = 0; i < numComps; i++)
        {
            CytoPanelComponent aComp = (CytoPanelComponent) tableBrowserPane
                    .getComponentAt(i);
            if (aComp.getTitle().equalsIgnoreCase(title))
            {
                found = true;
                componentIndex = i;
                break;
            }
        }
        if (found == false)
        {
            HotNetModuleBrowser hotnetBrowser = new HotNetModuleBrowser();
            hotnetBrowser.setTitle(title);
            if (tableBrowserPane.getState() == CytoPanelState.HIDE)
            {
                tableBrowserPane.setState(CytoPanelState.DOCK);
            }
            int index = tableBrowserPane.indexOfComponent(hotnetBrowser);
            if (index == -1) return;
            componentIndex = index;
            tableBrowserPane.setSelectedIndex(index);
        }
        HotNetModuleBrowser moduleBrowser = (HotNetModuleBrowser) tableBrowserPane
                .getComponentAt(componentIndex);
        tableBrowserPane.setSelectedIndex(componentIndex);
        moduleBrowser.setNetworkView(view);
        moduleBrowser.showHotNetModules(modules, sampleToGenes);

    }

    protected void showMCLModuleInTab(List<Set<String>> clusters,
            Map<Set<String>, Double> clusterToCorr, CyNetworkView view)
    {
        String title = "MCL Module Browser";
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp
                .getCytoPanel(CytoPanelName.SOUTH);
        boolean found = false;
        int numComps = tableBrowserPane.getCytoPanelComponentCount();
        int componentIndex = -1;
        for (int i = 0; i < numComps; i++)
        {
            CytoPanelComponent aComp = (CytoPanelComponent) tableBrowserPane
                    .getComponentAt(i);
            if (aComp.getTitle().equalsIgnoreCase(title))
            {
                found = true;
                componentIndex = i;
                break;
            }
        }
        if (found == false)
        {
            MCLMicroarrayModuleBrowser browser = new MCLMicroarrayModuleBrowser();
            browser.setTitle(title);
            if (tableBrowserPane.getState() == CytoPanelState.HIDE)
            {
                tableBrowserPane.setState(CytoPanelState.DOCK);
            }
            int index = tableBrowserPane.indexOfComponent(browser);
            if (index == -1) return;
            componentIndex = index;
            tableBrowserPane.setSelectedIndex(index);
        }
        MCLMicroarrayModuleBrowser moduleBrowser = (MCLMicroarrayModuleBrowser) tableBrowserPane
                .getComponentAt(componentIndex);
        tableBrowserPane.setSelectedIndex(componentIndex);
        moduleBrowser.setNetworkView(view);
        moduleBrowser.showModules(clusters, clusterToCorr);
    }
 

    public void displayModuleAnnotations(
            List<ModuleGeneSetAnnotation> annotations, CyNetworkView view,
            String type, boolean isForModule)
    {
        String title = null;
        if (isForModule) {
            if (type.equals("Pathway"))
                title = "Pathways in Modules";
            else
                title = "GO " + type + " in Modules";
        }
        else {
            if (type.equals("Pathway"))
                title = "Pathways in Network";
            else
                title = "GO " + type + " in Network";
        }
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp
                .getCytoPanel(CytoPanelName.SOUTH);
        boolean found = false;
        int numComps = tableBrowserPane.getCytoPanelComponentCount();
        int componentIndex = -1;
        for (int i = 0; i < numComps; i++)
        {
            CytoPanelComponent aComp = (CytoPanelComponent) tableBrowserPane
                    .getComponentAt(i);
            if (aComp.getTitle().equalsIgnoreCase(title))
            {
                found = true;
                componentIndex = i;
                break;
            }
        }
        if (found == false)
        {
            GeneSetAnnotationPanel annotationPane = null;
            if (isForModule)
                annotationPane = new GeneSetAnnotationPanelForModules(title);
            else
                annotationPane = new GeneSetAnnotationPanel(title);
            if (tableBrowserPane.getState() == CytoPanelState.HIDE)
            {
                tableBrowserPane.setState(CytoPanelState.DOCK);
            }
            int index = tableBrowserPane.indexOfComponent(annotationPane);
            if (index == -1) return;
            componentIndex = index;
            tableBrowserPane.setSelectedIndex(index);
            tableBrowserPane.setSelectedIndex(componentIndex);
        }
        GeneSetAnnotationPanel annotationPanel = (GeneSetAnnotationPanel) tableBrowserPane.getComponentAt(componentIndex);
        annotationPanel.setNetworkView(view);
        annotationPanel.setAnnotations(annotations);
        tableBrowserPane.setSelectedIndex(componentIndex);
    }

}
