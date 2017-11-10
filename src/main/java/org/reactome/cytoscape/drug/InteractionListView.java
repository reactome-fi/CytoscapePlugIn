/*
 * Created on Jan 23, 2017
 *
 */
package org.reactome.cytoscape.drug;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.dataModel.Interaction;

/**
 * A customized JDialog showing a list of drug/target interactions.
 * @author gwu
 *
 */
public class InteractionListView extends JDialog {
    protected JTable interactionTable;
    private DrugAffinityPlotPanel plotPane;
    private JButton viewDetailsBtn;
    private JButton overlayBtn;
    private TableListInteractionFilter interactionFilter;
    
    /**
     * Default constructor.
     */
    public InteractionListView() {
        this(PlugInObjectManager.getManager().getCytoscapeDesktop());
    }
    
    public InteractionListView(Window owner) {
        init();
    }
    
    public void setInteractions(List<Interaction> interactions) {
        InteractionListTableModel model = (InteractionListTableModel) interactionTable.getModel();
        model.setInteractions(interactions);
        if (plotPane != null)
            plotPane.setInteractions(interactions);
    }
    
    protected InteractionListTableModel createTableModel() {
        return new InteractionListTableModel();
    }
    
    protected JPanel createTablePane() {
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        
        interactionTable = new JTable();
        InteractionListTableModel model = createTableModel();
        interactionTable.setModel(model);
        TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(model);
        interactionTable.setRowSorter(rowSorter);
        interactionFilter = createInteractionFilter();
        interactionFilter.setTable(interactionTable);
        RowFilter<TableModel, Object> rowFilter = model.createFilter(interactionFilter);
        rowSorter.setRowFilter(rowFilter);
        pane.add(new JScrollPane(interactionTable), BorderLayout.CENTER);
        
        JPanel controlPane = createTableControlPane();
        if (controlPane != null)
            pane.add(controlPane, BorderLayout.SOUTH);
        
        return pane;
    }
    
    protected void init() {
        setTitle("Drug Targets View");
        
        JComponent tabbedPane = createContentPane();

        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Add a close button
        JPanel closePane = createDialogControlPane();
        getContentPane().add(closePane, BorderLayout.SOUTH);

        interactionTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    handleTableSelection();
                }
            }
        });
        
        setSize(625, 250);
        setLocationRelativeTo(getOwner());
    }

    protected JComponent createContentPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        
        JPanel tablePane = createTablePane();
        tabbedPane.addTab("Table View", tablePane);
        
        plotPane = new DrugAffinityPlotPanel();
        tabbedPane.addTab("Plot View", plotPane);
        return tabbedPane;
    }

    protected JPanel createDialogControlPane() {
        JPanel closePane = new JPanel();
        closePane.setBorder(BorderFactory.createEtchedBorder());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(event -> dispose());
        closePane.add(closeBtn);
        return closePane;
    }

    protected TableListInteractionFilter createInteractionFilter() {
        return new TableListInteractionFilter();
    }
    
    protected void handleTableSelection() {
        viewDetailsBtn.setEnabled(interactionTable.getSelectedRowCount() == 1);
        overlayBtn.setEnabled(interactionTable.getSelectedRowCount() > 0);
    }
    
    protected JPanel createTableControlPane() {
        JPanel controlPane = new JPanel();
        controlPane.setBorder(BorderFactory.createEtchedBorder());
        
        viewDetailsBtn = new JButton("View Details");
        viewDetailsBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                viewDetails();
            }
        });
        controlPane.add(viewDetailsBtn);
        viewDetailsBtn.setEnabled(false);
        
        JButton filterBtn = new JButton("Filter");
        filterBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                interactionFilter.showDialog(InteractionListView.this);
            }
        });
        controlPane.add(filterBtn);
        
        overlayBtn = createActionButton();
        overlayBtn.setEnabled(false);
        controlPane.add(overlayBtn);
        
        return controlPane;
    }
    
    protected JButton createActionButton() {
        JButton actionButton = new JButton("Overlay Targets to Pathways");
        actionButton.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                overlayToPathways();
            }
        });
        return actionButton;
    }
    
    private void overlayToPathways() {
        // Get the list of targets
        Set<String> targets = new HashSet<>();
        InteractionListTableModel model = (InteractionListTableModel) interactionTable.getModel();
        for (int viewRow : interactionTable.getSelectedRows()) {
            int modelRow = interactionTable.convertRowIndexToModel(viewRow);
            String gene = (String) model.getValueAt(modelRow, 2); // The second column
            targets.add(gene);
        }
        dispose();
        DrugListManager.getManager().overlayTargetsToPathways(targets);
    }
    
    private void viewDetails() {
        int viewIndex = interactionTable.getSelectedRow();
        int modelIndex = interactionTable.convertRowIndexToModel(viewIndex);
        InteractionListTableModel model = (InteractionListTableModel) interactionTable.getModel();
        String id = (String) model.getValueAt(modelIndex, 0);
        Interaction interaction = model.getInteraction(id);
        if (interaction == null)
            return;
        InteractionView view = new InteractionView(this);
        view.setInteraction(interaction);
        view.setModal(true);
        view.setVisible(true);
        view.toFront(); // Needed in Java 1.8.0_121 for some reason when the owner is a dialog.
    }
    
    protected class TableListInteractionFilter extends InteractionFilter {
        private JTable table;
        
        public TableListInteractionFilter() {
        }
        
//        @Override
//        protected void init() {
//            // Choose all
//            dataSources = new ArrayList<>();
//            for (DataSource source : DataSource.values())
//                dataSources.add(source);
//            affinityFilters = new ArrayList<>();
//            for (AssayType type : AssayType.values()) {
//                AffinityFilter filter = new AffinityFilter();
//                filter.setAssayType(type);
//                filter.setRelation(AffinityRelation.NOGREATER);
//                affinityFilters.add(filter);
//            }
//        }
        
        public void setTable(JTable table) {
            this.table = table;
        }

        @Override
        public void applyFilter() {
            if (table == null)
                return;
            InteractionListTableModel model = (InteractionListTableModel) table.getModel();
            RowFilter<TableModel, Object> rowFilter = model.createFilter(this);
            TableRowSorter<InteractionListTableModel> rowSorter = (TableRowSorter<InteractionListTableModel>) table.getRowSorter();
            rowSorter.setRowFilter(rowFilter);
        }
        
    }
    
}
