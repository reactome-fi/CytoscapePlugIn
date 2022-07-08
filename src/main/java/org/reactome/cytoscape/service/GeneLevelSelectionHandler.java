/*
 * Created on Apr 12, 2016
 *
 */
package org.reactome.cytoscape.service;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import org.gk.graphEditor.Selectable;
import org.reactome.booleannetwork.BooleanVariable;

/**
 * @author gwu
 *
 */
@SuppressWarnings("rawtypes")
public class GeneLevelSelectionHandler implements Selectable {
    private JTable geneLevelTable;
    
    public GeneLevelSelectionHandler() {
    }
    
    public JTable getGeneLevelTable() {
        return geneLevelTable;
    }

    public void setGeneLevelTable(JTable geneLevelTable) {
        this.geneLevelTable = geneLevelTable;
    }

    @Override
    public void setSelection(List selection) {
        if (geneLevelTable == null)
            return;
        ListSelectionModel selectionModel = geneLevelTable.getSelectionModel();
        selectionModel.setValueIsAdjusting(true);
        selectionModel.clearSelection();
        TableModel model = geneLevelTable.getModel();
        int lastRow = -1;
        for (int i = 0; i < model.getRowCount(); i++) {
        	String value = null;
        	Object obj = model.getValueAt(i, 0);
        	if (obj instanceof String)
        		value = obj.toString();
        	else if (obj instanceof BooleanVariable)
        		value = ((BooleanVariable)obj).getName();
        	else
        		value = obj.toString();
            if (selection.contains(value)) {
                int row = geneLevelTable.convertRowIndexToView(i);
                selectionModel.addSelectionInterval(row, row);
                if (row > lastRow)
                    lastRow = row;
            }
        }
        selectionModel.setValueIsAdjusting(false);
        if (lastRow > -1) {
            Rectangle rect = geneLevelTable.getCellRect(lastRow, 0, false);
            geneLevelTable.scrollRectToVisible(rect);
            // Apparently in Java 11, MacOS, repaint() is needed (NB by GW on July 8, 2022)
//            geneLevelTable.validate();
            geneLevelTable.repaint();
        }
    }

    @Override
    public List getSelection() {
        if (geneLevelTable == null)
            return new ArrayList<>();
        List<Object> selection = new ArrayList<>();
        int[] rows = geneLevelTable.getSelectedRows();
        if (rows != null && rows.length > 0) {
            TableModel model = geneLevelTable.getModel();
            for (int row : rows) {
                int modelRow = geneLevelTable.convertRowIndexToModel(row);
                Object value = model.getValueAt(modelRow, 0);
                if (value instanceof String)
                	selection.add(value);
                else if (value instanceof BooleanVariable) // This is more like a hacking wawy
                	selection.add(((BooleanVariable)value).getName());
                else
                	selection.add(value.toString());
            }
        }
        return selection;
    }
    
}
