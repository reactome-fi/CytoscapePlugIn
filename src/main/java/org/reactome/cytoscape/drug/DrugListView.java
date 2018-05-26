/*
 * Created on Jan 22, 2017
 *
 */
package org.reactome.cytoscape.drug;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.gk.util.StringUtils;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

import edu.ohsu.bcb.druggability.dataModel.Drug;

/**
 * A customized JDialog showing a list of cancer drugs fetched from the server-side database.
 * @author gwu
 *
 */
public class DrugListView extends JDialog {
    protected JTable drugTable;
    private JButton viewTargets;
    private JButton googleBtn;
    private JButton runImpactBtn;
    // Keep the value of the source
    protected DrugDataSource dataSource;
    
    /**
     * Default constructor.
     */
    public DrugListView() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    private void init() {
        setTitle("Cancer Drugs in Targetome");
        drugTable = new JTable();
        drugTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    googleBtn.setEnabled(drugTable.getSelectedRowCount() > 0);
                    viewTargets.setEnabled(drugTable.getSelectedRowCount() > 0);
                    runImpactBtn.setEnabled(drugTable.getSelectedRowCount() == 1);
                }
            }
        });
        
        DrugListTableModel tableModel = createTableModel();
        drugTable.setModel(tableModel);
        drugTable.setRowSorter(new TableRowSorter<>(tableModel));
        getContentPane().add(new JScrollPane(drugTable), BorderLayout.CENTER);
        addPopupMenu();
        
        JPanel controlPane = createControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        setSize(1055, 630);
        // Use this method to accommodate in a multi-display environment
        setLocationRelativeTo(getOwner());
//        GKApplicationUtilities.center(this);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    public DrugDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DrugDataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected DrugListTableModel createTableModel() {
        return new DrugListTableModel();
    }
    
    private void addPopupMenu() {
        drugTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doDrugTablePopup(e);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doDrugTablePopup(e);
            }
            
        });
    }
    
    protected void doDrugTablePopup(MouseEvent e) {
        final Point point = e.getPoint();
        int tableCol = drugTable.columnAtPoint(point);
        final int modelCol = drugTable.convertColumnIndexToModel(tableCol);
        if (modelCol != 0)
            return; // Show only for the drugName column
        JPopupMenu popup = new JPopupMenu();
        JMenuItem google = new JMenuItem("Google");
        google.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                google();
            }
        });
        popup.add(google);
        JMenuItem viewTargets = new JMenuItem("View Targets");
        viewTargets.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                viewTargets();
            }
        });
        popup.add(viewTargets);
        // Work for only one drug
        List<String> drugs = getSelectedDrugs();
        if (drugs.size() == 1) {
            JMenuItem runImpactAnalysis = new JMenuItem("Run Pathway Impact Analysis");
            runImpactAnalysis.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    runImpactPathwayAnalysis();
                }
            });
            popup.add(runImpactAnalysis);
        }
        popup.show(drugTable, point.x, point.y);
    }
    
    private void runImpactPathwayAnalysis() {
        List<String> drugs = getSelectedDrugs();
        if (drugs.size() != 1) {
            JOptionPane.showMessageDialog(this,
                                          "Choose one drug only for pathway impact analysis!",
                                          "Choose Drug",
                                          JOptionPane.INFORMATION_MESSAGE);
            return; // Just in case
        }
        dispose();
        Thread t = new Thread() {
            public void run() {
                ActionListener action = DrugListManager.getManager().getRunImpactAnalysisAction();
                if (action == null)
                    return;
                // This is a hack
                ActionEvent event = new ActionEvent(DrugListView.this, 0, drugs.get(0));
                action.actionPerformed(event);
            }
        };
        t.start();
    }
    
    private void google() {
        List<String> drugs = getSelectedDrugs();
        if (drugs == null || drugs.size() == 0)
            return;
        for (String drug : drugs)
            PlugInUtilities.queryGoogle(drug);
    }
    
    private void viewTargets() {
        if (drugTable.getSelectedRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                                          "Select one or more drugs to view their targets.",
                                          "No Drug Selected",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<String> drugs = getSelectedDrugs();
        // Close the dialog first
//        dispose(); // Keep the dialog for repeated use
        DrugListManager.getManager().showDrugTargetInteractions(drugs, this);
    }

    private List<String> getSelectedDrugs() {
        // Get the names of selected drugs
        DrugListTableModel model = (DrugListTableModel) drugTable.getModel();
        List<String> drugs = new ArrayList<>();
        for (int row : drugTable.getSelectedRows()) {
            int modelRow = drugTable.convertRowIndexToModel(row);
            String drugName = (String) model.getValueAt(modelRow, 0);
            drugs.add(drugName);
        }
        return drugs;
    }
    
    private JPanel createControlPane() {
        JPanel controlPane = new JPanel();
        controlPane.setBorder(BorderFactory.createEtchedBorder());
        
        googleBtn = new JButton("Google");
        googleBtn.setEnabled(false);
        googleBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                google();
            }
        });
        
        viewTargets = new JButton("View Targets");
        viewTargets.setEnabled(false);
        viewTargets.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                viewTargets();
            }
        });
        
        runImpactBtn = new JButton("Run Pathway Impact Analysis");
        runImpactBtn.setEnabled(false);
        runImpactBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                runImpactPathwayAnalysis();
            }
        });
        
        controlPane.add(googleBtn);
        controlPane.add(viewTargets);
        controlPane.add(runImpactBtn);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        controlPane.add(closeBtn);
        
        return controlPane;
    }
    
    public void setDrugs(List<Drug> drugs) {
        DrugListTableModel tableModel = (DrugListTableModel) drugTable.getModel();
        tableModel.setDrugs(drugs);
    }
    
    protected class DrugListTableModel extends AbstractTableModel {
        protected String[] tableHeaders = new String[] {
                "DrugName",
                "ApprovalDate",
                "ATC_Class_ID",
                "ATC_Class_Name",
                "EPC_Class_ID",
                "EPC_Class_Name",
                "Synonyms"
        };
        protected List<Object[]> data;

        public DrugListTableModel() {
        }
        
        public void setDrugs(List<Drug> drugs) {
            prepareDrugs(drugs);
            for (Drug drug : drugs) {
                String[] row = new String[tableHeaders.length];
                row[0] = drug.getDrugName();
                row[1] = drug.getApprovalDate();
                row[2] = drug.getAtcClassID();
                row[3] = drug.getAtcClassName();
                row[4] = drug.getEpcClassID();
                row[5] = drug.getEpcClassName();
                if (drug.getDrugSynonyms() != null)
                    row[6] = StringUtils.join(", ", new ArrayList<>(drug.getDrugSynonyms()));
                for (int i = 0; i < row.length; i++) {
                    if (row[i] == null || row[i].equals("null")) // The second check should be regarded as a bug
                        row[i] = "";
                }
                data.add(row);
            }
            fireTableDataChanged();
        }

        protected void prepareDrugs(List<Drug> drugs) {
            if (data == null)
                data = new ArrayList<>();
            else
                data.clear();
            Collections.sort(drugs, new Comparator<Drug>() {
                public int compare(Drug drug1, Drug drug2) {
                    return drug1.getDrugName().compareTo(drug2.getDrugName());
                }
            });
        }
        
        @Override
        public int getRowCount() {
            if (data == null)
                return 0;
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return tableHeaders.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return tableHeaders[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class; // Use String for display
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (data == null || rowIndex >= data.size())
                return null;
            return data.get(rowIndex)[columnIndex];
        }
        
    }
    
}
