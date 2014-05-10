package governance.plugin.handler;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import governance.plugin.license.LicenseFile;
import governance.plugin.license.PackJARDetailsReader;
import governance.plugin.rxt.AssetCreatorUtil;
import governance.plugin.rxt.artifact.ArtifactCreator;
import governance.plugin.rxt.module.ModuleCreator;
import governance.plugin.rxt.otherdependency.OtherDependencyCreator;
import governance.plugin.util.Configurations;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;

public class PackLicenseFileGenerateHandler {
	Log logger;
	Configurations configurations;
	ModuleCreator moduleCreator;
	ArtifactCreator artifactCreator;
	AssetCreatorUtil assetCreatorUtil;
	HashMap<String, MavenProject> jarsInPack = new HashMap<String, MavenProject>();// Key-JarFileName, Value-Project model created form the pom.xml of the jar 
	LicenseFile licenseFile;
	OtherDependencyCreator otherDependencyCreator;
	
	public PackLicenseFileGenerateHandler(Configurations configurations, Log logger) throws MojoExecutionException {
	    this.logger = logger;
	    this.configurations = configurations;
	    moduleCreator = new ModuleCreator(logger, configurations.getGergServiceUrl());
	    artifactCreator = new ArtifactCreator(logger, configurations.getGergServiceUrl());
	    assetCreatorUtil = new AssetCreatorUtil(configurations.getGergServiceUrl());
	    otherDependencyCreator = new OtherDependencyCreator(logger, configurations.getGergServiceUrl());
	    licenseFile = new LicenseFile(logger);
    }

	public void process(File pack) throws MojoExecutionException{
		jarsInPack = PackJARDetailsReader.readPack(pack);
		
		for (Entry<String, MavenProject> projectEntry : jarsInPack.entrySet()){
			MavenProject project = projectEntry.getValue();
			String resourcePath = artifactCreator.getResourcePath(new String[]{project.getGroupId(), project.getArtifactId(), project.getVersion()});
			Document assetDetails = assetCreatorUtil.getAssetContent(resourcePath);
			String key = AssetCreatorUtil.getKey(project.getGroupId(), project.getArtifactId(), project.getVersion());
			
			if (assetDetails != null){
				addLicenseEntryFromAssetContent(assetDetails, key, projectEntry.getKey());
			}else{
				resourcePath = moduleCreator.getResourcePath(new String[]{project.getArtifactId(), project.getVersion()});
				assetDetails = assetCreatorUtil.getAssetContent(resourcePath);
			}
			
			if (assetDetails != null){
				addLicenseEntryFromAssetContent(assetDetails, key, projectEntry.getKey());
			}else{
				licenseFile.addLicenseEntry(projectEntry.getKey(), LicenseFile.UNKOWN_ELEMENT, LicenseFile.UNKOWN_ELEMENT, key);
			}
		}
		
		HashSet<String> nonMavenJars = PackJARDetailsReader.getNonMavenJars();
		
		for (String jarFileName : nonMavenJars){
			String resourcePath = otherDependencyCreator.getResourcePath(new String[]{jarFileName});
			Document assetDetails = assetCreatorUtil.getAssetContent(resourcePath);
			
			if (assetDetails != null){
				addLicenseEntryFromAssetContent(assetDetails, jarFileName, jarFileName);
			}else{
				licenseFile.addLicenseEntry(jarFileName, LicenseFile.UNKOWN_ELEMENT, LicenseFile.UNKOWN_ELEMENT, jarFileName);
			}
		}
		licenseFile.generate();
	}
	
	private void addLicenseEntryFromAssetContent(Document assetContent, String key, String jarName) throws MojoExecutionException{
		String licenseType = LicenseFile.createLicenseEntryColumn(assetContent, AssetCreatorUtil.LICENSE_ELEMENT_NAME);
		String packagingType = LicenseFile.createLicenseEntryColumn(assetContent, AssetCreatorUtil.PACKAGINTYPE_ELEMENT_NAME);
		licenseFile.addLicenseEntry(jarName, licenseType, packagingType, key);
	}
}
