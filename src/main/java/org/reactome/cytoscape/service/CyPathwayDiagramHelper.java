/*
 * Created on Jul 28, 2010
 *
 */
package org.reactome.cytoscape.service;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.gk.gkEditor.PathwayOverviewPane;
import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.Node;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * A helper class to handle pathway diagram. This class is designed to be used as a singleton only.
 * @author wgm
 *
 */
public class CyPathwayDiagramHelper {
    private static CyPathwayDiagramHelper helper;
    // Cache these GUIs to avoid duplication
    private JFrame diagramFrame;
    private ZoomablePathwayEditor pathwayEditor;
    // Record the nodes for highlighting so that they can be used later
    private String highlightNodes;
    private PathwayOverviewPane overviewPane;
    private JPanel overviewContainer;
    
    private CyPathwayDiagramHelper() {
    }
    
    public static CyPathwayDiagramHelper getHelper() {
        if (helper == null)
            helper = new CyPathwayDiagramHelper();
        return helper;
    }
    
    /**
     * Highlight a select nodes
     * @param nodes
     */
    public void highlightNodes(String nodes) {
        this.highlightNodes = nodes;
        // Assuming this is a slow process, using a new thread with class pane
        Thread t = new Thread() {
            public void run() {
                ProgressPane progressPane = new ProgressPane();
                progressPane.setIndeterminate(true);
                diagramFrame.setGlassPane(progressPane);
                progressPane.setText("Highlighting proteins...");
                diagramFrame.getGlassPane().setVisible(true);
                if (highlightPathwayDiagram(pathwayEditor, highlightNodes)) {
                    pathwayEditor.getPathwayEditor().repaint(pathwayEditor.getPathwayEditor().getVisibleRect());
                    overviewPane.repaint();
                }
                diagramFrame.getGlassPane().setVisible(false);
            }
        };
        t.start();
    }
    
    public boolean highlightPathwayDiagram(ZoomablePathwayEditor pathwayEditor,
                                           String genes) {
        try {
            // Get the list of reactome ids from the display diagrams
            List<Long> dbIds = new ArrayList<Long>();
            @SuppressWarnings("unchecked")
            List<Renderable> renderables = pathwayEditor.getPathwayEditor().getDisplayedObjects();
            for (Renderable r : renderables) {
                if (r.getReactomeId() == null)
                    continue;
                if (r instanceof Node) {
                    dbIds.add(r.getReactomeId());
                }
            }
            // Ask the server what ids should be highlight
            RESTFulFIService service = new RESTFulFIService();
            List<Long> hitIds = service.highlight(dbIds,
                                                  genes);
            if (hitIds == null || hitIds.size() == 0) {
                return false;
            }
            // Highlight these nodes
            for (Renderable r : renderables) {
                if (r.getReactomeId() == null)
                    continue;
                if (r instanceof Node &&
                    hitIds.contains(r.getReactomeId())) {
//                    r.setForegroundColor(Color.BLUE);
//                  r.setLineColor(Color.BLUE);
                    // As of Sept 28, change the following highlight colors
                    r.setBackgroundColor(FIVisualStyle.NODE_HIGHLIGHT_COLOR); // A kind of purple
                    r.setForegroundColor(Color.WHITE);
                }
            }
            return true;
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(pathwayEditor,
                                          "Cannot highlight nodes: please see log for errors.", 
                                          "Error in Highlighting",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }
    
    public void showPathwayDiagram(String pathway) throws Exception {
        RESTFulFIService service = new RESTFulFIService();
        RenderablePathway diagram = service.queryPathwayDiagram(pathway);
        if (pathwayEditor != null) {
            pathwayEditor.getPathwayEditor().setRenderable(diagram);
            pathwayEditor.setTitle("<html><u>Diagram View: " + diagram.getDisplayName() + "</u></html>");
            overviewPane.setRenderable(diagram);
            diagramFrame.toFront();
        }
        else {
            // Need to create a JFrame to display the diagram
            pathwayEditor = new ZoomablePathwayEditor();
            pathwayEditor.getPathwayEditor().addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        doPathwayEditorPopup(e);
                    }
                }
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger())
                        doPathwayEditorPopup(e);
                }
            });
            pathwayEditor.getPathwayEditor().setRenderable(diagram);
            pathwayEditor.setTitle("<html><u>Diagram View: " + pathway + "</u></html>");
            pathwayEditor.getPathwayEditor().setEditable(false);
            diagramFrame = new JFrame("Pathway Diagram View");
            // Add an overview panel
            overviewContainer = new JPanel();
            overviewContainer.setLayout(new BorderLayout());
            JLabel label = new JLabel("Overview");
            label.setHorizontalAlignment(JLabel.CENTER);
            overviewContainer.add(label, BorderLayout.SOUTH);
            overviewPane = new PathwayOverviewPane();
            overviewContainer.add(overviewPane, BorderLayout.CENTER);
            overviewContainer.setBorder(BorderFactory.createEtchedBorder());
            overviewContainer.setSize(200, 130);
//            overviewContainer.setBounds(800 - overviewPane.getWidth() - 25, 
//                                        30, 
//                                        overviewPane.getWidth(),
//                                        overviewPane.getHeight());
            JLayeredPane layeredPane = diagramFrame.getLayeredPane();
            layeredPane.add(overviewContainer, JLayeredPane.PALETTE_LAYER);
            overviewPane.syncrhonizeScroll(pathwayEditor);
            overviewPane.setParentEditor(pathwayEditor.getPathwayEditor());
            overviewPane.setRenderable(diagram);
            
            diagramFrame.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosed(WindowEvent e) {
                    // GC labeled
                    diagramFrame = null;
                    pathwayEditor = null;
                    overviewPane = null;
                    overviewContainer = null;
                }
            });
            
            diagramFrame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    int totalWidth = diagramFrame.getWidth();
                    overviewContainer.setBounds(totalWidth - overviewPane.getWidth() - 25,
                                                30,
                                                overviewContainer.getWidth(),
                                                overviewContainer.getHeight());
                }
                
            });
            diagramFrame.getContentPane().add(pathwayEditor, BorderLayout.CENTER);
            diagramFrame.setLocationRelativeTo(PlugInObjectManager.getManager().getCytoscapeDesktop());
            diagramFrame.setSize(800, 600);
            diagramFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            diagramFrame.setVisible(true);
        }
    }
    
    private void doPathwayEditorPopup(MouseEvent e) {
        final PathwayEditor editor = (PathwayEditor) e.getSource();
        JPopupMenu popup = new JPopupMenu();
        // Used to view the selected entity
        List<?> selection = editor.getSelection();
        if (selection != null && selection.size() == 1) {
            final Renderable r = (Renderable) selection.get(0);
            if (r.getReactomeId() != null) {
                Action action = new AbstractAction("View Reactome Source") {
                    public void actionPerformed(ActionEvent e) {
                        ReactomeSourceView sourceView = new ReactomeSourceView();
                        // Get a window for a good location
                        Window parent = (Window) SwingUtilities.getAncestorOfClass(Window.class, editor);
                        sourceView.viewReactomeSource(r.getReactomeId(),
                                                      parent == null ? editor : parent);
                    }
                };
                popup.add(action);
                // Show detailed diagram for a process node
                if (r instanceof ProcessNode) {
                    Action subDiagramAction = new AbstractAction("Show Diagram") {
                        public void actionPerformed(ActionEvent e) {
                            showDiagram(r);
                        }
                    };
                    popup.add(subDiagramAction);
                }
            }
        }
//        Action exportDiagramAction = getExportDiagramAction(editor);
//        popup.add(exportDiagramAction);
        else if (selection == null || selection.size() == 0) {
            Action tightNodeAction = new AbstractAction("Tight Nodes") {
                public void actionPerformed(ActionEvent e) {
                    editor.tightNodes();
                }
            };
            popup.add(tightNodeAction);
            Action exportDiagramAction = new AbstractAction("Export Diagram") {
                public void actionPerformed(ActionEvent e) {
                    try {
                        editor.exportDiagram();
                    }
                    catch(IOException exp) {
                        JOptionPane.showMessageDialog(editor,
                                                      "Error in exporting diagram: " + exp,
                                                      "Error in Exporting Diagram",
                                                      JOptionPane.ERROR_MESSAGE);
                        exp.printStackTrace();
                    }
                }
            };
            popup.add(exportDiagramAction);
        }
        if (popup.getComponentCount() > 0)
            popup.show(editor, e.getX(), e.getY());
    }
    
    private void showDiagram(Renderable r) {
        try {
            showPathwayDiagram(r.getReactomeId() + "");
            highlightNodes(highlightNodes);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(diagramFrame,
                                          "Cannot show diagram: " + e,
                                          "Error in Showing Diagram",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
