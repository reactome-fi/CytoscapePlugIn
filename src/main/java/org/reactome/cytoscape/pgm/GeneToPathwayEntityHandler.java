/*
 * Created on Dec 9, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.Selectable;
import org.gk.render.RenderablePathway;
import org.reactome.cytoscape.service.RESTFulFIService;

/**
 * This class is used to handle map and selection from genes to entities displayed in a pathway diagram.
 * @author gwu
 *
 */
public class GeneToPathwayEntityHandler implements Selectable {
    private Map<String, List<Long>> geneToDbIds;
    private Set<Long> selectedDBIds;
    private JTable observationTable;
    
    /**
     * Default constructor.
     */
    public GeneToPathwayEntityHandler() {
        selectedDBIds = new HashSet<>();
    }
    
    /**
     * Set the observation table. The first column should list observation variable names.
     * @param table
     */
    public void setObservationTable(JTable table) {
        this.observationTable = table;
    }

    @Override
    public void setSelection(List selection) {
        if (observationTable == null)
            return;
        ListSelectionModel selectionModel = observationTable.getSelectionModel();
        selectionModel.clearSelection();
        if (selection == null || selection.size() == 0)
            return;
        selectionModel.setValueIsAdjusting(true);
        TableModel model = observationTable.getModel();
        int lastRow = -1;
        for (int i = 0; i < observationTable.getRowCount(); i++) {
            int modelRow = observationTable.convertRowIndexToModel(i);
            String varName = (String) model.getValueAt(modelRow, 0);
            int index = varName.indexOf("_");
            String geneName = varName.substring(0, index);
            List<Long> geneDBIds = geneToDbIds.get(geneName);
            if (isShared(geneDBIds, selection)) {
                selectionModel.addSelectionInterval(i, i);
                lastRow = i;
            }
        }
        selectionModel.setValueIsAdjusting(false);
        if (lastRow > -1) {
            Rectangle rect = observationTable.getCellRect(observationTable.getSelectedRow(),
                                                          0,
                                                          false);
            observationTable.scrollRectToVisible(rect);
        }
    }

    @Override
    public List getSelection() {
        selectedDBIds.clear();
        if (observationTable != null) {
            int[] selectedRows = observationTable.getSelectedRows();
            if (selectedRows != null && selectedRows.length > 0) {
                TableModel model = observationTable.getModel(); // We have to use model in case column's order is changed
                for (int selectedRow : selectedRows) {
                    String name = (String) model.getValueAt(observationTable.convertRowIndexToModel(selectedRow),
                                                            0);
                    int index = name.indexOf("_");
                    String gene = name.substring(0, index);
                    List<Long> dbIds = geneToDbIds.get(gene);
                    if (dbIds != null)
                        selectedDBIds.addAll(dbIds);
                }
            }
        }
        return new ArrayList<Long>(selectedDBIds);
    }
    
    private boolean isShared(Collection<?> dbIds1, Collection<?> dbIds2) {
        for (Object dbId1 :dbIds1) {
            if (dbIds2.contains(dbId1))
                return true;
        }
        return false;
    }
    
    public void enableDiagramSelection(final PathwayEditor editor) {
        // Use a new thread to load the map from genes to dbIds
        Thread t = new Thread() {
            public void run() {
                loadGenesToDBIds(editor);
            }
        };
        t.start(); // Use a new thread so that the loading will not interfere the user's work.
    }
    
    private void loadGenesToDBIds(PathwayEditor pathwayEditor) {
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
