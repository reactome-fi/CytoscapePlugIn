/*
 * Created on Sep 1, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.fipgm.Threshold.ThresholdRelation;
import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;
import org.reactome.factorgraph.common.DataType;

/**
 * @author gwu
 *
 */
public class PGMImpactAnalysisDialog extends FIActionDialog {
    // DataTable for listing selected data
    private JTable dataListTable;
    // For synchronization purpose
    private JButton deleteBtn;
    // For data distribution
    private JComboBox<DataTypeDistribution> distTypeBox;
    private JLabel annotationLabel;
    
    /**
     * Default constructor.
     */
    public PGMImpactAnalysisDialog() {
        // Reset the default size
        setSize(625, 535);
    }

    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.FIActionDialog#createInnerPanel(org.reactome.cytoscape.service.FIVersionSelectionPanel, java.awt.Font)
     */
    @Override
    protected JPanel createInnerPanel(FIVersionSelectionPanel versionPanel,
                                      Font font) {
        JPanel contentPane = new JPanel();
        //Select the Box Layout Manager and add the FI Version
        //Selection panel to the tabbed pane.
        contentPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(versionPanel);
        
        JPanel dataPane = createDataPane(font);
        contentPane.add(dataPane);
        
        return contentPane;
    }
    
    private JPanel createDataPane(Font font) {
        //File parameter panel
        JPanel dataPane = new JPanel();
        dataPane.setLayout(new BoxLayout(dataPane, BoxLayout.Y_AXIS));
        Border etchedBorder = BorderFactory.createEtchedBorder();
        Border dataBorder = BorderFactory.createTitledBorder(etchedBorder,
                                                             "Data Parameters", 
                                                             TitledBorder.LEFT,
                                                             TitledBorder.CENTER,
                                                             font);
        dataPane.setBorder(dataBorder);
        // A sub-panel for choosing a data
        JPanel dataActionPane = createDataActionPane();
        // Another sub-panel for listing the chosen data
        JPanel dataListPane = createDataListPane();
        
        dataPane.add(dataActionPane);
        dataPane.add(dataListPane);
        
        return dataPane;
    }
    
    private JPanel createDataActionPane() {
        JPanel dataActionPane = new JPanel();
        dataActionPane.setBorder(BorderFactory.createEtchedBorder());
        GridBagLayout layout = new GridBagLayout();
        dataActionPane.setLayout(layout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.1;
        JLabel fileChooseLabel = new JLabel("Choose data file:");
        fileTF = new JTextField();
        JButton browseButton = new JButton("Browse");
        constraints.gridy = 0;
        constraints.gridx = 0;
        createFileChooserGui(fileTF,
                             fileChooseLabel, 
                             browseButton,
                             dataActionPane,
                             constraints,
                             false);
        JLabel fileFormatLabel = new JLabel("Specify data type:");
        constraints.gridy = 1;
        constraints.gridx = 0;
        dataActionPane.add(fileFormatLabel, constraints);
        
        // Use a list to show pre-defined data types
        DataType[] types = DataType.values();
        // We will not support miRNA right now
        Vector<DataType> typeVector = new Vector<DataType>();
        for (DataType type : types) {
            if (type == DataType.miRNA)
                continue;
            typeVector.add(type);
        }
        final JComboBox<DataType> typeList = new JComboBox<DataType>(typeVector);
        typeList.setEditable(false);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        dataActionPane.add(typeList, constraints);
        
        // Specify distribution to be used
        addTypeDistributionGUIs(dataActionPane, 
                                constraints);
        
        // Add two buttons to control the action
        JPanel buttonPane = new JPanel();
        JButton addBtn = new JButton("Add");
        JButton deleteBtn = new JButton("Delete");
        addBtn.setPreferredSize(deleteBtn.getPreferredSize()); // Make these two buttons the same size
        buttonPane.add(addBtn);
        buttonPane.add(deleteBtn);
        constraints.gridx = 0;
        constraints.gridy ++;
        constraints.gridwidth = 3;
        dataActionPane.add(buttonPane, constraints);
        
        synchronizeDataActions(addBtn, deleteBtn, typeList);
        
        return dataActionPane;
    }

    private void addTypeDistributionGUIs(JPanel dataActionPane,
                                         GridBagConstraints constraints) {
        JLabel distributionLabel = new JLabel("Specify distribution:");
        constraints.gridx = 0;
        constraints.gridy ++;
        constraints.gridwidth = 1;
        dataActionPane.add(distributionLabel, constraints);
        // Three distributions
        DataTypeDistribution[] distTypes = DataTypeDistribution.values();
        distTypeBox = new JComboBox<DataTypeDistribution>(distTypes);
        distTypeBox.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected,
                                                          cellHasFocus);
                if (value instanceof DataTypeDistribution) {
                    DataTypeDistribution dist = (DataTypeDistribution) value;
                    setText(dist.toListString());
                }
                return comp;
            }
            
        });
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        dataActionPane.add(distTypeBox, constraints);
        final JButton thresholdBtn = new JButton("Specify Thresholds");
        thresholdBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                specifyThresholds();
            }
        });
        constraints.gridx = 2;
        dataActionPane.add(thresholdBtn, constraints);
        // Some annotation for distribution use
        // Choose the first type as default
        annotationLabel = new JLabel("Note: " + DataTypeDistribution.values()[0].toNoteString());
        Font font = annotationLabel.getFont();
        annotationLabel.setFont(font.deriveFont(Font.ITALIC));
        constraints.gridx = 0;
        constraints.gridy ++;
        constraints.gridwidth = 3;
        dataActionPane.add(annotationLabel, constraints);
        distTypeBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateAnnotationLabel();
                DataTypeDistribution dist = (DataTypeDistribution) distTypeBox.getSelectedItem();
                if (dist == DataTypeDistribution.Discrete)
                    thresholdBtn.setVisible(true);
                else
                    thresholdBtn.setVisible(false);
            }
        });
    }
    
    private void updateAnnotationLabel() {
        DataTypeDistribution dist = (DataTypeDistribution) distTypeBox.getSelectedItem();
        annotationLabel.setText("Note: " + dist.toNoteString());
    }
    
    private void specifyThresholds() {
        ThresholdDialog dialog = new ThresholdDialog();
        dialog.setDataTypeDistribution((DataTypeDistribution)distTypeBox.getSelectedItem());
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked)
            return;
        dialog.commit();
        updateAnnotationLabel();
    }
    
    private void synchronizeDataActions(final JButton addBtn, 
                                        final JButton deleteBtn,
                                        final JComboBox<DataType> typeList) {
        addBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                File file = getSelectedFile();
                if (file == null)
                    return;
                // Make sure threshold values are specified
                DataTypeDistribution dist = (DataTypeDistribution) distTypeBox.getSelectedItem();
                if (dist == DataTypeDistribution.Discrete) {
                    if (dist.getThresholds() == null || dist.getThresholds().length == 0) {
                        JOptionPane.showMessageDialog(PGMImpactAnalysisDialog.this,
                                                      "Please specify thresholds for discretizing.",
                                                      "Null Thresholds",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                DataType type = (DataType) typeList.getSelectedItem();
                DataTableModel model = (DataTableModel) dataListTable.getModel();
                model.addData(file,
                              type,
                              (DataTypeDistribution)distTypeBox.getSelectedItem());
            }
        });
        deleteBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = dataListTable.getSelectedRow();
                if (selectedRow > -1) {
                    DataTableModel model = (DataTableModel) dataListTable.getModel();
                    model.delete(selectedRow);
                }
            }
        });
        // Need to modify the original file selection listener a little bit
        addBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        fileTF.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                addBtn.setEnabled(fileTF.getText().trim().length() > 0);
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                addBtn.setEnabled(fileTF.getText().trim().length() > 0);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        this.deleteBtn = deleteBtn; // To be used later on
    }
    
    private JPanel createDataListPane() {
        JPanel listPane = new JPanel();
        listPane.setBorder(BorderFactory.createEtchedBorder());
        listPane.setLayout(new BorderLayout(4, 4));
        JLabel label = new JLabel("The following data will be used for impact analysis:");
        listPane.add(label, BorderLayout.NORTH);
        dataListTable = new JTable();
        dataListTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DataTableModel dataTableModel = new DataTableModel();
        dataListTable.setModel(dataTableModel);
        listPane.add(new JScrollPane(dataListTable), BorderLayout.CENTER);
        synchronizeDataListActions();
        return listPane;
    }
    
    private void synchronizeDataListActions() {
        okBtn.setEnabled(false); // Disable when the dialog first shows
        // For the OK button
        dataListTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                okBtn.setEnabled(dataListTable.getRowCount() > 0);
            }
        });
        dataListTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                deleteBtn.setEnabled(dataListTable.getSelectedRowCount() > 0);
            }
        });
    }

    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.FIActionDialog#getTabTitle()
     */
    @Override
    protected String getTabTitle() {
        return "PGM Impact Analysis";
    }
    
    private class DataTableModel extends AbstractTableModel {
        private final String[] colNames = new String[]{"Data File Name", "Data Type", "Abnormal Distribution"};
        // The data to be displayed
        private List<String> fileNames;
        private List<DataType> dataTypes;
        private List<String> distributions;

        public DataTableModel() {
        }
        
        public void addData(File file,
                            DataType dataType,
                            DataTypeDistribution dataDistribution) {
            if (fileNames == null)
                fileNames = new ArrayList<String>();
            if (fileNames.contains(file.getAbsolutePath())) {
                // Block adding the same file
                JOptionPane.showMessageDialog(PGMImpactAnalysisDialog.this,
                                              "This file has been added into the list already: \n" + file.getAbsolutePath(), 
                                              "Error in Adding Data", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            fileNames.add(file.getAbsolutePath());
            if (dataTypes == null)
                dataTypes = new ArrayList<DataType>();
            dataTypes.add(dataType);
            if (distributions == null)
                distributions = new ArrayList<String>();
            distributions.add(dataDistribution.toString()); // We have to use String since enum is singleton
            fireTableDataChanged();
        }
        
        public void delete(int row) {
            if (fileNames == null || row >= fileNames.size())
                return;
            fileNames.remove(row);
            dataTypes.remove(row);
            distributions.remove(row);
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return fileNames == null ? 0 : fileNames.size();
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 1)
                return DataType.class;
            return String.class;
        }

        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (fileNames == null || rowIndex >= fileNames.size())
                return null;
            if (columnIndex == 0)
                return fileNames.get(rowIndex);
            else if (columnIndex == 1)
                return dataTypes.get(rowIndex);
            else if (columnIndex == 2)
                return distributions.get(rowIndex);
            return null;
        }
    }
    
    /**
     * Customized JDialog for selecting thresholds.
     * @author gwu
     *
     */
    private class ThresholdDialog extends JDialog {
        private boolean isOkClicked;
        private JButton okBtn;
        private JComboBox<Threshold.ValueRelation> valueRelationBox1;
        private JTextField valueTF1;
        private JComboBox<Threshold.ValueRelation> valueRelationBox2;
        private JTextField valueTF2;
        private JComboBox<Threshold.ThresholdRelation> thresholdRelationBox;
        private DataTypeDistribution dataDistribution;
        
        public ThresholdDialog() {
            super(PGMImpactAnalysisDialog.this);
            init();
        }
        
        public void setDataTypeDistribution(DataTypeDistribution dataTypeDistribution) {
            this.dataDistribution = dataTypeDistribution;
            if (dataDistribution == DataTypeDistribution.Discrete) {
                Threshold[] thresholds = dataTypeDistribution.getThresholds();
                if (thresholds != null) {
                    if (thresholds.length > 0) {
                        Threshold threshold = thresholds[0];
                        valueRelationBox1.setSelectedItem(threshold.getValueRelation());
                        valueTF1.setText(threshold.getValue() + "");
                    }
                    if (thresholds.length > 1) {
                        Threshold threshold = thresholds[1];
                        valueRelationBox2.setSelectedItem(threshold.getValueRelation());
                        valueTF2.setText(threshold.getValue() + "");
                        thresholdRelationBox.setSelectedItem(dataDistribution.getRelation());
                    }
                }
            }
        }
        
        public void commit() {
            if (dataDistribution == null || dataDistribution != DataTypeDistribution.Discrete)
                return; // Nothing can be done
            // Get the newly selected values
            List<Threshold> thresholds = new ArrayList<Threshold>();
            Threshold threshold1 = getThreshold(valueRelationBox1, valueTF1);
            if (threshold1 != null)
                thresholds.add(threshold1);
            Threshold threshold2 = getThreshold(valueRelationBox2, valueTF2);
            if (threshold2 != null)
                thresholds.add(threshold2);
            dataDistribution.setThresholds(thresholds);
            dataDistribution.setRelation((ThresholdRelation)thresholdRelationBox.getSelectedItem());
        }

        private Threshold getThreshold(JComboBox<Threshold.ValueRelation> valueRelationBox,
                                  JTextField valueTF) {
            if (valueTF.getText().trim().length() > 0) {
                double value = new Double(valueTF.getText().trim());
                Threshold threshold = new Threshold();
                threshold.setValue(value);
                threshold.setValueRelation((Threshold.ValueRelation)valueRelationBox.getSelectedItem());
                return threshold;
            }
            return null;
        }
        
        private void init() {
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            JLabel label = new JLabel("Specify abnormal values as:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 2;
            contentPane.add(label, constraints);
            
            // The first threshold
            JComponent[] comps = createThresholdGUIs(contentPane, constraints);
            valueRelationBox1 = (JComboBox<Threshold.ValueRelation>) comps[0];
            valueTF1 = (JTextField) comps[1];
            
            // Threshold relationship if any
            Threshold.ThresholdRelation[] thresholdRelations = Threshold.ThresholdRelation.values();
            thresholdRelationBox = new JComboBox<Threshold.ThresholdRelation>(thresholdRelations);
            constraints.gridx = 0;
            constraints.gridy ++;
            constraints.gridwidth = 2;
            contentPane.add(thresholdRelationBox, constraints);
            
            // The second threshold
            comps = createThresholdGUIs(contentPane, constraints);
            valueRelationBox2 = (JComboBox<Threshold.ValueRelation>) comps[0];
            valueTF2 = (JTextField) comps[1];
            
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Double values check
                    try {
                        if (valueTF1.getText().trim().length() > 0) {
                            Double value = new Double(valueTF1.getText().trim());
                        }
                        if (valueTF2.getText().trim().length() > 0) {
                            Double value = new Double(valueTF2.getText().trim());
                        }
                    }
                    catch(NumberFormatException e1) {
                        JOptionPane.showMessageDialog(ThresholdDialog.this,
                                                      "Please make sure the entered values are real numbers.",
                                                      "Error in Number",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    dispose();
                    isOkClicked = true;
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    isOkClicked = false;
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setTitle("Specify Thresholds");
            setSize(345, 230);
            setLocationRelativeTo(getOwner());
            
            // Need to control the OK button behavior
            okBtn = controlPane.getOKBtn();
            okBtn.setEnabled(false);
            
            valueTF1.getDocument().addDocumentListener(new DocumentListener() {
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    okBtn.setEnabled(valueTF1.getText().trim().length() > 0);
                }
                
                @Override
                public void insertUpdate(DocumentEvent e) {
                    okBtn.setEnabled(valueTF1.getText().trim().length() > 0);
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
        }

        private JComponent[] createThresholdGUIs(JPanel contentPane,
                                                 GridBagConstraints constraints) {
            Threshold.ValueRelation[] valueRelations = Threshold.ValueRelation.values();
            JComboBox<Threshold.ValueRelation> valueRelationBox = new JComboBox<Threshold.ValueRelation>(valueRelations);
            constraints.gridx = 0;
            constraints.gridy ++;
            constraints.gridwidth = 1;
            contentPane.add(valueRelationBox, constraints);
            JTextField valueTF = new JTextField();
            valueTF.setColumns(4);
            constraints.gridx = 1;
            contentPane.add(valueTF, constraints);
            return new JComponent[]{valueRelationBox, valueTF};
        }
        
    }
    
}
