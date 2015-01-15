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
import org.reactome.cytoscape.pgm.*;
import org.reactome.cytoscape.service.AbstractPopupMenuHandler;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.ReactomeSourceView;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Factor;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.common.DataType;

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
    private void showIPANodeValues(FactorGraphInferenceResults fgResults) {
        if (!fgResults.hasPosteriorResults()) // Just prior probabilities
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
    
    private void showIPAPathwayValues(FactorGraphInferenceResults fgResults) {
        if (!fgResults.hasPosteriorResults())
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
    
    private void _runInference(final CyNetwork network,
                               final boolean needFinishDialog) {
        FactorGraph fg = FactorGraphRegistry.getRegistry().get(network);
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
        infAlgDialog.setSize(500, 300);
        infAlgDialog.setModal(true);
        infAlgDialog.setVisible(true);
        if (!infAlgDialog.isOkClicked())
            return; // Cancelled
        final Inferencer algorithm = infAlgDialog.getSelectedAlgorithm();
        if (algorithm == null) {
            JOptionPane.showMessageDialog(frame, 
                                          "Cannot perform inference: no algorithm has been specified.", 
                                          "No Inference Algorithm", 
                                          JOptionPane.ERROR_MESSAGE);
            return; // Algorithm has not been selected
        }
        try {
            ProgressPane progressPane = new ProgressPane();
            frame.setGlassPane(progressPane);
            progressPane.setTitle("Run Inference");
            progressPane.setIndeterminate(true);
            frame.getGlassPane().setVisible(true);
            progressPane.setText("Performing inference...");
            final InferenceThread inferenceThread = new InferenceThread();
            progressPane.enableCancelAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    inferenceThread.abort();
                    // Need a interface to abort inference.
//                    if (algorithm instanceof LoopyBeliefPropagation)
//                        ((LoopyBeliefPropagation)algorithm).setMaxIteration(0);
//                    else if (algorithm instanceof GibbsSampling)
//                        ((GibbsSampling)algorithm).setMaxIteration(0);
                }
            });
            algorithm.setFactorGraph(fg);
            inferenceThread.setInferencer(algorithm);
            inferenceThread.setProgressPane(progressPane);
            inferenceThread.start();
            while (!progressPane.isCancelled()) {
                InferenceStatus status = inferenceThread.getStatus();
                if (status == InferenceStatus.DONE || status == InferenceStatus.ERROR || status == InferenceStatus.ABORT) {
                    break;
                }
                // Sleep for 2 seconds
                Thread.sleep(2000);
            }
            if (frame.getGlassPane() != null)
                frame.getGlassPane().setVisible(false);
            InferenceStatus status = inferenceThread.getStatus();
            if (progressPane.isCancelled() || status != InferenceStatus.DONE) {
                return;
            }
            if (status == InferenceStatus.DONE) {
                FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(fg);
                showIPANodeValues(fgResults);
                showIPAPathwayValues(fgResults);
                if (needFinishDialog) {
                    String message = "Inference has finished successfully. You may use \"View Marginal Probabilities\" by\n" + 
                            "selecting a variable node";
                    // Check if any posterior inference is done
                    if (!fgResults.hasPosteriorResults())
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
            FactorGraph fg = FactorGraphRegistry.getRegistry().get(netView.getModel());
            if (fg == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched factor graph for the network!", 
                                              "No Factor Graph", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            Factor factor = getFactor(fg, netView.getModel(), nodeView.getModel());
            if (factor == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched factor for the selected node!", 
                                              "No Factor", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            FactorValuesDialog dialog = new FactorValuesDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setFactor(factor);
            
            dialog.setSize(500, 350);
            dialog.setModal(false);
            dialog.setLocationRelativeTo(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setVisible(true);
        }
        
        private Factor getFactor(FactorGraph fg,
                                 CyNetwork network,
                                 CyNode node) {
            String name = new TableHelper().getStoredNodeAttribute(network, node, "name", String.class);
            for (Factor factor : fg.getFactors()) {
                if (name.equals("factor:" + factor.getId()))
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
            FactorGraph fg = FactorGraphRegistry.getRegistry().get(netView.getModel());
            if (fg == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched factor graph for the network!", 
                                              "No Factor Graph", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            final Variable variable = getVariable(fg, netView.getModel(), nodeView.getModel());
            if (variable == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find a matched variable for the selected node!", 
                                              "No Variable", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Check if the found variable has values associated. If not, 
            // ask the user to do an inference first
            final FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(fg);
            VariableInferenceResults varResults = fgResults.getVariableInferenceResults(variable);
            if (varResults == null || varResults.getPriorValues() == null) {
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
                        VariableInferenceResults varResults = fgResults.getVariableInferenceResults(variable);
                        if (varResults == null || varResults.getPriorValues() == null) {
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
                viewVariableMarginal(varResults);
        }

        private void viewVariableMarginal(VariableInferenceResults varResults) {
            VariableValuesDialog dialog = new VariableValuesDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setVariableValues(varResults);
            if (varResults.getPosteriorValues() == null || varResults.getPosteriorValues().size() == 0)
                dialog.setSize(300, 250);
            else
                dialog.setSize(600, 500);
            dialog.setLocationRelativeTo(dialog.getOwner());
            // Don't use a modal dialog so that multiple dialogs can be opened for comparison
//            dialog.setModal(true);
            dialog.setVisible(true);
        }
        
        private Variable getVariable(FactorGraph fg,
                                     CyNetwork network,
                                     CyNode node) {
            String name = new TableHelper().getStoredNodeAttribute(network, 
                                                                   node,
                                                                   "name",
                                                                   String.class);
            if (name == null)
                return null;
            for (Variable variable : fg.getVariables()) {
                if (name.equals("variable:" + variable.getId()))
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
            final FactorGraph fg = FactorGraphRegistry.getRegistry().get(netView.getModel());
            if (fg == null) {
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
                    loadObservationData(fg, netView, dialog);
                }
            };
            t.start();
        }

        @SuppressWarnings("unchecked")
        private void loadObservationData(FactorGraph fg,
                                         CyNetworkView netView,
                                         ObservationDataLoadDialog dialog) {
            ObservationDataHelper helper = new ObservationDataHelper(fg, netView);
            JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
            try {
                ProgressPane progressPane = new ProgressPane();
                progressPane.setTitle("Load Observation Data");
                progressPane.setIndeterminate(true);
                frame.setGlassPane(progressPane);
                frame.getGlassPane().setVisible(true);
                Map<Variable, Map<String, Integer>> dnaVarToSampleToState = null;
                if (dialog.getDNAFile() != null) {
                    progressPane.setText("Loading CNV data...");
                    dnaVarToSampleToState = helper.loadData(dialog.getDNAFile(), 
                                                            DataType.CNV,
                                                            dialog.getDNAThresholdValues());
                }
                Map<Variable, Map<String, Integer>> geneExpVarToSampleToState = null;
                if (dialog.getGeneExpFile() != null) {
                    progressPane.setText("Loading mRNA expression data...");
                    geneExpVarToSampleToState = helper.loadData(dialog.getGeneExpFile(),
                                                                DataType.mRNA_EXP,
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
                FactorGraphRegistry.getRegistry().setObservations(fg, observations);
                progressPane.setText("Generating random data...");
                List<Observation> randomData = null;
                if (dnaVarToSampleToState != null && geneExpVarToSampleToState != null)
                    randomData = helper.generateRandomObservations(dnaVarToSampleToState,
                                                                   geneExpVarToSampleToState);
                else if (dnaVarToSampleToState != null)
                    randomData = helper.generateRandomObservations(dnaVarToSampleToState);
                else if (geneExpVarToSampleToState != null)
                    randomData = helper.generateRandomObservations(geneExpVarToSampleToState);
                FactorGraphRegistry.getRegistry().setRandomObservations(fg, randomData);
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
