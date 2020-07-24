package org.reactome.cytoscape.sc;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * Provide an entry point for performing single cell data analysis and visualization.
 * @author wug
 *
 */
public class ScActionDialog extends FIActionDialog {
    private final Dimension DEFAULT_SIZE = new Dimension(500, 475);
    private DataSetPanel datasetPane;
    private PreprocessPane preprocessPane;
    
    public ScActionDialog(JFrame parent) {
        super(parent);
        setSize(DEFAULT_SIZE);
    }
    
    public ScActionDialog() {
        setSize(DEFAULT_SIZE);
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
                                                                  "Preprocess Parameters",
                                                                  TitledBorder.LEFT,
                                                                  TitledBorder.CENTER,
                                                                  font));
        contentPane.add(preprocessPane);
        
        return contentPane;
    }
    
    /**
     * No filer for the time being. To be implemented based on the file format.
     */
    @Override
    protected Collection<FileChooserFilter> createFileFilters() {
        Collection<FileChooserFilter> filters = new HashSet<>();
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
        dialog.hidePreprocessPane();
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
        }
    }

}
