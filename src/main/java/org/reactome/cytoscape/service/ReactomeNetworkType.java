/*
 * Created on Mar 3, 2014
 *
 */
package org.reactome.cytoscape.service;

/**
 * @author gwu
 *
 */
// TODO: Refactor to use a new manager to manage FIVisualStyle for different types of networks.
public enum ReactomeNetworkType {
    
    FINetwork, // Just normal FI network
    PathwayFINetwork, // A FI converted from a pathway diagram
    FactorGraph, // A network showing a factor graph
    PGMFINetwork, // FI generated based on FI PGM
    ReactionNetwork, // Networks generated based on preceding/following relationships or shared inputs among reactions
    MechismoNetwork, // Used to perform mechismo network visualization
    SingleCellNetwork, // for single cell data analysis
    SingleCellClusterNetwork,
    DorotheaTFTargetNetwork,
    TFPathwayNetwork // Generate based on scRNA-seq data using Python script, https://github.com/reactome-fi/single-cell-analysis/blob/master/aml/AMLTet2Analysis.py.
    
}
