package org.reactome.cytoscape3.Design;

import javax.swing.JCheckBox;

public interface ActionDialog
{
    /**
     * Return whether or not the "OK" button at the bottom
     * of the dialog has been clicked.
     * @return true if ok has been clicked.
     */
    public abstract boolean isOkClicked();
    
    /**
     * Return the integer entered for a sample cutoff value
     * when the file format has sample data.
     * @return the sample cutoff value
     */
    public abstract int getSampleCutoffValue();
    
    /**
     * Returns the actual JCheckBox for unlinked genes,
     * allowing one to see if it is selected or not.
     * Used in Gene Set/Mutation Analysis.
     * @return the raw JCheckBox for unlinked genes.
     */
    public abstract JCheckBox getUnlinkedCheckBox();
    
    /**
     * Returns whether or not solely genes mutated at both
     * alleles will be used for analysis.
     * @return true if the checkbox is selected (only homozygous samples used)
     */
    public abstract boolean chooseHomoGenes();
    
    /**
     * Returns the file format of the user provided file
     * in a short descriptive string.
     * @return The string file format
     */
    public abstract String getFileFormat();
    
    /**
     * Returns whether or not FI annotations should be fetched during
     * network creation.
     * @return true if annotations should be fetched at network creation.
     */
    public abstract boolean shouldFIAnnotationsBeFetched();
    
    /**
     * Returns whether delta should be automatically
     * determined.
     * @return true if the user wishes delta to be automatically chosen.
     */
    public abstract boolean isAutoDeltaSelected();
    
    /**
     * Returns the False Discovery Rate cutoff.
     * @return
     */
    public abstract Double getFDRCutoff();
    
    /**
     * Returns the number of permutations to be performed.
     * @return
     */
    public abstract Integer getPermutationNumber();
    
    /**
     * Returns whether or not the correlation box is selected.
     * @return true if the correlation box is selected.
     */
    public abstract boolean isSelectedCorBox();
    
    /**
     * Returns the file path of the Microarray file.
     * @return
     */
    public abstract String getMclTIFPath();
}
