/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.cytoscape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;


import org.apache.log4j.Logger;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.xbench.core.api.Application;

public class CytoscapeCyrest {

	private static final Logger klog = XLogger.getLogger(CytoscapeLaunch.class);

	
	//path to the edb directory
	private EnrichmentMapParameters params;
	private String baseURL;
	
	public CytoscapeCyrest(EnrichmentMapParameters params){
		this.params = params;
		this.baseURL = "http://localhost:1234/v1/";
	}
	
	/*
	 * Method to test if the cytoscape rest service is up and running
	 * and if one of the commands listed is enrichment map
	 */
	public boolean CytoscapeRestActive() throws IOException {

			  URL url = new URL(baseURL);
			  HttpURLConnection conn =
			      (HttpURLConnection) url.openConnection();
			  
			  if (conn.getResponseCode() != 200) {
			    klog.info("cannot connet to cytoscape rest server");
			    conn.disconnect();
			    return false;
			  }

			  else{
				  klog.info("successfully connected to cytoscape rest");
				  conn.disconnect();
				  return true;
			  }
			 
			}
	
	
	/*
	 * Method to test if the cytoscape rest service and get a list of commands that are available
	 */
	public String CytoscapeRestCommands() throws IOException {
		URL url = new URL(baseURL + "commands/");
		  HttpURLConnection conn =
		      (HttpURLConnection) url.openConnection();

		  if (conn.getResponseCode() != 200) {
		    throw new IOException(conn.getResponseMessage());
		  }

		  // Buffer the result into a string
		  BufferedReader rd = new BufferedReader(
		      new InputStreamReader(conn.getInputStream()));
		  StringBuilder sb = new StringBuilder();
		  String line;
		  while ((line = rd.readLine()) != null) {
				  sb.append(line);
			 
		  }
		  rd.close();

		  conn.disconnect();
		  return sb.toString();
		}

	/*
	 * Method to test if the cytoscape rest service is up and running
	 * and if one of the commands listed is enrichment map
	 */
	public boolean CytoscapeRestCommandEM() throws IOException {
		URL url = new URL(baseURL + "commands/");
		  HttpURLConnection conn =
		      (HttpURLConnection) url.openConnection();

		  if (conn.getResponseCode() != 200) {
		    throw new IOException(conn.getResponseMessage());
		  }

		  // Buffer the result into a string
		  BufferedReader rd = new BufferedReader(
		      new InputStreamReader(conn.getInputStream()));
		  StringBuilder sb = new StringBuilder();
		  String line;
		  while ((line = rd.readLine()) != null) {
			  if(line.contains("enrichmentmap")){
				  conn.disconnect();
				  klog.info("Found enrichment map command");
				  return true;
			  }
			 
		  }
		  rd.close();

		  conn.disconnect();
		  return false;
		}

	public String createEM_post() throws IOException {
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
		
		String request = baseURL + "commands/enrichmentmap/gseabuild";
		HttpPost httpPost = new HttpPost(request);
		

	    // Add POST parameters

	    List<NameValuePair> formparams = new ArrayList<NameValuePair>();
	    formparams.add(new BasicNameValuePair("edbdir", this.params.getEdbdir()));
	    formparams.add(new BasicNameValuePair("pvalue",Double.toString(this.params.getPvalue())));
	    formparams.add(new BasicNameValuePair("qvalue",Double.toString(this.params.getQvalue())));
	    formparams.add(new BasicNameValuePair("overlap",Double.toString(this.params.getSimilarityCutOff())));
	    formparams.add(new BasicNameValuePair("similaritymetric",this.params.getSimilarityMetric()));
	    formparams.add(new BasicNameValuePair("combinedconstant",Double.toString(this.params.getCombinedConstant())));
	    if(!this.params.getExpressionFilePath().equals(""))
	    	formparams.add(new BasicNameValuePair("expressionfile",this.params.getExpressionFilePath()));
	 	 
	    if(this.params.getEdbdir2() != null && !this.params.getEdbdir2().equalsIgnoreCase("")){
	    	formparams.add(new BasicNameValuePair("edbdir2",this.params.getEdbdir2()));

		    	if(!this.params.getExpression2FilePath().equals(""))
		    		formparams.add(new BasicNameValuePair("expressionfile2",this.params.getExpression2FilePath()));
		    }   
	    
	    httpPost.setEntity(new UrlEncodedFormEntity(formparams,"UTF-8"));
        //httpPost.addHeader("User-Agent","elink/1.0");
        response = httpclient.execute(httpPost);
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode != 200) {
        	klog.info(String.format("Cytoscape responsed with: %d", responseCode));
            
        }
	     
								
			
	    klog.info("posted:" + new UrlEncodedFormEntity(formparams,"UTF-8"));
		return "Success";	 
	}
	
	
	public boolean createEM_get() throws IOException, URISyntaxException {
		
		URIBuilder builder = new URIBuilder();
		URI uri;
		
		builder.setScheme("http")
        .setHost("localhost:1234/v1")
        .setPath("/commands/enrichmentmap/gseabuild")
        .setParameter("edbdir", this.params.getEdbdir())
        .setParameter("pvalue",Double.toString(this.params.getPvalue()))
        .setParameter("qvalue",Double.toString(this.params.getQvalue()))
        .setParameter("overlap",Double.toString(this.params.getSimilarityCutOff()))
        .setParameter("similaritymetric",this.params.getSimilarityMetric())
        .setParameter("combinedconstant",Double.toString(this.params.getCombinedConstant()));
		
		
		if(!this.params.getExpressionFilePath().equals(""))
			builder.setParameter("expressionfile",this.params.getExpressionFilePath());

		if(this.params.getEdbdir2() != null && !this.params.getEdbdir2().equalsIgnoreCase("")){
			builder.setParameter("edbdir2",this.params.getEdbdir2());

		    	if(!this.params.getExpression2FilePath().equals(""))
		    		builder.setParameter("expressionfile2",this.params.getExpression2FilePath());
		    }   
		
		uri = builder.build();
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(uri);
		klog.info("Get URL:"+httpget.getURI());
		CloseableHttpResponse response = httpclient.execute(httpget);
	
		HttpEntity entity = response.getEntity();
		StatusLine statusLine = response.getStatusLine();
		klog.info("status:" + statusLine.getReasonPhrase());
		if(!statusLine.getReasonPhrase().equalsIgnoreCase("ok")){
			Application.getWindowManager().showMessage("Unable to create Enrichment Map:" +statusLine.getReasonPhrase() + entity.toString());
			httpclient.close();						
			return false;
		}
		
		  httpclient.close();
		  return true;
		}
	
	/*
	 * Method to create a temporary file in the Java temp file directory that contains the commands
	 * needed by cytoscape command tool to run enrichment map at the launch of cytoscape  
	 * (can not pass the command at the commandline as the -S option expects a script file)
	 * 
	 * return - path to the temporary script file created with the cytoscape command tool script to 
	 * 		load the current edb directory as an EM
	 */
	private String createTempCommandFile(){
		/*try{
			File temp;
			
			temp = File.createTempFile("gsea_em_commands", ".txt",new File(this.workingDirectory) );
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
			
			String command;
			if(CytoscapeLocationSingleton.getInstance().getMain_version() == 2)
				command = "EnrichmentMap_gsea build edbdir=\"" + this.params.getEdbdir() + "\" pvalue="+this.params.getPvalue()+" qvalue="+this.params.getQvalue()+" overlap=" + this.params.getSimilarityCutOff();
			else
				command = "EnrichmentMap gseabuild edbdir=\"" + this.params.getEdbdir() + "\" pvalue="+this.params.getPvalue()+" qvalue="+this.params.getQvalue()+" overlap=" + this.params.getSimilarityCutOff();
					
			command += " similaritymetric=\"" + this.params.getSimilarityMetric() + "\" combinedconstant=" + this.params.getCombinedConstant();
			if(!this.params.getExpressionFilePath().equals(""))
				command += " expressionfile=\"" + this.params.getExpressionFilePath() + "\"";
			
			//if there is a second edb directory specified:
			if(this.params.getEdbdir2() != null && !this.params.getEdbdir2().equalsIgnoreCase("")){
				command += " edbdir2=\"" + this.params.getEdbdir2() + "\"";
				if(!this.params.getExpression2FilePath().equals(""))
					command += " expressionfile2=\"" + this.params.getExpression2FilePath() + "\"";
			}
			
			if(this.os.startsWith("win"))
				command = command.replace("\\", "\\\\");
			bw.write(command);
				
			bw.close();
			return temp.getAbsolutePath();
			
		}catch (IOException ie){
			ie.printStackTrace();
			return null;
		}*/
			return "";
	}

	
	/*
	 * Method to issue build enrichment map command through Cytoscape rest interface.
	 */
		
}
