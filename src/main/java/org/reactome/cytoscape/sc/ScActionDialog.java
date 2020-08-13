package org.reactome.cytoscape.sc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * Provide an entry point for performing single cell data analysis and visualization.
 * @author wug
 *
 */
@SuppressWarnings("serial")
public class ScActionDialog extends FIActionDialog {
    private final Dimension DEFAULT_SIZE = new Dimension(500, 535);
    private DataSetPanel datasetPane;
    private PreprocessPane preprocessPane;
    private boolean isForRNAVelocity;
    // Use a list is much easier than ButtonGroup
    private List<JRadioButton> velocityModeBtns;
    private JPanel velocityModePane;
    
    public ScActionDialog(JFrame parent) {
        super(parent);
        setSize(DEFAULT_SIZE);
    }
    
    public ScActionDialog() {
        setSize(DEFAULT_SIZE);
    }
    
    public boolean isForRNAVelocity() {
        return this.isForRNAVelocity;
    }
    
    public File selectFile() {
        setLocationRelativeTo(getOwner());
        setModal(true);
        setVisible(true);
        if (!isOkClicked())
            return null;
        File file = getSelectedFile();
        if (file == null || !file.exists()) {
            JOptionPane.showMessageDialog(this, 
                    "No data is chosen or the selected data doesn't exist!", 
                    "Error in File", 
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return file;
    }
    
    public List<String> getRegressoutKeys() {
        return preprocessPane.getRegressoutKeys();
    }
    
    /**
     * This method will be expanded to include more imputation method. Currently it returns either null or magic.
     * @return
     */
    public String getImputationMethod() {
        if (preprocessPane.magicImputationBox.isSelected())
            return "magic";
        return null;
    }
    
    @Override
    protected File getFile(FileUtil fileUtil, Collection<FileChooserFilter> filters) {
        if (isForRNAVelocity)
            return super.getFile(fileUtil, filters);
        CyApplicationManager appManager = PlugInObjectManager.getManager().getApplicationManager();
        File startDir = appManager.getCurrentDirectory();
        File dataFile = fileUtil.getFolder(this,
                                           "Select a folder for Analysis", 
                                           startDir == null ? null : startDir.getAbsolutePath());
        if (dataFile != null && dataFile.getParentFile() != null)
            appManager.setCurrentDirectory(dataFile.getParentFile());
        return dataFile;
    }
    
    @Override
    protected JPanel createInnerPanel(FIVersionSelectionPanel versionPanel, Font font) {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        Border border = BorderFactory.createEtchedBorder();
        
        JPanel approachPane = createApproachPane();
        approachPane.setBorder(BorderFactory.createTitledBorder(border,
                                                                "Approach",
                                                                TitledBorder.LEFT,
                                                                TitledBorder.CENTER,
                                                                font));
        contentPane.add(approachPane);
        
        velocityModePane = createVelocityModePane();
        velocityModePane.setBorder(BorderFactory.createTitledBorder(border,
                                                                    "Velocity Mode",
                                                                    TitledBorder.LEFT,
                                                                    TitledBorder.CENTER,
                                                                    font));
        contentPane.add(velocityModePane);
        velocityModePane.setVisible(false); // Default should be false
        
        datasetPane = new DataSetPanel() {
            @Override
            protected void createFileChooserGui(JTextField fileTF,
                                                JLabel fileChooseLabel,
                                                JButton browseButton,
                                                JPanel loadPanel,
                                                GridBagConstraints constraints) {
                ScActionDialog.this.fileTF = fileTF;
                ScActionDialog.this.createFileChooserGui(fileTF,
                                                         fileChooseLabel,
                                                         browseButton,
                                                         loadPanel,
                                                         constraints);
            }

        };
        datasetPane.setBorder(BorderFactory.createTitledBorder(border,
                                                               "Data Information",
                                                               TitledBorder.LEFT, 
                                                               TitledBorder.CENTER,
                                                               font));
        contentPane.add(datasetPane);
        
        preprocessPane = new PreprocessPane();
        preprocessPane.setBorder(BorderFactory.createTitledBorder(border,
                                                                  "Preprocess",
                                                                  TitledBorder.LEFT,
                                                                  TitledBorder.CENTER,
                                                                  font));
        contentPane.add(preprocessPane);
        
        return contentPane;
    }
    
    private JPanel createApproachPane() {
        JPanel pane = new JPanel();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        JLabel approachLabel = new JLabel("Choose an approach:");
        JRadioButton scanpyBtn = new JRadioButton("Standard analysis via Scanpy");
        JRadioButton scevoBtn = new JRadioButton("RNA Velocity Analysis via scVelo");
        ButtonGroup approachGroup = new ButtonGroup();
        approachGroup.add(scanpyBtn);
        approachGroup.add(scevoBtn);
        ItemListener l = e -> isForRNAVelocity = scevoBtn.isSelected();
        scanpyBtn.addItemListener(l);
        scevoBtn.addItemListener(l);
        scanpyBtn.setSelected(true);
        // Add these approaches
        constraints.gridx = 0;
        constraints.gridy = 0;
        pane.add(approachLabel, constraints);
        constraints.gridx ++;
        pane.add(scanpyBtn, constraints);
        constraints.gridy ++;
        pane.add(scevoBtn, constraints);
        // Update GUIs based on choice
        scanpyBtn.addActionListener(e -> {
            datasetPane.setFormatGUIsVisible(true);
            preprocessPane.setIsForVelocity(false);
            velocityModePane.setVisible(false);
        });
        scevoBtn.addActionListener(e -> {
            datasetPane.setFormatGUIsVisible(false);
            preprocessPane.setIsForVelocity(true);
            velocityModePane.setVisible(true);
        });
        return pane;
    }
    
    private JPanel createVelocityModePane() {
        JPanel pane = new JPanel();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        // RNA velocity specific GUIs
        JLabel velocityLabel = new JLabel("Choose an RNA velocity mode:");
        constraints.gridx = 0;
        constraints.gridy = 0;
        pane.add(velocityLabel, constraints);
        // Default should be the first stochastic
        ButtonGroup velocityModeGroup = new ButtonGroup();
        constraints.gridx = 1;
        // A much easier way than using ButtonGroup
        velocityModeBtns = new ArrayList<>();
        Stream.of(ScvVelocityMode.values()).forEach(mode -> {
            JRadioButton btn = new JRadioButton(mode.toString());
            velocityModeBtns.add(btn);
            velocityModeGroup.add(btn);
            pane.add(btn, constraints);
            constraints.gridy ++;
        });
        velocityModeBtns.get(0).setSelected(true);
        return pane;
    }
    
    public ScvVelocityMode getVelocityMode() {
        for (JRadioButton btn : velocityModeBtns) {
            if (btn.isSelected()) {
                return ScvVelocityMode.valueOf(btn.getText());
            }
        }
        return ScvVelocityMode.stochastic;
    }
    
    /**
     * No filer for the time being. To be implemented based on the file format.
     */
    @Override
    protected Collection<FileChooserFilter> createFileFilters() {
        Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
        // The following code to choose two file formats is not reliable.
        // The user may choose a customized file (e.g. tab delimited).
        String[] exts = {"txt", "loom", "h5d", "h5ad"};
        FileChooserFilter filter = new FileChooserFilter("Single Cell Files",
                                                            exts);
        filters.add(filter);
        return filters;
    }

    @Override
    protected String getTabTitle() {
        return "Single Cell Data Analysis";
    }
    
    public PathwaySpecies getSpecies() {
        return this.datasetPane.getSpecies();
    }
    
    public String getFormat() {
        return datasetPane.getFormat();
    }
    
    public void hidePreprocessPane() {
        this.preprocessPane.setVisible(false);
    }
    
    public static void main(String[] args) {
        ScActionDialog dialog = new ScActionDialog(null);
//        dialog.hidePreprocessPane();
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        dialog.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                // TODO Auto-generated method stub
                System.out.println(dialog.getSize());
            }
            
        });
        dialog.setVisible(true);
    }
    
    private class PreprocessPane extends JPanel {
        private JCheckBox totalCountsBox;
        private JCheckBox pctCountsBox;
        private JRadioButton magicImputationBox;
        // Used for showing velocity information
        private JEditorPane velocityTA;
        
        public PreprocessPane() {
            init();
        }
        
        public List<String> getRegressoutKeys() {
            List<String> rtn = new ArrayList<>();
            if (totalCountsBox.isSelected())
                rtn.add(totalCountsBox.getText());
            if (pctCountsBox.isSelected())
                rtn.add(pctCountsBox.getText());
            return rtn;
        }
        
        public void setIsForVelocity(boolean isTrue) {
            for (int i = 0; i < getComponentCount(); i++) {
                Component comp = getComponent(i);
                if (comp == velocityTA)
                    comp.setVisible(isTrue);
                else
                    comp.setVisible(!isTrue);
            }
        }
        
        private void init() {
            this.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.gridx = 0;
            constraints.gridy = 0;
            JLabel label = new JLabel("Choose an imputation method:");
            magicImputationBox = new JRadioButton("MAGIC");
            add(label, constraints);
            constraints.gridx ++;
            add(magicImputationBox, constraints);
            label = new JLabel("Choose attributes for regressout:");
            constraints.gridy ++;
            constraints.gridx = 0;
            this.add(label, constraints);
            totalCountsBox = new JCheckBox("total_counts");
            constraints.gridx ++;
            this.add(totalCountsBox, constraints);
            pctCountsBox = new JCheckBox("pct_counts_mt");
            constraints.gridy ++;
            this.add(pctCountsBox, constraints);
            
            // For velocity information
            String velocityText = "<html>For the RNA velocity analysis, the input data should contain "
                    + "two matrices for unspliced and spliced abundances, which can be generated using "
                    + "velocyto or loompy/kallisto pipeline. For details, see "
                    + "<a href=\"https://scvelo.readthedocs.io/getting_started.html\">https://scvelo.readthedocs.io/getting_started.html</a>.</html>";
            velocityTA = new JEditorPane();
            velocityTA.setContentType("text/html");
            velocityTA.setText(velocityText);
            velocityTA.setEditable(false);
            velocityTA.setBackground(getBackground());
            velocityTA.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String desc = e.getDescription();
                    PlugInUtilities.openURL(desc);
                }
            });
            constraints.gridy ++;
            constraints.gridwidth = 2;
            add(velocityTA, constraints);
            velocityTA.setVisible(false); // Default 
        }
    }

}
