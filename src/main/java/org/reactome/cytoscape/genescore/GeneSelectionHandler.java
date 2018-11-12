/*
 * Created on Apr 25, 2017
 *
 */
package org.reactome.cytoscape.genescore;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import org.gk.graphEditor.Selectable;

@SuppressWarnings({"unchecked", "rawtypes"})
public class GeneSelectionHandler implements Selectable {
    private JTable geneScoreTable;
    private Map<Long, Set<String>> dbIdToGenes;
    private Map<String, List<Long>> geneToDBIDs;
    
    public GeneSelectionHandler() {
    }

    public void setDbIdToGenes(Map<Long, Set<String>> dbIdToGenes) {
        this.dbIdToGenes = dbIdToGenes;
    }

    public void setGeneToDBIDs(Map<String, List<Long>> geneToDBIDs) {
        this.geneToDBIDs = geneToDBIDs;
    }

    public void setGeneScoreTable(JTable sampleTable) {
        this.geneScoreTable = sampleTable;
    }

    /**
     * The passed selection should be a list of DB_IDs.
     */
    @Override
    public void setSelection(List selection) {
        TableModel tableModel = geneScoreTable.getModel();
        ListSelectionModel selectionModel = geneScoreTable.getSelectionModel();
        selectionModel.clearSelection();
        selectionModel.setValueIsAdjusting(true);
        Set<String> genes = new HashSet<>();
        selection.forEach(obj -> {
            Long dbId = (Long) obj;
            Set<String> set = dbIdToGenes.get(dbId);
            if (set != null)
                genes.addAll(set);
        });
        for (int i = 0; i < tableModel.getRowCount(); i++) {        
            String gene = (String) tableModel.getValueAt(i, 0);
            if (genes.contains(gene)) {
                int viewRow = geneScoreTable.convertRowIndexToView(i);
                selectionModel.addSelectionInterval(viewRow, viewRow);
            }
        }
        selectionModel.setValueIsAdjusting(false);
        // Need to scroll
        int selected = geneScoreTable.getSelectedRow();
        if (selected > -1) {
            Rectangle rect = geneScoreTable.getCellRect(selected, 0, false);
            geneScoreTable.scrollRectToVisible(rect);
        }
    }

    @Override
    public List getSelection() {
        TableModel model = geneScoreTable.getModel();
        int[] selectedRows = geneScoreTable.getSelectedRows();
        Set<Long> selectedIds = new HashSet<>();
        if (selectedRows != null) {
            for (int selectedRow : selectedRows) {
                int modelRow = geneScoreTable.convertRowIndexToModel(selectedRow);
                String value = (String) model.getValueAt(modelRow, 0);
                List<Long> dbIds = geneToDBIDs.get(value);
                if (dbIds != null)
                    selectedIds.addAll(dbIds);
            }
        }
        return new ArrayList<>(selectedIds);
    }
}