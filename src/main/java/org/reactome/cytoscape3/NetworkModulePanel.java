package org.reactome.cytoscape3;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.r3.util.FileUtility;
import org.reactome.r3.util.InteractionUtilities;


@SuppressWarnings("serial")
public abstract class NetworkModulePanel extends JPanel implements CytoPanelComponent, RowsSetListener {
    private String title = "";
    // Used to control view
    protected JCheckBox hideOtherNodesBox;
    // Table for display detailed information on modules
    protected JTable contentTable;
    protected CyNetworkView view;
    // Used for control
    protected JToolBar controlToolBar;
    // The CytoPanel container used to hold this Panel.
    protected CytoPanel container;
    // Used to control selection
    private boolean isFromTable;
    // Used to link to table selection
    private ListSelectionListener tableSelectionListener;
    // To control its position
    protected JButton closeBtn;
    protected Component closeGlue;
    protected FileUtil fileUtil;

    public NetworkModulePanel()
    {
        init();
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceRegistration servReg = context.registerService(CytoPanelComponent.class.getName(), this, new Properties());
    }
    
    public NetworkModulePanel(String title)
    {
        setTitle(title);
        init();
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(CytoPanelComponent.class.getName(), this, new Properties());
        ServiceReference servRef = context.getServiceReference(FileUtil.class.getName());
        if (servRef != null)
            this.fileUtil = (FileUtil) context.getService(servRef);
    }
    
    public void setNetworkView(CyNetworkView view)
    {
        this.view = view;
    }
    
    @Override
    public void handleEvent(RowsSetEvent event) {
        if (view == null)
            return;
        if (isFromTable) { // Split these two checks for debugging purpose
            return;
        }
        if (!event.containsColumn(CyNetwork.SELECTED)) {
            return;
        }
        CyNetwork network = view.getModel();
        List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network,
                                                                 CyNetwork.SELECTED,
                                                                 true);
        List<String> nodeIds = new ArrayList<String>();
        for (CyNode node : selectedNodes) {
            nodeIds.add(network.getRow(node).get(CyNetwork.NAME, String.class));
        }
        selectTableRowsForNodes(nodeIds);
    }

    private void init()
    {
        // Register selection listener
        PlugInObjectManager.getManager().getBundleContext().registerService(RowsSetListener.class.getName(), 
                                                                            this, 
                                                                            new Properties());
        setLayout(new BorderLayout());
        contentTable = new JTable();
        TableModel moduleModel = createTableModel();
        contentTable.setModel(moduleModel);
        add(new JScrollPane(contentTable), BorderLayout.CENTER);
        tableSelectionListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                doTableSelection();
            }
        };
        contentTable.getSelectionModel().addListSelectionListener(tableSelectionListener);
        // Used to control other nodes
        controlToolBar = new JToolBar();
        controlToolBar.setFloatable(false);
        hideOtherNodesBox = new JCheckBox("Hide nodes in not selected rows");
        hideOtherNodesBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doHideOtherNodesAction();
            }
        });
        controlToolBar.add(hideOtherNodesBox);
        // Add a close button
        closeBtn = new JButton(new CloseTabIcon());
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        closeBtn.setToolTipText("close tab");
        closeBtn.setRolloverEnabled(true);
        closeGlue = Box.createHorizontalGlue();
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        add(controlToolBar, BorderLayout.NORTH);
        
        //TODO if a network is destroyed/removed, remove the module panel as well.
        
        
     // Add a popup menu to show detailed information for pathways
        contentTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doContentTablePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doContentTablePopup(e);
            }
        });
        this.setVisible(true);
    }
    
    private void doHideOtherNodesAction()
    {
        if (!hideOtherNodesBox.isSelected())
        {
            showAllEdges();
            showAllNodes();
        }
        doTableSelection();
    }
    private void showAllEdges()
    {
        if (this.view != null)
        {
            for (View<CyEdge> edgeView : view.getEdgeViews())
            {
                NetworkActionCollection.showEdge(edgeView);
            }
        }
    }
    
    private void showAllNodes()
    {
        if (this.view != null)
        {
            for (View<CyNode> nodeView : view.getNodeViews())
            {
                NetworkActionCollection.showNode(nodeView);
            }
        }
    }
    
    private void doTableSelection()
    {
        //Check if there is a network view.
        if (this.view == null)
            return;
        //Retrieve a selection from user input.
        Set<String> selectedNodes = getSelectedNodes();
        //Select the given nodes.
        isFromTable = true;
        if (hideOtherNodesBox.isSelected())
        {
            //First, show all of the edges. This is an old
            //fix from the original app.
            showAllEdges();
            //Cover the corner case in which no nodes are selected.
            if (selectedNodes.isEmpty())
            {
                showAllNodes();
            }
            else
            {
                showAllEdges();
                CyTable nodeTable = view.getModel().getDefaultNodeTable();
                for (View<CyNode> nodeView : view.getNodeViews())
                {
                    Long nodeSUID = nodeView.getModel().getSUID();
                    String nodeName = nodeTable.getRow(nodeSUID).get("name", String.class);
                    if (selectedNodes.contains(nodeName))
                        NetworkActionCollection.showNode(nodeView);
                    else
                        NetworkActionCollection.hideNode(nodeView);
                }
            }
            //Redraw edges. Otherwise, the edges don't appear if nodes
            //have been hidden/unhidden.
            showAllEdges();
        }
        else
        {
            showAllEdges();
            for (View<CyNode> nodeView : view.getNodeViews())
            {
                Long nodeSUID = nodeView.getModel().getSUID();
                String nodeName = view.getModel().getDefaultNodeTable().getRow(nodeSUID).get("name", String.class);
                setSelectedOrUnselected(nodeView, selectedNodes.contains(nodeName));
            }
        }
        view.updateView();
        isFromTable = false; // Make sure updateView() is called before reset the flag to avoid multiple node selection.
    }
    
    private void setSelectedOrUnselected(View<CyNode> nodeView, boolean value)
    {
        Long nodeSUID = nodeView.getModel().getSUID();
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        nodeTable.getRow(nodeSUID).set("selected", value);
    }
    
    protected abstract NetworkModuleTableModel createTableModel();

    public void close()
    {
        if (container == null)
            return;
        //To have the rest of the network reappear after
        //closing the browser panel, uncomment the next two lines.
        //this.hideOtherNodesBox.setSelected(false);
        //showAllEdges();
        ((Container) container).remove(this);

    }
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    public void setContainer(CytoPanel container)
    {
        this.container = container;
    }
    
    @Override
    public Component getComponent()
    {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName()
    {
        return CytoPanelName.SOUTH;
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }

    @Override
    public Icon getIcon()
    {
        return null;
    }
    
    
    /**
     * Default implementation to export the whole table.
     * @param e
     */
    protected void doContentTablePopup(MouseEvent e) {
        JPopupMenu popup = createExportAnnotationPopup();
        popup.show(contentTable, 
                   e.getX(), 
                   e.getY());
    }
    
    protected void exportAnnotations() {
        // Export annotations in a text file
        Collection<FileChooserFilter> fileFilters = new ArrayList<FileChooserFilter>();
        FileChooserFilter fileFilter = new FileChooserFilter("A file containing FI annotations", ".txt");
        fileFilters.add(fileFilter);
        Collection<FileChooserFilter> filters = new ArrayList<FileChooserFilter>();
        FileChooserFilter filter = new FileChooserFilter("Annotation File", "txt");
        filters.add(filter);
        FIPlugInHelper r = FIPlugInHelper.getHelper();
        File file = fileUtil.getFile(PlugInObjectManager.getManager().getCytoscapeDesktop(), "Save Annotation File", FileUtil.SAVE, filters);
        NetworkModuleTableModel model = (NetworkModuleTableModel) contentTable.getModel();
        FileUtility fu = new FileUtility();
        try {
            fu.setOutput(file.getAbsolutePath());
            // Output headers
            StringBuilder builder = new StringBuilder();
            int colCount = model.getColumnCount();
            for (int i = 0; i < colCount; i++) {
                builder.append(model.getColumnName(i));
                if (i < colCount - 1)
                    builder.append("\t");
            }
            fu.printLine(builder.toString());
            builder.setLength(0);
            for (int i = 0; i < contentTable.getRowCount(); i++) {
                for (int j = 0; j < colCount; j++) {
                    builder.append(model.getValueAt(i, j));
                    if (j < colCount - 1)
                        builder.append("\t");
                }
                fu.printLine(builder.toString());
                builder.setLength(0);
            }
            fu.close();
        }
        catch(IOException e) {
            System.err.println("NetworkModulePanel.exportAnnotations(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                                          "Error in exporting annotations: " + e.getMessage(),
                                          "Error in Exporting",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    protected JPopupMenu createExportAnnotationPopup() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem export = new JMenuItem("Export Annotations");
        export.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportAnnotations();
            }
        });
        popupMenu.add(export);
        return popupMenu;
    }
    
    protected abstract class NetworkModuleTableModel extends AbstractTableModel {
        protected List<String[]> tableData;
        protected String[] columnHeaders;
        
        public NetworkModuleTableModel() {
            tableData = new ArrayList<String[]>();
        }
        
        protected int getNodeIdColumn() {
            return getColumnCount() - 1;
        }
        
        /**
         * nodes ids in the same row should be delimited by ",".
         * @param rows
         * @return
         */
        public List<String> getNodeIdsAtRows(int[] rows) {
            List<String> rtn = new ArrayList<String>();
            int idCol = getNodeIdColumn();
            for (int row : rows) {
                String[] rowData = tableData.get(row);
                rtn.add(rowData[idCol]);
            }
            return rtn;
        }
        
        /**
         * Get a list of rows containing any of passed node ids.
         * @param nodes
         * @return
         */
        public List<Integer> getRowsForNodeIds(List<String> nodes) {
            List<Integer> rows = new ArrayList<Integer>();
            int idCol = getNodeIdColumn();
            for (int i = 0; i < tableData.size(); i++) {
                String[] rowData = tableData.get(i);
                String[] ids = rowData[idCol].split(",");
                for (String id : ids) {
                    if (nodes.contains(id)) {
                        rows.add(i);
                        break;
                    }
                }
            }
            return rows;
        }

        public int getColumnCount() {
            return columnHeaders.length;
        }

        public int getRowCount() {
            return tableData.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            // Just in case
            if (rowIndex >= tableData.size())
                return null;
            String[] rowData = tableData.get(rowIndex);
            return rowData[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public String getColumnName(int column) {
            return columnHeaders[column];
        }
        
        protected String createIDText(Collection<String> ids) {
            List<String> list = new ArrayList<String>(ids);
            Collections.sort(list);
            return InteractionUtilities.joinStringElements(",", list);
        }
    }
    
    
    protected Set<String> getSelectedNodes() {
        int[] selectedRows = contentTable.getSelectedRows();
        NetworkModuleTableModel model = (NetworkModuleTableModel) contentTable.getModel();
        List<String> idsInRows = model.getNodeIdsAtRows(selectedRows);
        // Do a selection
        Set<String> selectedIds = new HashSet<String>();
        for (String ids : idsInRows) {
            String[] tokens = ids.split(",");
            for (String token : tokens)
                selectedIds.add(token);
        }
        return selectedIds;
    }
    
    private void selectTableRowsForNodes(List<String> nodes) {
        NetworkModuleTableModel model = (NetworkModuleTableModel) contentTable.getModel();
        List<Integer> rows = model.getRowsForNodeIds(nodes);
        // Need to disable table selection to avoid circular calling
        contentTable.getSelectionModel().removeListSelectionListener(tableSelectionListener);
        ListSelectionModel selectionModel = contentTable.getSelectionModel();
        selectionModel.clearSelection();
        Integer min = Integer.MAX_VALUE;
        for (Integer row : rows) {
            selectionModel.addSelectionInterval(row, row);
            if (row < min)
                min = row;
        }
        if (rows.size() > 0) { // Only for selection
            // Need to scroll
            Rectangle rect = contentTable.getCellRect(min, 0, false);
            contentTable.scrollRectToVisible(rect);
        }
        contentTable.getSelectionModel().addListSelectionListener(tableSelectionListener);
    }
  
    /**
     * The class which generates the 'X' icon for the tabs. The constructor 
     * accepts an icon which is extra to the 'X' icon, so you can have tabs 
     * like in JBuilder. This value is null if no extra icon is required. 
     * @author Mr_Silly @jdc modified by wgm.
     * */
    class CloseTabIcon implements Icon
    {
        private int x_pos;
        private int y_pos;
        private int width;
        private int height;
        //private Icon fileIcon;
        
        public CloseTabIcon() {
            width = 14;
            height = 14;
        }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            x_pos = x;
            y_pos = y;
            Color col = g.getColor();
            g.setColor(Color.black);
            int b = 4;
            g.drawLine(x + b - 1, y + b, x + width - b - 1, y + height - b);
            g.drawLine(x + b , y + b, x + width - b, y + height - b);
            g.drawLine(x + width - b - 1, y + b, x + b - 1, y + height - b);
            g.drawLine(x + width - b, y + b, x + b, y + height - b);
            g.setColor(col);
            //if (fileIcon != null) {
            //  fileIcon.paintIcon(c, g, x + width, y_p);
            //}
        }
        public int getIconWidth() {
            return width;
        }
        public int getIconHeight() {
            return height;
        }
        public Rectangle getBounds() {
            return new Rectangle(x_pos, y_pos, width, height);
        }
    }
}
