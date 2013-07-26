//package org.reactome.cytoscape3;
//
//import java.awt.BorderLayout;
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Graphics;
//import java.awt.Rectangle;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.MouseEvent;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Set;
//
//import javax.swing.Box;
//import javax.swing.Icon;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JMenuItem;
//import javax.swing.JPanel;
//import javax.swing.JPopupMenu;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JToolBar;
//import javax.swing.event.ListSelectionEvent;
//import javax.swing.event.ListSelectionListener;
//import javax.swing.table.AbstractTableModel;
//import javax.swing.table.TableModel;
//
//import org.cytoscape.application.swing.CytoPanel;
//import org.cytoscape.application.swing.CytoPanelComponent;
//import org.cytoscape.application.swing.CytoPanelName;
//import org.cytoscape.util.swing.FileUtil;
//import org.cytoscape.view.model.CyNetworkView;
//import org.reactome.cytoscape.NetworkModulePanel.CloseTabIcon;
//import org.reactome.r3.util.InteractionUtilities;
//
//
//@SuppressWarnings("serial")
//public class NetworkModulePanel extends JPanel implements CytoPanelComponent
//{
//    private String TITLE = "";
//    // Used to control view
//    protected JCheckBox hideOtherNodesBox;
//    // Table for display detailed information on modules
//    protected JTable contentTable;
//    protected CyNetworkView view;
//    // Used for control
//    protected JToolBar controlToolBar;
//    // The CytoPanel container used to hold this Panel.
//    protected CytoPanel container;
//    // Used to control selection
//    private boolean isFromTable;
//    // Used to link to table selection
//    private ListSelectionListener tableSelectionListener;
//    // To control its position
//    protected JButton closeBtn;
//    protected Component closeGlue;
//    protected FileUtil fileUtil;
//    private CySwingApplication deskopApp;
//    public NetworkModulePanel()
//    {
//        init();
//    }
//    private void init()
//    {
//        setLayout(new BorderLayout());
//        contentTable = new JTable();
//        TableModel moduleModel = createTableModel();
//        contentTable.setModel(moduleModel);
//        add(new JScrollPane(contentTable), BorderLayout.CENTER);
//        tableSelectionListener = new ListSelectionListener() {
//            public void valueChanged(ListSelectionEvent e) {
//                doTableSelection();
//            }
//        };
//        contentTable.getSelectionModel().addListSelectionListener(tableSelectionListener);
//        // Used to control other nodes
//        controlToolBar = new JToolBar();
//        controlToolBar.setFloatable(false);
//        hideOtherNodesBox = new JCheckBox("Hide nodes in not selected rows");
//        hideOtherNodesBox.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                doHideOtherNodesAction();
//            }
//        });
//        controlToolBar.add(hideOtherNodesBox);
//        // Add a close button
//        closeBtn = new JButton(new CloseTabIcon());
//        closeBtn.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                close();
//            }
//        });
//        closeBtn.setToolTipText("close tab");
//        closeBtn.setRolloverEnabled(true);
//        closeGlue = Box.createHorizontalGlue();
//        controlToolBar.add(closeGlue);
//        controlToolBar.add(closeBtn);
//        add(controlToolBar, BorderLayout.NORTH);
//        
//        //Remove if no view is open.   
//    }
//    protected void doContentTablePopup(MouseEvent e) {
//        JPopupMenu popup = createExportAnnotationPopup();
//        popup.show(contentTable, 
//                   e.getX(), 
//                   e.getY());
//    private void doTableSelection()
//    {
//        if (view == null)
//            return;
//        Set<String>selectedNames = getSelectedNodes();
//    }
//    public void close() {
//        if (container == null)
//            return;
//        container.remove(this);
//    }
//    private Set<String> getSelectedNodes()
//    {
//        int[] selectedRows = contentTable.getSelectedRows();
//        NetworkModuleTableModel model = (NetworkModuleTableModel) contentTable.getModel();
//        List<String> selected 
//    }
//    @Override
//    public Component getComponent()
//    {
//        return this;
//    }
//
//    @Override
//    public CytoPanelName getCytoPanelName()
//    {
//        return CytoPanelName.SOUTH;
//    }
//
//    @Override
//    public Icon getIcon()
//    {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    public void setTitle(String title)
//    {
//        this.TITLE = title;
//    }
//    @Override
//    public String getTitle()
//    {
//        return TITLE;
//    }
//    protected void exportAnnotations()
//    {
//        // TODO implement this method properly
//        File[] files = fileUtil.getFiles(desktopApp.getJFrame(), arg1, arg2, arg3, arg4, arg5, arg6)
//    }
//    protected JPopupMenu createExportAnnotationPopup() {
//        JPopupMenu popupMenu = new JPopupMenu();
//        JMenuItem export = new JMenuItem("Export Annotations");
//        export.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                exportAnnotations();
//            }
//        });
//        popupMenu.add(export);
//        return popupMenu;
//    }
//    protected abstract class NetworkModuleTableModel extends AbstractTableModel {
//        protected List<String[]> tableData;
//        protected String[] columnHeaders;
//        
//        public NetworkModuleTableModel() {
//            tableData = new ArrayList<String[]>();
//        }
//        
//        protected int getNodeIdColumn() {
//            return getColumnCount() - 1;
//        }
//        
//        /**
//         * nodes ids in the same row should be delimited by ",".
//         * @param rows
//         * @return
//         */
//        public List<String> getNodeIdsAtRows(int[] rows) {
//            List<String> rtn = new ArrayList<String>();
//            int idCol = getNodeIdColumn();
//            for (int row : rows) {
//                String[] rowData = tableData.get(row);
//                rtn.add(rowData[idCol]);
//            }
//            return rtn;
//        }
//        
//        /**
//         * Get a list of rows containing any of passed node ids.
//         * @param nodes
//         * @return
//         */
//        public List<Integer> getRowsForNodeIds(List<String> nodes) {
//            List<Integer> rows = new ArrayList<Integer>();
//            int idCol = getNodeIdColumn();
//            for (int i = 0; i < tableData.size(); i++) {
//                String[] rowData = tableData.get(i);
//                String[] ids = rowData[idCol].split(",");
//                for (String id : ids) {
//                    if (nodes.contains(id)) {
//                        rows.add(i);
//                        break;
//                    }
//                }
//            }
//            return rows;
//        }
//
//        public int getColumnCount() {
//            return columnHeaders.length;
//        }
//
//        public int getRowCount() {
//            return tableData.size();
//        }
//
//        public Object getValueAt(int rowIndex, int columnIndex) {
//            String[] rowData = tableData.get(rowIndex);
//            return rowData[columnIndex];
//        }
//
//        @Override
//        public Class<?> getColumnClass(int columnIndex) {
//            return String.class;
//        }
//
//        @Override
//        public String getColumnName(int column) {
//            return columnHeaders[column];
//        }
//        
//        protected String createIDText(Collection<String> ids) {
//            List<String> list = new ArrayList<String>(ids);
//            Collections.sort(list);
//            return InteractionUtilities.joinStringElements(",", list);
//        }
//    }
//    /**
//     * The class which generates the 'X' icon for the tabs. The constructor 
//     * accepts an icon which is extra to the 'X' icon, so you can have tabs 
//     * like in JBuilder. This value is null if no extra icon is required. 
//     * @author Mr_Silly @jdc modified by wgm ported by Eric T Dawson July 2013
//     * */
//    class CloseTabIcon implements Icon
//    {
//        private int x_pos;
//        private int y_pos;
//        private int width;
//        private int height;
//        //private Icon fileIcon;
//        
//        public CloseTabIcon() {
//            width = 14;
//            height = 14;
//        }
//        public void paintIcon(Component c, Graphics g, int x, int y) {
//            x_pos = x;
//            y_pos = y;
//            Color col = g.getColor();
//            g.setColor(Color.black);
//            int b = 4;
//            g.drawLine(x + b - 1, y + b, x + width - b - 1, y + height - b);
//            g.drawLine(x + b , y + b, x + width - b, y + height - b);
//            g.drawLine(x + width - b - 1, y + b, x + b - 1, y + height - b);
//            g.drawLine(x + width - b, y + b, x + b, y + height - b);
//            g.setColor(col);
//            //if (fileIcon != null) {
//            //  fileIcon.paintIcon(c, g, x + width, y_p);
//            //}
//        }
//        public int getIconWidth() {
//            return width;
//        }
//        public int getIconHeight() {
//            return height;
//        }
//        public Rectangle getBounds() {
//            return new Rectangle(x_pos, y_pos, width, height);
//        }
//    }
//}
