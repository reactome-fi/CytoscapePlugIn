/*
 * Created on Jul 16, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.gk.graphEditor.PathwayEditor;
import org.gk.persistence.DiagramGKBReader;
import org.gk.render.RenderablePathway;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This class is used to load pathway diagram from a Reactome database via RESTful API.
 * @author gwu
 *
 */
public class PathwayLoadTask extends AbstractTask {
    private String reactomeRestfulURL;
    private JDesktopPane desktop;
    
    public PathwayLoadTask() {
    }
    
    public void setReactomeRestfulURL(String url) {
        this.reactomeRestfulURL = url;
    }
    
    public void setDesktopPane(JDesktopPane desktopPane) {
        this.desktop = desktopPane;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Loading Pathway");
        taskMonitor.setProgress(0);
        taskMonitor.setStatusMessage("Load pathway diagram...");
        // This is just for test by query pathway diagram for Cell Cycle Checkpoints 
        Long dbId = 69620L;
        String url = reactomeRestfulURL + "pathwayDiagram/69620/xml";
        String text = PlugInUtilities.callHttpInText(url, PlugInUtilities.HTTP_GET, "");
//        System.out.println(text);
        PathwayEditor pathwayEditor = createPathwayEditor(text);
        JInternalFrame pathwayFrame = createInteranalFrame(pathwayEditor);
        desktop.add(pathwayFrame);
    }
    
    private JInternalFrame createInteranalFrame(PathwayEditor pathwayEditor) {
        JInternalFrame pathwayFrame = new JInternalFrame("Pathway: Cell Cycle Checkpoints", true, true, true, true);
        pathwayFrame.setSize(600, 450);
        JScrollPane jsp = new JScrollPane();
        jsp.setViewportView(pathwayEditor);
//        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
//        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pathwayFrame.getContentPane().add(jsp, 
                                          BorderLayout.CENTER);
        pathwayFrame.setVisible(true);
        return pathwayFrame;
    }
    
    private PathwayEditor createPathwayEditor(String xml) throws Exception {
        final PathwayEditor editor = new CyPathwayEditor();
        DiagramGKBReader reader = new DiagramGKBReader();
        RenderablePathway pathway = reader.openDiagram(xml);
        editor.setRenderable(pathway);
        editor.setEditable(false);
        return editor;
    }
    
}
