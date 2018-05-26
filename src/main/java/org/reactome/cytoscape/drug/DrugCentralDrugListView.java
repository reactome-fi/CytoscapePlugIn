package org.reactome.cytoscape.drug;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.reactome.cytoscape.util.PlugInUtilities;

import edu.ohsu.bcb.druggability.dataModel.Drug;

public class DrugCentralDrugListView extends DrugListView {
    
    public DrugCentralDrugListView() {
        super();
        setTitle("Drugs in DrugCentral");
        setSize(700, 630);
    }

    @Override
    protected DrugListTableModel createTableModel() {
        return new DrugCentralTableModel();
    }
    
    @Override
    protected void doDrugTablePopup(MouseEvent e) {
        final Point point = e.getPoint();
        int tableCol = drugTable.columnAtPoint(point);
        final int modelCol = drugTable.convertColumnIndexToModel(tableCol);
        if (modelCol == 0) {
            super.doDrugTablePopup(e);
            return;
        }
        if (modelCol != 1)
            return; 
        // We will add a link for DrugControl
        JPopupMenu popup = new JPopupMenu();
        JMenuItem viewInDrugCentral = new JMenuItem("View in DrugCentral");
        viewInDrugCentral.addActionListener(ac -> {
            viewInDrugCentral();
        });
        popup.add(viewInDrugCentral);
        popup.show(drugTable, e.getX(), e.getY());
    }

    private void viewInDrugCentral() {
        int row = drugTable.getSelectedRow();
        if (row == -1)
            return;
        DrugListTableModel model = (DrugListTableModel) drugTable.getModel();
        int modelRow = drugTable.convertRowIndexToModel(row);
        Object drugId = model.getValueAt(modelRow, 1);
        String url = DrugDataSource.DrugCentral.getAccessUrl() + drugId;
        PlugInUtilities.openURL(url);
    }

    private class DrugCentralTableModel extends DrugListTableModel {
        
        public DrugCentralTableModel() {
            tableHeaders = new String[]{"DrugName", "DrugCentralID"};
        }

        @Override
        public void setDrugs(List<Drug> drugs) {
            prepareDrugs(drugs);
            for (Drug drug : drugs) {
                Object[] row = new Object[tableHeaders.length];
                row[0] = drug.getDrugName();
                row[1] = drug.getDrugID();
                data.add(row);
            }
            fireTableDataChanged();
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 1)
                return Integer.class;
            return String.class;
        }
        
    }

}
