package governance.plugin.handler;

import java.io.File;
import java.util.List;

import governance.plugin.rxt.GRegDependencyHandler;
import governance.plugin.rxt.artifact.ArtifactCreator;
import governance.plugin.rxt.module.ModuleCreator;
import governance.plugin.util.Configurations;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class ModuleDependecnyHandler {
	private Log logger;
	private ModuleCreator moduleCreator; 
	private ArtifactCreator artifactCreator;
	private GRegDependencyHandler gregDependencyHandler;
	
	public ModuleDependecnyHandler(Configurations configurations, Log logger) throws MojoExecutionException{
		this.logger = logger;
	 	gregDependencyHandler = new GRegDependencyHandler(logger, configurations.getGergServiceUrl());
   	 	moduleCreator = new ModuleCreator(logger, configurations.getGergServiceUrl());
   	 	artifactCreator = new ArtifactCreator(logger, configurations.getGergServiceUrl());
	}
	
	public void process(List<MavenProject> pomTree) throws MojoExecutionException{
		logger.info("Creating modules...");
		createModules(pomTree);
		
		logger.info("Creating artifacts and assocations...");
		createDependencies(pomTree);
		
		
		logger.info("SUMMARY:" 
                      + "\npom.xml Files Processed..........." + pomTree.size()
                      + "\nModules ........[Created:" + moduleCreator.getCreatedAssetCount() 
                      + ", Existing:" + moduleCreator.getExistingAssetCount() + "]"
                      + "\nArtifacts ......[Created:" + artifactCreator.getCreatedAssetCount() 
                      + ", Existing:" + artifactCreator.getExistingAssetCount() + "]"
                      + "\nAssocations.....[Added:" + gregDependencyHandler.getAddedAssocationCount()
                      + ", Deleted:" + gregDependencyHandler.getRemovedAssocationCount() + "]");     
	}
	
	private void createModules(List<MavenProject> pomTree) throws MojoExecutionException{
		for (MavenProject project : pomTree){
	    	moduleCreator.create(new String[]{project.getArtifactId(), project.getVersion(), project.getFile().getAbsolutePath()});
		}
	}
	
	private void createDependencies(List<MavenProject> pomTree) throws MojoExecutionException{
		for (MavenProject project: pomTree){
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
    		logger.debug("Adding dependency to destination 'Module'" + returnValue);
    	}
    	else{
    		artifactCreator.create(new String[]{dependency.getGroupId(), dependency.getArtifactId(), 
		                                      dependency.getVersion()});
    	
    		returnValue = artifactCreator.
    			getAbsoluteResourcePath(new String[]{dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()});
    		
    		logger.debug("Adding dependency to destination 'Artifact'" + returnValue);
    	}
    	
    	return returnValue;
    }
	
}
