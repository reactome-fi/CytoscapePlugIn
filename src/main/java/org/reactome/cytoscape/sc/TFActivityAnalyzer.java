package org.reactome.cytoscape.sc;

import java.awt.GridBagConstraints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.cytoscape.application.swing.CytoPanelName;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.sc.server.JSONServerCaller;
import org.reactome.cytoscape.sc.utils.ScPathwayDataType;
import org.reactome.cytoscape.sc.utils.ScPathwayMethod;
import org.reactome.cytoscape.sc.utils.Scpy4ReactomeDownloader;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TFActivityAnalyzer extends PathwayActivityAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(TFActivityAnalyzer.class);
    private static TFActivityAnalyzer tfAnalyzer;
    
    public static final TFActivityAnalyzer getTFAnalyzer() {
        if (tfAnalyzer == null)
            tfAnalyzer = new TFActivityAnalyzer();
        return tfAnalyzer;
    }
    
    /**
     * Conduct pathway analysis.
     * @param species
     * @param caller
     */
    @Override
    public void performAnalysis(PathwaySpecies species,
                                JSONServerCaller caller) {
        TFMethodChooseDialog dialog = new TFMethodChooseDialog();
        if (!dialog.isOKClicked)
            return;
        String confidenceLevels = dialog.getConfidenceLevels();
        if (confidenceLevels.length() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "You have to select at least one confidence level for this analysis.",
                                          "No Evidence Level Selected",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        ScPathwayMethod method = (ScPathwayMethod) dialog.methodBox.getSelectedItem();
        Thread t = new Thread() {
            public void run() {
                JFrame parentFrame = PlugInObjectManager.getManager().getCytoscapeDesktop();
                try {
                    ProgressPane progressPane = new ProgressPane();
                    progressPane.setIndeterminate(true);
                    parentFrame.setGlassPane(progressPane);
                    progressPane.setTitle("Transcription Factor Analysis");
                    progressPane.setVisible(true);
                    progressPane.setText("Download the Dorothea file...");
                    Scpy4ReactomeDownloader downloader = new Scpy4ReactomeDownloader();
                    String gmtFileName = downloader.downloadDorotheaFIs(species, confidenceLevels);
                    progressPane.setText("Perform analysis...");
                    String dataKey = caller.doTFsAnalysis(gmtFileName, method);
                    if (dataKey.toLowerCase().startsWith("error"))
                        throw new IllegalStateException(dataKey); // Something not right
                    progressPane.setText("Done");
                    method2key.put(method, dataKey);
                    JOptionPane.showMessageDialog(parentFrame,
                                                  "The analysis is done. Use \"Perform ANOVA\" or \"View TF Activities\"\n" + 
                                                  "for further visualization or analysis.",
                                                  "Transcription Factor Analysis",
                                                  JOptionPane.INFORMATION_MESSAGE);
                }
                catch(Exception e) {
                    JOptionPane.showMessageDialog(parentFrame,
                                                  e.getMessage(),
                                                  "Error in Transcription Factor Analysis",
                                                  JOptionPane.ERROR_MESSAGE);
                    logger.error(e.getMessage(), e);
                }
                parentFrame.getGlassPane().setVisible(false);
            }
        };
        t.start();
    }
    
    /**
     * We only need to override this method for ANOVA.
     */
    @Override
    protected void displayANOVA(Map<String, Map<String, Double>> anovaResults,
                                ScPathwayMethod method,
                                PathwaySpecies species,
                                ProgressPane progressPane) throws Exception {
        progressPane.setText("Display ANOVA results...");
        PathwayANOVAResultPane resultPane = new TFANOVAResultPane(method);
        resultPane.setResults(method, anovaResults);
        // Need to select it
        PlugInObjectManager.getManager().selectCytoPane(resultPane, CytoPanelName.SOUTH);
    }
    
    @Override
    protected PathwayNameDialog createNameDialog() {
        return new TFNameDialog();
    }

    private class TFNameDialog extends PathwayNameDialog {
        @Override
        protected String getDialogTitle() {
            return "Choose a Transcription Factor";
        }

        @Override
        protected String getLabelText() {
            return "Enter a transcription factor:";
        }
    }
    
    private class TFANOVAResultPane extends PathwayANOVAResultPane {
        
        public TFANOVAResultPane(ScPathwayMethod method) {
            super(null, "TF ANOVA: " + method);
            setDataType(ScPathwayDataType.Transcription_Factor);
        }

        @Override
        protected void doContentTablePopup(MouseEvent e) {
            JPopupMenu popupMenu = createExportAnnotationPopup();
            createViewActivitiesMenu(popupMenu);
            // Link to GeneCard
            String tf = getSelectedTopic();
            if (tf != null) {
                JMenuItem item = new JMenuItem("Query Gene Card");
                item.addActionListener(e1 -> PlugInUtilities.queryGeneCard(tf));
                popupMenu.add(item);
            }
            popupMenu.show(contentTable, e.getX(), e.getY());
        }
    }
    
    private class TFMethodChooseDialog extends MethodChooseDialog {
        private List<JCheckBox> confidenceBoxes;
        
        @Override
        protected void customizeContentPane(JPanel contentPane, GridBagConstraints constraints) {
            super.customizeContentPane(contentPane, constraints);
            addConfidenceBoxes(contentPane, constraints);
        }
        
        public String getConfidenceLevels() {
            StringBuilder builder = new StringBuilder();
            for (JCheckBox box : confidenceBoxes) {
                if (box.isSelected())
                    builder.append(box.getText());
            }
            return builder.toString();
        }

        protected void addConfidenceBoxes(JPanel contentPane, GridBagConstraints constraints) {
            JLabel label = new JLabel("Check confidence levels:");
            constraints.gridx = 0;
            constraints.gridy ++;
            constraints.gridwidth = 2;
            constraints.gridheight = 1;
            contentPane.add(label, constraints);
            // Add confidence level boxes
            confidenceBoxes = new ArrayList<>();
            Stream.of("A", "B", "C", "D", "E").forEach(e -> confidenceBoxes.add(new JCheckBox(e)));
            JPanel panel = new JPanel();
            panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
            confidenceBoxes.forEach(b -> panel.add(b));
            IntStream.range(0, 3).forEach(i -> confidenceBoxes.get(i).setSelected(true));
            constraints.gridy ++;
            contentPane.add(panel, constraints);
            // Add a note
            String note = "*: For information about the confidence levels, double click"
                    + " <a href=\"https://genome.cshlp.org/content/29/8/1363\">DoRothEA</a>";
            addNotePane(note, contentPane, constraints);
        }
        
        @Override
        protected void setDialogSize() {
            setSize(490, 290);
        }
    }
    
}
