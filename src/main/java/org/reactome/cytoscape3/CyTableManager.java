package org.reactome.cytoscape3;

import java.util.Iterator;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;

public class CyTableManager
{

    public CyTableManager()
    {
        // TODO Auto-generated constructor stub
    }

    public void storeDataSetType(CyNetwork network, String dataSetType)
    {
        CyTable netTable = network.getDefaultNetworkTable();
        netTable.getRow(network).set("dataSetType", dataSetType);
    }

    private Class<?> guessAttributeType(Map<String, ?> idToValue)
    {
        for (Object key : idToValue.keySet())
        {
            Object obj = idToValue.get(key);
            if (obj == null) continue;
            return obj.getClass();
        }
        return null;
    }
    
    private void setAttributeValue(String attributeName,
            Map<String, ?> idToValue, CyTable cyIdenTable, Long cyIdenSUID,
            Class<?> type)
    {
        Object value = idToValue.get(cyIdenSUID);
        if (type == Integer.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Integer) value);
        else if (type == Double.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Double)value);
        else if (type == String.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (String) value);
        else if (type == Boolean.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Boolean) value);
    }
    
    public void loadNodeAttributes(CyNetwork network, String attributeName,
            Map<String, ?> idToValue)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        Class<?> type = guessAttributeType(idToValue);
        for (Iterator<?> it = network.getNodeList().iterator(); it.hasNext();)
        {
            CyNode node = (CyNode) it.next();
            Long nodeSUID = node.getSUID();
            setAttributeValue(attributeName,
                                idToValue,
                                nodeTable,
                                nodeSUID,
                                type);
        }
    }



    public void loadNodeAttributes(CyNetworkView view, String string,
            Map<String, ?> idToValue)
    {
        loadNodeAttributes(view.getModel(), string, idToValue);
    }

    public Map<Long, Object> getNodeTableValues(CyNetwork model, String string)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
