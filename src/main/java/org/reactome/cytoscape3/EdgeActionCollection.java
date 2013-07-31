package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.funcInt.FIAnnotation;

public class EdgeActionCollection
{

    class queryFISourceMenu implements CyEdgeViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0, View<CyEdge> arg1)
        {
            JMenuItem queryFIMenuItem = new JMenuItem("Query FI Source");

            return new CyMenuItem(queryFIMenuItem, 1.0f);
        }

    }

    public void annotateFIs(CyNetworkView view)
    {
        ProgressPane progPane = new ProgressPane();
        progPane.setIndeterminate(true);
        progPane.setText("Fetching FI Annotations...");
        CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
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
                return;
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
    
    class edgeQueryFIMenuItem implements CyEdgeViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view, View<CyEdge> edgeView)
        {
            JMenuItem edgeQueryFIItem = new JMenuItem("Fetch FI Annotations");
            edgeQueryFIItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    
                    
                }
                
            });
            return new CyMenuItem(edgeQueryFIItem, 1.0f);
        }
        
    }
}
