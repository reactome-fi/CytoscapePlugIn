/*
 * Created on Oct 6, 2006
 *
 */
package org.reactome.cytoscape.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.reactome.funcInt.ReactomeSource;

public class ReactomeSourceTableModel extends AbstractTableModel {
    
    private String[] colNames = new String[] {
            "Reactome ID",
            "Type",
            "Data Source"
    };
    private List<ReactomeSource> sources;
    
    public ReactomeSourceTableModel() {
    }
    
    public void setReactomeSources(Set<ReactomeSource> newSrcs) {
        if (sources == null)
            sources = new ArrayList<ReactomeSource>();
        else
            sources.clear();
        if (newSrcs != null)
            sources.addAll(newSrcs);
        super.fireTableDataChanged();
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }

    public int getColumnCount() {
        return colNames.length;
    }
    
    public int getRowCount() {
        return sources == null ? 0 : sources.size();
    }
    
    public Object getValueAt(int rowIndex, int columnIndex) {
        ReactomeSource src = sources.get(rowIndex);
        switch(columnIndex) {
            case 0 :
                return src.getReactomeId();
            case 1 :
                return src.getSourceType().toString();
            case 2 :
                return src.getDataSource();
        }
        return null;
    }
    
}
