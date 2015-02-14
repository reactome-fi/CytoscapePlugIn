/*
 * Created on Feb 13, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.graphEditor.PathwayEditor;
import org.gk.render.RenderablePathway;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This is a subclass for showing some gene level information in a dialog.
 * @author gwu
 *
 */
public abstract class GeneLevelDialog extends JDialog {
    // For diagram selection
    private PathwayEditor pathwayEditor;
    private Map<String, List<Long>> geneToDbIds;
    
    /**
     * Default constructor.
     */
    public GeneLevelDialog() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    protected abstract void init();
    
    protected void addTableSelectionListener(final JTable table) {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    return;
                selectEntitiesInDiagram(table);
            }
        });
    }
    
    private void selectEntitiesInDiagram(JTable table) {
        if (pathwayEditor == null || geneToDbIds == null || geneToDbIds.size() == 0)
            return;
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0)
            return;
        // Get selected genes
        Set<Long> dbIdsForSelection = new HashSet<Long>();
        for (int selectedRow : selectedRows) {
            String name = (String) table.getValueAt(selectedRow, 0); // The first should be variable name
            int index = name.indexOf("_");
            String gene = name.substring(0, index);
            List<Long> dbIds = geneToDbIds.get(gene);
            if (dbIds != null)
                dbIdsForSelection.addAll(dbIds);
        }
        if (dbIdsForSelection.size() == 0)
            return; // Just do nothing to avoid de-selection
        PlugInUtilities.selectByDbIds(pathwayEditor, dbIdsForSelection);
    }
    
    public void enableDiagramSelection(PathwayEditor editor) {
        this.pathwayEditor = editor;
        // Use a new thread to load the map from genes to dbIds
        Thread t = new Thread() {
            public void run() {
                loadGenesToDBIds();
            }
        };
        t.start(); // Use a new thread so that the loading will not interfere the user's work.
    }
    
    private void loadGenesToDBIds() {
        if (pathwayEditor == null)
            return;
        RESTFulFIService service = new RESTFulFIService();
        RenderablePathway diagram = (RenderablePathway) pathwayEditor.getRenderable();
        try {
            geneToDbIds = service.getGeneToDbIds(diagram.getReactomeDiagramId());
        }
        catch(Exception e) {
            e.printStackTrace(); // Don't want to show anything to the user.
        }
    }
    
}
