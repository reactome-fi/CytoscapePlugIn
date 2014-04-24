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
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
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
import org.reactome.cytoscape.pgm.IPAPathwayAnalysisPane;
import org.reactome.cytoscape.pgm.IPAValueTablePane;
import org.reactome.cytoscape.pgm.InferenceAlgorithmDialog;
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
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pgm.InferenceResults;
import org.reactome.pgm.InferenceStatus;
import org.reactome.pgm.Observation;
import org.reactome.pgm.PGMFactor;
import org.reactome.pgm.PGMFactorGraph;
import org.reactome.pgm.PGMInferenceAlgorithm;
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
    private void runInference(final CyNetwork network) {
        Thread t = new Thread() {
            public void run() {
                _runInference(network,
                              true);
            }
        };
        t.start();
    }
    
    /**
     * Calculate and show IPA values.
     * @param resultsList
     * @param target
     * @return true if values are shown.
     */
    private void showIPANodeValues(List<InferenceResults> resultsList) {
        if (resultsList.size() <= 1) // Just prior probabilities
            return ;
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        String title = "IPA Node Values";
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        IPAValueTablePane valuePane = null;
        if (index < 0)
            valuePane = new IPAValueTablePane(title);
        else
            valuePane = (IPAValueTablePane) tableBrowserPane.getComponentAt(index);
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
        // Don't select it. Let the overview panel to be selected.
//        // Need to select it
//        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
//        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
//        int index = tableBrowserPane.indexOfComponent(valuePane);
//        if (index >= 0)
//            tableBrowserPane.setSelectedIndex(index);
    }
    
    private void showIPAPathwayValues(List<InferenceResults> resultsList,
                                      PGMFactorGraph fg) {
        if (resultsList.size() <= 1)
            return; 
        String title = "IPA Pathway Analysis";
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane,
                                                                    title);
        IPAPathwayAnalysisPane valuePane = null;
        if (index > -1)
            valuePane = (IPAPathwayAnalysisPane) tableBrowserPane.getComponentAt(index);
        else
            valuePane = new IPAPathwayAnalysisPane(title);
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
        // The following should be taken care of by the above method invocation.
//        valuePane.setFactorGraph(fg);
        if (index == -1)
            index = tableBrowserPane.indexOfComponent(valuePane);
        if (index >= 0) // Select this as the default table for viewing the results
            tableBrowserPane.setSelectedIndex(index);
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
        if (resultsList.size() < 2)
            return; // Only prior information
        // Need to reset the previously assigned values
        for (PGMVariable var : target.getVariables()) {
            var.clearPosteriorValues();
            var.clearRandomPosteriorValues();
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
                if (sample.startsWith(ObservationDataHelper.RANDOM_SAMPLE_PREFIX))
                    var.addRandomPosteriorValues(sample, probs);
                else
                    var.addPosteriorValues(sample, probs);
            }
        }
    }
    
    private void _runInference(final CyNetwork network,
                               final boolean needFinishDialog) {
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
        // Set up an inference algorithm
        InferenceAlgorithmDialog infAlgDialog = new InferenceAlgorithmDialog(frame);
        infAlgDialog.setLocationRelativeTo(frame);
        infAlgDialog.setSize(400, 355);
        infAlgDialog.setModal(true);
        infAlgDialog.setVisible(true);
        if (!infAlgDialog.isOkClicked())
            return; // Cancelled
        PGMInferenceAlgorithm algorithm = infAlgDialog.getSelectedAlgorithm();
        if (algorithm == null) {
            JOptionPane.showMessageDialog(frame, 
                                          "Cannot perform inference: no algorithm has been specified.", 
                                          "No Inference Algorithm", 
                                          JOptionPane.ERROR_MESSAGE);
            return; // Algorithm has not been selected
        }
        fg.setInferenceAlgorithm(algorithm);
        try {
            ProgressPane progressPane = new ProgressPane();
            frame.setGlassPane(progressPane);
            progressPane.setTitle("Run Inference");
            progressPane.setText("Sending data to the server...");
            progressPane.setIndeterminate(true);
            frame.getGlassPane().setVisible(true);
            final RESTFulFIService fiService = new RESTFulFIService();
            final String processId = fiService.runInferenceOnFGViaProcess(fg);
            if (processId == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot run inference at the server.",
                                              "Inference Error",
                                              JOptionPane.ERROR_MESSAGE);
                if (frame.getGlassPane() != null)
                    frame.getGlassPane().setVisible(false);
                return;
            }
            // A special case
            if (processId.equals(InferenceStatus.SERVER_BUSY.toString())) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "The server is busy right now. Please try again later on.",
                                              "Server Busy",
                                              JOptionPane.ERROR_MESSAGE);
                if (frame.getGlassPane() != null)
                    frame.getGlassPane().setVisible(false);
                return;
            }
            progressPane.setText("Performing inference...");
            progressPane.enableCancelAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        fiService.abortInferenceProcess(processId);
                    }
                    catch(Exception e1) {
                        
                    }
                }
            });
            InferenceStatus status = null;
            while (!progressPane.isCancelled()) {
                status = fiService.checkInferenceStatus(processId);
                if (status == InferenceStatus.DONE || status == InferenceStatus.ERROR) {
                    break;
                }
                // Sleep for 2 seconds
                Thread.sleep(2000);
            }
            if (progressPane.isCancelled()) {
                if (frame.getGlassPane() != null)
                    frame.getGlassPane().setVisible(false);
                return;
            }
            if (status == InferenceStatus.DONE) {
//                List<InferenceResults> inferenceResults = fiService.runInferenceOnFactorGraph(fg);
                List<InferenceResults> inferenceResults = fiService.getInferenceResults(processId);
                // Want to copy values from pfgWithValues to the original factor graph.
                // The original factor graph can be replaced by the returned new factor graph
                // too. However, it is felt that copying values is more reliable, which is just
                // my gut feeling.
                copyVariableValues(inferenceResults,
                                   fg);
                showIPANodeValues(inferenceResults);
                showIPAPathwayValues(inferenceResults,
                                     fg);
                frame.getGlassPane().setVisible(false);
                if (needFinishDialog) {
                    String message = "Inference has finished successfully. You may use \"View Marginal Probabilities\" by\n" + 
                            "selecting a variable node";
                    if (inferenceResults.size() == 1)
                        message += ".";
                    else
                        message += ", and view IPA values at the bottom \"IPA Node Values\" tab. \n" + 
                                "You may also view pathway level results at the \"IPA Pathway Analysis\" tab.\n" +
                                "Note: IPA stands for \"Integrated Pathway Activity\".";
                    JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                  message,
                                                  "Inference Finished",
                                                  JOptionPane.INFORMATION_MESSAGE);
                }
            }
            else if (status == InferenceStatus.ERROR) {
                throw new IllegalStateException(fiService.getInferenceError(processId));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in running inference on factor graph: " + e.getMessage(),
                                          "Error in Inference",
                                          JOptionPane.ERROR_MESSAGE);
            if (frame.getGlassPane() != null)
                frame.getGlassPane().setVisible(false);
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
                    runInference(netView.getModel());
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
        
        private void viewVariableMarginal(final CyNetworkView netView,
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
            final PGMVariable variable = getVariable(fg, netView.getModel(), nodeView.getModel());
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
                                                          "In order to view the marginal probabilities for a variable, please \n" + 
                                                          "run an inference first. Do you want to run inference?",
                                                          "Run Inference?",
                                                          JOptionPane.OK_CANCEL_OPTION);
                if (reply != JOptionPane.OK_OPTION)
                    return;
                Thread t = new Thread() {
                    public void run() {
                        _runInference(netView.getModel(), false);
                        if (variable.getValues() == null) {
                            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                          "Cannot find marginal probabilities for the selected variable.",
                                                          "No Marginal Probabilities", 
                                                          JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                };
                t.start();
            }
            else
                viewVariableMarginal(variable);
        }

        private void viewVariableMarginal(PGMVariable variable) {
            VariableValuesDialog dialog = new VariableValuesDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setPGMNode(variable);
            if (variable.getPosteriorValues() == null || variable.getPosteriorValues().size() == 0)
                dialog.setSize(300, 250);
            else
                dialog.setSize(600, 500);
            dialog.setLocationRelativeTo(dialog.getOwner());
            // Don't use a modal dialog so that multiple dialogs can be opened for comparison
//            dialog.setModal(true);
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
        
        private void loadObservationData(final CyNetworkView netView) {
            final PGMFactorGraph pfg = NetworkToFactorGraphMap.getMap().get(netView.getModel());
            if (pfg == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched factor graph for the network!", 
                                              "No Factor Graph", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            final ObservationDataLoadDialog dialog = new ObservationDataLoadDialog();
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setModal(true);
            dialog.setVisible(true);
            if (!dialog.isOkClicked())
                return;
            if (dialog.getDNAFile() == null && dialog.getGeneExpFile() == null)
                return;
            Thread t = new Thread() {
                public void run() {
                    loadObservationData(pfg, netView, dialog);
                }
            };
            t.start();
        }

        @SuppressWarnings("unchecked")
        private void loadObservationData(PGMFactorGraph pfg,
                                         CyNetworkView netView,
                                         ObservationDataLoadDialog dialog) {
            ObservationDataHelper helper = new ObservationDataHelper(pfg, netView);
            JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
            try {
                ProgressPane progressPane = new ProgressPane();
                progressPane.setTitle("Load Observation Data");
                progressPane.setIndeterminate(true);
                frame.setGlassPane(progressPane);
                frame.getGlassPane().setVisible(true);
                Map<PGMVariable, Map<String, Integer>> dnaVarToSampleToState = null;
                if (dialog.getDNAFile() != null) {
                    progressPane.setText("Loading CNV data...");
                    dnaVarToSampleToState = helper.loadData(dialog.getDNAFile(), 
                                                            ObservationType.CNV,
                                                            dialog.getDNAThresholdValues());
                }
                Map<PGMVariable, Map<String, Integer>> geneExpVarToSampleToState = null;
                if (dialog.getGeneExpFile() != null) {
                    progressPane.setText("Loading mRNA expression data...");
                    geneExpVarToSampleToState = helper.loadData(dialog.getGeneExpFile(),
                                                                ObservationType.GENE_EXPRESSION,
                                                                dialog.getGeneExpThresholdValues());
                }
                if (dnaVarToSampleToState == null && geneExpVarToSampleToState == null) {
                    frame.getGlassPane().setVisible(false);
                    return;
                }
                progressPane.setText("Generating observations...");
                List<Observation> observations = null;
                if (dnaVarToSampleToState != null && geneExpVarToSampleToState != null)
                    observations = helper.generateObservations(dnaVarToSampleToState,
                                                               geneExpVarToSampleToState);
                else if (dnaVarToSampleToState != null)
                    observations = helper.generateObservations(dnaVarToSampleToState);
                else if (geneExpVarToSampleToState != null)
                    observations = helper.generateObservations(geneExpVarToSampleToState);
                pfg.setObservations(observations);  
//                System.out.println("Real data:");
//                checkObservations(observations);
                progressPane.setText("Generating random data...");
                List<Observation> randomData = null;
                if (dnaVarToSampleToState != null && geneExpVarToSampleToState != null)
                    randomData = helper.generateRandomObservations(dnaVarToSampleToState,
                                                                   geneExpVarToSampleToState);
                else if (dnaVarToSampleToState != null)
                    randomData = helper.generateRandomObservations(dnaVarToSampleToState);
                else if (geneExpVarToSampleToState != null)
                    randomData = helper.generateRandomObservations(geneExpVarToSampleToState);
//                System.out.println("\nRandom data:");
//                checkObservations(randomData);
                pfg.setRandomObservations(randomData);
                frame.getGlassPane().setVisible(false);
                JOptionPane.showMessageDialog(frame,
                                              "The data has been loaded successfully.",
                                              "Loading Data",
                                              JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Error in loading observation data: " + e.getMessage(),
                                              "Error in Loading Data",
                                              JOptionPane.ERROR_MESSAGE);
                if (frame.getGlassPane() != null)
                    frame.getGlassPane().setVisible(false);
                return;
            }
        }
    }
    
//    /**
//     * This method is used for debugging purpose.
//     * @param observations
//     */
//    private void checkObservations(List<Observation> observations) {
//        for (Observation observation : observations) {
//            System.out.println(observation.getSample());
//            Map<String, Integer> obsToState = observation.getObserved();
//            System.out.println(obsToState);
//        }
//    }
    
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
