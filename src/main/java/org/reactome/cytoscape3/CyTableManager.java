package org.reactome.cytoscape3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;

public class CyTableManager
{
    private final String FI_NETWORK_VERSION = CyTableFormatter.getFINetworkVersion();
    public CyTableManager()
    {
        // TODO Auto-generated constructor stub
    }
    
    public void storeFINetworkVersion(CyNetworkView view)
    {
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        String version = PlugInScopeObjectManager.getManager().getFiNetworkVersion();
        netTable.getRow(view.getModel().getSUID()).set(FI_NETWORK_VERSION, version);
    }
    public boolean isFINetwork(CyNetworkView view)
    {
        return true;
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
    private void setAttributeValueByName(String attributeName,
            Map<String, ?> idToValue, CyTable cyIdenTable, Long cyIdenSUID,
            String nodeName, Class<?> type)
    {
        Object value = idToValue.get(nodeName);
        if (type == Integer.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Integer) value);
        else if (type == Double.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Double)value);
        else if (type == String.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (String) value);
        else if (type == Boolean.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Boolean) value);
    }
    
    public void loadNodeAttributesByName(CyNetwork network, String attributeName,
            Map<String, ?> idToValue)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        Class<?> type = guessAttributeType(idToValue);
        for (Iterator<?> it = network.getNodeList().iterator(); it.hasNext();)
        {
            CyNode node = (CyNode) it.next();
            Long nodeSUID = node.getSUID();
            String name = nodeTable.getRow(nodeSUID).get("name", String.class);
            setAttributeValueByName(attributeName, idToValue, nodeTable, nodeSUID,
                                    name, type);
        }
    }


    public void loadNodeAttributesByName(CyNetworkView view, String string,
            Map<String, ?> idToValue)
    {
        loadNodeAttributesByName(view.getModel(), string, idToValue);
    }
    public void loadNodeAttributes(CyNetworkView view, String string,
            Map<String, ?> idToValue)
    {
        loadNodeAttributesByName(view.getModel(), string, idToValue);
    }
    public Map<Long, Object> getNodeTableValues(CyNetwork model, String attr, Class<?> type)
    {
        
        CyTable nodeTable = model.getDefaultNodeTable();
        boolean isFound = false;
        for (Iterator<?> it = nodeTable.getColumns().iterator(); it.hasNext();)
        {
            CyColumn column = (CyColumn) it.next();
            if (column.getName().equals(attr))
            {isFound = true; break;}
        }
        if (!isFound)
            return null;
        Map<Long, Object> idToValue = new HashMap<Long, Object>();
        for (Iterator<?> it = model.getNodeList().iterator(); it.hasNext();)
        {
            CyNode node = (CyNode) it.next();
            Long nodeSUID = node.getSUID();
            Object value = nodeTable.getRow(nodeSUID).get(attr, type);
            if (value == null)
                continue;
            idToValue.put(nodeSUID, value);
        }
        return idToValue;
    }

}
