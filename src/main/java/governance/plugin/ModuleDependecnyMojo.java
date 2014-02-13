package governance.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import governance.plugin.rxt.ArtifactCreator;
import governance.plugin.rxt.ModuleCreator;

import java.io.File;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.Profile;

/**
 * Generates dependency tree by reading a pom.xml
 */
@Mojo( name = "module", defaultPhase = LifecyclePhase.DEPLOY,  aggregator = true)
public class ModuleDependecnyMojo extends AbstractMojo
{
 	@Parameter ( defaultValue = "${project}" )
    private MavenProject project;
	
    @Parameter( property = "location" )
    private String repositoryLocation;
    
    @Parameter( defaultValue = "${settings}" )
    private Settings settings;
    
    @Parameter( property = "gregServiceUrl")
    private String gregServiceUrl;
    
    @Parameter( property = "gregUsername")
    private String gregUsername;
    
    @Parameter( property = "gregPassword")
    private String gregPassword;
    
    @Parameter( property = "gregHome")
	private String gregHome;
    
	//GReg resource paths
	public static final String GREG_TRUNK_LOCATION = "/_system/governance";
	public static final String GREG_MODULE_RESOURCE_PATH = "/trunk/modules/";
	public static final String GREG_ARTIFACT_RESOURCE_PATH = "/trunk/artifacts/";
	
	private int pomFileCount = 0;
	private int directoryCount = 0;

	private ModuleCreator moduleCreator; 
	private ArtifactCreator artifactCreator;
	private GRegDependencyHandler gregDependencyHandler;
	private Configurations configurations;


    public ModuleDependecnyMojo() throws MojoExecutionException{
    	 
    }
    
    public void execute() throws MojoExecutionException
    {	
    	configurations = new Configurations(project, settings, repositoryLocation, gregServiceUrl, gregUsername, gregPassword, gregHome);
   	 
   	 	gregDependencyHandler = new GRegDependencyHandler(getLog(), configurations.getGergServiceUrl());
   	 	moduleCreator = new ModuleCreator(getLog(), configurations.getGergServiceUrl());
   	 	artifactCreator = new ArtifactCreator(getLog(), configurations.getGergServiceUrl());
   	 
    	configure();
    	
    	getLog().info("Starting to scan with root:" +  configurations.getRepoLocation());
        scanDirectory(configurations.getRepoLocation());
    		
        getLog().info("SUMMARY:" 
        			  + "\nDirectories Scanned..............." + directoryCount 
                      + "\npom.xml Files Processed..........." + pomFileCount
                      + "\nModules ........[Created:" + moduleCreator.getCreatedAssetCount() 
                      + ", Existing:" + moduleCreator.getExistingAssetCount() + "]"
                      + "\nArtifacts ......[Created:" + artifactCreator.getCreatedAssetCount() 
                      + ", Existing:" + artifactCreator.getExistingAssetCount() + "]"
                      + "\nAssocations.....[Added:" + gregDependencyHandler.getAddedAssocationCount()
                      + ", Deleted:" + gregDependencyHandler.getRemovedAssocationCount() + "]");
                      
    }
    
    private void configure(){
    	
    	System.setProperty("javax.net.ssl.trustStore", configurations.getGregHome() +  File.separator + "repository" + File.separator + 
    	                   "resources" + File.separator + "security"+ File.separator + "client-truststore.jks");
    	
    	System.setProperty("javax.net.ssl.trustStorePassword","wso2carbon");
    	System.setProperty("javax.net.ssl.trustStoreType","JKS");
    	
    	System.setProperty("javax.net.ssl.trustStore", configurations.getGregHome() + File.separator + "repository" +
                File.separator + "resources" + File.separator + "security" + File.separator +
                "wso2carbon.jks");
    	
    	RegistrySOAPClient.setCredentials(configurations.getGregUserName(), configurations.getGregPassword());
    }
    
    private void scanDirectory(String path) throws MojoExecutionException{
      	File root = new File(path);
    	if (root.isDirectory()){
    		directoryCount++;
    		File[] children = root.listFiles();
    		if (children == null){
       		 	getLog().debug("Empty directoy skipping.. :" + path);
    		}else{
    			for (File child : children){
    				scanDirectory(child.getAbsolutePath());
    			}
    		}
    	}
    	else{
    		process(new File(path));
    	}
    	getLog().debug("Finished scanning directory :" + path);
    }
    
   
        
    public void process(File file) throws MojoExecutionException{
    	if (file.isFile() &&  file.getName().equals("pom.xml")){
    		pomFileCount++;
    		getLog().debug("Processing " + file.getAbsoluteFile());
    		
    		Model model = PomParser.parse(file);
    		if (model == null){
    			throw new MojoExecutionException("Error while processing  " + file.getAbsoluteFile());
    		}
    		
        	MavenProject project = new MavenProject(model);
        	
        	moduleCreator.create(new String[]{project.getArtifactId(), project.getVersion(), file.getAbsolutePath()});
        	
        	String moduleAbsolutPath = moduleCreator.
        			getAbsoluteResourcePath(new String[]{project.getArtifactId(), project.getVersion()});
        	gregDependencyHandler.removeExistingAssociations(moduleAbsolutPath,  
        	                                                GRegDependencyHandler.GREG_ASSOCIATION_TYPE_DEPENDS);
        	List<Dependency> dependencies = 
        			MavenDependencyVersionResolver.resolveDependencyVersions(project, file.getAbsolutePath());
        	
        	for (Dependency dependency : dependencies){
        		String dependencyReosurcePath = getDependencyPath(dependency);
        		
        		// Adding the dependency
        		gregDependencyHandler.addAssociation(moduleAbsolutPath, dependencyReosurcePath, 
        		                                     GRegDependencyHandler.GREG_ASSOCIATION_TYPE_DEPENDS);
        		
        		// Adding the invert association(i.e.dependency is usedBy source)
        		gregDependencyHandler.addAssociation(dependencyReosurcePath, moduleAbsolutPath, 
        		                                     GRegDependencyHandler.GREG_ASSOCIATION_TYPE_USEDBY);
        	}
    	}
    }  
    
    /**
     * Check if there's 'Module' asset representing the given dependency, if there's is not 'Module' asset
     * create an 'Artifact' asset to represent the dependency
     * @param dependency Dependency to be added
     * @return If there's a module already created for the dependency, return the resource path of that module
     * 		   or return the resource path of newly create 'Artifact' asset
     * @throws MojoExecutionException
     */
    private String getDependencyPath(Dependency dependency) throws MojoExecutionException{
    	String returnValue = null;
    	if (moduleCreator.isModuleExisting(dependency.getArtifactId(), dependency.getVersion())){
    		returnValue =  moduleCreator.getAbsoluteResourcePath(new String[]{dependency.getArtifactId(), dependency.getVersion()});
    		getLog().debug("Adding dependency to destination 'Module'" + returnValue);
    	}
    	else{
    		artifactCreator.create(new String[]{dependency.getGroupId(), dependency.getArtifactId(), 
		                                      dependency.getVersion()});
    	
    		returnValue = artifactCreator.
    			getAbsoluteResourcePath(new String[]{dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()});
    		
    		getLog().debug("Adding dependency to destination 'Artifact'" + returnValue);
    	}
    	
    	return returnValue;
    }
}
