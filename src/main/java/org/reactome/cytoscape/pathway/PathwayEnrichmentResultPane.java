/*
 * Created on Nov 21, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.reactome.cytoscape.pathway.EventTreePane.EventObject;
import org.reactome.cytoscape.service.GeneSetAnnotationPanel;

/**
 * Used to display pathway enrichment analysis results using all Reactome pathways.
 * @author gwu
 *
 */
public class PathwayEnrichmentResultPane extends GeneSetAnnotationPanel {
    
    private EventTreePane eventTreePane;
    private boolean isFromTable = false;
    private boolean isFromTree = false;
    
    public PathwayEnrichmentResultPane(EventTreePane eventTreePane, String title) {
        super(title);
        this.eventTreePane = eventTreePane;
        hideOtherNodesBox.setVisible(false);
        // To syncrhonization selection
        contentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (isFromTree)
                    return;
                isFromTable = true;
                DefaultTreeModel model = (DefaultTreeModel) PathwayEnrichmentResultPane.this.eventTreePane.eventTree.getModel();
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) PathwayEnrichmentResultPane.this.eventTreePane.eventTree.getModel().getRoot();
                List<TreePath> paths = new ArrayList<TreePath>();
                TableModel tableModel = contentTable.getModel();
                int[] selectedRows = contentTable.getSelectedRows();
                for (int i = 0; i < contentTable.getSelectedRowCount(); i++) {
                    int modelIndex = contentTable.convertRowIndexToModel(selectedRows[i]);
                    String pathway = (String) tableModel.getValueAt(modelIndex, 0);
                    PathwayEnrichmentResultPane.this.eventTreePane.searchPathway(root, pathway, model, paths);
                }
                PathwayEnrichmentResultPane.this.eventTreePane.eventTree.clearSelection();
                if (paths.size() > 0) {
                    for (TreePath path : paths)
                        PathwayEnrichmentResultPane.this.eventTreePane.eventTree.expandPath(path);
                    // Scroll to the first path
                    PathwayEnrichmentResultPane.this.eventTreePane.eventTree.setSelectionPath(paths.get(0));
                    PathwayEnrichmentResultPane.this.eventTreePane.eventTree.scrollPathToVisible(paths.get(0));
                }
                isFromTable = false;
            }
        });
    }
    
    public void doTreeSelection() {
        if (isFromTable)
            return;
        isFromTree = true;
        TreePath[] selectionPaths = this.eventTreePane.eventTree.getSelectionPaths();
        if (selectionPaths == null)
            return;
        contentTable.clearSelection();
        TableModel model = contentTable.getModel();
        int firstRow = -1;
        for (TreePath path : selectionPaths) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            EventObject event = (EventObject) treeNode.getUserObject();
            // Select table
            for (int i = 0; i < model.getRowCount(); i++) {
                String value = (String) model.getValueAt(i, 0);
                if (event.name.equals(value)) {
                    int row = contentTable.convertRowIndexToView(i);
                    contentTable.setRowSelectionInterval(row, row);
                    if (firstRow == -1)
                        firstRow = row;
                    break;
                }
            }
        }
        if (firstRow >= 0) {
            Rectangle rect = contentTable.getCellRect(firstRow, 0, true);
            contentTable.scrollRectToVisible(rect);
        }
        isFromTree = false;
    }


    @Override
    protected NetworkModuleTableModel createTableModel() {
        PathwayEnrichmentTableModel model = new PathwayEnrichmentTableModel();
        return model;
    }
    
    @Override
    protected TableRowSorter<NetworkModuleTableModel> createTableRowSorter(NetworkModuleTableModel model) {
        return new PathwayEnrichmentTableRowSorter(model);
    }
    
    @Override
    protected void doContentTablePopup(MouseEvent e) {
        JPopupMenu popupMenu = createExportAnnotationPopup();
        JMenuItem item = new JMenuItem("View in Diagram");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eventTreePane.viewEventInDiagram();
            }
        });
        popupMenu.add(item);
        popupMenu.show(contentTable, e.getX(), e.getY());
    }

    private class PathwayEnrichmentTableModel extends AnnotationTableModel {
        private String[] geneSetHeaders = new String[] {
                "ReactomePathway",
                "RatioOfProteinInPathway",
                "NumberOfProteinInPathway",
                "ProteinFromGeneSet",
                "P-value",
                "FDR",
                "HitGenes"
        };
        
        public PathwayEnrichmentTableModel() {
            columnHeaders = geneSetHeaders;
        }
    }
    
    private class PathwayEnrichmentTableRowSorter extends TableRowSorter<NetworkModuleTableModel> {
        
        public PathwayEnrichmentTableRowSorter(NetworkModuleTableModel model) {
            super(model);
        }

        @Override
        public Comparator<?> getComparator(int column) {
            if (column == 0 || column == 6)
                return super.getComparator(column);
            // Something special for FDR since it may contains "<"
            if (column == 5) {
                Comparator<String> comparator = new Comparator<String>() {
                    public int compare(String value1, String value2) {
                        if (value1.startsWith("<") && value1.startsWith(">")) {
                            String value11 = value1.substring(1);
                            String value21 = value2.substring(1);
                            return new Double(value11).compareTo(new Double(value21));
                        }
                        else if (value1.startsWith("<"))
                            return -1;
                        else if (value2.startsWith("<"))
                            return 1;
                        else {
                            Double d1 = new Double(value1);
                            Double d2 = new Double(value2);
                            return d1.compareTo(d2);
                        }
                    }
                };
            }
            // Do based on numbers
            Comparator<String> comparator = new Comparator<String>() {
                public int compare(String value1, String value2) {
                    Double d1 = new Double(value1.toString());
                    Double d2 = new Double(value2.toString());
                    return d1.compareTo(d2);
                }
            };
            return comparator;
        }
        
        
        
    }
    
}