/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.cytoscape;

/**
 * Created by
 * User: risserlin
 * <p>
 * Enrichment map Parameters define all the variables that are needed to create, manipulate, explore
 * and save an individual Enrichment map.  It stores the parsed files used to create it, all genes in the network,
 * all enrichment results, cuts-offs, expression files, and ranks
 */
public class EnrichmentMapParameters {
    
	private String edbdir;
	private String edbdir2;
	
	
    //flag to indicate if the similarity cut off is jaccard or overlap or combined
    private String similarityMetric;
        
    // DEFAULT VALUES for pvalue, qvalue, similarityCutOff and jaccard
    // will be assigned in the constructor
    //p-value cutoff
    private double pvalue;
    //fdr q-value cutoff
    private double qvalue;
    //similarity cutoff
    private double similarityCutOff;
    //value to store the constant needed for constructing the combined similarity metric
    private double combinedConstant;
    //path to the expression file  (if data doesn't get collpased by GSEA then this is just the path to the expression file)
    private String expressionFilePath;
    private String expression2FilePath;

    //Constants   
    //with more similarity metric can't use a boolean to reprensent them.
    final public static String SM_JACCARD = "JACCARD";
    final public static String SM_OVERLAP = "OVERLAP";
    final public static String SM_COMBINED = "COMBINED"; 
    
    private double defaultJaccardCutOff = 0.25;
    private double defaultOverlapCutOff = 0.5;
    private String defaultSimilarityMetric = SM_OVERLAP;
    
    /**
     * Default constructor to create a fresh instance.
     */
    public EnrichmentMapParameters(String edbdir, String expressionFilePath) {
    		
    		this.edbdir = edbdir;
    		this.expressionFilePath = expressionFilePath;
    	
        //assign the defaults:
        this.pvalue = 0.005;    
        this.qvalue = 0.1;
        this.similarityCutOff = 0.5;
       //get the default combined metric constant
        this.combinedConstant = 0.5;
        
        this.similarityMetric = SM_OVERLAP;
                
    }
    
     /**
     * String representation of EnrichmentMapParameters.
     * Is used to store the persistent Attributes as a property file in the Cytoscape Session file.
     *
     * @see java.lang.Object#toString()
     */
    public String toString(){
        StringBuffer paramVariables = new StringBuffer();

       
        paramVariables.append("jaccard\t" + similarityMetric + "\n");

        //add the combined constant
        paramVariables.append("CombinedConstant" + combinedConstant + "\n");

        //cutoffs
        paramVariables.append("pvalue\t" + pvalue + "\n");
        paramVariables.append("qvalue\t" + qvalue + "\n");
        paramVariables.append("similarityCutOff\t" + similarityCutOff + "\n");

        return paramVariables.toString();
    }
    
    public String getExpressionFilePath() {
		return expressionFilePath;
	}

	public void setExpressionFilePath(String expressionFilePath) {
		this.expressionFilePath = expressionFilePath;
	}

	public String getSimilarityMetric() {
        return similarityMetric;
    }

	public void setSimilarityMetric(String similarityMetric) {
        this.similarityMetric = similarityMetric;
    }

    public double getPvalue() {
        return pvalue;
    }

    public void setPvalue(double pvalue) {
        this.pvalue = pvalue;

    }

    public double getQvalue() {
        return qvalue;
    }

    public void setQvalue(double qvalue) {
        this.qvalue = qvalue;

    }

    public double getSimilarityCutOff() {
        return similarityCutOff;
    }

    public void setSimilarityCutOff(double similarityCutOff) {
        this.similarityCutOff = similarityCutOff;
    }
         
    public double getCombinedConstant() {
        return combinedConstant;
    }

    public void setCombinedConstant(double combinedConstant) {
        this.combinedConstant = combinedConstant;
    }

	public double getDefaultJaccardCutOff() {
		return defaultJaccardCutOff;
	}

	public void setDefaultJaccardCutOff(double defaultJaccardCutOff) {
		this.defaultJaccardCutOff = defaultJaccardCutOff;
	}

	public double getDefaultOverlapCutOff() {
		return defaultOverlapCutOff;
	}

	public void setDefaultOverlapCutOff(double defaultOverlapCutOff) {
		this.defaultOverlapCutOff = defaultOverlapCutOff;
	}

	public String getDefaultSimilarityMetric() {
		return defaultSimilarityMetric;
	}

	public void setDefaultSimilarityMetric(String defaultSimilarityMetric) {
		this.defaultSimilarityMetric = defaultSimilarityMetric;
	}

	public String getEdbdir() {
		return edbdir;
	}

	public void setEdbdir(String edbdir) {
		if(edbdir.endsWith("edb"))
			this.edbdir = edbdir;
		else
			 this.edbdir = edbdir + System.getProperty("file.separator") + "edb";
		
		//given the edb file try and deduce the expression file.
		//this.expressionFilePath = getExpressionFile(this.edbdir);
	}

	public String getEdbdir2() {
		return edbdir2;
	}

	public void setEdbdir2(String edbdir2) {
		if(edbdir2.endsWith("edb"))
			this.edbdir2 = edbdir2;
		else
			 this.edbdir2 = edbdir2 + System.getProperty("file.separator") + "edb";
		//given the edb file try and deduce the expression file.
		//this.expression2FilePath = getExpressionFile(this.edbdir2);
	}

	public String getExpression2FilePath() {
		return expression2FilePath;
	}

	public void setExpression2FilePath(String expression2FilePath) {
		this.expression2FilePath = expression2FilePath;
	}

}

