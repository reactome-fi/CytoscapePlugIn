package org.reactome.cytoscape.sc;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.reactome.cytoscape.service.PathwaySpecies;

/**
 * A customized JPanel to provide user interfaces for choosing a data set.
 * @author wug
 *
 */
public abstract class DataSetPanel extends JPanel {
    private JRadioButton humanBtn;
    private JRadioButton mouseBtn;
    private JRadioButton x10MtxBtn;
    private JRadioButton x10Hdf5Btn;
    private JRadioButton x10visumBtn;
    
    public DataSetPanel() {
        init();
    }
    
    public PathwaySpecies getSpecies() {
        if (mouseBtn.isSelected())
            return PathwaySpecies.Mus_musculus;
        return PathwaySpecies.Homo_sapiens; // as the default
    }
    
    public String getFormat() {
        return x10MtxBtn.getText(); // To be supported. Currently only one choice.
    }
    
    private void init() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 4, 0, 0);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel fileLabel = new JLabel("Choose a folder or file:");
        JTextField fileTF = new JTextField();
        JButton browseButton = new JButton("Browse");
        createFileChooserGui(fileTF, 
                             fileLabel,
                             browseButton, 
                             this, 
                             constraints);
        // Species related stuff
        JLabel speciesLabel = new JLabel("Specify a species:");
        humanBtn = new JRadioButton("human");
        mouseBtn = new JRadioButton("mouse");
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(humanBtn);
        btnGroup.add(mouseBtn);
        mouseBtn.setSelected(true);
        constraints.gridx = 0;
        constraints.gridy = 1;
        add(speciesLabel, constraints);
        constraints.gridx = 1;
        add(humanBtn, constraints);
        constraints.gridy ++;
        add(mouseBtn, constraints);
        // Data format
        JLabel dataFormatLabel = new JLabel("Specify a format:");
        // The following list is based on 
        // https://scanpy.readthedocs.io/en/stable/api/index.html#reading
        x10MtxBtn = new JRadioButton("10x-Genomics-mtx");
        x10Hdf5Btn = new JRadioButton("10x-Genomics-hdf5");
        x10visumBtn = new JRadioButton("10x-Genomics-visum");
        x10MtxBtn.setSelected(true);
        x10Hdf5Btn.setEnabled(false);
        x10visumBtn.setEnabled(false);
        ButtonGroup formatGroup = new ButtonGroup();
        formatGroup.add(x10visumBtn);
        formatGroup.add(x10Hdf5Btn);
        formatGroup.add(x10MtxBtn);
        constraints.gridy ++;
        constraints.gridx = 0;
        add(dataFormatLabel, constraints);
        constraints.gridx ++;
        add(x10MtxBtn, constraints);
        constraints.gridy ++;
        add(x10Hdf5Btn, constraints);
        constraints.gridy ++;
        add(x10visumBtn, constraints);
    }
    
    protected abstract void createFileChooserGui(JTextField fileTF,
                                                 JLabel fileChooseLabel,
                                                 JButton browseButton, 
                                                 JPanel loadPanel,
                                                 GridBagConstraints constraints);

}
