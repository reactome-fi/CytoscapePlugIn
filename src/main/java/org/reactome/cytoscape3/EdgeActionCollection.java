package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.border.Border;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.gk.util.ProgressPane;

import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.funcInt.FIAnnotation;
import org.reactome.funcInt.Interaction;
import org.reactome.funcInt.ReactomeSource;
/**
 * A class which contains functions performed on edges
 * in the FI network.
 * @author Eric T. Dawson
 *
 */
public class EdgeActionCollection
{

    public static void annotateFIs(final CyNetworkView view)
    {
                ProgressPane progPane = new ProgressPane();
                progPane.setIndeterminate(true);
                progPane.setText("Fetching FI Annotations...");
                final CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
                desktopApp.getJFrame().setGlassPane(progPane);
                desktopApp.getJFrame().getGlassPane().setVisible(true);
                TableHelper tableHelper = new TableHelper();
                List<CyEdge> edges = null;
                List<CyEdge> annotatedEdges = new ArrayList<CyEdge>();
                List<CyEdge> unannotatedEdges = new ArrayList<CyEdge>();
                for (View<CyEdge> edgeView : view.getEdgeViews())
                {
                    CyEdge edge = edgeView.getModel();

                    if (tableHelper.hasEdgeAttribute(view, edge, "FI Direction", String.class))
                        annotatedEdges.add(edge);
                    else
                        unannotatedEdges.add(edge);
                }
                if (!annotatedEdges.isEmpty() && !unannotatedEdges.isEmpty())
                {
                    int reply = JOptionPane.showConfirmDialog(desktopApp.getJFrame(), "Some FIs have already been annotated. Would you like to annotate\n" +
                            "only those FIs without annotations? Choosing \"No\" will annotate all FIs.",
                            "FI Annotation", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (reply == JOptionPane.CANCEL_OPTION)
                        return;
                    if (reply == JOptionPane.YES_OPTION)
                        edges = unannotatedEdges;
                    else
                    {
                        edges = unannotatedEdges;
                        edges.addAll(annotatedEdges);
                    }
                }
                else if (annotatedEdges.size() > 0)
                {
                    int reply = JOptionPane.showConfirmDialog(desktopApp.getJFrame(), "All FIs have been annotated. Would you like to re-annotate them?",
                            "FI Annotation", JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.NO_OPTION)
                    { desktopApp.getJFrame().getGlassPane().setVisible(false); return;}
                    edges = annotatedEdges;
                }
                else if (unannotatedEdges.size() > 0)
                    edges = unannotatedEdges;
                if (edges == null || edges.size() == 0)
                {
                    JOptionPane.showMessageDialog(desktopApp.getJFrame(),
                            "No FIs need to be annotated.",
                            "FI Annotation", 
                            JOptionPane.INFORMATION_MESSAGE);
                    desktopApp.getJFrame().getGlassPane().setVisible(false);
                    return;
                }
                try{
                    Map<String, String> edgeToAnnotation = new HashMap<String, String>();
                    Map<String, String> edgeToDirection = new HashMap<String, String>();
                    Map<String, Double> edgeToScore = new HashMap<String, Double>();
                    RESTFulFIService service = new RESTFulFIService(view);
                    Map<String, FIAnnotation> edgeIdToAnnotation = service.annotate(edges, view);
                    for (String edgeId : edgeIdToAnnotation.keySet())
                    {
                        FIAnnotation annotation = edgeIdToAnnotation.get(edgeId);
                        edgeToAnnotation.put(edgeId, annotation.getAnnotation());
                        edgeToDirection.put(edgeId, annotation.getDirection());
                        edgeToScore.put(edgeId, annotation.getScore());
                    }
                    tableHelper.loadEdgeAttributesByName(view, "FI Annotation", edgeToAnnotation);
                    tableHelper.loadEdgeAttributesByName(view, "FI Direction", edgeToDirection);
                    tableHelper.loadEdgeAttributesByName(view, "FI Score", edgeToScore);
                } 
                catch (Throwable t)
                {
                    PlugInUtilities.showErrorMessage("Error in annotating FIs", "FI Annotation failed. Please try again.");
                    t.printStackTrace();
                }
                desktopApp.getJFrame().getGlassPane().setVisible(false);
                progPane = null;
    }

    public static void setEdgeNames(CyNetworkView view)
    {
        TableHelper tableHelper = new TableHelper();
        if (view.getModel().getEdgeCount() != 0)
        {
            for (CyEdge edge : view.getModel().getEdgeList())
            {
                tableHelper.storeEdgeName(edge, view);
            }
        }
        tableHelper = null;
    }

    class EdgeQueryFIMenuItem implements CyEdgeViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view, final View<CyEdge> edgeView)
        {
            JMenuItem edgeQueryFIItem = new JMenuItem("Query FI Source");
            edgeQueryFIItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    fetchInteraction(view, edgeView);
                }

            });
            return new CyMenuItem(edgeQueryFIItem, 1.0f);
        }

    }
    private void fetchInteraction(CyNetworkView view,
            View<CyEdge> edgeView)
    {
        ProgressPane progPane = new ProgressPane();
        progPane.setText("Fetching FI Annotation(s)");
        CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
        desktopApp.getJFrame().setGlassPane(progPane);
        desktopApp.getJFrame().getGlassPane().setVisible(true);
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        Long sourceSUID =  edgeView.getModel().getSource().getSUID();
        String source = nodeTable.getRow(sourceSUID).get("name", String.class);

        Long targetSUID = edgeView.getModel().getTarget().getSUID();
        String target = nodeTable.getRow(targetSUID).get("name", String.class);
        try
        {

            RESTFulFIService fiService = new RESTFulFIService(view);
            List<Interaction> interactions = fiService.queryEdge(source, target);
            //There should be exactly one reaction
            if (interactions.isEmpty())
            {
                PlugInUtilities.showErrorMessage("Error in FI Query", "No interactions could be found.");
                return;
            }
            displayInteraction(edgeView, interactions, source, target);
        }
        catch (Exception e)
        {
            PlugInUtilities.showErrorMessage("Error in Fetching Annotation", "There was an error in fetching the FI annotation. Please try again.");
        }
        desktopApp.getJFrame().getGlassPane().setVisible(false);
    }
    private void displayInteraction(View<CyEdge> edgeView, List<Interaction> interactions, String source, String target)
    {
        CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
        JDialog dialog = new JDialog(desktopApp.getJFrame());
        dialog.setTitle("Interaction Info");
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(desktopApp.getJFrame());
        JPanel supportPanel = createSupportPane(interactions);
        dialog.getContentPane().add(supportPanel, BorderLayout.CENTER);
        // Add a label
        JLabel label = new JLabel("Interaction: " + source + " - " + target);
        // Add an etched border for label
        Border emptyBorder = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        label.setBorder(BorderFactory.createCompoundBorder(etchedBorder, emptyBorder));
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.BOLD));
        dialog.getContentPane().add(label, BorderLayout.NORTH);
        dialog.setVisible(true);
    }
    private JPanel createSupportPane(List<Interaction> interactions)
    {
        JPanel supportPane = new JPanel();
        supportPane.setBorder(BorderFactory.createEtchedBorder());
        supportPane.setLayout(new BorderLayout());
        JTabbedPane supportTabbedPane = new JTabbedPane();
        List<Interaction> predictedFIs = new ArrayList<Interaction>();
        List<Interaction> pathwayFIs = new ArrayList<Interaction>();
        /*
         * It is possible that one pair of FIs may actually come from two
         * different sources due to the fact that data comes from normalized
         * amino acid data from Uniprot.
         */
        for (Interaction i : interactions)
        {
            if (i.getEvidence() == null)
                pathwayFIs.add(i);
            else
                predictedFIs.add(i);
        }
        if (pathwayFIs.isEmpty())
        {
            //Grab the FIs with the highest score.
            Interaction highest = null;
            for (Interaction i : predictedFIs)
            {
                if (highest == null)
                    highest = i;
                else if (highest.getEvidence().getProbability() < i.getEvidence().getProbability())
                    highest = i;
            }
            JTable evidenceTable = new JTable();
            EvidenceTableModel evidenceModel = new EvidenceTableModel();
            evidenceModel.setEvidence(highest.getEvidence());
            evidenceTable.setModel(evidenceModel);
            supportTabbedPane.addTab("Support Evidence",
                    new JScrollPane(evidenceTable));
        }
        else
        {
            //Only allow pathway FIs.
            Set<ReactomeSource> allSources = new HashSet<ReactomeSource>();
            for (Interaction interaction : pathwayFIs)
                allSources.addAll(interaction.getReactomeSources());
            setReactomeSourceTab(supportTabbedPane, allSources);
        }
        supportPane.add(supportTabbedPane, BorderLayout.CENTER);
        return supportPane;
    }
    private void setReactomeSourceTab(JTabbedPane supportTabbedPane,
            Set<ReactomeSource> sources) {
        final JTable sourceTable = new JTable();
        sourceTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) 
                    doSourceTablePopup(sourceTable, e);
                else if (e.getClickCount() == 2) {
                    showReactomeSource(sourceTable);
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doSourceTablePopup(sourceTable, e);
            }
        });
        sourceTable.setToolTipText("Double click or right clik to use popup menu for details");
        ReactomeSourceTableModel sourceModel = new ReactomeSourceTableModel();
        sourceModel.setReactomeSources(sources);
        sourceTable.setModel(sourceModel);
        supportTabbedPane.addTab("Reactome Sources",
                new JScrollPane(sourceTable));
    }
    private void doSourceTablePopup(final JTable table, 
            MouseEvent e) 
    {
        // Work for one selection only
        final int selectedRow = table.getSelectedRow();
        if (selectedRow < 0)
            return;
        JPopupMenu popup = new JPopupMenu();
        JMenuItem goToReactome = new JMenuItem("Open Reactome Source");
        goToReactome.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showReactomeSource(table);
            }
        });
        popup.add(goToReactome);
        popup.show(table, e.getX(), e.getY());
    }
    private void showReactomeSource(final JTable table) {
        // Work for one selection only
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0)
            return;
        ReactomeSourceTableModel tableModel = (ReactomeSourceTableModel) table.getModel();
        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        String dataSourceURL = PlugInScopeObjectManager.getManager().getDataSourceURL();
        if (dataSourceURL == null) {
            PlugInUtilities.showErrorMessage("Data source URL has not been set.", 
                    "Error in Opening Source");
        }
        PlugInUtilities.openURL(dataSourceURL + id);
    }
}
