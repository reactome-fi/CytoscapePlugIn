/*
 * Created on Dec 9, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.gk.graphEditor.PathwayEditor;
import org.gk.render.RenderablePathway;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This class is used to handle map and selection from genes to entities displayed in a pathway diagram.
 * @author gwu
 *
 */
public class GeneToPathwayEntityHandler {
    private PathwayEditor pathwayEditor;
    private Map<String, List<Long>> geneToDbIds;
    
    /**
     * Default constructor.
     */
    public GeneToPathwayEntityHandler() {
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    public void handleTableSelection(JTable table,
                                     int obsVarNameCol) {
        if (pathwayEditor == null || geneToDbIds == null || geneToDbIds.size() == 0)
            return;
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0)
            return;
        // Get selected genes
        Set<Long> dbIdsForSelection = new HashSet<Long>();
        TableModel model = table.getModel(); // We have to use model in case column's order is changed
        for (int selectedRow : selectedRows) {
            String name = (String) model.getValueAt(table.convertRowIndexToModel(selectedRow),
                                                    obsVarNameCol);
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
