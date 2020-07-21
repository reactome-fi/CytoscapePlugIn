package org.reactome.cytoscape.genescore;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.RenderablePathway;
import org.gk.util.DialogControlPane;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to overlay gene scores to a opened pathway diagram.
 * @author wug
 *
 */
public class GeneScoreOverlayHelper {
    private static Logger logger = LoggerFactory.getLogger(GeneScoreOverlayHelper.class);
    
    public GeneScoreOverlayHelper() {
    }
    
    public void overlayGeneScores(PathwayEditor pathwayEditor, 
                                  PathwayHighlightControlPanel hilitePane) {
        Window window = (Window) SwingUtilities.getAncestorOfClass(Window.class, pathwayEditor);
        GeneScoreLoadingDialog dialog = new GeneScoreLoadingDialog(window);
        if (!dialog.isOkClicked)
            return;
        String fileName = dialog.fileTF.getText().trim();
        if (fileName.length() == 0)
            return;
        File file = new File(fileName);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(pathwayEditor,
                    "The specified file doesn't exist: " + fileName,
                    "No File",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Map<String, Double> geneToScore = loadGeneToScore(fileName);
            overlayGeneScores(geneToScore, pathwayEditor, hilitePane);
        }
        catch(Exception e) {
            logger.error("Error in overlay gene scores: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(pathwayEditor,
                    "Error in overlay gene scores: " + e.getMessage(),
                    "Error in Overlay",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    public void overlayGeneScores(Map<String, Double> geneToScore,
                                  PathwayEditor pathwayEditor,
                                  PathwayHighlightControlPanel hilitePane) {
        OverlayGeneScoreTask task = new OverlayGeneScoreTask();
        task.geneToScore = geneToScore;
        task.pathwayEditor = pathwayEditor;
        task.hilitePane = hilitePane;
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        taskManager.execute(new TaskIterator(task));
    }

    private Map<String, Double> loadGeneToScore(String fileName) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(fileName)))  {
            Map<String, Double> geneToScore = new HashMap<>();
            stream.skip(1)
                  .forEach(line -> {
                      String[] tokens = line.split("\t");
                      geneToScore.put(tokens[0], new Double(tokens[1]));
                  });
            return geneToScore;
        }
    }
    
    public void removeGeneScores(PathwayHighlightControlPanel hilitePane) {
        if (hilitePane == null)
            return;
        // Remove highlight colors
        hilitePane.removeHighlight();
        hilitePane.setVisible(false);
        
        GeneScorePane geneScorePane = (GeneScorePane) PlugInUtilities.getCytoPanelComponent(CytoPanelName.EAST,
                                                                                            GeneScorePane.TITLE);
        geneScorePane.close();
    }
    
    private class OverlayGeneScoreTask extends AbstractTask {
        private PathwayEditor pathwayEditor;
        private PathwayHighlightControlPanel hilitePane;
        private Map<String, Double> geneToScore;
        
        public OverlayGeneScoreTask() {
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            if (pathwayEditor == null || geneToScore == null || hilitePane == null)
                return;
            RESTFulFIService wsService = new RESTFulFIService();
            RenderablePathway pathway = (RenderablePathway) pathwayEditor.getRenderable();
            Map<String, List<Long>> geneToDBIDs = wsService.getGeneToDbIds(pathway.getReactomeDiagramId());
            Map<Long, Set<String>> dbIdToGenes = new HashMap<>();
            geneToDBIDs.forEach((gene, dbIds) -> {
                dbIds.forEach(dbId -> {
                    dbIdToGenes.compute(dbId, (key, set) -> {
                        if (set == null)
                            set = new HashSet<>();
                        set.add(gene);
                        return set;
                    });
                });
            });
            // Calculate scores for each DB_ID
            Map<String, Double> dbIdToScore = new HashMap<>();
            dbIdToGenes.forEach((dbId, genes) -> {
                double total = 0.0d;
                int count = 0;
                for (String gene : genes) {
                    Double score = geneToScore.get(gene);
                    if (score == null)
                        continue;
                    total += score;
                    count ++;
                }
                if (count == 0)
                    return;
                dbIdToScore.put(dbId.toString(), total / count);
            });
            hilitePathway(dbIdToScore);
            showGeneScores(geneToScore, geneToDBIDs, dbIdToGenes);
        }
        
        private void showGeneScores(Map<String, Double> geneToScore,
                                    Map<String, List<Long>> geneToDBIDs,
                                    Map<Long, Set<String>> dbIdToGenes) {
            GeneScorePane geneScorePane = PlugInUtilities.getCytoPanelComponent(GeneScorePane.class,
                    CytoPanelName.EAST,
                    GeneScorePane.TITLE);
            geneScorePane.setGeneToScore(geneToScore);
            geneScorePane.setDBIDToGenes(dbIdToGenes);
            geneScorePane.setGeneToDBIDs(geneToDBIDs);
            geneScorePane.setGeneToScore(geneToScore, geneToDBIDs.keySet());
            geneScorePane.setPathwayGenes(geneToDBIDs.keySet());
        }
        
        private void hilitePathway(Map<String, Double> idToValue) {
            hilitePane.setVisible(true);
            hilitePane.setIdToValue(idToValue);
            // Use 0 and 1 as default
            double[] minMaxValues = hilitePane.calculateMinMaxValues(idToValue.values());
            hilitePane.resetMinMaxValues(minMaxValues);
        }
        
    }
    
    private class GeneScoreLoadingDialog extends JDialog {
        private boolean isOkClicked;
        private JTextField fileTF;
        
        public GeneScoreLoadingDialog(Window window) {
            super(window);
            init();
        }
        
        private void init() {
            setTitle("Loading Gene Scores");
            JPanel contentPane = createContentPane();
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            controlPane.getOKBtn().addActionListener(e -> {
                isOkClicked = true;
                dispose();
            });
            controlPane.getCancelBtn().addActionListener(e -> {
                isOkClicked = false;
                dispose();
            });
            
            setSize(550, 225);
            setLocationRelativeTo(getOwner());
            setModal(true);
            setVisible(true);
        }
        
        private JPanel createContentPane() {
            JPanel pane = new JPanel();
            pane.setBorder(BorderFactory.createEtchedBorder());
            pane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            JLabel label = new JLabel("Choose a gene score file: ");
            constraints.gridy ++;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.WEST;
            pane.add(label, constraints);
            fileTF = new JTextField();
            fileTF.setEditable(false);
            fileTF.setColumns(20);
            constraints.gridx ++;
            pane.add(fileTF, constraints);
            JButton browseBtn = new JButton("Browse");
            browseBtn.addActionListener(e -> PlugInUtilities.browseFileForLoad(fileTF, "Gene Score File", new String[] {"txt", "rnk"}));
            constraints.gridx ++;
            pane.add(browseBtn, constraints);
            
            JTextArea noteTF = createNoteTF();
            constraints.gridwidth = 3;
            constraints.gridy ++;
            constraints.gridx = 0;
            pane.add(noteTF, constraints);
            
            return pane;
        }
        
        private JTextArea createNoteTF() {
            JTextArea noteTF = new JTextArea();
            noteTF.setEditable(false);
            noteTF.setBackground(getBackground());
            noteTF.setLineWrap(true);
            noteTF.setWrapStyleWord(true);
            Font font = noteTF.getFont();
            noteTF.setFont(font.deriveFont(Font.ITALIC, font.getSize() - 1));
            String note = "Note: The gene score file should contain at least two tab-delimited "
                    + "columns, first for human gene symbols and second for scores. The first row "
                    + "should be for column headers.";
            noteTF.setText(note);
            return noteTF;
        }
        
    }

}
