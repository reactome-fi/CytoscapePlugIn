/*
 * Created on Mar 4, 2014
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.ServiceProperties;
import org.gk.util.ProgressPane;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.pgm.FactorValuesDialog;
import org.reactome.cytoscape.pgm.NetworkToFactorGraphMap;
import org.reactome.cytoscape.pgm.ObservationDataHelper;
import org.reactome.cytoscape.pgm.ObservationDataLoadDialog;
import org.reactome.cytoscape.pgm.ObservationType;
import org.reactome.cytoscape.pgm.VariableValuesDialog;
import org.reactome.cytoscape.service.AbstractPopupMenuHandler;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.ReactomeSourceView;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.pgm.InferenceResults;
import org.reactome.pgm.Observation;
import org.reactome.pgm.PGMFactor;
import org.reactome.pgm.PGMFactorGraph;
import org.reactome.pgm.PGMVariable;

/**
 * This PopupMenuHandler is used for a factor graph network.
 * @author gwu
 *
 */
public class FactorGraphPopupMenuHandler extends AbstractPopupMenuHandler {
    // Keep these two menus for install/uninstall based on node type
    private CyNodeViewContextMenuFactory viewVariableMarginalMenu;
    private CyNodeViewContextMenuFactory viewFactorValuesMenu;
    protected Map<CyNodeViewContextMenuFactory, ServiceRegistration> menuToRegistration;
    
    /**
     * Default constructor.
     */
    public FactorGraphPopupMenuHandler() {
        menuToRegistration = new HashMap<CyNodeViewContextMenuFactory, ServiceRegistration>();
        // Add a selection listener in order to switch the menus
        RowsSetListener selectionListener = new RowsSetListener() {
            
            @Override
            public void handleEvent(RowsSetEvent event) {
                // We want to check node selection only
                if (!event.containsColumn(CyNetwork.SELECTED)) {
                    return;
                }
                CyNetworkView currentView = PopupMenuManager.getManager().getCurrentNetworkView();
                if (currentView == null)
                    return;
                List<CyNode> nodes = CyTableUtil.getNodesInState(currentView.getModel(),
                                                                 CyNetwork.SELECTED,
                                                                 true);
                if (nodes.size() != 1)
                    return;
                CyNode node = nodes.get(0);
                doNodePopupMenu(currentView.getModel(),
                                node);
            }

        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(RowsSetListener.class.getName(),
                                selectionListener, 
                                null);
    }
    
    /**
     * Create a node specific popup menu.
     * @param network
     * @param node
     */
    private void doNodePopupMenu(CyNetwork network,
                                 CyNode node) {
        TableHelper tableHelper = new TableHelper();
        ReactomeNetworkType networkType = tableHelper.getReactomeNetworkType(network);
        if (networkType != ReactomeNetworkType.FactorGraph)
            return;
        String nodeType = tableHelper.getStoredNodeAttribute(network,
                                                             node,
                                                             "nodeType",
                                                             String.class);
        if ("factor".equals(nodeType)) {
            uninstallDynamicMenu(viewVariableMarginalMenu);
            installViewFactorValuesMenu();
        }
        else if ("variable".equals(nodeType) || "observation".equals(nodeType)) { // Though there is no need to view marginal for an observation node,
                                                                                  // it will be nicer for comparison.
            installViewVariableMarginalMenu();
            uninstallDynamicMenu(viewFactorValuesMenu);
        }
        else {
            uninstallDynamicMenu(viewFactorValuesMenu);
            uninstallDynamicMenu(viewVariableMarginalMenu);
        }
    }
    
    private void installViewFactorValuesMenu() {
        if (viewFactorValuesMenu == null)
            viewFactorValuesMenu = new ViewFactorValueMenu();
        installDynamicMenu(viewFactorValuesMenu, "View Factor Values");
    }
    
    private void installViewVariableMarginalMenu() {
        if (viewVariableMarginalMenu == null)
            viewVariableMarginalMenu = new ViewVariableMarginalMenu();
        installDynamicMenu(viewVariableMarginalMenu, "View Marginal Probabilities");
    }
    
    private void installDynamicMenu(CyNodeViewContextMenuFactory menu,
                                    String title) {
        ServiceRegistration registration = menuToRegistration.get(menu);
        if (registration != null)
            return; // This menu has been installed.
        Properties props = new Properties();
        props.setProperty(ServiceProperties.TITLE, title);
        props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        registration = context.registerService(CyNodeViewContextMenuFactory.class.getName(),
                                               menu,
                                               props);
        menuToRegistration.put(menu, registration);
    }
    
    private void uninstallDynamicMenu(CyNodeViewContextMenuFactory menu) {
        if (menu == null)
            return;
        ServiceRegistration registration = menuToRegistration.get(menu);
        if (registration == null)
            return; // This menu has been uninstalled already
        registration.unregister();
        menuToRegistration.remove(menu);
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.PopupMenuHandler#install()
     */
    @Override
    protected void installMenus() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        String preferredMenu = PREFERRED_MENU;
        
        CyNodeViewContextMenuFactory viewReactomeSourceMenu = new ViewReactomeSourceMenu();
        installMenu(viewReactomeSourceMenu, "View Reactome Source", CyNodeViewContextMenuFactory.class);
        
        CyNetworkViewContextMenuFactory convertToPathwayMenu = new ConvertToDiagramMenu();
        installMenu(convertToPathwayMenu, "Convert to Pathway", CyNetworkViewContextMenuFactory.class);
        
        CyNetworkViewContextMenuFactory runInferneceMenu = new RunInferenceMenu();
        installMenu(runInferneceMenu, "Run Inference", CyNetworkViewContextMenuFactory.class);
        
        CyNetworkViewContextMenuFactory loadObservationDataMenu = new LoadObservationDataMenu();
        installMenu(loadObservationDataMenu, "Load Observation Data", CyNetworkViewContextMenuFactory.class);
        
    }
    
    private <T> void installMenu(T menu,
                                 String title,
                                 Class<T> cls) {
        Properties props = new Properties();
        props.setProperty(ServiceProperties.TITLE, title);
        props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceRegistration registration = context.registerService(cls.getName(),
                                                                   menu,
                                                                   props);
        menuRegistrations.add(registration);
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.PopupMenuHandler#uninstall()
     */
    @Override
    public void uninstallMenus() {
        super.uninstallMenus();
    }
    
    /**
     * Run a factor graph inference by calling a remote RESTful API.
     * @param needFinishDialog
     * @return true if the results are returned from the remote server. Otherwise,
     * false is returned.
     */
    private void runInference(final CyNetwork network,
                              final boolean needFinishDialog) {
        Thread t = new Thread() {
            public void run() {
                PGMFactorGraph fg = NetworkToFactorGraphMap.getMap().get(network);
                if (fg == null) {
                    JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                  "There is no factor graph found for the displayed network.\n" + 
                                                          "No inference can be done.",
                                                          "No Factor Graph",
                                                          JOptionPane.ERROR_MESSAGE);
                    return ;
                }
                JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
                try {
                    ProgressPane progressPane = new ProgressPane();
                    frame.setGlassPane(progressPane);
                    progressPane.setTitle("Run Inference");
                    progressPane.setText("Run inference on factor graph...");
                    progressPane.setIndeterminate(true);
                    frame.getGlassPane().setVisible(true);
                    RESTFulFIService fiService = new RESTFulFIService();
                    List<InferenceResults> inferenceResults = fiService.runInferenceOnFactorGraph(fg);
                    // Want to copy values from pfgWithValues to the original factor graph.
                    // The original factor graph can be replaced by the returned new factor graph
                    // too. However, it is felt that copying values is more reliable, which is just
                    // my gut feeling.
                    copyVariableValues(inferenceResults,
                                       fg);
                    frame.getGlassPane().setVisible(false);
                    if (needFinishDialog) {
                        JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                      "Inference has finished successfully. You may use \"View Marginal Probabilities\"\n" + 
                                                              "by selecting a variable node.",
                                                              "Inference Finished",
                                                              JOptionPane.INFORMATION_MESSAGE);
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                  "Error in running infernece on factor graph: " + e.getMessage(),
                                                  "Error in Inference",
                                                  JOptionPane.ERROR_MESSAGE);
                    if (frame.getGlassPane() != null)
                        frame.getGlassPane().setVisible(false);
                }
            }
        };
        t.start();
    }
    
    private void copyVariableValues(List<InferenceResults> resultsList, 
                                    PGMFactorGraph target) {
        // The first results should be the prior marginals
        InferenceResults results = resultsList.get(0);
        Map<String, List<Double>> varIdToProbs = results.getResults();
        for (PGMVariable var : target.getVariables()) {
            List<Double> probs = varIdToProbs.get(var.getId());
            if (probs != null)
                var.setValues(probs); // Just use the original List object directly
        }
        // All others should be posterior probabilities
        for (int i = 1; i < resultsList.size(); i++) {
            results = resultsList.get(i);
            if (results.getSample() == null)
                continue;
            String sample = results.getSample();
            varIdToProbs = results.getResults();
            for (PGMVariable var : target.getVariables()) {
                List<Double> probs = varIdToProbs.get(var.getId());
                if (probs == null)
                    continue;
                var.addPosteriorValues(sample, probs);
            }
        }
    }
    
    private class ViewFactorValueMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                                         final View<CyNode> nodeView) {
            JMenuItem menuItem = new JMenuItem("View Factor Values");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    viewFactorValues(netView, nodeView);
                }
            });
            return new CyMenuItem(menuItem, 10.0f);
        }
        
        private void viewFactorValues(CyNetworkView netView,
                                      View<CyNode> nodeView) {
            // Need to find the factor first
            PGMFactorGraph fg = NetworkToFactorGraphMap.getMap().get(netView.getModel());
            if (fg == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched factor graph for the network!", 
                                              "No Factor Graph", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            PGMFactor factor = getFactor(fg, netView.getModel(), nodeView.getModel());
            if (factor == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched factor for the selected node!", 
                                              "No Factor", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            FactorValuesDialog dialog = new FactorValuesDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setPGMNode(factor);
            
            dialog.setSize(500, 350);
            dialog.setModal(false);
            dialog.setLocationRelativeTo(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setVisible(true);
        }
        
        private PGMFactor getFactor(PGMFactorGraph fg,
                                    CyNetwork network,
                                    CyNode node) {
            String name = new TableHelper().getStoredNodeAttribute(network, node, "name", String.class);
            for (PGMFactor factor : fg.getFactors()) {
                if (factor.getLabel().equals(name))
                    return factor;
            }
            return null;
        }
    }
    
    private class RunInferenceMenu implements CyNetworkViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView) {
            JMenuItem menuItem = new JMenuItem("Run Inference");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    runInference(netView.getModel(),
                                 true);
                }
            });
            return new CyMenuItem(menuItem, 10.f);
        }
        
    }
    
    private class ViewVariableMarginalMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                                         final View<CyNode> nodeView) {
            JMenuItem menuItem = new JMenuItem("View Marginal Probabilities");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    viewVariableMarginal(netView, nodeView);
                }
            });
            return new CyMenuItem(menuItem, 10.0f);
        }
        
        private void viewVariableMarginal(CyNetworkView netView,
                                          View<CyNode> nodeView) {
            // Need to find the factor first
            PGMFactorGraph fg = NetworkToFactorGraphMap.getMap().get(netView.getModel());
            if (fg == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched factor graph for the network!", 
                                              "No Factor Graph", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            PGMVariable variable = getVariable(fg, netView.getModel(), nodeView.getModel());
            if (variable == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched variable for the selected node!", 
                                              "No Variable", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Check if the found variable has values associated. If not, 
            // ask the user to do an inference first
            if (variable.getValues() == null) {
                int reply = JOptionPane.showConfirmDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                          "In order to view the marginal probabilities for a variable. Please \n" + 
                                                          "run an inference first. Do you want to run inference?",
                                                          "Run Inference?",
                                                          JOptionPane.OK_CANCEL_OPTION);
                if (reply != JOptionPane.OK_OPTION)
                    return;
                runInference(netView.getModel(), false);
            }
            if (variable.getValues() == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find marginal probabilities for the selected variable.",
                                              "No Marginal Probabilities", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            VariableValuesDialog dialog = new VariableValuesDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setPGMNode(variable);
            dialog.setSize(400, 275);
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setModal(true);
            dialog.setVisible(true);
        }
        
        private PGMVariable getVariable(PGMFactorGraph fg,
                                        CyNetwork network,
                                        CyNode node) {
            String name = new TableHelper().getStoredNodeAttribute(network, node, "name", String.class);
            for (PGMVariable variable : fg.getVariables()) {
                if (variable.getLabel().equals(name))
                    return variable;
            }
            return null;
        }
    }
    
    private class LoadObservationDataMenu implements CyNetworkViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView) {
            JMenuItem menuItem = new JMenuItem("Load Observation Data");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    loadObservationData(netView);
                }
            });
            return new CyMenuItem(menuItem, 1001.0f);
        }
        
        @SuppressWarnings("unchecked")
        private void loadObservationData(CyNetworkView netView) {
            PGMFactorGraph pfg = NetworkToFactorGraphMap.getMap().get(netView.getModel());
            if (pfg == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched factor graph for the network!", 
                                              "No Factor Graph", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            ObservationDataLoadDialog dialog = new ObservationDataLoadDialog();
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setModal(true);
            dialog.setVisible(true);
            if (!dialog.isOkClicked())
                return;
            if (dialog.getDNAFile() == null && dialog.getGeneExpFile() == null)
                return;
            ObservationDataHelper helper = new ObservationDataHelper(pfg, netView);
            try {
                Map<PGMVariable, Map<String, Integer>> dnaVarToSampleToState = null;
                if (dialog.getDNAFile() != null)
                    dnaVarToSampleToState = helper.loadData(dialog.getDNAFile(), 
                                                            ObservationType.CNV,
                                                            dialog.getDNAThresholdValues());
                Map<PGMVariable, Map<String, Integer>> geneExpVarToSampleToState = null;
                if (dialog.getGeneExpFile() != null)
                    geneExpVarToSampleToState = helper.loadData(dialog.getGeneExpFile(),
                                                                ObservationType.GENE_EXPRESSION,
                                                                dialog.getGeneExpThresholdValues());
                if (dnaVarToSampleToState == null && geneExpVarToSampleToState == null)
                    return;
                List<Observation> observations = null;
                if (dnaVarToSampleToState != null && geneExpVarToSampleToState != null)
                    observations = helper.generateObservations(dnaVarToSampleToState,
                                                               geneExpVarToSampleToState);
                else if (dnaVarToSampleToState != null)
                    observations = helper.generateObservations(dnaVarToSampleToState);
                else if (geneExpVarToSampleToState != null)
                    observations = helper.generateObservations(geneExpVarToSampleToState);
                pfg.setObservations(observations);    
                // Give the user an information
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "The data has been loaded successfully.",
                                              "Data Loaded",
                                              JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            catch(Exception e) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Error in loading observation data: " + e.getMessage(),
                                              "Error in Loading Data",
                                              JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return;
            }
        }
        
    }
    
    private class ConvertToDiagramMenu implements CyNetworkViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView) {
            JMenuItem menuItem = new JMenuItem("Convert to Pathway");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    convertToPathway(netView);
                }
            });
            return new CyMenuItem(menuItem, 0.0f);
        }
        
        private void convertToPathway(CyNetworkView netView) {
            DiagramAndNetworkSwitcher helper = new DiagramAndNetworkSwitcher();
            helper.convertToDiagram(netView);
        }
        
    }
    
    private class ViewReactomeSourceMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                                         final View<CyNode> nodeView) {
            JMenuItem menuItem = new JMenuItem("View Reactome Source");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent event) {
                    viewReactomeSource(netView, nodeView);
                }
            });
            CyMenuItem rtn = new CyMenuItem(menuItem, 0.0f);
            return rtn;
        }
        
        private void viewReactomeSource(CyNetworkView netView,
                                        View<CyNode> nodeView) {
            // Get the label for the node
            CyNode node = nodeView.getModel();
            TableHelper tableHelper = new TableHelper();
            String nodeLabel = tableHelper.getStoredNodeAttribute(netView.getModel(),
                                                                  node,
                                                                  "name", 
                                                                  String.class);
            if (nodeLabel.matches("(\\d+)")) {
                ReactomeSourceView sourceView = new ReactomeSourceView();
                sourceView.viewReactomeSource(new Long(nodeLabel),
                                              PlugInObjectManager.getManager().getCytoscapeDesktop());
            }
            else {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "The selected node is a derived node from a Reactome pathway object. Please try\n" + 
                                              "to select another node related to this node for information in Reactome.",
                                              "No Information",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        }
        
    }
    
}
