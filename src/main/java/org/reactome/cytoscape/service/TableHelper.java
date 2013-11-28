package org.reactome.cytoscape.service;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;

public class TableHelper {
    private final String MCL_ARRAY_CLUSTERING = TableFormatterImpl
            .getMCLArrayClustering();
    private final String SAMPLE_MUTATION_DATA = TableFormatterImpl
            .getSampleMutationData();

    public TableHelper() {
    }

    public void createNewColumn(CyTable table, String columnName, Class<?> type) {
        table.createColumn(columnName, type, Boolean.FALSE);
    }
    
    public void storeFINetworkVersion(CyNetwork network, String version) {
        CyTable netTable = network.getDefaultNetworkTable();
        netTable.getRow(network.getSUID()).set(TableFormatterImpl.getFINetworkVersion(),
                version);
    }
    
    public String getStoredFINetworkVersion(CyNetworkView view) {
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        Long netSUID = view.getModel().getSUID();
        return netTable.getRow(netSUID).get(TableFormatterImpl.getFINetworkVersion(), 
                                            String.class);
    }

    public boolean isFINetwork(CyNetworkView view) {
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
    }
    
    /**
     * Set a network attribute value.
     * @param network
     * @param attName
     * @param value
     */
    public <T> void storeNetworkAttribute(CyNetwork network,
                                          String attName,
                                          T value) {
        CyTable networkTable = network.getDefaultNetworkTable();
        // Make sure networkTable has the attName column
        if (networkTable.getColumn(attName) == null) {
            networkTable.createColumn(attName, 
                                      value.getClass(), 
                                      true);
        }
        CyRow row = networkTable.getRow(network.getSUID());
        row.set(attName,
                value);
    }
    
    /**
     * Get a stored attribute value for a CyNetwork.
     * @param network
     * @param attName
     * @param type
     * @return
     */
    public <T> T getStoredNetworkAttribute(CyNetwork network,
                                           String attName,
                                           Class<T> type) {
        CyTable networkTable = network.getDefaultNetworkTable();
        // Make sure networkTable has the attName column
        if (networkTable.getColumn(attName) == null) {
            return null;
        }
        CyRow row = networkTable.getRow(network.getSUID());
        return row.get(attName, type);
    }
    
    /**
     * A simple method to select a CyEdge in a CyNetwork
     * @param network
     * @param edge
     * @param isSelected
     */
    public void setEdgeSelected(CyNetwork network,
                                CyEdge edge,
                                boolean isSelected) {
        CyTable edgeTable = network.getDefaultEdgeTable();
        CyRow row = edgeTable.getRow(edge.getSUID());
        row.set(CyNetwork.SELECTED, isSelected);
    }
    
    public void setNodeSelected(CyNetwork network,
                                CyNode node,
                                boolean isSelected) {
        CyTable nodeTable = network.getDefaultNodeTable();
        CyRow row = nodeTable.getRow(node.getSUID());
        row.set(CyNetwork.SELECTED, isSelected);
    }
    
    /**
     * Mark a Network as a FINetwork. 
     * @param network
     */
    public void markAsFINetwork(CyNetwork network) {
        storeNetworkAttribute(network,
                              "isReactomeFINetwork",
                              Boolean.TRUE);
    }

    public void storeClusteringType(CyNetwork network, String clusteringType)
    {
        storeNetworkAttribute(network, "clusteringType", clusteringType);
    }
    
    public void storeClusteringType(CyNetworkView view, String clusteringType)
    {
        storeClusteringType(view.getModel(), clusteringType);
    }

    public String getClusteringType(CyNetwork network)
    {
        return getStoredNetworkAttribute(network, "clusteringType", String.class);
    }

    private <V, T> void storeAttributeValue(CyTable cyIdenTable, 
                                            String attributeName,
                                            Map<V, T> idToValue, 
                                            V key,
                                            Long suid) {
        T value = idToValue.get(key);
        cyIdenTable.getRow(suid).set(attributeName, value);
    }

    public void storeNodeAttributesBySUID(CyNetwork network,
                                         String attributeName, 
                                         Map<Long, ?> suIdToValue) {
        CyTable nodeTable = network.getDefaultNodeTable();
        for (Object name : network.getNodeList())  {
            CyNode node = (CyNode) name;
            Long nodeSUID = node.getSUID();
            storeAttributeValue(nodeTable,
                                attributeName,
                                suIdToValue,
                                nodeSUID,
                                nodeSUID);
        }
    }

    public void storeNodeAttributesByName(CyNetwork network,
                                          String attributeName, 
                                          Map<String, ?> nameToValue) {
        if (nameToValue == null || nameToValue.size() == 0)
            return;
        CyTable nodeTable = network.getDefaultNodeTable();
        // Make sure it has the attribute
        if (nodeTable.getColumn(attributeName) == null) {
            Object value = nameToValue.values().iterator().next();
            // This type check may not be reliable enough if a hierarchy of class
            // is used.
            createNewColumn(nodeTable, attributeName, value.getClass());
        }
        for (CyNode node : network.getNodeList()) {
            //CyNode node = (CyNode) name2;
            Long nodeSUID = node.getSUID();
            String name = nodeTable.getRow(nodeSUID).get("name", String.class);
            storeAttributeValue(nodeTable,
                                attributeName, 
                                nameToValue, 
                                name,
                                nodeSUID);
        }
    }

    public void storeNodeAttributesByName(CyNetworkView view, String string,
            Map<String, ?> nameToValue) {
        storeNodeAttributesByName(view.getModel(), string, nameToValue);
    }

    public void storeNodeAttributesBySUID(CyNetworkView view, String attr,
            Map<Long, ?> suIdToValue) {
        storeNodeAttributesBySUID(view.getModel(), 
                                  attr, 
                                  suIdToValue);
    }

    public Map<Long, Object> getNodeTableValuesBySUID(CyNetwork model,
                                                      String attr,
                                                      Class<?> type) {
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
    
    /**
     * Get a stored attribute for a passed CyEdge.
     * @param network
     * @param edge
     * @param att
     * @param type
     * @return
     */
    public <T> T getStoredEdgeAttribute(CyNetwork network,
                                        CyEdge edge,
                                        String att,
                                        Class<T> type) {
        CyTable table = network.getDefaultEdgeTable();
        if (table.getColumn(att) == null)
            return null;
        CyRow row = table.getRow(edge.getSUID());
        if (row == null)
            return null;
        return row.get(att, type);
    }
    
    public <T> T getStoredNodeAttribute(CyNetwork network,
                                       CyNode node,
                                       String att,
                                       Class<T> type) {
        CyTable table = network.getDefaultNodeTable();
        if (table.getColumn(att) == null)
            return null;
        CyRow row = table.getRow(node.getSUID());
        if (row == null)
            return null;
        return row.get(att, type);
    }

    public <T> void storeEdgeAttributesByName(CyNetwork network, 
                                          String attr, 
                                          Map<String, T> nameToValue) {
        // Get a value
        T value = null;
        for (T t : nameToValue.values()) {
            if (t != null) {
                value = t;
                break;
            }
        }
        CyTable edgeTable = network.getDefaultEdgeTable();
        // Make sure networkTable has the attName column
        if (edgeTable.getColumn(attr) == null) {
            edgeTable.createColumn(attr, 
                                   value.getClass(), 
                                   true);
        }
        for (CyEdge edge : network.getEdgeList()) {
            Long edgeSUID = edge.getSUID();
            String name = edgeTable.getRow(edgeSUID).get("name", String.class);
            storeAttributeValue(edgeTable,
                                attr,
                                nameToValue,
                                name, 
                                edgeSUID);
        }
    }
    
    public void storeEdgeAttributesByName(CyNetworkView view, String attr, Map<String, ?> nameToValue) {
        storeEdgeAttributesByName(view.getModel(), 
                                  attr,
                                  nameToValue);
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
