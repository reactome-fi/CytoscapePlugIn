package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.service.FISourceQueryHelper;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.funcInt.FIAnnotation;
/**
 * A class which contains functions performed on edges
 * in the FI network.
 * @author Eric T. Dawson
 *
 */
public class EdgeActionCollection {
    
    public static void annotateFIs(final CyNetworkView view) {
        ProgressPane progPane = new ProgressPane();
        progPane.setIndeterminate(true);
        progPane.setText("Fetching FI Annotations...");
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        JFrame parentFrame = desktopApp.getJFrame();
        parentFrame.setGlassPane(progPane);
        parentFrame.getGlassPane().setVisible(true);
        TableHelper tableHelper = new TableHelper();
        List<CyEdge> edges = null;
        List<CyEdge> annotatedEdges = new ArrayList<CyEdge>();
        List<CyEdge> unannotatedEdges = new ArrayList<CyEdge>();
        for (View<CyEdge> edgeView : view.getEdgeViews()) {
            CyEdge edge = edgeView.getModel();
            
            if (tableHelper.hasEdgeAttribute(view, edge, "FI Direction", String.class))
                annotatedEdges.add(edge);
            else
                unannotatedEdges.add(edge);
        }
        if (!annotatedEdges.isEmpty() && !unannotatedEdges.isEmpty()) {
            int reply = JOptionPane.showConfirmDialog(parentFrame, "Some FIs have already been annotated. Would you like to annotate\n" +
                    "only those FIs without annotations? Choosing \"No\" will annotate all FIs.",
                    "FI Annotation", JOptionPane.YES_NO_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION) {
                parentFrame.getGlassPane().setVisible(false); 
                return;
            }
            if (reply == JOptionPane.YES_OPTION)
                edges = unannotatedEdges;
            else {
                edges = unannotatedEdges;
                edges.addAll(annotatedEdges);
            }
        }
        else if (annotatedEdges.size() > 0) {
            int reply = JOptionPane.showConfirmDialog(parentFrame, "All FIs have been annotated. Would you like to re-annotate them?",
                                                      "FI Annotation", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.NO_OPTION) { 
                parentFrame.getGlassPane().setVisible(false); 
                return;
            }
            edges = annotatedEdges;
        }
        else if (unannotatedEdges.size() > 0)
            edges = unannotatedEdges;
        
        if (edges == null || edges.size() == 0) {
            JOptionPane.showMessageDialog(parentFrame,
                                          "No FIs need to be annotated.",
                                          "FI Annotation", 
                                          JOptionPane.INFORMATION_MESSAGE);
            parentFrame.setVisible(false);
            return;
        }
        
        try {
            Map<String, String> edgeToAnnotation = new HashMap<String, String>();
            Map<String, String> edgeToDirection = new HashMap<String, String>();
            Map<String, Double> edgeToScore = new HashMap<String, Double>();
            RESTFulFIService service = new RESTFulFIService(view);
            Map<String, FIAnnotation> edgeIdToAnnotation = service.annotate(edges, view);
            for (String edgeId : edgeIdToAnnotation.keySet()) {
                FIAnnotation annotation = edgeIdToAnnotation.get(edgeId);
                edgeToAnnotation.put(edgeId, annotation.getAnnotation());
                edgeToDirection.put(edgeId, annotation.getDirection());
                edgeToScore.put(edgeId, annotation.getScore());
            }
            tableHelper.storeEdgeAttributesByName(view, "FI Annotation", edgeToAnnotation);
            tableHelper.storeEdgeAttributesByName(view, "FI Direction", edgeToDirection);
            tableHelper.storeEdgeAttributesByName(view, "FI Score", edgeToScore);
        } 
        catch (Exception t) {
            PlugInUtilities.showErrorMessage("Error in annotating FIs",
                                             "FI Annotation failed. Please try again.");
            t.printStackTrace();
        }
        parentFrame.getGlassPane().setVisible(false);
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
                    queryFISource(view, edgeView);
                }
                
            });
            return new CyMenuItem(edgeQueryFIItem, 1.0f);
        }
        
    }
    
    private void queryFISource(CyNetworkView view,
                               View<CyEdge> edgeView) {
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        Long sourceSUID =  edgeView.getModel().getSource().getSUID();
        String source = nodeTable.getRow(sourceSUID).get("name", String.class);
        
        Long targetSUID = edgeView.getModel().getTarget().getSUID();
        String target = nodeTable.getRow(targetSUID).get("name", String.class);
        
        FISourceQueryHelper helper = new FISourceQueryHelper();
        helper.queryFISource(source, target, view);
    }
    
}
