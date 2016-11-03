/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.cytoscape;

import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

public class CytoscapeLocationSingleton {
	
	private static CytoscapeLocationSingleton locationInfo = null;
	
	private String cytoscapeRootLocation = null;
	private String cytoscapeConfigLocatin = null;
	private static final Logger klog = XLogger.getLogger(CytoscapeLocationSingleton.class);
	private Integer main_version =0;
	private Integer sub_version = 0;
	private String cytoscape_download_url = "http://www.cytoscape.org/download.html";
	
	private static JFileChooser fc = new JFileChooser(); 
	
	private boolean errorMsgviewed = false;
	
	public static CytoscapeLocationSingleton getInstance(){
		if(locationInfo == null || locationInfo.cytoscapeRootLocation == null || locationInfo.cytoscapeRootLocation == "")
			locationInfo = new CytoscapeLocationSingleton();

		return locationInfo;
		
	}
	
	private CytoscapeLocationSingleton() {
		klog.info(XPreferencesFactory.kCytoscapeDirectory.getValue().toString());
		//check to see if Cytoscape preference is already set.	
		String userdefined_Location = "";
		if(!XPreferencesFactory.kCytoscapeDirectory.getValue().toString().equalsIgnoreCase("Cytoscape not set yet") &&
				!XPreferencesFactory.kCytoscapeDirectory.getValue().toString().equalsIgnoreCase("null") &&
				!XPreferencesFactory.kCytoscapeDirectory.getValue().toString().equalsIgnoreCase(""))
			userdefined_Location = XPreferencesFactory.kCytoscapeDirectory.getDir(false).toString();
		
		//if the directory doesn't exist don't bother checking it. 
		if(!(new File(userdefined_Location)).exists() )
			userdefined_Location = "";
		
		this.cytoscapeRootLocation = getCytoscapeLocation(userdefined_Location);
	}

	private String getCytoscapeLocation(String userdefined_Location){

		//figure out which operating system we are on.
		String os = System.getProperty("os.name").toLowerCase();
		String cytoscpaeLocation = "";
		
		//irrespective of operating system if the user defined directory is set then check there first
		//If default location is not null then check there first
		if(userdefined_Location != ""){
			File path = new File(userdefined_Location).getParentFile();
			String[] cyto_list_user = path.list(new OnlyCyto("Cytoscape"));
			
			cytoscpaeLocation = new File(userdefined_Location).getParent()+ File.separator + getLatestCytoscapeVersion(cyto_list_user) ;
			
			if(cytoscpaeLocation.equalsIgnoreCase(new File(userdefined_Location).getParent()+ File.separator ))
				cytoscpaeLocation = "";
		}
		
		
		//on Mac cytoscape is located in /Applications
		if(os.startsWith("mac")){
						
			//if there is no cytoscape in the user defined location then check the default location
			if(cytoscpaeLocation == ""){
				//get the cytoscapes installed on the machine
				File root = new File("/Applications");
				String[] cyto_list = root.list(new OnlyCyto("Cytoscape"));
			
				cytoscpaeLocation = "/Applications/" + getLatestCytoscapeVersion(cyto_list) ;			
				if(cytoscpaeLocation.equalsIgnoreCase("/Applications/"))
					cytoscpaeLocation = "";
			}
			

					
		}
		else if(os.startsWith("win")){
			
			//if there is no cytoscape in the user defined location then check the default location
			if(cytoscpaeLocation == ""){
				//get the cytoscapes installed on the machine
				File root = new File("C:\\Program Files");
				String[] cyto_list = root.list(new OnlyCyto("Cytoscape"));
			
				//on windows there is an issue with spaces in the path.  Put quotes around the whole thing
				cytoscpaeLocation = "C:\\Program Files\\" + getLatestCytoscapeVersion(cyto_list) ;
				if(cytoscpaeLocation.equalsIgnoreCase("C:\\Program Files\\"))
					cytoscpaeLocation = "";
			}		
			
		}
		else if(os.startsWith("unix")){
			
		}
		else {
			klog.debug("Your OS:" + os + " is not supported");
		}

		
		//if there is no version of Cytoscape installed then direct user to Cytoscape website to download
		//and install it.
		if(cytoscpaeLocation == null || cytoscpaeLocation.equals("")|| (this.main_version<3) || (this.main_version==3 && this.sub_version<3)){
			klog.info("No compatible version of cytoscape is installed");
			cytoscpaeLocation = RedirectCytoscapeDownload();
		}
		try {
			if(cytoscpaeLocation != null || cytoscpaeLocation != "")
				XPreferencesFactory.kCytoscapeDirectory.setValue(cytoscpaeLocation);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return cytoscpaeLocation;
	}
	
	/*
	 * Class to filter Files - only accept files or directories starting with the string defined.
	 */
	public class OnlyCyto implements FilenameFilter{
		String cyto;
		public OnlyCyto(String cyto){
			this.cyto = cyto;
		}
		public boolean accept(File dir, String name){
			return name.startsWith(cyto);
		}
	}
	
	/*
	 * 
	 * Method to go through the list of cytoscape versions and return the latest version directory name
	 * Given a array of all files or directories starting with Cytoscape
	 * If there is no version of cytoscape in the list then method return "" (empty string)
	 */
	private String getLatestCytoscapeVersion(String[] cyto_list){
		//go through all the cytoscapes - get the latest version
		String latest = "";
		Integer release = 0;
		for(int i =0; i<cyto_list.length;i++){
			String version = (cyto_list[i].split("Cytoscape_v").length > 1) ? cyto_list[i].split("Cytoscape_v")[1] : "";

			String[] version_parts = version.split("\\.");
			
			//there should be 3 parts to the version otherwise not the right format.
			//current versioning system since 2.3.0
			if(version_parts.length == 3){
				try{
					Integer temp_main_version = Integer.parseInt(version_parts[0]);
					Integer temp_sub_version = Integer.parseInt(version_parts[1]);
					Integer temp_release = Integer.parseInt(version_parts[2]);
									
					if((latest.equals("") || main_version < temp_main_version || sub_version < temp_sub_version ||
						release < temp_release)){
						latest = cyto_list[i];
						this.main_version = temp_main_version;
						this.sub_version = temp_sub_version;
						release = temp_release;
					}
				}catch(NumberFormatException e){
					//invalid version, skip this one
					continue;
				}
			}
		}
		klog.info("Latest version of cytoscape is:" + latest);
		return latest;
	}
	
	/*
	 * If Cytoscape is not installed on this system pop up a console indicating this and rederict browser to 
	 * cytoscape download page.
	 */
	private String RedirectCytoscapeDownload(){
		
		String cytoscapeLocation = "";
		try{
			String[] options = new String[]{"Download","Locate"};
			int n = JOptionPane.showOptionDialog(null, "Unable to find Cytoscape 3.3 on your System.\n Would you like to install it?\n Or navigate to the directory where it is installed?\n PLEASE make sure to install CYTOSCAPE 3.3 or higher", "Cytoscape Not found", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,null,options,options[0]);
			
			//redirect to cytoscape download page
			if(n == 0){				
				Desktop.getDesktop().browse(new URL(this.cytoscape_download_url).toURI());
			}
			//allow user to navigate to the directory where cytoscape is installed
			else if( n == 1){
				 //fc = new JFileChooser();
				 fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				 int returnVal = fc.showOpenDialog(null);				 
				 if (returnVal == JFileChooser.APPROVE_OPTION) {
			            File file = fc.getSelectedFile();
			            cytoscapeLocation = file.getAbsolutePath();
			            
			            String version = file.getCanonicalPath().split("Cytoscape_v")[1];
						String[] version_parts = version.split("\\.");
			            if(version_parts.length > 1){
			            		this.main_version = Integer.parseInt(version_parts[0]);
			            		this.sub_version = Integer.parseInt(version_parts[1]);
			            }
			            else
			            		System.out.println("Can't calculate cytoscape version from specified location");
			        } else {
			            System.out.println("Open command cancelled by user.\n" );
			        }
			}
		} catch (Throwable t) {
			Application.getWindowManager().showError(t);
		}
		return cytoscapeLocation;
	}

	public String getCytoscapeRootLocation() {
		return cytoscapeRootLocation;
	}

	public void setCytoscapeRootLocation(String cytoscapeRootLocation) {
		this.cytoscapeRootLocation = cytoscapeRootLocation;
		
		String version = this.cytoscapeRootLocation.split("Cytoscape_v")[1];
		String[] version_parts = version.split("\\.");
		
		//there should be 3 parts to the version otherwise not the right format.
		//current versioning system since 2.3.0
		if(version_parts.length == 3){
			this.main_version = Integer.parseInt(version_parts[0]);
			this.sub_version = Integer.parseInt(version_parts[1]);
			Integer temp_release = Integer.parseInt(version_parts[2]);
			//only get the version of cytoscape less than 3.0.0
			}
		
	}

	public Integer getMain_version() {
		return main_version;
	}

	public void setMain_version(Integer main_version) {
		this.main_version = main_version;
	}

	public Integer getSub_version() {
		return sub_version;
	}

	public void setSub_version(Integer sub_version) {
		this.sub_version = sub_version;
	}

	public boolean isErrorMsgViewed() {
		return errorMsgviewed;
	}

	public void setErrorMsgViewed(boolean errorMsg) {
		this.errorMsgviewed = errorMsg;
	}

	public String getCytoscapeConfigLocatin() {
		return cytoscapeConfigLocatin;
	}

	public void setCytoscapeConfigLocatin(String cytoscapeConfigLocatin) {
		this.cytoscapeConfigLocatin = cytoscapeConfigLocatin;
	}
	
	
}
