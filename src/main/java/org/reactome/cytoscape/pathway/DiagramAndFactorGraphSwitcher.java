/*
 * Created on Mar 3, 2014
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.Border;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.util.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.pgm.FactorGraphRegistry;
import org.reactome.cytoscape.pgm.FactorGraphVisualStyle;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.Factor;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Variable;

/**
 * A similar class to DiagramAndNetworkSwitcher. This class is used to switch between a pathway diagram view and its
 * factor graph view.
 * Note the following mappings from factor's properties to node's attributes:
 * factor.id: not mapped
 * factor.label: node's name, common name, shared name
 * factor.name: node label (changed), node tool tip (the type of node is added, e.g., variable, factor).
 * @author gwu
 *
 */
public class DiagramAndFactorGraphSwitcher {
    
    /**
     * Default constructor.
     */
    public DiagramAndFactorGraphSwitcher() {
    }
    
    public void convertToFactorGraph(final Long pathwayId,
                                     final RenderablePathway pathway,
                                     final PathwayInternalFrame pathwayFrame) throws Exception {
        if (!canConvertToFactorGraph(pathway))
            return;
        final String escapeNames = getEscapeNames();
        if (escapeNames == null)
            return;  // Aborted
        Task task = new AbstractTask() {
            
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                convertPathwayToFactorGraph(pathwayId,
                                            pathway,
                                            pathwayFrame,
                                            escapeNames,
                                            taskMonitor);
            }
        }; 
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        taskManager.execute(new TaskIterator(task)); 
    }
    
    /**
     * Call this method to convert a Pathway and its associated PathwayDiagram into a
     * FactorGraph without using an Asynchronous Task object. The converted FactorGraph
     * will not be displayed in the main Cytoscape desktop.
     * @param pathwayId
     * @param pathway
     * @return
     * @throws Exception
     */
    public FactorGraph convertPathwayToFactorGraph(Long pathwayId,
                                                   RenderablePathway pathway) throws Exception {
        if (!canConvertToFactorGraph(pathway))
            return null;
        String escapeNames = getEscapeNames();
        if (escapeNames == null)
            return null; // Aborted
        FactorGraph fg = convertPathwayToFactorGraph(pathwayId, pathway, escapeNames);
        if (fg != null)
            FactorGraphRegistry.getRegistry().registerDiagramToFactorGraph(pathway, fg);
        return fg;
    }

    /**
     * Check if a displayed RenderablePathway can be converted into a 
     * factor graph. If a pathway contains sub-pathways only, it cannot
     * be converted into a factor graph.
     * @param pathway
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean canConvertToFactorGraph(RenderablePathway pathway) {
        JFrame parentFrame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        List<Renderable> components = pathway.getComponents();
        // An empty diagram cannot be converted into a factor graph.
        if (components == null || components.size() == 0) {
            JOptionPane.showMessageDialog(parentFrame,
                                          "This is an empty pathway diagram and cannot be converted into a factor graph.",
                                          "Empty Diagram",
                                          JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        // Make sure there is at least one reaction is drawn and has
        // at least one entity is linked to it.
        boolean isSuperPathway = true;
        for (Renderable r : components) {
            if (r instanceof RenderableReaction) {
                RenderableReaction rxt = (RenderableReaction) r;
                List<Node> nodes = rxt.getConnectedNodes();
                if (nodes != null && nodes.size() > 0) {
                    isSuperPathway = false;
                }
            }
        }
        if (isSuperPathway) {
            JOptionPane.showMessageDialog(parentFrame,
                                          "The selected pathway diagram doesn't have any reaction drawn. Try to use its contained\n" +
                                          "sub-pathway for factor graph data analysis.",
                                          "Super Pathway Choosing",
                                          JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }
    
    private String getEscapeNames() {
        if (!FactorGraphRegistry.getRegistry().isNeedEscapeNameDialog() &&
            FactorGraphRegistry.getRegistry().getEscapeNames() != null)
            return FactorGraphRegistry.getRegistry().getEscapeNames();
        final EscapeNameDialog dialog = new EscapeNameDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                dialog.okBtn.requestFocus();
            }
        });
        if (FactorGraphRegistry.getRegistry().getEscapeNames() != null)
            dialog.setEscapeNames(FactorGraphRegistry.getRegistry().getEscapeNames());
        dialog.setSize(400, 275);
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked)
            return null;
        String names = dialog.getEscapeList();
        FactorGraphRegistry.getRegistry().setEscapeNames(names);
        return names;
    }
    
    private void convertPathwayToFactorGraph(Long pathwayId,
                                             RenderablePathway pathway,
                                             PathwayInternalFrame pathwayFrame,
                                             String escapeNames,
                                             TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Convert Pathway to Factor Graph");
        taskMonitor.setStatusMessage("Converting to factor graph...");
        taskMonitor.setProgress(0.0d);
        FactorGraph fg = convertPathwayToFactorGraph(pathwayId,
                                                     pathway,
                                                     escapeNames);
        if (fg == null)
            return;
        // Make sure this PathwayInternalFrame should be closed
        pathwayFrame.setVisible(false);
        pathwayFrame.dispose();
        
        taskMonitor.setProgress(0.50d);
        CyNetwork network = convertFactorGraphToNetwork(fg, 
                                                        pathway.getDisplayName(), 
                                                        pathwayId);

        // Cache the fetched pathway diagram to avoid another slow query
        PathwayDiagramRegistry.getRegistry().registerNetworkToDiagram(network,
                                                                      pathway);
        
        // A new set of Popup menus are needed
        PopupMenuManager.getManager().installPopupMenu(ReactomeNetworkType.FactorGraph);
        
        taskMonitor.setProgress(1.0d);
        PropertyChangeEvent event = new PropertyChangeEvent(this, 
                                                            "ConvertPathwayToFactorGraph",
                                                            pathway,
                                                            null);
        PathwayDiagramRegistry.getRegistry().firePropertyChange(event);
        
        FactorGraphRegistry.getRegistry().registerNetworkToFactorGraph(network, fg);
    }

    public CyNetwork convertFactorGraphToNetwork(FactorGraph fg, 
                                                 String pathwayName, 
                                                 Long pathwayId) {
        // Need to create a new CyNetwork
        FINetworkGenerator generator = new FINetworkGenerator();
        Set<String> interactions = createInteractionsFromFactorGraph(fg);
        CyNetwork network = generator.constructFINetwork(interactions);
        // Add some meta information
        CyRow row = network.getDefaultNetworkTable().getRow(network.getSUID());
        row.set("name",
                "Factor Graph for " + pathwayName);
        TableHelper tableHelper = new TableHelper();
        //TODO: Treat it as a FI network for the time being. This will be changed in the future.
        tableHelper.markAsReactomeNetwork(network, ReactomeNetworkType.FactorGraph);
        tableHelper.storeDataSetType(network, 
                                    "PathwayDiagram");
        if (pathwayId != null)
            tableHelper.storeNetworkAttribute(network,
                                              "PathwayId", 
                                              pathwayId);
        
        Map<String, String> nodeTypeInfo = generateNodeTypeInfo(fg);
        tableHelper.storeNodeAttributesByName(network, 
                                              "NodeType",
                                              nodeTypeInfo);
        Map<String, String> nodeLabelInfo = generateNodeLabel(fg);
        tableHelper.storeNodeAttributesByName(network, 
                                              "nodeLabel",
                                              nodeLabelInfo);
        Map<String, String> nodeToolTipInfo = generateNodeToolTip(fg);
        tableHelper.storeNodeAttributesByName(network,
                                              "nodeToolTip",
                                              nodeToolTipInfo);
        Map<String, String> sourceIdInfo = generateSourceIdInfo(fg);
        tableHelper.storeNodeAttributesByName(network, 
                                              "SourceIds",
                                              sourceIdInfo);
        
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        
        ServiceReference reference = context.getServiceReference(CyNetworkManager.class.getName());
        CyNetworkManager networkManager = (CyNetworkManager) context.getService(reference);
        networkManager.addNetwork(network);
        networkManager = null;
        context.ungetService(reference);
        
        reference = context.getServiceReference(CyNetworkViewFactory.class.getName());
        CyNetworkViewFactory viewFactory = (CyNetworkViewFactory) context.getService(reference);
        CyNetworkView view = viewFactory.createNetworkView(network);
        viewFactory = null;
        context.ungetService(reference);
        
        reference = context.getServiceReference(CyNetworkViewManager.class.getName());
        CyNetworkViewManager viewManager = (CyNetworkViewManager) context.getService(reference);
        viewManager.addNetworkView(view);
        viewManager = null;
        context.ungetService(reference);
        
        // This FIVisualStyle is not registered as a service to save some coding by
        // avoiding clash with FIVisualStyleImp
        FIVisualStyle visStyler = new FactorGraphVisualStyle();
        visStyler.setVisualStyle(view);
        visStyler.doLayout();
        
        return network;
    }

    private FactorGraph convertPathwayToFactorGraph(Long pathwayId,
                                                    RenderablePathway pathway,
                                                    String escapeNames) throws Exception {
        RESTFulFIService fiService = new RESTFulFIService();
        FactorGraph fg = fiService.convertPathwayToFactorGraph(pathwayId,
                                                               escapeNames);
        if (fg == null || fg.getFactors() == null || fg.getFactors().size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Pathway" + "\"" + pathway.getDisplayName() + "\"" + 
                                          " cannot be converted into a factor graph.",
                                          "No Factor Graph",
                                          JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return fg;
    }
    
    private Map<String, String> generateNodeToolTip(FactorGraph fg) {
        Map<String, String> nodeToolTipInfo = new HashMap<String, String>();
        for (Factor factor : fg.getFactors()) {
            nodeToolTipInfo.put(factor.getId(), 
                                factor.getName());
        }
        for (Variable variable : fg.getVariables()) {
            nodeToolTipInfo.put(variable.getId(), 
                                variable.getName());
        }
        return nodeToolTipInfo;
    }
    
    /**
     * Use this method to generate a customized node labels for a factor graph.
     * @param fg
     * @return
     */
    private Map<String, String> generateNodeLabel(FactorGraph fg) {
        Map<String, String> nodeLabelInfo = new HashMap<String, String>();
        for (Factor factor : fg.getFactors()) {
            // Don't want to display anything for factors
            nodeLabelInfo.put(factor.getId(), null);
        }
        for (Variable variable : fg.getVariables()) {
            String name = variable.getName();
            // Want to remove the compartment information
            if (name.endsWith("]")) {
                int index = name.lastIndexOf("[");
                if (index > 0)
                    name = name.substring(0, index);
            }
            nodeLabelInfo.put(variable.getId(), 
                              name);
        }
        return nodeLabelInfo;
    }
    
    private Map<String, String> generateNodeTypeInfo(FactorGraph fg) {
        Map<String, String> nodeTypeInfo = new HashMap<String, String>();
        for (Factor factor : fg.getFactors()) {
            nodeTypeInfo.put(factor.getId(), "factor");
        }
        for (Variable variable : fg.getVariables()) {
            nodeTypeInfo.put(variable.getId(), "variable");
        }
        return nodeTypeInfo;
    }
    
    private Map<String, String> generateSourceIdInfo(FactorGraph fg) {
        Map<String, String> sourceIdInfo = new HashMap<String, String>();
        for (Factor factor : fg.getFactors()) {
            String dbId = factor.getProperty(ReactomeJavaConstants.DB_ID);
            if (dbId == null)
                continue;
            sourceIdInfo.put(factor.getId(), 
                             dbId);
        }
        for (Variable variable : fg.getVariables()) {
            String dbId = variable.getProperty(ReactomeJavaConstants.DB_ID);
            if (dbId == null)
                continue;
            sourceIdInfo.put(variable.getId(),
                             dbId);
        }
        return sourceIdInfo;
    }
    
    /**
     * A helper method to create a set of interactions from a factor graph object.
     * @param fg
     * @return
     */
    private Set<String> createInteractionsFromFactorGraph(FactorGraph fg) {
        Set<String> edges = new HashSet<String>();
        for (Factor factor : fg.getFactors()) {
            for (Variable var : factor.getVariables()) {
                // Use IDs for creating interactions, which are unique.
                edges.add(factor.getId() + "\t" + 
                          var.getId()); 
            }
        }
        return edges;
    }
    
    /**
     * Use this customized JDialog for the user to enter a list of names for small molecules
     * for escaping during a pathway converting to a factor graph.
     * @author gwu
     *
     */
    private class EscapeNameDialog extends JDialog {
        private JTextArea listTA;
        private boolean isOkClicked;
        private JButton okBtn;
        
        public EscapeNameDialog(JFrame parentFrame) {
            super(parentFrame);
            init();
        }
        
        public void setEscapeNames(String names) {
            listTA.setText(names);
        }
        
        private void init() {
            setTitle("Escape Names");
            
            JPanel contentPane = new JPanel();
            Border border1 = BorderFactory.createEtchedBorder();
            Border border2 = BorderFactory.createEmptyBorder(8, 8, 8, 8);
            contentPane.setBorder(BorderFactory.createCompoundBorder(border1, border2));
            contentPane.setLayout(new BorderLayout(2, 2));
            JLabel label = new JLabel("<html>The following list of small molecules will be excluded"
                    + " from converting into the factor graph:</html>");
            contentPane.add(label, BorderLayout.NORTH);
            String preDefinedList = getPredefinedList();
            listTA = new JTextArea(preDefinedList);
            listTA.setLineWrap(true);
            listTA.setWrapStyleWord(true);
            contentPane.add(new JScrollPane(listTA), BorderLayout.CENTER);
            label = new JLabel("<html>Note: You can edit the above list. Use \", \" to delimit names.</html>");
            contentPane.add(label, BorderLayout.SOUTH);
            
            // Control pane
            JPanel controlPane = new JPanel();
            okBtn = new JButton("OK");
            okBtn.setDefaultCapable(true);
            getRootPane().setDefaultButton(okBtn);
            okBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = true;
                    dispose();
                }
            });
            controlPane.add(okBtn);
            
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = false;
                    dispose();
                }
            });
            controlPane.add(cancelBtn);
            
            JButton resetBtn = new JButton("Reset");
            resetBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    reset();
                }
            });
            controlPane.add(resetBtn);
            
            getContentPane().add(contentPane, BorderLayout.CENTER);
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setLocationRelativeTo(getOwner());
        }
        
        public String getEscapeList() {
            String text = listTA.getText().trim();
            String[] tokens = text.split(",( )?");
            return StringUtils.join(",", Arrays.asList(tokens));
        }
        
        private void reset() {
            listTA.setText(getPredefinedList());
        }
        
        private String getPredefinedList() {
            String names = PlugInObjectManager.getManager().getProperties().getProperty("fgEscapeNames");
            if (names == null)
                names = "ATP, ADP, Pi, H2O, GTP, GDP, CO2, H+";
            String[] escapeNames = names.split(",( )*");
            List<String> escapeList = Arrays.asList(escapeNames);
            return StringUtils.join(", ", escapeList);
        }
    }
}
