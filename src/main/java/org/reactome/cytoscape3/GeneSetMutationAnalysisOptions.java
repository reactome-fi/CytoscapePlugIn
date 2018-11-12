package org.reactome.cytoscape3;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Wrap all options for gene set mutation analysis in this class.
 * @author wug
 */
@ApiModel(value = "Gene Set Analysis Parameters", description = "Parameters for gene set mutation analysis.")
public class GeneSetMutationAnalysisOptions {
    @ApiModelProperty(value = "FI Version", example = "2016", notes = "Call fiVersions API to get the list.")
    private String fiVersion;
    @ApiModelProperty(value = "Format", example = "GeneSet", allowableValues = "GeneSet, GeneSample, MAF")
    private String format;
    @ApiModelProperty(value = "Data File", example = "null", notes = "Need the full path pointing to a file containing gene set information.")
    private String file;
    // For manually entered genes
    @ApiModelProperty(value = "Gene List", example = "EGF,EGFR,KRAS,TP53", notes = "Provide nothing if a file is specified. Genes should be delimited by \",\".")
    private String enteredGenes;
    @ApiModelProperty(value = "Choose Homo Genes", example = "false")
    private boolean chooseHomoGenes;
    @ApiModelProperty(value = "Flag if linkers should be used", example = "false")
    private boolean useLinkers;
    @ApiModelProperty(value = "Choose Homo Genes", example = "false")
    private boolean showUnlinked;
    @ApiModelProperty(value = "Flag if annotations should be fetched", example = "true")
    private boolean fetchFIAnnotations;
    @ApiModelProperty(value = "Sample Cutoff Value", example = "4", notes = "Used for GeneSample and MAF file formats.")
    private int sampleCutoffValue;
    
    public GeneSetMutationAnalysisOptions() {
    }

    public String getFiVersion() {
        return fiVersion;
    }

    public void setFiVersion(String fiVersion) {
        this.fiVersion = fiVersion;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getEnteredGenes() {
        return enteredGenes;
    }

    public void setEnteredGenes(String enteredGenes) {
        this.enteredGenes = enteredGenes;
    }

    public boolean isChooseHomoGenes() {
        return chooseHomoGenes;
    }

    public void setChooseHomoGenes(boolean chooseHomoGenes) {
        this.chooseHomoGenes = chooseHomoGenes;
    }

    public boolean isUseLinkers() {
        return useLinkers;
    }

    public void setUseLinkers(boolean useLinkers) {
        this.useLinkers = useLinkers;
    }

    public boolean isShowUnlinked() {
        return showUnlinked;
    }

    public void setShowUnlinked(boolean showUnlinked) {
        this.showUnlinked = showUnlinked;
    }

    public boolean isFetchFIAnnotations() {
        return fetchFIAnnotations;
    }

    public void setFetchFIAnnotations(boolean fetchFIAnnotations) {
        this.fetchFIAnnotations = fetchFIAnnotations;
    }

    public int getSampleCutoffValue() {
        return sampleCutoffValue;
    }

    public void setSampleCutoffValue(int sampleCutoffValue) {
        this.sampleCutoffValue = sampleCutoffValue;
    }

}
