/*
 * Created on Apr 25, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import org.gk.graphEditor.Selectable;
import org.reactome.booleannetwork.BooleanVariable;

public class VariableSelectionHandler implements Selectable {
    
    private JTable variableTable;
    private List<Long> selectedIds; // Use a single List object to save some GC
    
    public VariableSelectionHandler() {
        selectedIds = new ArrayList<>();
    }

    public JTable getVariableTable() {
        return variableTable;
    }

    public void setVariableTable(JTable sampleTable) {
        this.variableTable = sampleTable;
    }

    @Override
    public void setSelection(List selection) {
        TableModel tableModel = variableTable.getModel();
        if (!(tableModel instanceof VariableTableModelInterface))
            return;
        VariableTableModelInterface vTableModel = (VariableTableModelInterface) tableModel;
        ListSelectionModel selectionModel = variableTable.getSelectionModel();
        selectionModel.clearSelection();
        selectionModel.setValueIsAdjusting(true);
        int index = 0;
        List<Integer> rows = vTableModel.getRowsForSelectedIds(selection);
        for (Integer modelRow : rows) {
            int viewRow = variableTable.convertRowIndexToView(modelRow);
            selectionModel.addSelectionInterval(viewRow, viewRow);
        }
        selectionModel.setValueIsAdjusting(false);
        // Need to scroll
        int selected = variableTable.getSelectedRow();
        if (selected > -1) {
            Rectangle rect = variableTable.getCellRect(selected, 0, false);
            variableTable.scrollRectToVisible(rect);
        }
    }

    @Override
    public List getSelection() {
        selectedIds.clear();
        TableModel model = variableTable.getModel();
        int[] selectedRows = variableTable.getSelectedRows();
        if (selectedRows != null) {
            for (int selectedRow : selectedRows) {
                int modelRow = variableTable.convertRowIndexToModel(selectedRow);
                Object value = model.getValueAt(modelRow, 0);
                if (value instanceof BooleanVariable) {
                    BooleanVariable var = (BooleanVariable) value;
                    String reactomeId = var.getProperty("reactomeId");
                    if (reactomeId != null)
                        selectedIds.add(new Long(reactomeId));
                }
            }
        }
        return selectedIds;
    }
}