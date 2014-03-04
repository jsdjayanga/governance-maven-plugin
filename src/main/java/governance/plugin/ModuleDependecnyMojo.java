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

import governance.plugin.common.RegistrySOAPClient;
import governance.plugin.rxt.artifact.ArtifactCreator;
import governance.plugin.rxt.module.ModuleCreator;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

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
    
    @Parameter( property = "buildProfile")
   	private String buildProfile;
    
	private ModuleCreator moduleCreator; 
	private ArtifactCreator artifactCreator;
	private GRegDependencyHandler gregDependencyHandler;
	private Configurations configurations;
	
	private Set<MavenProject> mavenProjects = new HashSet<MavenProject>();
    
    public void execute() throws MojoExecutionException
    {	
    	configurations = new Configurations(project, settings, repositoryLocation, gregServiceUrl, gregUsername, gregPassword, gregHome, buildProfile);
   	 
   	 	gregDependencyHandler = new GRegDependencyHandler(getLog(), configurations.getGergServiceUrl());
   	 	moduleCreator = new ModuleCreator(getLog(), configurations.getGergServiceUrl());
   	 	artifactCreator = new ArtifactCreator(getLog(), configurations.getGergServiceUrl());
   	 
    	configure();
    	
    	getLog().info("Creating modules....");
    	createModules(configurations.getRepoLocation());
    	
    	getLog().info("Creating artifacts and adding associations....");
    	createDependencies();
    		
        getLog().info("SUMMARY:" 
                      + "\npom.xml Files Processed..........." + mavenProjects.size()
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
    
    private void createModules(String rootPomPath) throws MojoExecutionException{
    	String filePath = rootPomPath.concat(File.separatorChar + "pom.xml");
    	MavenProject project = createMavenProject(new File(filePath));
    	if (project == null){
    		throw new MojoExecutionException("Cannot find pom.xml @ " + rootPomPath);
    	}
    	
    	moduleCreator.create(new String[]{project.getArtifactId(), project.getVersion(), rootPomPath});
    	mavenProjects.add(project);
    	
    	// Go through module section of the pom and create modules for them as well
    	List<String> modules = project.getModules();
    	List<Profile> profiles = project.getModel().getProfiles();
    	for (Profile profile : profiles){
    		if (profile.getId().equals(configurations.getBuildProfileId())){
    			getLog().info("Adding modules of maven profile '"  + configurations.getBuildProfileId() + "'");
    			modules.addAll(profile.getModules());
    		}
    	}
    	
    	for (String module : modules){
    		createModules(rootPomPath.concat(File.separatorChar + module.replace('/', File.separatorChar)));
    	}
    }
    
    private void createDependencies() throws MojoExecutionException{
    	for (MavenProject project: mavenProjects){
    		String moduleAbsolutPath = moduleCreator.
        			getAbsoluteResourcePath(new String[]{project.getArtifactId(), project.getVersion()});
        	
        	gregDependencyHandler.removeExistingAssociations(moduleAbsolutPath);
        	
        	List<Dependency> dependencies = project.getDependencies();	
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
  
    public MavenProject createMavenProject(File file) throws MojoExecutionException{
    	MavenProject project = null;
    	if (file.exists()){
    		getLog().debug("Processing " + file.getAbsoluteFile());

    		Model model = XmlParser.parsePom(file);
    		if (model == null){
    			throw new MojoExecutionException("Error while processing  " + file.getAbsoluteFile());
    		}
    		project = new MavenProject(model);
    		
    		EffectivePom effectivePom = new EffectivePom(file);
        	project = effectivePom.fillChildProject(project);
    	}
    	return project;
    }  

    /**
     * Check if there's 'Module' asset representing the given dependency, if there's is no 'Module' asset
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
