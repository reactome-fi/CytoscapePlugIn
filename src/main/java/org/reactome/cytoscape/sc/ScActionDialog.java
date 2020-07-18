package org.reactome.cytoscape.sc;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;
import org.reactome.cytoscape.service.PathwaySpecies;

/**
 * Provide an entry point for performing single cell data analysis and visualization.
 * @author wug
 *
 */
public class ScActionDialog extends FIActionDialog {
    private final Dimension DEFAULT_SIZE = new Dimension(500, 300);
    private DataSetPanel datasetPane;
    
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
    
    @Override
    protected File getFile(FileUtil fileUtil, Collection<FileChooserFilter> filters) {
        File dataFile = fileUtil.getFolder(this,
                                           "Select a folder for Analysis", 
                                           null);
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
    
    public static void main(String[] args) {
        ScActionDialog dialog = new ScActionDialog(null);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        dialog.setVisible(true);
    }

}
