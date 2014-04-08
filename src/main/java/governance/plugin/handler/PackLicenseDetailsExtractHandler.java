package governance.plugin.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;

import governance.plugin.license.LicenseTypeReader;
import governance.plugin.license.PackJARDetailsReader;
import governance.plugin.rxt.AssetCreatorUtil;
import governance.plugin.rxt.artifact.ArtifactCreator;
import governance.plugin.rxt.module.ModuleCreator;
import governance.plugin.util.Configurations;

public class PackLicenseDetailsExtractHandler {
	Log logger;
	Configurations configurations;
	LicenseTypeReader licenseTypeReader;
	ModuleCreator moduleCreator;
	ArtifactCreator artifactCreator;
	AssetCreatorUtil assetCreatorUtil;
	HashMap<String, MavenProject> jarsInPack = new HashMap<String, MavenProject>();// Key-JarFileName, Value-Project model created form the pom.xml of the jar 
	HashMap<String, String> licenseInfo = new HashMap<String, String>(); // Key-dependeny, Value-normalized license type 
	int updatedAssetCount = 0;
	
	public PackLicenseDetailsExtractHandler(Configurations configurations, Log logger) throws MojoExecutionException {
	    this.logger = logger;
	    this.configurations = configurations;
	    licenseTypeReader = new LicenseTypeReader(logger);
	    moduleCreator = new ModuleCreator(logger, configurations.getGergServiceUrl());
	    artifactCreator = new ArtifactCreator(logger, configurations.getGergServiceUrl());
	    assetCreatorUtil = new AssetCreatorUtil(configurations.getGergServiceUrl());
    }
	
	public void process(File pack, List<MavenProject> pomTree) throws MojoExecutionException{
		logger.info("Extracting license details...");
		indexLicenseInformation(pomTree);
		
		logger.info("Extracting maven project detils of jars inside the pack...");
		jarsInPack = PackJARDetailsReader.readPack(pack);
		logger.info("Maven project detials of " + jarsInPack.size() + " jars fetched.");
		
		logger.info("Updating license details..");
		updateLicenseInformation();
		
		logger.info("Summary. ");
		logger.info("-Updated Artifact/Module Count = " + updatedAssetCount); 
		logger.info("-Created Artifact Count = " + artifactCreator.getCreatedAssetCount());
		logger.info("-Total JAR File Count = " + jarsInPack.size());
	}
	
	private void indexLicenseInformation(List<MavenProject> pomTree){
		for (MavenProject project : pomTree){
			try {
				if (!project.getDependencies().isEmpty()){	
					logger.debug("Extracting licennse details of " + project.getFile().getAbsolutePath());
					licenseTypeReader.executePerPomCommand(new File(project.getFile().getParent()));
					licenseInfo.putAll(licenseTypeReader.getInformation(project));
				}
            }catch (FileNotFoundException ex){
            	//if there are no licese details available it migh be possible for file not to be generated
            	//therefor, this exception can be ignored.
            }
			catch (Exception e) {
            	// Continue to process the prom tree even if resolving of a single pom file fails
				logger.warn(e.getMessage());
            }
		}
		logger.info("Licenense information of " + licenseInfo.size() + " dependencies fetched.");
	}
	
	private void updateLicenseInformation() throws MojoExecutionException{
		for (Entry<String, MavenProject> projectEntry : jarsInPack.entrySet()){
			MavenProject project = projectEntry.getValue();
			String groupId = null;
			String artifactId = null;
			String version = null;
			try{
    			groupId = (project.getGroupId().contains("$")) ?  resolveProperty(project, project.getGroupId()) : project.getGroupId();
    			artifactId = (project.getArtifactId().contains("$")) ?  resolveProperty(project, project.getArtifactId()) : project.getArtifactId();
    			version = (project.getVersion().contains("$")) ?  resolveProperty(project, project.getVersion()) : project.getVersion();
			}catch (MojoExecutionException ex){
				logger.warn("Failed to add/update artifact for JAR " + projectEntry.getKey() + ". Error: " + ex.getMessage());
				continue;
			}
			
			String key = AssetCreatorUtil.getKey(groupId, artifactId, version);
			String resourcePath = moduleCreator.getResourcePath(new String[]{artifactId, version});
			Document assetDetails = assetCreatorUtil.getAssetContent(resourcePath);
			
			if (assetDetails != null){
				updateAsset(resourcePath, assetDetails, "module",  projectEntry.getKey(), key, project.getPackaging());
			}else{
				resourcePath = artifactCreator.getResourcePath(new String[]{groupId, artifactId, version});
				assetDetails = assetCreatorUtil.getAssetContent(resourcePath);
			}
			
			if (assetDetails != null){
				updateAsset(resourcePath, assetDetails, "artifact",  projectEntry.getKey(), key, project.getPackaging());
			}else{
				artifactCreator.create(new String[]{groupId, artifactId, version, projectEntry.getKey(), licenseInfo.get(key), project.getPackaging()});
			}
		}
	}
	
	private void updateAsset(String resourcePath, Document assetDetails, String assetType, String jarFileName, String dependencyKey, String packagingType) throws MojoExecutionException{
		assetDetails = AssetCreatorUtil.addJARFileNameToAsset(assetDetails, jarFileName);
		assetDetails = AssetCreatorUtil.addPackagingTypeToAsset(assetDetails, packagingType);
		assetDetails = AssetCreatorUtil.addLicenseTypeToAssset(assetDetails, licenseInfo.get(dependencyKey));
		assetCreatorUtil.editAsset(resourcePath, assetType, assetDetails);
		updatedAssetCount++;
	}
	
	private String resolveProperty(MavenProject project, String property) throws MojoExecutionException{
		Properties properties = project.getProperties();
		String key = property.substring(property.indexOf("{") + 1, property.indexOf("}"));
		String value = (String)properties.get(key);
		if (value == null){
			throw new MojoExecutionException("Cannot find the value of " + property);
		}
		property = property.replace("${" + key + "}", value);
		return property;
	}
}


