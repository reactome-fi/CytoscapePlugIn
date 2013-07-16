package org.reactome.cytoscape3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;

public class CyTableManager
{
    private final String FI_NETWORK_VERSION = CyTableFormatter
            .getFINetworkVersion();
    private final String MCL_ARRAY_CLUSTERING = CyTableFormatter
            .getMCLArrayClustering();
    private final String SAMPLE_MUTATION_DATA = CyTableFormatter
            .getSampleMutationData();

    public CyTableManager()
    {
    }

    public void storeFINetworkVersion(CyNetworkView view)
    {
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        String version = PlugInScopeObjectManager.getManager()
                .getFiNetworkVersion();
        netTable.getRow(view.getModel().getSUID()).set(FI_NETWORK_VERSION,
                version);
    }

    public String getStoredFINetworkVersion(CyNetworkView view)
    {
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        Long netSUID = view.getModel().getSUID();
        return netTable.getRow(netSUID).get(FI_NETWORK_VERSION, String.class);
    }

    public boolean isFINetwork(CyNetworkView view)
    {
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        Long netSUID = view.getModel().getSUID();
        Boolean isFINetwork = netTable.getRow(netSUID).get(
                "isReactomeFINetwork", Boolean.class);

        if (isFINetwork != null) return isFINetwork;

        // For sessions loaded from a file, it is necessary to set
        // the value again.
        String dataSetType = netTable.getRow(view.getModel().getSUID()).get(
                "dataSetType", String.class);
        if (dataSetType.equals(MCL_ARRAY_CLUSTERING)
                || dataSetType.equals(SAMPLE_MUTATION_DATA)) return true;
        return false;
    }

    public void storeDataSetType(CyNetwork network, String dataSetType)
    {
        CyTable netTable = network.getDefaultNetworkTable();
        netTable.getRow(network).set("dataSetType", dataSetType);
    }

    private Class<?> guessAttributeType(Map<?, ?> idToValue)
    {
        for (Object key : idToValue.keySet())
        {
            Object obj = idToValue.get(key);
            if (obj == null) continue;
            return obj.getClass();
        }
        return null;
    }

    public void storeClusteringType(CyNetworkView view, String clusteringType)
    {
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        Long netSUID = view.getModel().getSUID();
        netTable.getRow(netSUID).set("clusteringType", clusteringType);
    }

    public String getClusteringType(CyNetwork network)
    {
        CyTable netTable = network.getDefaultNetworkTable();
        String clusteringType = netTable.getRow(network.getSUID()).get(
                "clusteringType", String.class);
        return clusteringType;
    }

    private void setAttributeValueBySUID(String attributeName,
            Map<Long, ?> idToValue, CyTable cyIdenTable, Long cyIdenSUID,
            Class<?> type)
    {
        Object value = idToValue.get(cyIdenSUID);
        if (type == Integer.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Integer) value);
        else if (type == Double.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Double) value);
        else if (type == String.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (String) value);
        else if (type == Boolean.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Boolean) value);
    }

    public void loadNodeAttributesBySUID(CyNetwork network,
            String attributeName, Map<Long, ?> idToValue)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        Class<?> type = guessAttributeType(idToValue);
        for (Iterator<?> it = network.getNodeList().iterator(); it.hasNext();)
        {
            CyNode node = (CyNode) it.next();
            Long nodeSUID = node.getSUID();
            setAttributeValueBySUID(attributeName, idToValue, nodeTable,
                    nodeSUID, type);
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
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Double) value);
        else if (type == String.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (String) value);
        else if (type == Boolean.class)
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Boolean) value);
    }

    public void loadNodeAttributesByName(CyNetwork network,
            String attributeName, Map<String, ?> idToValue)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        Class<?> type = guessAttributeType(idToValue);
        for (Iterator<?> it = network.getNodeList().iterator(); it.hasNext();)
        {
            CyNode node = (CyNode) it.next();
            Long nodeSUID = node.getSUID();
            String name = nodeTable.getRow(nodeSUID).get("name", String.class);
            setAttributeValueByName(attributeName, idToValue, nodeTable,
                    nodeSUID, name, type);
        }
    }

    public void loadNodeAttributesByName(CyNetworkView view, String string,
            Map<String, ?> idToValue)
    {
        loadNodeAttributesByName(view.getModel(), string, idToValue);
    }

    public void loadNodeAttributesBySUID(CyNetworkView view, String attr,
            Map<Long, ?> idToValue)
    {
        loadNodeAttributesBySUID(view.getModel(), attr, idToValue);
    }

    public Map<Long, Object> getNodeTableValuesBySUID(CyNetwork model,
            String attr, Class<?> type)
    {

        CyTable nodeTable = model.getDefaultNodeTable();
        boolean isFound = false;
        for (Iterator<?> it = nodeTable.getColumns().iterator(); it.hasNext();)
        {
            CyColumn column = (CyColumn) it.next();
            if (column.getName().equals(attr))
            {
                isFound = true;
                break;
            }
        }
        if (!isFound) return null;
        Map<Long, Object> idToValue = new HashMap<Long, Object>();
        for (Iterator<?> it = model.getNodeList().iterator(); it.hasNext();)
        {
            CyNode node = (CyNode) it.next();
            Long nodeSUID = node.getSUID();
            Object value = nodeTable.getRow(nodeSUID).get(attr, type);
            if (value == null) continue;
            idToValue.put(nodeSUID, value);
        }
        return idToValue;
    }

    public Map<String, Object> getNodeTableValuesByName(CyNetwork model,
            String attr, Class<?> type)
    {

        CyTable nodeTable = model.getDefaultNodeTable();
        boolean isFound = false;
        for (Iterator<?> it = nodeTable.getColumns().iterator(); it.hasNext();)
        {
            CyColumn column = (CyColumn) it.next();
            if (column.getName().equals(attr))
            {
                isFound = true;
                break;
            }
        }
        if (!isFound) return null;
        Map<String, Object> idToValue = new HashMap<String, Object>();
        for (Iterator<?> it = model.getNodeList().iterator(); it.hasNext();)
        {
            CyNode node = (CyNode) it.next();
            Long nodeSUID = node.getSUID();
            String name = nodeTable.getRow(nodeSUID).get("name", String.class);
            Object value = nodeTable.getRow(nodeSUID).get(attr, type);
            if (value == null) continue;
            idToValue.put(name, value);
        }
        return idToValue;
    }

    public void storeEdgeName(CyEdge edge, CyNetworkView view)
    {
        // This method stores the edge name in a manner consistent with the
        // 2.x era cytoscape app. This is because some of the older Reactome
        // scripts rely on this format.
        CyTable edgeTable = view.getModel().getDefaultEdgeTable();
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        Long sourceID = edge.getSource().getSUID();
        String sourceName = nodeTable.getRow(sourceID)
                .get("name", String.class);
        Long targetID = edge.getTarget().getSUID();
        String targetName = nodeTable.getRow(targetID)
                .get("name", String.class);

        String edgeName = sourceName + " (FI) " + targetName;
        edgeTable.getRow(edge.getSUID()).set("name", edgeName);
    }

}
