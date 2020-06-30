package org.reactome.cytoscape.sc;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

@SuppressWarnings("serial")
public class DiffExpResultTableFilterPane extends JPanel {
    private final String[] DOUBLE_OPERATORS = {
            "=",
            ">",
            ">=",
            "<",
            "<=",
            "> for abs",
            ">= for abs"
    };
    private final String[] STRING_OPERATORS = {
            "equals",
            "contains"
    };
    private JButton filterBtn;
    private JButton addBtn;
    private JComboBox<String> colNameBox;
    private JComboBox<String> operatorBox;
    private JTextField filterTextField;
    private JTable table;

    public DiffExpResultTableFilterPane(JTable table) {
        this.table = table;
        init();
    }

    private void init() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 4, 1));
        // Table filter field and label.
        JLabel label = new JLabel("Filter rows by:");
        colNameBox = new JComboBox<>();
        for (int i = 0; i < table.getColumnCount(); i++)
            colNameBox.addItem(table.getColumnName(i));
        colNameBox.setSelectedIndex(0);
        // Operators
        operatorBox = new JComboBox<>();
        DefaultComboBoxModel<String> boxModel = new DefaultComboBoxModel<>();
        // Initialize the box first in case it cannot be displayed in GUI
        Stream.of(STRING_OPERATORS).forEach(boxModel::addElement);
        operatorBox.setModel(boxModel);
        filterTextField = new JTextField(8);
        addBtn = new JButton("Add"); 
        addBtn.addActionListener(e -> updateFilter());
        addBtn.setToolTipText("Click to add a new filter");
        filterBtn = new JButton("Filter");
        ActionListener al = (e -> doFilter(colNameBox, filterTextField));
        filterBtn.addActionListener(al);
        filterTextField.addActionListener(al);
        add(label);
        add(colNameBox);
        add(operatorBox);
        add(filterTextField);
        add(addBtn);
        add(filterBtn);
        // Control the display
        colNameBox.addItemListener(e -> {
            int selectedIndex = colNameBox.getSelectedIndex();
            Class<?> cls = table.getColumnClass(selectedIndex);
            // There are only two types of data
            DefaultComboBoxModel<String> tmpModel = (DefaultComboBoxModel<String>) operatorBox.getModel();
            tmpModel.removeAllElements();
            if (cls.isAssignableFrom(String.class)) {
                Stream.of(STRING_OPERATORS).forEach(tmpModel::addElement);
            }
            else if (cls.isAssignableFrom(Double.class)) {
                Stream.of(DOUBLE_OPERATORS).forEach(tmpModel::addElement);
            }
        });
    }
    
    private void updateFilter() {
        Container container = getParent();
        if (container == null)
            return;
        if (addBtn.getText().equals("Add")) {
            DiffExpResultTableFilterPane newPane = new DiffExpResultTableFilterPane(table);
            newPane.addBtn.setText("Remove");
            newPane.addBtn.setToolTipText("Click to remove this filter");
            newPane.filterBtn.setVisible(false);
            container.add(newPane);
            revalidate(); // Need to call the topmost container
        }
        else if (addBtn.getText().equals("Remove")) {
            container.remove(this);
            container.revalidate(); // Just need to call container. Weird!
        }
    }
    
    private RowFilter<TableModel, Object> getFilterForText() {
        String filterText = filterTextField.getText().trim();
        if (filterText.length() == 0)
            return null; // Nothing to do
        RowFilter<TableModel, Object> rowFilter = new RowFilter<TableModel, Object>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                int colIndex = colNameBox.getSelectedIndex();
                String value = (String) entry.getValue(colIndex);
                String operator = operatorBox.getSelectedItem().toString();
                if (operator.equals("equals"))
                    return value.equalsIgnoreCase(filterText);
                if (operator.equals("contains"))
                    return value.toLowerCase().contains(filterText.toLowerCase());
                return true; // As default
            }
        };
        return rowFilter;
    }
    
    private RowFilter<TableModel, Object> getFilter() {
        int selectedIndex = colNameBox.getSelectedIndex();
        Class<?> cls = table.getColumnClass(selectedIndex);
        // There are only two types of data
        if (cls.isAssignableFrom(String.class)) {
            return getFilterForText();
        }
        else if (cls.isAssignableFrom(Double.class)) {
            return getFilterForDouble();
        }
        return null;
    }

    private void doFilter(JComboBox<String> typeBox,
                          JTextField filterText) {
        Container container = getParent();
        List<RowFilter<TableModel, Object>> filters = new ArrayList<>();
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component comp = container.getComponent(i);
            if (comp instanceof DiffExpResultTableFilterPane) {
                RowFilter<TableModel, Object> filter = ((DiffExpResultTableFilterPane)comp).getFilter();
                if (filter != null)
                    filters.add(filter);
            }
        }
        if (filters.size() > 0) {
            @SuppressWarnings("unchecked")
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            sorter.setRowFilter(RowFilter.andFilter(filters));
            firePropertyChange("doFilter", null, null);
        }
    }

    private RowFilter<TableModel, Object> getFilterForDouble() {
        String filterText = filterTextField.getText().trim();
        if (filterText.length() == 0)
            return null; // Nothing to do
        // Try parse
        try {
            Double.parseDouble(filterText); // Hope there is a method to check this
        }
        catch(NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                                          "Make sure a number is entered for filering.",
                                          "Error in Filtering",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }
        RowFilter<TableModel, Object> rowFilter = new RowFilter<TableModel, Object>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                int colIndex = colNameBox.getSelectedIndex();
                Double value = (Double) entry.getValue(colIndex);
                String operator = operatorBox.getSelectedItem().toString();
                Double filterValue = new Double(filterText);
                if (operator.equals("="))
                    return value.equals(filterValue);
                if (operator.equals(">"))
                    return value > filterValue;
                if(operator.equals(">="))
                    return value >= filterValue;
                if (operator.equals("<"))
                    return value < filterValue;
                if (operator.equals("<="))
                    return value <= filterValue;
                if (operator.equals("> for abs")) 
                    return Math.abs(value) > filterValue;
                if (operator.endsWith(">= for abs"))
                    return Math.abs(value) >= filterValue;
                return true; // Just keep the row
            }
        };
        return rowFilter;
    }
}
