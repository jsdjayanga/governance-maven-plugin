package governance.plugin.handler;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import governance.plugin.license.LicenseFile;
import governance.plugin.rxt.AssetCreatorUtil;
import governance.plugin.rxt.artifact.ArtifactCreator;
import governance.plugin.rxt.module.ModuleCreator;
import governance.plugin.util.Configurations;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;

/**
 * Reads information that are required  from the GReg repository and generate LICENSE.txt
 * @author sajith
 */
public class LicenseFileGenerateHandler {
	private ModuleCreator moduleCreator; 
	private ArtifactCreator artifactCreator;
	private AssetCreatorUtil assetCreatorUtil;
	private LicenseFile licenseFile;
	
	Log logger;

	public LicenseFileGenerateHandler(Configurations configurations, Log logger) throws MojoExecutionException {
		this.logger = logger;
		moduleCreator = new ModuleCreator(logger, configurations.getGergServiceUrl());
   	 	artifactCreator = new ArtifactCreator(logger, configurations.getGergServiceUrl());
   	 	assetCreatorUtil = new AssetCreatorUtil(configurations.getGergServiceUrl());
   	 	licenseFile = new LicenseFile(logger);
    }
	
	public void process(List<MavenProject> pomTree) throws MojoExecutionException{
		TreeMap<String, MavenProject> sortedProjects = new TreeMap<String, MavenProject>();
		for (MavenProject project: pomTree){
			sortedProjects.put(AssetCreatorUtil.getKey(project.getGroupId(), project.getArtifactId(), project.getVersion()), project);
		}
		
		for (Entry<String, MavenProject> entry: sortedProjects.entrySet()){
			logger.debug("Adding license entries of " + entry.getValue().getFile().getAbsolutePath());
			
			for (Dependency dependecny : entry.getValue().getDependencies()){
				String resourcePath = moduleCreator.getResourcePath(new String[]{dependecny.getArtifactId(), dependecny.getVersion()});
				Document assetContentElement = assetCreatorUtil.getAssetContent(resourcePath);
				
				if (assetContentElement == null){
					resourcePath = artifactCreator.getResourcePath(new String[]{dependecny.getGroupId(), dependecny.getArtifactId(), dependecny.getVersion()});
					assetContentElement = assetCreatorUtil.getAssetContent(resourcePath);
				}
				
				if (assetContentElement == null){
					throw new MojoExecutionException("Cannot find information of dependency " + dependecny.getGroupId() + ":" 
							+ dependecny.getArtifactId() + ":" + dependecny.getVersion() + " in the repostory");
				}else{
					String license = LicenseFile.createLicenseEntryColumn(assetContentElement, AssetCreatorUtil.LICENSE_ELEMENT_NAME);
					String jarName = LicenseFile.createLicenseEntryColumn(assetContentElement, AssetCreatorUtil.JAR_FILE_ELEMENT_NAME).split(",")[0];
					String packagingType = LicenseFile.createLicenseEntryColumn(assetContentElement, AssetCreatorUtil.PACKAGINTYPE_ELEMENT_NAME);
					String key = AssetCreatorUtil.getKey(dependecny);
					licenseFile.addLicenseEntry(jarName, license, packagingType, key);
				}
				
			}
		}
		licenseFile.generate();
	}
}