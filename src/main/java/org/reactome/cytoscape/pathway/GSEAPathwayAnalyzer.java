package org.reactome.cytoscape.pathway;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.gsea.model.GseaAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is used to perform Reactome GSEA analysis.
 * @author wug
 *
 */
public class GSEAPathwayAnalyzer {
    private static Logger logger = LoggerFactory.getLogger(GSEAPathwayAnalysisTask.class);
    
    public GSEAPathwayAnalyzer() {
    }
    
    public void performGSEAAnalysis(String fileName,
                                    int minPathwaySize,
                                    int maxPathwaySize,
                                    int permutation,
                                    EventTreePane eventPane) {
        try {
            String geneToScore = loadGeneToScoreFile(fileName);
            // Use a Task will make some text in the tree truncated for displaying
            // the enrichment result. This is more like a GUI threading issue.
            @SuppressWarnings("rawtypes")
            TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
            GSEAPathwayAnalysisTask task = new GSEAPathwayAnalysisTask();
            task.setEventPane(eventPane);
            task.setGeneToScore(geneToScore);
            task.setMinPathwaySize(minPathwaySize);
            task.setMaxPathwaySize(maxPathwaySize);
            task.setPermutation(permutation);
            taskManager.execute(new TaskIterator(task));
        }
        catch(Exception e) {
            logger.error("GSEAPathwayAnalyzer.performGSEAAnalysis(): " + e.getMessage(), e);
            JOptionPane.showMessageDialog(eventPane,
                                          "Error in perform GSEA analysis: " + e.getMessage(), 
                                          "GSEA Analysis Error", 
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String loadGeneToScoreFile(String fileName) throws IOException {
        try(Stream<String> stream = Files.lines(Paths.get(fileName))) {
            StringBuilder builder = new StringBuilder();
            stream.skip(1).forEach(line -> {
                String[] tokens = line.split("\t");
                builder.append(tokens[0] + "\t" + tokens[1]).append("\n");
            });
            return builder.toString();
        }
    }
    
    public static class GSEAPathwayAnalysisTask extends AbstractTask {
        private String geneToScore;
        private Integer minPathwaySize;
        private Integer maxPathwaySize;
        private Integer permutation;
        private EventTreePane eventPane;
        
        public GSEAPathwayAnalysisTask() {
        }

        public EventTreePane getEventPane() {
            return eventPane;
        }

        public void setEventPane(EventTreePane eventPane) {
            this.eventPane = eventPane;
        }

        public String getGeneToScore() {
            return geneToScore;
        }

        public void setGeneToScore(String geneToScore) {
            this.geneToScore = geneToScore;
        }
        
        public void setGeneToScore(Map<String, Double> geneToScore) {
            StringBuilder builder = new StringBuilder();
            geneToScore.forEach((gene, score) -> {
                builder.append(gene + "\t" + score + "\n");
            });
            setGeneToScore(builder.toString());
        }

        public int getMinPathwaySize() {
            return minPathwaySize;
        }

        public void setMinPathwaySize(int minPathwaySize) {
            this.minPathwaySize = minPathwaySize;
        }

        public int getMaxPathwaySize() {
            return maxPathwaySize;
        }

        public void setMaxPathwaySize(int maxPathwaySize) {
            this.maxPathwaySize = maxPathwaySize;
        }

        public int getPermutation() {
            return permutation;
        }

        public void setPermutation(int permutation) {
            this.permutation = permutation;
        }
        
        private List<GseaAnalysisResult> performGSEAAnalysis() throws IOException {
            String url = PlugInObjectManager.getManager().getProperties().getProperty("gseaWSURL");
            if (url == null || url.length() == 0) {
                throw new IllegalStateException("gseaWSURL is not configured!");
            }
            // WS URL
//            @RequestMapping(value="/analyse", method=RequestMethod.POST,
//                    consumes = "text/plain")
//            public @ResponseBody List<GseaAnalysisResult> analyseText(
//                    @RequestParam(value="nperms", required=false) Integer nperms,
//                    @RequestParam(value="dataSetSizeMin", required=false) Integer dataSetSizeMin,
//                    @RequestParam(value="dataSetSizeMax", required=false) Integer dataSetSizeMax,
//                    @RequestBody String payload
//            )
            StringBuilder builder = new StringBuilder();
            builder.append(url).append("/analyse?");
            // Attach species
            builder.append("species=");
            if (PathwayControlPanel.getInstance().getCurrentSpecies() == PathwaySpecies.Mus_musculus)
                builder.append("mouse");
            else 
                builder.append("human"); // Default
            if (permutation != null)
                builder.append("&nperms=").append(permutation);
            if (minPathwaySize != null)
                builder.append("&dataSetSizeMin=").append(minPathwaySize);
            if (maxPathwaySize != null)
                builder.append("&dataSetSizeMax=").append(maxPathwaySize);

            String rtn = PlugInUtilities.callHttpInJson(builder.toString(),
                                                        PlugInUtilities.HTTP_POST,
                                                        geneToScore);
            ObjectMapper mapper = new ObjectMapper();
            List<GseaAnalysisResult> results = mapper.readValue(rtn, new TypeReference<List<GseaAnalysisResult>>() {});
            return results;
        }
        
        private List<GeneSetAnnotation> convertGSEAResultsToAnnotation(List<GseaAnalysisResult> gseaResults) {
            List<GeneSetAnnotation> annotations = gseaResults.stream().map(gseaResult -> {
                GeneSetAnnotation annotation = new GeneSetAnnotation();
                annotation.setTopic(gseaResult.getPathway().getName());
                annotation.setFdr(gseaResult.getFdr() + "");
                annotation.setPValue(new Double(gseaResult.getPvalue()));
                annotation.setHitNumber(gseaResult.getHitCount());
                return annotation;
            }).collect(Collectors.toList());
            return annotations;
        }
        
        private void showResults(List<GseaAnalysisResult> results) {
            GSEAResultPane resultPane = PlugInUtilities.getCytoPanelComponent(GSEAResultPane.class,
                                                                              CytoPanelName.SOUTH,
                                                                              GSEAResultPane.RESULT_PANE_TITLE);
            resultPane.setEventTreePane(eventPane);
            resultPane.setResults(results);
            PlugInObjectManager.getManager().selectCytoPane(resultPane, CytoPanelName.SOUTH);

        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("GSEA Pathway Analysis");
            if (geneToScore == null) {
                taskMonitor.setStatusMessage("No data is provided!");
                taskMonitor.setProgress(1.0d);
                return; // Nothing to be displayed!
            }
            taskMonitor.setProgress(0);
            taskMonitor.setStatusMessage("Do GSEA analysis...");
            taskMonitor.setProgress(0.25d); // A rather arbitrary progress

            List<GseaAnalysisResult> gseaResults = performGSEAAnalysis();
            taskMonitor.setProgress(0.75d);
            taskMonitor.setStatusMessage("Show GSEA results...");
            List<GeneSetAnnotation> annotations = convertGSEAResultsToAnnotation(gseaResults);
            if (eventPane != null) {
                // Need to use a new thread to make sure all text in the tree can be displayed without truncation.
                SwingUtilities.invokeLater(() -> {
                    eventPane.showPathwayEnrichments(annotations, false);
                    // Prepare for pathway highlighting
                    // For pathway highlighting
                    Map<String, Double> map = new HashMap<>();
                    // Use this classic way to avoid duplicated keys in Stream expression.
                    for (String line : geneToScore.split("\n")) {
                        String[] tokens = line.split("\t");
                        map.put(tokens[0], new Double(tokens[1]));
                    }
                    PathwayEnrichmentHighlighter highlighter = PathwayEnrichmentHighlighter.getHighlighter();
                    highlighter.setGeneToScore(map);
                });
            }
            showResults(gseaResults);
            taskMonitor.setProgress(1.0d);            
        }
        
    }
    
}
