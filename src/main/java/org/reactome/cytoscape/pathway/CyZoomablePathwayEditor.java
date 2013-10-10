/*
 * Created on Jul 22, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.graphEditor.PathwayEditor;

/**
 * Because of the overload of method getBounds() in class BiModalJSplitPane, which is one of JDesktopPane used in Cyotscape,
 * we have to method getVisibleRect() in this customized PathwayEditor in order to provide correct value.
 * @author gwu
 *
 */
public class CyZoomablePathwayEditor extends ZoomablePathwayEditor {
    
    public CyZoomablePathwayEditor() {
        // Don't need the title
        titleLabel.setVisible(false);
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
