package org.reactome.cytoscape.rest.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.reactome.cytoscape.pathway.EventTreePane;
import org.reactome.cytoscape.pathway.PathwayControlPanel;
import org.reactome.cytoscape.pathway.PathwayEnrichmentAnalysisTask;
import org.reactome.cytoscape.pathway.PathwayEnrichmentResultPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservablePathwayEnrichmentAnalysisTask extends PathwayEnrichmentAnalysisTask implements ObservableTask {
    private final Logger logger = LoggerFactory.getLogger(ObservablePathwayEnrichmentAnalysisTask.class);
    private ReactomeFIVizTable result;
    private boolean isResultsDisplayed = false;
    
    public ObservablePathwayEnrichmentAnalysisTask() {
        showEmptyResultDialog = false;
    }
    
    @Override
    public void run(TaskMonitor monitor) throws Exception {
        // Make sure we have event tree displayed already
        EventTreePane treePane = PathwayControlPanel.getInstance().getEventTreePane();
        if (treePane == null) {
            logger.error("Cannot find EventTreePane. Load pathways first for Reactome pathway enrichment analysis.");
            return;
        }
        treePane.addPropertyChangeListener(event -> {
            if (event.getPropertyName().equals("showPathwayEnrichments"))
                isResultsDisplayed = true;
        });
        super.run(monitor);
        // Because a thread is used in EventTreePane, we have to use the following loop
        // to break the execution
        long time0 = System.currentTimeMillis();
        while (true) {
            if (isResultsDisplayed || resultIsEmpty)
                break;
            try {
                Thread.sleep(100); // Wait for 0.1 second
            }
            catch(InterruptedException e) {}
            long time2 = System.currentTimeMillis();
            long duration = time2 - time0;
            if (duration > 3000)
                break; // If more than 3 seconds, break the loop!
        }
        result = new ReactomeFIVizTable();
        if (resultIsEmpty)
            return;
        String title = "Reactome Pathway Enrichment";
        result.fillTableFromResults(PathwayEnrichmentResultPane.class, title);
    }

    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(ReactomeFIVizTable.class))
            return (R) result;
        return null;
    }

    @Override
    public List<Class<?>> getResultClasses() {
        return Collections.singletonList(ReactomeFIVizTable.class);
    }
    
}
