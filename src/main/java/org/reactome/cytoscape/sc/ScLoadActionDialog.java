package org.reactome.cytoscape.sc;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import org.reactome.cytoscape.service.FIActionDialog;
import org.reactome.cytoscape.service.FIVersionSelectionPanel;
import org.reactome.cytoscape.service.PathwaySpecies;

/**
 * Provide an entry point for performing single cell data analysis and visualization.
 * @author wug
 *
 */
@SuppressWarnings("serial")
public class ScLoadActionDialog extends FIActionDialog {
    private final Dimension DEFAULT_SIZE = new Dimension(500, 250);
    private DataSetPanel datasetPane;
    
    public ScLoadActionDialog(JFrame parent) {
        super(parent);
        setSize(DEFAULT_SIZE);
    }
    
    public ScLoadActionDialog() {
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
                ScLoadActionDialog.this.fileTF = fileTF;
                ScLoadActionDialog.this.createFileChooserGui(fileTF,
                                                         fileChooseLabel,
                                                         browseButton,
                                                         loadPanel,
                                                         constraints);
            }

        };
        datasetPane.setFormatGUIsVisible(false);
        datasetPane.getFileLabel().setText("Choose a saved h5ad file:");
        datasetPane.setBorder(BorderFactory.createTitledBorder(border,
                                                               "Data Information",
                                                               TitledBorder.LEFT, 
                                                               TitledBorder.CENTER,
                                                               font));
        contentPane.add(datasetPane);
        
        return contentPane;
    }
    
    @Override
    protected Collection<FileChooserFilter> createFileFilters() {
        Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
        // The following code to choose two file formats is not reliable.
        // The user may choose a customized file (e.g. tab delimited).
        String[] exts = {"h5ad"};
        FileChooserFilter filter = new FileChooserFilter("Single Cell File",
                                                          exts);
        filters.add(filter);
        return filters;
    }

    @Override
    protected String getTabTitle() {
        return "Single Cell Analyzed Data Loading";
    }
    
    public PathwaySpecies getSpecies() {
        return this.datasetPane.getSpecies();
    }
    
    public static void main(String[] args) {
        ScLoadActionDialog dialog = new ScLoadActionDialog(null);
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

}
