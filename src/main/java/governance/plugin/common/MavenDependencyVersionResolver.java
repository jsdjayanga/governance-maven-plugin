package governance.plugin.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @deprecated
 * Use governance.plugin.util.EffectivePom instead of this
 */
@Deprecated
public class MavenDependencyVersionResolver {
	private static Map<String, String> resolvedVersions = new HashMap<String, String>();
	private static boolean isDependenciesResolved = false;
	
	private static final String OUTPUT_FILE_NAME ="resolved_dependencies.txt";
	private static final String MAVEN_COMMAND = "mvn dependency:resolve -DexcludeTransitive=true -DoutputFile=" + OUTPUT_FILE_NAME;
	private static final String MAVEN_PROPERTY_PLACE_HOLDER = "$";
	
	private static final int GROUP_ID_COLUMN_INDEX = 0;
	private static final int ARTIFACT_ID_COLUMN_INDEX = 1;
	private static final int VERSION_COLUMN_INDEX = 3;
	private static final int VALID_LINE_COLUMN_COUNT = 5;
	
	
	public static List<Dependency> resolveDependencyVersions(MavenProject project, String pomFileLocation) 
			throws MojoExecutionException{
		List<Dependency> dependencies = project.getDependencies();
		
		for (Dependency dependency: dependencies){
			// If Maven project has at least one version to be resolved, resolve all versions(lazy evaluation)
			if ((dependency.getVersion() == null) || (dependency.getVersion().isEmpty()) 
					|| (dependency.getVersion().contains(MAVEN_PROPERTY_PLACE_HOLDER))){
				
				if (isDependenciesResolved == false){
					resolveVersions(pomFileLocation);
					isDependenciesResolved = true;
				}

				String resolvedVersion = (String)resolvedVersions.get(makeKey(dependency.getGroupId(), dependency.getArtifactId()));
				if (resolvedVersion == null || resolvedVersion.isEmpty()){
					throw new MojoExecutionException("Cannot resolve dependency for " + dependency.getGroupId() + ":" 
												+ dependency.getArtifactId());
				}
				System.out.println("Resolved Version :"+ dependency.getGroupId() + ":" + dependency.getArtifactId() + " => " + resolvedVersion);
				dependency.setVersion(resolvedVersion);
			}
		}
		resetResolvedDependencies();
		return dependencies;
	}
	
	public static void resolveVersions(String pomFileLocation) throws MojoExecutionException{
    	String location = pomFileLocation.substring(0, pomFileLocation.lastIndexOf(File.separatorChar));
    	runDependencyMavenPlugin(location);
    	readVersionsFromFile(location);
	}
	
	public static void resetResolvedDependencies(){
		resolvedVersions.clear();
		isDependenciesResolved = false;
	}
	
	public static void runDependencyMavenPlugin(String location) throws MojoExecutionException{
        try {
        	System.out.println("Running " + MAVEN_COMMAND + " on " + location + ". This might take few minutes....");
        	Process mavenCommandProcess = Runtime.getRuntime().exec(MAVEN_COMMAND, null,new File(location));
        	
        	String currentInput = null;
        	BufferedReader input = new BufferedReader(new InputStreamReader(mavenCommandProcess.getInputStream()));
        	while ((currentInput = input.readLine()) != null) {//Draining output
        	}
        	
            mavenCommandProcess.waitFor();
            if (mavenCommandProcess.exitValue() != 0){
            	System.out.println("------------------------------------------------------------------------------------------------");
            	System.out.println("WRNNING! running command '" + MAVEN_COMMAND + "' on '" + location +"' FAILED!");
            	System.out.println("------------------------------------------------------------------------------------------------");
            }
            
            mavenCommandProcess.destroy();
        } catch (Exception e) {
        	throw new MojoExecutionException(e.getMessage(), e);
        }
	}
	
	private static void readVersionsFromFile(String location) throws MojoExecutionException{
		BufferedReader inputBuffer;
        try {
        	File outputFile = new File(location + File.separatorChar + OUTPUT_FILE_NAME);
	        inputBuffer = new BufferedReader(new FileReader(outputFile));
	        while (inputBuffer.ready()) {
	  		  String inputLine = inputBuffer.readLine();
	  		  populateDependency(inputLine);
	  		}
	  		inputBuffer.close();
	  		outputFile.delete();
        } catch (Exception e) {
	        throw new MojoExecutionException(e.getMessage(), e);
        }
	}
	
	private static String makeKey(String groupId, String artifactId){
		return groupId + "@" + artifactId;
	}
	
	private static void populateDependency(String line){
		String[] parts = line.split(":");
		if (parts.length == VALID_LINE_COLUMN_COUNT){
			String key = makeKey(parts[GROUP_ID_COLUMN_INDEX].trim(), parts[ARTIFACT_ID_COLUMN_INDEX].trim());
			resolvedVersions.put(key, parts[VERSION_COLUMN_INDEX]);
		}
	}
}
  