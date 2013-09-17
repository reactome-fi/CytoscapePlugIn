package org.reactome.cytoscape3;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;

public class TableHelper
{
    private final String FI_NETWORK_VERSION = TableFormatterImpl
            .getFINetworkVersion();
    private final String MCL_ARRAY_CLUSTERING = TableFormatterImpl
            .getMCLArrayClustering();
    private final String SAMPLE_MUTATION_DATA = TableFormatterImpl
            .getSampleMutationData();

    public TableHelper()
    {
    }

    public void createNewColumn(CyTable table, String columnName, Class<?> type)
    {
        table.createColumn(columnName, type, Boolean.FALSE);
    }
    public void storeFINetworkVersion(CyNetwork network)
    {
        CyTable netTable = network.getDefaultNetworkTable();
        String version = PlugInScopeObjectManager.getManager()
                .getFiNetworkVersion();
        netTable.getRow(network.getSUID()).set(FI_NETWORK_VERSION,
                version);
    }
    public void storeFINetworkVersion(CyNetworkView view)
    {
        storeFINetworkVersion(view.getModel());
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
        netTable.getRow(network.getSUID()).set("dataSetType", dataSetType);
        netTable.getRow(network.getSUID()).set("isReactomeFINetwork", true);
    }

    private Class<?> guessAttributeType(Map<?, ?> idToValue)
    {
        for (Object key : idToValue.keySet())
        {
            Object obj = idToValue.get(key);
            if (obj == null)
            {
                continue;
            }
            return obj.getClass();
        }
        return null;
    }

    public void storeClusteringType(CyNetwork network, String clusteringType)
    {
        CyTable netTable = network.getDefaultNetworkTable();
        Long netSUID = network.getSUID();
        netTable.getRow(netSUID).set("clustering_Type", clusteringType);
    }
    public void storeClusteringType(CyNetworkView view, String clusteringType)
    {
        storeClusteringType(view.getModel(), clusteringType);
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
        {
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Integer) value);
        }
        else if (type == Double.class)
        {
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Double) value);
        }
        else if (type == String.class)
        {
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (String) value);
        }
        else if (type == Boolean.class)
        {
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Boolean) value);
        }
    }

    public void loadNodeAttributesBySUID(CyNetwork network,
            String attributeName, Map<Long, ?> idToValue)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        Class<?> type = guessAttributeType(idToValue);
        for (Object name : network.getNodeList())
        {
            CyNode node = (CyNode) name;
            Long nodeSUID = node.getSUID();
            setAttributeValueBySUID(attributeName, idToValue, nodeTable,
                    nodeSUID, type);
        }
    }

    private void setAttributeValueByName(String attributeName,
            Map<String, ?> idToValue, CyTable cyIdenTable, Long cyIdenSUID,
            String idenName, Class<?> type)
    {
        Object value = idToValue.get(idenName);
        if (type == Integer.class)
        {
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Integer) value);
        }
        else if (type == Double.class)
        {
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Double) value);
        }
        else if (type == String.class)
        {
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (String) value);
        }
        else if (type == Boolean.class)
        {
            cyIdenTable.getRow(cyIdenSUID).set(attributeName, (Boolean) value);
        }
    }

    public void loadNodeAttributesByName(CyNetwork network,
            String attributeName, Map<String, ?> idToValue)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        Class<?> type = guessAttributeType(idToValue);
        for (CyNode node : network.getNodeList())
        {
            //CyNode node = (CyNode) name2;
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
        for (Object name : nodeTable.getColumns())
        {
            CyColumn column = (CyColumn) name;
            if (column.getName().equals(attr))
            {
                isFound = true;
                break;
            }
        }
        if (!isFound) return null;
        Map<Long, Object> idToValue = new HashMap<Long, Object>();
        for (Object name : model.getNodeList())
        {
            CyNode node = (CyNode) name;
            Long nodeSUID = node.getSUID();
            Object value = nodeTable.getRow(nodeSUID).get(attr, type);
            if (value == null)
            {
                continue;
            }
            idToValue.put(nodeSUID, value);
        }
        return idToValue;
    }

    public Map<String, Object> getNodeTableValuesByName(CyNetwork model,
            String attr, Class<?> type)
    {

        CyTable nodeTable = model.getDefaultNodeTable();
        boolean isFound = false;
        for (Object name : nodeTable.getColumns())
        {
            CyColumn column = (CyColumn) name;
            if (column.getName().equals(attr))
            {
                isFound = true;
                break;
            }
        }
        if (!isFound) return null;
        Map<String, Object> idToValue = new HashMap<String, Object>();
        for (Object name2 : model.getNodeList())
        {
            CyNode node = (CyNode) name2;
            Long nodeSUID = node.getSUID();
            String name = nodeTable.getRow(nodeSUID).get("name", String.class);
            Object value = nodeTable.getRow(nodeSUID).get(attr, type);
            if (value == null)
            {
                continue;
            }
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

    public void loadEdgeAttributesByName(CyNetwork network, String attr, Map<String, ?> idToValue)
    {
        CyTable edgeTable = network.getDefaultEdgeTable();
        Class<?> type = guessAttributeType(idToValue);
        for (CyEdge edge : network.getEdgeList())
        {
            Long edgeSUID = edge.getSUID();
            String name = edgeTable.getRow(edgeSUID).get("name", String.class);
            setAttributeValueByName(attr, idToValue, edgeTable, edgeSUID, name, type);
        }
    }
    public void loadEdgeAttributesByName(CyNetworkView view, String attr, Map<String, ?> idToValue)
    {
        loadEdgeAttributesByName(view.getModel(), attr, idToValue);
    }
    public boolean hasEdgeAttribute(CyNetworkView view, CyEdge edge, String attr, Class<?> t)
    {
        CyTable edgeTable = view.getModel().getDefaultEdgeTable();
        if (edgeTable.getRow(edge.getSUID()).get(attr, t) != null)
        {
            if( edgeTable.getRow(edge.getSUID()).get(attr, t) instanceof String && ((String) edgeTable.getRow(edge.getSUID()).get(attr, t)).length() <= 0)
                return false;
            return true;
        }
        return false;
    }

    public String getDataSetType(CyNetworkView view)
    {
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        Long netSUID = view.getModel().getSUID();
        String dataType = null;
        dataType = netTable.getRow(netSUID).get("dataSetType", String.class);
        return dataType;
    }

}
