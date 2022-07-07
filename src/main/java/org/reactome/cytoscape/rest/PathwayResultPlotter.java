package org.reactome.cytoscape.rest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.pathway.EventTreePane;
import org.reactome.cytoscape.pathway.EventTreePane.EventObject;
import org.reactome.cytoscape.pathway.GSEAResultPane;
import org.reactome.cytoscape.pathway.PathwayControlPanel;
import org.reactome.cytoscape.pathway.PathwayEnrichmentResultPane;
import org.reactome.cytoscape.sc.ScNetworkManager;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.gsea.model.GseaAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * This class is used to generate a JavaScript based pathway result plot using a plotly.js template.
 * @author wug
 *
 */
public class PathwayResultPlotter {
	private static final Logger logger = LoggerFactory.getLogger(PathwayResultPlotter.class);
	private ObjectWriter objectWriter; 
	
	public PathwayResultPlotter() {
	}
	
	public void setObjectWriter(ObjectWriter writer) {
		this.objectWriter = writer;
	}
	
    public void plotEnrichmentResults(OutputStream os) throws Exception {
    	EventTreePane treePane = PathwayControlPanel.getInstance().getEventTreePane();
    	// Get the list of pathways
    	List<EventObject> eventObjects = treePane.getHierarchicalOrderedPathways();
    	// Get the template from the backend server
    	String template = loadPlotTemplate();
    	template = setPathwayList(eventObjects, template);
    	String traceText = createPathwayTraces(treePane, eventObjects);
    	template = template.replace("$data", traceText);
    	if (treePane.getAnnotationPane() instanceof GSEAResultPane) {
    		template = template.replace("$y_title", "NormalizedEnrichedScore");
    		template = template.replace("$title", "Reactome GSEA Analysis");
    	}
    	else {
    		template = template.replace("$y_title", "-Log10(pValue)");
    		template = template.replace("$title", "Reactome Pathway Enrichment Analysis");
    	}
    	// There should be no lines for this type plot
    	template = template.replace("$lines", "");
    	os.write(template.getBytes());
    	os.flush();
    }

	private String setPathwayList(List<EventObject> eventObjects, String template) {
		String pathways = eventObjects
    			.stream()
    			.map(e -> e.getName())
    			.map(n -> "\"" + n + "\"")
    			.collect(Collectors.joining(","));
    	pathways = "[" + pathways + "]";
    	template = template.replace("$pathway_list", pathways);
		return template;
	}
    
    public void plotScClusterPathwayActivities(OutputStream os, String analysisToken) throws Exception {
    	EventTreePane treePane = PathwayControlPanel.getInstance().getEventTreePane();
    	// Get the list of pathways
    	List<EventObject> eventObjects = treePane.getHierarchicalOrderedPathways();
    	// Get the template from the backend server
    	String template = loadPlotTemplate();
    	template = setPathwayList(eventObjects, template);
    	List<PathwayTrace> traces = null;
    	String title = null;
    	final String[] tokens = analysisToken.split("_");
    	String lines = null;
    	// There are two cases:
    	if (analysisToken.contains("_vs_")) {
    		// For comparison: e.g. reactomefiviz_sc_cluster_1_vs_2_aucell
    		Map<String, Double> pathway2score1 = ScNetworkManager.getManager().fetchClusterPathwayActivities(tokens[tokens.length - 1], tokens[3]);
    		List<PathwayTrace> traces1 = createPathwayTraces(eventObjects, null, pathway2score1);
    		// Try to enhance it
    		traces1.forEach(t -> {
    			t.name = t.name + "(" + tokens[3] + ")";
    			t.text = t.text.stream().map(e -> e + "<br>Cluster: " + tokens[3]).collect(Collectors.toList());
    		});
    		Map<String, Double> pathway2score2 = ScNetworkManager.getManager().fetchClusterPathwayActivities(tokens[tokens.length - 1], tokens[5]);
    		List<PathwayTrace> traces2 = createPathwayTraces(eventObjects, null, pathway2score2);
    		traces2.forEach(t -> {
    			t.name = t.name + "(" + tokens[5] + ")";
    			t.text = t.text.stream().map(e -> e + "<br>Cluster: " + tokens[5]).collect(Collectors.toList());
    		});
    		traces2.addAll(traces1);
    		traces = traces2;
    		title = "Cluster " + tokens[3] + " vs Cluster " + tokens[5] + " " + tokens[tokens.length - 1].toUpperCase();
    		lines = generateLines(pathway2score1, pathway2score2);
    	}
    	else {
    		// For single cluster: e.g. reactomefiviz_sc_cluster_1_aucell
    		Map<String, Double> pathway2score = ScNetworkManager.getManager().fetchClusterPathwayActivities(tokens[tokens.length - 1], tokens[3]);
    		traces = createPathwayTraces(eventObjects, null, pathway2score);
    		title = "Cluster " + tokens[3] + " " + tokens[tokens.length - 1].toUpperCase();
    		lines = "";
    	}
    	String traceText = createPathwayTraces(traces);
    	template = template.replace("$data", traceText);
    	// Format: reactomefiviz_sc_cluster_(\\d)+_(\\w)+"
    	template = template.replace("$y_title", tokens[tokens.length - 1].toUpperCase());
    	template = template.replace("$title", title);
    	template = template.replace("$lines", lines);
    	os.write(template.getBytes());
    	os.flush();
    }
    
    private String generateLines(Map<String, Double> pathway2score1,
                                 Map<String, Double> pathway2score2) throws JsonProcessingException {
    	List<LineShape> lines = new ArrayList<>();
    	for (String pathway : pathway2score1.keySet()) {
    		if (!pathway2score2.containsKey(pathway))
    			continue;
    		Double score1 = pathway2score1.get(pathway);
    		Double score2 = pathway2score2.get(pathway);
    		LineShape line = new LineShape();
    		line.x0 = line.x1 = pathway;
    		line.y0 = score1;
    		line.y1 = score2;
    		lines.add(line);
    	}
    	if (lines.size() == 0) return "";
    	String text = objectWriter.writeValueAsString(lines);
    	// Want to get rid of the pair of brackets
    	return text.substring(1, text.length() - 1);
    }
    
    private String createPathwayTraces(EventTreePane treePane,
                                       List<EventObject> eventObjects) throws Exception {
    	// Construct the pathway traces
    	Map<String, GeneSetAnnotation> pathway2annotation = treePane.getPathwayToAnnotation();
    	Map<String, Double> pathway2score = new HashMap<>();
    	PathwayEnrichmentResultPane resultPane = treePane.getAnnotationPane();
    	if (resultPane instanceof GSEAResultPane) {
    		GSEAResultPane gseaResultPane = (GSEAResultPane) resultPane;
    		List<GseaAnalysisResult> gseaResults = gseaResultPane.getResults();
    		gseaResults.stream()
    				   .forEach(r -> {
    					   pathway2score.put(r.getPathway().getName(), Double.valueOf(r.getNormalizedScore()));
    				   });
    	}
    	else {
    		pathway2annotation.forEach((p, a) -> {
    			pathway2score.put(p, -Math.log10(a.getPValue()));
    		});
    	}
    	List<PathwayTrace> traces = createPathwayTraces(eventObjects, pathway2annotation, pathway2score);
    	return createPathwayTraces(traces);
    }
    
    private List<PathwayTrace> createPathwayTraces(List<EventObject> eventObjects,
                                       Map<String, GeneSetAnnotation> pathway2annotation,
                                       Map<String, Double> pathway2score) throws Exception {
    	// Check how many top level pathways we have
    	Set<String> topLevelPathways = eventObjects.stream().map(e -> e.getTopLevelPathway()).collect(Collectors.toSet());
    	Map<String, PathwayTrace> top2traces = topLevelPathways.stream().collect(Collectors.toMap(Function.identity(), 
    																							  t -> new PathwayTrace(t)));
    	Map<String, String> pathway2top = eventObjects.stream()
    			.collect(Collectors.toMap(p -> p.getName(), p -> p.getTopLevelPathway()));
    	// Some hacky way for sharing ANOVA results when 1/F is used
    	final String dataType = PathwayControlPanel.getInstance().getEventTreePane().getDataType();
    	pathway2score.forEach((p, score) -> {
    		String top = pathway2top.get(p);
    		// Just in case if the version is not right
    		if (top == null) {
    			logger.error("Cannot find top pathway for " + p + " with score " + score + ".");
    			return;
    		}
    		PathwayTrace trace = top2traces.get(top);
    		trace.addDataPoint(p, score);
    		StringBuilder toolTip = new StringBuilder();
    		toolTip.append("<b>Pathway: ").append(p).append("</b><br>");
    		toolTip.append("Top pathway: ").append(top);
    		if (pathway2annotation != null && pathway2annotation.containsKey(p)) {
    			GeneSetAnnotation a = pathway2annotation.get(p);
    			toolTip.append("<br>");
    			toolTip.append("pValue: ").append(PlugInUtilities.formatProbability(a.getPValue())).append("<br>");
    			toolTip.append(dataType + ": ").append(PlugInUtilities.formatProbability(Double.valueOf(a.getFdr())));
    		}
    		trace.addText(toolTip.toString());
    	});
    	return new ArrayList<>(top2traces.values());
    }
    
    private String createPathwayTraces(List<PathwayTrace> traces) throws JsonProcessingException  {
    	List<PathwayTrace> sortedTraces = traces.stream()
    			.sorted((t1, t2) -> t1.name.compareTo(t2.name))
    			.collect(Collectors.toList());
    	String traceText = objectWriter.writeValueAsString(sortedTraces);
        return traceText;
    }
    
    private String loadPlotTemplate() throws Exception {
    	String appUrlName = PlugInObjectManager.getManager().getReactomeRESTfulAppURL();
    	URL url = new URL(appUrlName + "/Cytoscape/pathway_plot_template.html");
    	InputStream is = url.openStream();
    	BufferedReader br = new BufferedReader(new InputStreamReader(is));
    	StringBuilder builder = new StringBuilder();
    	String line = null;
    	while ((line = br.readLine()) != null)
    		builder.append(line + "\n");
    	br.close();
    	is.close();
    	return builder.toString();
    }
	
    private class PathwayTrace {
    	List<String> x;
    	List<Double> y;
    	String mode = "markers";
    	String type = "scattergl";
    	Map<String, Object> marker;
    	String name;
    	List<String> text;
    	
    	public PathwayTrace(String name) {
			x = new ArrayList<>();
			y = new ArrayList<>();
			text = new ArrayList<>();
			this.name = name;
			marker = new HashMap<>();
			marker.put("size", 5);
		}
    	
    	public void addDataPoint(String pathway,
    	                         Double value) {
    		x.add(pathway);
    		y.add(value);
    	}
    	
    	public void addText(String tooltip) {
    		text.add(tooltip);
    	}
    }
    
    private class LineShape {
    	String type = "line";
//    	String yref = "paper";
    	String x0;
    	Double y0;
    	String x1;
    	Double y1;
    	Map<String, Object> line;
    	
    	public LineShape() {
    		line = new HashMap<>();
    		line.put("color", "#a6a6a6"); // Light black
    		line.put("width", 1.0d);
    	}
    	
    }

}
