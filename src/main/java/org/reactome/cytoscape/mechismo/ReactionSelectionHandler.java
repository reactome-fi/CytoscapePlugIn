package org.reactome.cytoscape.mechismo;

import java.util.List;

import javax.swing.table.TableModel;

import org.reactome.cytoscape.bn.VariableSelectionHandler;

public class ReactionSelectionHandler extends VariableSelectionHandler {

    @Override
    public List getSelection() {
        selectedIds.clear();
        TableModel model = variableTable.getModel();
        int[] selectedRows = variableTable.getSelectedRows();
        if (selectedRows != null) {
            for (int selectedRow : selectedRows) {
                int modelRow = variableTable.convertRowIndexToModel(selectedRow);
                Object value = model.getValueAt(modelRow, 0);
                selectedIds.add((Long)value);
            }
        }
        return selectedIds;
    }

}
