/*
 * Created on Jul 24, 2013
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
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.graphEditor.PathwayEditor;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * Customized JInternalFrame for displaying Reactome pathway diagrams.
 * @author gwu
 *
 */
public class PathwayInternalFrame extends JInternalFrame {
    private ZoomablePathwayEditor pathwayEditor;
    
    /**
     * Default constructor.
     */
    public PathwayInternalFrame() {
        init();
    }
    
    private void init() {
        pathwayEditor = new CyZoomablePathwayEditor();
        pathwayEditor.getPathwayEditor().setEditable(false);
        getContentPane().add(pathwayEditor, BorderLayout.CENTER);
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor.getPathwayEditor();
    }
    
    public ZoomablePathwayEditor getZoomablePathwayEditor() {
        return this.pathwayEditor;
    }
    
    public RenderablePathway getDisplayedPathway() {
        return (RenderablePathway) pathwayEditor.getPathwayEditor().getRenderable();
    }
    
    /**
     * @param title
     * @param resizable
     * @param closable
     * @param maximizable
     * @param iconifiable
     */
    public PathwayInternalFrame(String title, boolean resizable,
                                boolean closable, boolean maximizable, boolean iconifiable) {
        super(title, resizable, closable, maximizable, iconifiable);
        init();
    }
    
    /**
     * Set the pathway diagram in XML to be displayed
     */
    public void setPathwayDiagramInXML(String xml) throws Exception {
        DiagramGKBReader reader = new DiagramGKBReader();
        RenderablePathway pathway = reader.openDiagram(xml);
        pathwayEditor.getPathwayEditor().setRenderable(pathway);
    }
    
    private class CyZoomablePathwayEditor extends ZoomablePathwayEditor {
        
        public CyZoomablePathwayEditor() {
            // Don't need the title
            titleLabel.setVisible(false);
        }
        
        @Override
        protected PathwayEditor createPathwayEditor() {
            return new CyPathwayEditor();
        }
    }
    
    
    /**
     * Because of the overload of method getBounds() in class BiModalJSplitPane, which is one of JDesktopPane used in Cyotscape,
     * we have to method getVisibleRect() in this customized PathwayEditor in order to provide correct value.
     * @author gwu
     *
     */
    private class CyPathwayEditor extends PathwayEditor {
        
        public CyPathwayEditor() {
            init();
        }
        
        private void init() {
            addMouseListener(new MouseAdapter() {

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
        
        private void doPopup(MouseEvent e) {
            List<?> selection = getSelection();
            if (selection.size() != 1)
                return;
            Renderable r = (Renderable) selection.get(0);
            final Long dbId = r.getReactomeId();
            if (dbId == null)
                return;
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
            popup.show(this, e.getX(), e.getY());
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
