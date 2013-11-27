/*
 * Created on Jul 22, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableProtein;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * Because of the overload of method getBounds() in class BiModalJSplitPane, which is one of JDesktopPane used in Cyotscape,
 * we have to method getVisibleRect() in this customized PathwayEditor in order to provide correct value.
 * @author gwu
 *
 */
public class CyZoomablePathwayEditor extends ZoomablePathwayEditor {
    // For pathway enrichment result highlight
    private PathwayEnrichmentHighlighter pathwayHighlighter;
    
    public CyZoomablePathwayEditor() {
        // Don't need the title
        titleLabel.setVisible(false);
        init();
    }
    
    public void setPathwayEnrichmentHighlighter(PathwayEnrichmentHighlighter highlighter) {
        this.pathwayHighlighter = highlighter;
    }
    
    public PathwayEnrichmentHighlighter getPathwayEnrichmentHighlighter() {
        return this.pathwayHighlighter;
    }
    
    /**
     * Get a set of genes that should be highlighted.
     * @return
     */
    public Set<String> getHitGenes() {
        if (pathwayHighlighter != null)
            return pathwayHighlighter.getHitGenes();
        return new HashSet<String>();
    }
    
    private void init() {
        // Add a popup listener
        getPathwayEditor().addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doPopup(e);
            }
            
        });
    }
    
    private void doPopup(MouseEvent event) {
        @SuppressWarnings("unchecked")
        List<Renderable> selection = getPathwayEditor().getSelection();
        if (selection.size() != 1)
            return;
        Renderable r = selection.get(0);
        if (r.getReactomeId() == null)
            return;
        final Long dbId = r.getReactomeId();
        final String name = r.getDisplayName();
        JPopupMenu popup = new JPopupMenu();
        JMenuItem showDetailed = new JMenuItem("View in Reactome");
        showDetailed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String reactomeURL = PlugInObjectManager.getManager().getProperties().getProperty("ReactomeURL");
                String url = reactomeURL + dbId;
                PlugInUtilities.openURL(url);
            }
        });
        popup.add(showDetailed);
        // If it is a Pathway, add a menu item
        if (r instanceof ProcessNode) {
            JMenuItem openInDiagram = new JMenuItem("Show Diagram");
            openInDiagram.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    showPathwayDiagram(dbId, name);
                }
            });
            popup.add(openInDiagram);
        }
        else if (r instanceof RenderableProtein || r instanceof RenderableComplex) {
            JMenuItem listGenesItem = new JMenuItem("List Genes");
            listGenesItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    listGenes(dbId, name);
                }
            });
            popup.add(listGenesItem);
        }
        popup.show(getPathwayEditor(), 
                   event.getX(),
                   event.getY());
    }
    
    /**
     * Get a gene list
     * @param entityId
     */
    private void listGenes(Long entityId,
                           String name) {
        try {
            RESTFulFIService service = new RESTFulFIService();
            String genes = service.listGenes(entityId);
            
            JEditorPane pane = new JEditorPane();
            pane.setEditable(false);
            pane.setContentType("text/html");
            pane.setBorder(BorderFactory.createEtchedBorder());
            // Using HTML so that genes can be clicked
            StringBuilder builder = new StringBuilder();
            builder.append("<html><body><br /><br /><b><center><u>");
            // Generate a label
            String text = null;
            if (genes.contains(",") && genes.contains("|")) {
                text = "Genes contained by " + name + " (\",\" for complex subunit, and \"|\" for member in a protein set)";
            }
            else if (genes.contains(",")) {
                text = "Genes contained by " + name + " (\",\" for complex subunit)";
            }
            else if (genes.contains("|")) {
                text = "Genes contained by " + name + " (\"|\" for member in a protein set)";
            }
            else
                text = "Corresponded gene for protein " + name;
            builder.append(text).append("</u></center></b>");
            builder.append("<br /><br />");
            
            builder.append(formatGenesText(genes)).append("</body></html>");
            
            pane.setText(builder.toString());
            pane.addHyperlinkListener(new HyperlinkListener() {
                
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        String gene = e.getDescription();
                        String url = "http://www.genecards.org/cgi-bin/carddisp.pl?gene=" + gene;
                        PlugInUtilities.openURL(url);
                    }
                }
            });
            
            final JDialog dialog = GKApplicationUtilities.createDialog(this, "Gene List");
            dialog.getContentPane().add(new JScrollPane(pane), BorderLayout.CENTER);
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getCancelBtn().setVisible(false);
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            dialog.setSize(400, 300);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(this,
                                          "Error in listing genes in a selected object: " + e,
                                          "Error in Listing Geens",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private String formatGenesText(String genes) {
        Set<String> hitGenes = getHitGenes();
        // Add an extra space before delimit , and |
        StringBuilder builder = new StringBuilder();
        String gene = "";
        builder.append("<a href=\">");
        for (char c : genes.toCharArray()) {
            if (c == '|' || c == ',') {
                builder.append(gene).append("\">").append(gene).append("</a>").append(c).append(" <a ");
                if (hitGenes.contains(gene))
                    builder.append("style=\"background-color:purple;color:white;\" ");
                builder.append(" href=\""); 
                gene = "";
            }
            else
                gene += c;
        }
        builder.append(gene).append("\">").append(gene).append("</a>");
        return builder.toString();
    }
    
    private void showPathwayDiagram(Long pathwayId,
                                    String pathwayName) {
        PathwayDiagramRegistry.getRegistry().showPathwayDiagram(pathwayId);
        if (pathwayHighlighter != null) {
            PathwayInternalFrame pathwayFrame = PathwayDiagramRegistry.getRegistry().getPathwayFrameWithWait(pathwayId);
            pathwayHighlighter.highlightPathways(pathwayFrame, 
                                                 pathwayName);
        }
    }
    
    /**
     * Do selection based on a list of DB_IDs for objects displayed in
     * the wrapped PathwayEditor.
     * @param sourceIds
     */
    public void selectBySourceIds(Collection<Long> sourceIds) {
        List<Renderable> selection = new ArrayList<Renderable>();
        for (Object o : getPathwayEditor().getDisplayedObjects()) {
            Renderable r = (Renderable) o;
            if (sourceIds.contains(r.getReactomeId()))
                selection.add(r);
        }
        getPathwayEditor().setSelection(selection);
    }

    @Override
    protected PathwayEditor createPathwayEditor() {
        return new CyPathwayEditor();
    }

    private class CyPathwayEditor extends PathwayEditor {
        
        public CyPathwayEditor() {
            setEditable(false); // Only used as a pathway diagram view and not for editing
        }

        @Override
        public Rectangle getVisibleRect() {
            if (getParent() instanceof JViewport) {
                JViewport viewport = (JViewport) getParent();
                Rectangle parentBounds = viewport.getBounds();
                Rectangle visibleRect = new Rectangle();
                Point position = SwingUtilities.convertPoint(viewport.getParent(),
                                                             parentBounds.x,
                                                             parentBounds.y,
                                                             this);
                visibleRect.x = position.x;
                visibleRect.y = position.y;
                visibleRect.width = parentBounds.width;
                visibleRect.height = parentBounds.height;
                return visibleRect;
            }
            return super.getVisibleRect();
        }
    }

    
}
