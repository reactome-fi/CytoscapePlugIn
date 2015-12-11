/*
 * Created on Dec 9, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import org.gk.graphEditor.PathwayEditor;
import org.gk.render.Renderable;
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
    private boolean isFromTable;
    private boolean isFromPathway;
    
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
        if (isFromPathway)
            return;
        if (pathwayEditor == null || geneToDbIds == null || geneToDbIds.size() == 0)
            return;
        isFromTable = true;
        // Get selected genes
        Set<Long> dbIdsForSelection = new HashSet<Long>();
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows != null && selectedRows.length > 0) {
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
        }
        PlugInUtilities.selectByDbIds(pathwayEditor, dbIdsForSelection);
        isFromTable = false;
    }
    
    public void handlePathwaySelection(JTable table,
                                       int obsVarNameCol) {
        if (isFromTable)
            return;
        if (pathwayEditor == null || geneToDbIds == null || geneToDbIds.size() == 0)
            return;
        isFromPathway = true;
        ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.clearSelection();
        selectionModel.setValueIsAdjusting(true);
        @SuppressWarnings("unchecked")
        List<Renderable> selection = pathwayEditor.getSelection();
        Set<Long> dbIds = new HashSet<>();
        for (Renderable r : selection) {
            Long dbId = r.getReactomeId();
            dbIds.add(dbId);
        }
        if (dbIds.size() > 0) {
            for (int i = 0; i < table.getRowCount(); i++) {
                String varName = (String) table.getValueAt(i, obsVarNameCol);
                int index = varName.indexOf("_");
                String geneName = varName.substring(0, index);
                List<Long> geneDBIds = geneToDbIds.get(geneName);
                if (isShared(geneDBIds, dbIds))
                    selectionModel.addSelectionInterval(i, i);
            }
        }
        selectionModel.setValueIsAdjusting(false);
        if (table.getSelectedRow() > -1) {
            Rectangle rect = table.getCellRect(table.getSelectedRow(),
                                              0,
                                              false);
            table.scrollRectToVisible(rect);
        }
        isFromPathway = false;
    }
    
    private boolean isShared(Collection<Long> dbIds1, Set<Long> dbIds2) {
        for (Long dbId1 :dbIds1) {
            if (dbIds2.contains(dbId1))
                return true;
        }
        return false;
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
