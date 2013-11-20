/*
 * Created on Oct 6, 2006
 *
 */
package org.reactome.cytoscape3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.funcInt.Evidence;

public class EvidenceTableModel extends AbstractTableModel {
    private String[] columnNames = new String[] {
            "Predictor",
            "Value"
    };
    // Need to keep the order. So a map cannot be used.
    private List<String> props;
    private List<String> propNames;
    // Displayed Evidence
    private Evidence evidence;
    
    // No column names here
    public EvidenceTableModel() {
        // Need to create evidence property to name mapping
        props = new ArrayList<String>();
        propNames = new ArrayList<String>();
        String prop = PlugInObjectManager.getManager().getProperties().getProperty("evidenceProperties");
        String names = PlugInObjectManager.getManager().getProperties().getProperty("evidenceNames");
        String[] propTokens = prop.split(",");
        String[] nameTokens = names.split(",");
        for (int i = 0; i < propTokens.length; i++) {
            props.add(propTokens[i]);
            propNames.add(nameTokens[i]);
        }
    }
    
    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
        // Have to check to make sure empty properties are not shown
        List<String> removedProps = new ArrayList<String>();
        List<String> removedNames = new ArrayList<String>();
        for (int i = 0; i < props.size(); i++) {
            String prop = props.get(i);
            Object value = getPropertyValue(evidence, prop);
            if (value == null) {
                removedProps.add(prop);
                removedNames.add(propNames.get(i));
            }
        }
        props.removeAll(removedProps);
        propNames.removeAll(removedNames);
        super.fireTableDataChanged();
    }
    
    private Object getPropertyValue(Evidence evidence,
                                    String prop) {
        try {
            String methodName = "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
            Method method = Evidence.class.getMethod(methodName);
            Object value = method.invoke(evidence);
            return value;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public int getColumnCount() {
        return 2;
    }
    
    public String getColumnName(int col) {
        return columnNames[col];
    }
    
    public int getRowCount() {
        return propNames.size();
    }
    
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return propNames.get(rowIndex);
        }
        else {
            if (evidence == null)
                return null;
            String prop = props.get(rowIndex);
            return getPropertyValue(evidence, prop);
        }
    }

}
