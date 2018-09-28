/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.cytoscape;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import edu.mit.broad.xbench.core.api.Application;

/* 
 * Class to launch cytoscape with Rest service enabled
 * 
 * Checks for user's cytoscape, checks if enrichmentmap are installed, launches cytoscape
 * with the rest enabled.
 */
public class CytoscapeLaunch {
	
	private static final Logger klog = Logger.getLogger(CytoscapeLaunch.class);

	//files to remotely install plugins
	private String emgsea_jar_toinstall_cyto3 = "enrichmentmap-v2.1.0.jar"; 
	private String install_site = "http://download.baderlab.org/EM_GSEA/"; 
	String file_separator;
	
	//the jvm used
	private String java;
	private String os;
	
	
	public CytoscapeLaunch(/*EnrichmentMapParameters params*/){

		this.os = System.getProperty("os.name").toLowerCase();
		this.file_separator = System.getProperty("file.separator");
		this.java = "java";
			
		
	}
	
	
	//Launch Cytoscape - specify whether to launch cytoscape with Cyrest enabled
		public void launch(){
			String launch_command = "";
			if(CytoscapeLocationSingleton.getInstance().getCytoscapeRootLocation() != null && this.java != null){
				try{
					String cytoscape;
					/*
					 * launch cytoscape with -R option to open up rest capabilities
					 */
					if(SystemUtils.IS_OS_WINDOWS)
						cytoscape = "cmd /c start cytoscape.bat -R 1234";
					else
						cytoscape = CytoscapeLocationSingleton.getInstance().getCytoscapeRootLocation() + this.file_separator +"cytoscape.sh -R 1234"; 
					    
					launch_command = cytoscape;
					
					//will always extract the EM jar as we package a special gsea EM with gsea.
					//There is a conflict when there is an EM already installed.  If the version is prior to 1.3.
					//If EM version <= 1.2 issue error message to say you might have unexpected behaviour, upgrade EM
					// or uninstall the previous version
					//only show error message once.
					double version_installed = alreadyInstalled(this.emgsea_jar_toinstall_cyto3);
					klog.info("EM version installed:" + version_installed );
					
					if(version_installed <= 1.3 && version_installed > 0 && !CytoscapeLocationSingleton.getInstance().isErrorMsgViewed()){
						Application.getWindowManager().showConfirm("An older incompatible version of Enrichment map is already installed on your system.\n  Please update to a version higher than v1.2 or uninstall your Enrichment Map plugin.  Cytoscape Launch will still work but you may experience unexpected errors.");
						CytoscapeLocationSingleton.getInstance().setErrorMsgViewed(true);
					}
					
					//For cytoscape 2.X need to install the gsea version
					if(CytoscapeLocationSingleton.getInstance().getMain_version() == 2){						
						Application.getWindowManager().showConfirm("Cytoscape 2 is not supported by GSEA.  Please install latest version of Cytoscape");						
					}
					
					//If they have cytoscape 3.X and EM is not installed, try and install it.
					else if(CytoscapeLocationSingleton.getInstance().getMain_version() >= 3 && version_installed <0){
						this.installPlugin(this.emgsea_jar_toinstall_cyto3);								
					}
					
					klog.info("command issued:" + launch_command );
					Process proc;
					if(SystemUtils.IS_OS_WINDOWS){
						
						proc = Runtime.getRuntime().exec(launch_command,null,new File(CytoscapeLocationSingleton.getInstance().getCytoscapeRootLocation()));
					}
//					else if (SystemUtils.IS_OS_MAC_OSX) {
//					}
					else{
                        String javahome = SystemUtils.JAVA_HOME;
					    
                        
                        // This should all be unnecessary...
                        
	                       //proc = Runtime.getRuntime().exec(launch_command);
	                       
					    //get the path to java - can't execute command from the envp that we supply to the process so run it separately.
					    //this would be much more elegant if I could just pass "JAVA_HOME=/usr/libexec/java_home" to the process below
					    //but it won't recognize the command (tried with surrounding `` and ($ )
//					    Process proc_javahome = Runtime.getRuntime().exec("/usr/libexec/java_home");
//					    InputStream stdin_javahome = proc_javahome.getInputStream();
//                        InputStreamReader isi_javahome = new InputStreamReader(stdin_javahome);
//                        BufferedReader br_in_javahome = new BufferedReader(isi_javahome);
//                        String line_in_javahome = null, javahome = null;
//                        while ((line_in_javahome = br_in_javahome.readLine()) != null){
//                            klog.info("Java home:" + line_in_javahome);
//                            javahome=line_in_javahome;
//                            
//                        }

                        //if there are multiple javas installed (specifically java 7 and java 8 on a Mac 
                        //cytoscape was not launching.  specify the java version fixes it.  When launching with envp
                        //none of the environment variables seem to be defined so also pass it in the home directory which the script
                        //also needs. 
                        
                        // Can we hoist this above both branches?  Should work all around, I would think.
       				    String[] envp = {"JAVA_HOME="+javahome,"HOME=" + System.getProperty("user.home")};
					    proc = Runtime.getRuntime().exec(launch_command,envp);
						
					}
														
					} catch(IOException e){
						klog.info("command issued:" + launch_command + "crashed." + e.getMessage() );
					}
					System.gc(); //do garbage collection so we can re-run command with no problem.					
				
			}
			else{
				klog.info("cytoscape root or java is null:" + this.java + "," + CytoscapeLocationSingleton.getInstance().getCytoscapeRootLocation());
			}
				
		}
	
			

		
	/*
	 * Install the app into the cytsocape configuration diroectory.
	 * 
	 * Given the name of the jar to get
	 * returns - the path to the jar on the current system
	 */
	private String installPlugin(String name){
		URL url;
        URLConnection con;
        DataInputStream dis; 
        FileOutputStream fos; 
        byte[] fileData;
        
        //get the user's home
		String home_dir = System.getProperty("user.home");
		
        //cytoscape home directory
		String cyto_home = home_dir + this.file_separator  + "CytoscapeConfiguration" + this.file_separator 
				+ CytoscapeLocationSingleton.getInstance().getMain_version() + this.file_separator  + "apps" 
				+ this.file_separator  + "installed";
        
        if(CytoscapeLocationSingleton.getInstance().getCytoscapeConfigLocatin() != null){
        		try {
        			url = new URL(this.install_site + name); //File Location goes here
        			con = url.openConnection(); // open the url connection.
        			dis = new DataInputStream(con.getInputStream());
        			fileData = new byte[con.getContentLength()]; 
        			for (int q = 0; q < fileData.length; q++) { 
        				fileData[q] = dis.readByte();
        			}
        			dis.close(); // close the data input stream
            
           			String filename = cyto_home + File.separator + name;
        			fos = new FileOutputStream(new File(filename)); //FILE Save Location goes here
        			
        			fos.write(fileData);  // write out the file we want to save.
        			fos.close(); // close the output stream writer
        			
        			return filename;
        		}
        		catch(Exception m) {
        			System.out.println(m);
        			return null;
        		}
        }
        else
        		klog.info("Cytoscape Configuration not set.  Nowhere to install the app to");
        return null;
	}
	
	/*
	 * Check to see if the given cytoscape app jar is installed on the current system	 
	 * Return - version of the app installed on the system.  If it isn't installed return -1. 
	 */
	private double alreadyInstalled(String plugin_jar){
		double version = -1.0;
		String plugin = (plugin_jar.split("\\.").length > 1) ? plugin_jar.split("\\.")[0] : plugin_jar;
		
		//check to see if the plugin has a -v attached
		plugin = (plugin.contains("-v")) ? plugin.split("-v")[0] : plugin;
		
		klog.info("looking for prefix plugin:" + plugin );
		//For version 2.X check in the plugin directory and hidden directory
		if(CytoscapeLocationSingleton.getInstance().getMain_version() == 2){
		
			Application.getWindowManager().showConfirm("Cytoscape 2 is not supported by GSEA.  Please install latest version of Cytoscape");
			
		}
		
		//For version 3.X check in the CytoscapeConfiguration directory in the home directory
		if(CytoscapeLocationSingleton.getInstance().getMain_version() == 3){
			//check for the app in cytoscape directory under the user's home directory
			
			//~/CytoscapeConfiguration/3/apps/installed
			//get the user's home
			String home_dir = System.getProperty("user.home");
			
			//cytoscape home directory
			String cyto_home = home_dir + this.file_separator  + "CytoscapeConfiguration" + this.file_separator 
					+ CytoscapeLocationSingleton.getInstance().getMain_version() + this.file_separator  + "apps" 
					+ this.file_separator  + "installed";
			//check to see if home directory exists
			File cyto_home_dir = new File(cyto_home);
			
			if(cyto_home_dir.isDirectory()){

				//initialize the cytoscape home directory
				CytoscapeLocationSingleton.getInstance().setCytoscapeConfigLocatin(cyto_home);
				
				String[] cytohome_plugins = cyto_home_dir.list();
				for(int i =0; i<cytohome_plugins.length;i++){
					if(cytohome_plugins[i].startsWith(plugin)){
						String[] parts = cytohome_plugins[i].split("-v");
						String[] version_parts = (parts.length>1) ? parts[1].split("\\.") : null ;
						
						//there should be 3 parts to the version otherwise not the right format.
						//current versioning system since 1.3.0
						if(version_parts != null && version_parts.length >= 3){
							Integer temp_main_version = Integer.parseInt(version_parts[0]);
							Integer temp_sub_version = Integer.parseInt(version_parts[1]);
							//Integer temp_release = Integer.parseInt(version_parts[2]);
							
							version = temp_main_version + (temp_sub_version * 0.1);
						}
						if(version_parts != null && version_parts.length == 2){
							Integer temp_main_version = Integer.parseInt(version_parts[0]);
							Integer temp_sub_version = Integer.parseInt(version_parts[1]);
							
							version = temp_main_version + (temp_sub_version * 0.1);
						}
					}//if plugin
						
				}//for loop of all files in directory
			}//is directory
		}//cytoscape version 3 
		return version;
	}
	
	
	
	
	
}

