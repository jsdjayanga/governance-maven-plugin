package governance.plugin.handler;

import governance.plugin.license.JARFileNameReader;
import governance.plugin.license.LicenseTypeReader;
import governance.plugin.rxt.AssetCreatorUtil;
import governance.plugin.rxt.artifact.ArtifactCreator;
import governance.plugin.rxt.module.ModuleCreator;
import governance.plugin.util.Configurations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LicenseDetailsExtractHandler {
	private Map<String, String> jarNames = new HashMap<String, String>();
	private Map<String, String> licenseTypes =  new  HashMap<String, String>();
	private ModuleCreator moduleCreator; 
	private ArtifactCreator artifactCreator;
	private AssetCreatorUtil assetCreatorUtil;
	private JARFileNameReader jarFileNameReader;
	private LicenseTypeReader licenseTypeReader;
	Log logger;
	boolean resolvePerPom;
	
	public LicenseDetailsExtractHandler(Configurations configurations, Log logger, boolean resolvePerPom) throws MojoExecutionException{
		this.logger = logger;
		jarFileNameReader = new JARFileNameReader(logger); 
		licenseTypeReader = new LicenseTypeReader(logger);
		moduleCreator = new ModuleCreator(logger, configurations.getGergServiceUrl());
   	 	artifactCreator = new ArtifactCreator(logger, configurations.getGergServiceUrl());
   	 	assetCreatorUtil = new AssetCreatorUtil(configurations.getGergServiceUrl());
   	 	this.resolvePerPom = resolvePerPom;
	}
	
	private void extractDetailsFromPerPomCommand(List<MavenProject> pomTree) throws MojoExecutionException{
		for (MavenProject project : pomTree){
			try {
				if (!project.getDependencies().isEmpty()){
					jarFileNameReader.executePerPomCommand(new File(project.getFile().getParent()));
					jarNames = jarFileNameReader.getInformation(project);
				
					licenseTypeReader.executePerPomCommand(new File(project.getFile().getParent()));
					licenseTypes = licenseTypeReader.getInformation(project);
				}
            } catch (Exception e) {
            	// Continue to process the prom tree even if resolving of a single pom file fails
				logger.warn(e.getMessage());
            }
			updateAssets(project);
		}
	}
	
	private void extractDetailsFromRootPomCommand(List<MavenProject> pomTree) throws MojoExecutionException{
		try {
	        jarFileNameReader.executeFromRootPomCommand(new File(pomTree.get(0).getFile().getParent()));
	        licenseTypeReader.executeFromRootPomCommand(new File(pomTree.get(0).getFile().getParent()));
	        licenseTypes = licenseTypeReader.getInformation(pomTree.get(0));
			for (MavenProject project : pomTree){
				if (!project.getDependencies().isEmpty()){
    				jarNames = jarFileNameReader.getInformation(project);
    				jarFileNameReader.deleteOutputFile(project);
				}
				licenseTypeReader.deleteOutputFile(project);
				updateAssets(project);
			}
        } 
		catch (Exception e) {
			throw new MojoExecutionException("Following error occured while trying to execute with root pom " + e.getMessage() 
			                                 + ". Try with -DresolvePerPom=true to continue processing by igonreing errors occure while processing single pom.xml", e);
        }
	}
	
	public void process(List<MavenProject> pomTree) throws MojoExecutionException{
		if (resolvePerPom){
			extractDetailsFromPerPomCommand(pomTree);
		}else{
			extractDetailsFromRootPomCommand(pomTree);
		}
	}
	
	private void updateAssets(MavenProject project) throws MojoExecutionException{
		for (Dependency dependecny : project.getDependencies()){
			String key = AssetCreatorUtil.getKey(dependecny);
			String jarName = jarNames.get(key);
			String licenseType = licenseTypes.get(key);
			if ((jarName == null) && (licenseType == null)){
				logger.info("Cannot find 'Jar Name' and 'License Type' of " + key);
				continue;
			}
			
			// Retrive the exisitng asset from the Greg
			String type = null;
			String resourcePath = moduleCreator.getResourcePath(new String[]{dependecny.getArtifactId(), dependecny.getVersion()});
			Document assetContent = assetCreatorUtil.getAssetContent(resourcePath);
			if (assetContent != null){
				type = "module";
			}else{
				resourcePath = artifactCreator.getResourcePath(new String[]{dependecny.getGroupId(), dependecny.getArtifactId(), dependecny.getVersion()});
				assetContent = assetCreatorUtil.getAssetContent(resourcePath);
				if (assetContent != null){
					type = "artifact";
				}
			}
			
			if (assetContent == null){
				throw new MojoExecutionException("Cannot find information of dependency " + dependecny.getGroupId() + ":" 
														+ dependecny.getArtifactId() + ":" + dependecny.getVersion() + " in the repostory");
			}
			
			// Update the contents of the asset to have the license detials
			assetContent = addLicenseDetialsToContent(assetContent, dependecny, jarName, licenseType, project);
			
			logger.debug("Updating modules and artifacts for dependecny" + key + " of " + project.getFile().getAbsolutePath());
			
			// Send a requests to update the asset to GReg
			assetCreatorUtil.editAsset(resourcePath, type, assetContent);
		}
	}
	
	private Document addLicenseDetialsToContent(Document existingContent, Dependency dependecny, String jarName, String licenseType, MavenProject project){	
		if (jarName != null){
			AssetCreatorUtil.addJARFileNameToAsset(existingContent, jarName);
		}
		
		if (licenseType != null){
			AssetCreatorUtil.addLicenseTypeToAssset(existingContent, licenseType);
		}
		
		if (project != null){
			AssetCreatorUtil.addPackagingTypeToAsset(existingContent, project.getPackaging());
		}
		
		return existingContent;
	}
	
	
}