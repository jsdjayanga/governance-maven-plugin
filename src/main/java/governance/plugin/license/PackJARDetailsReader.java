package governance.plugin.license;

import governance.plugin.common.XmlParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public class PackJARDetailsReader {
	
	private static BufferedReader bufferedReader;
	
	public static final String POM_PROPERTIES_KEY_VERSION = "version";
	public static final String POM_PROPERTIES_KEY_GROUPID = "groupId";
	public static final String POM_PROPERTIES_KEY_ARTIFACTID = "artifactId";
	private static HashMap<String, MavenProject> projectsInPack = new HashMap<String, MavenProject>();
	private static HashSet<String> nonMavenjars = new HashSet<String>();

	public static HashMap<String, MavenProject> readPack(File packFile) throws MojoExecutionException {
		projectsInPack.clear();
		try {
	        ZipFile pack = new ZipFile(packFile);
	        final Enumeration<? extends ZipEntry> entries = pack.entries();
     	    
	        while (entries.hasMoreElements()){
	            final ZipEntry packEntry = entries.nextElement();
	            
	            if (packEntry.getName().endsWith(".jar")){
	            	File tempJarFile = zipEntryToFile(pack, packEntry, "tempFile", "jar");
	         	    JarFile jarFile = new JarFile(tempJarFile);
	         	    String jarFileName = packEntry.getName().substring(packEntry.getName().lastIndexOf(File.separatorChar) + 1);
	         	    processJarFile(jarFile, jarFileName, false);
	            }
	        }
	        return projectsInPack;
        }catch (Exception e) {
	       throw new MojoExecutionException(e.getMessage(), e);
        }
	}
	
	private static void processJarFile(JarFile jarFile, String jarFileName, boolean isRecursed) throws MojoExecutionException{
		try {
            Properties pomProperties = null;
     	    MavenProject project = null;
     	    final Enumeration<? extends JarEntry> jarEntries = jarFile.entries();
     	    
     	    while (jarEntries.hasMoreElements()){
     	    	final JarEntry jarEntry = jarEntries.nextElement();

     	    	if (jarEntry.getName().endsWith(".jar")){
     	    		File tempJarFile = zipEntryToFile(jarFile, jarEntry, "tempFile", "jar");
	         	    JarFile innerJarFile = new JarFile(tempJarFile);
	         	    String innerJarFileName = jarEntry.getName().substring(jarEntry.getName().lastIndexOf(File.separatorChar) + 1);
	         	    processJarFile(innerJarFile, innerJarFileName, true);
     	    	}
     	    	else if (jarEntry.getName().contains("pom.xml")){// check for mar files
     	    		File pomFile = zipEntryToFile(jarFile, jarEntry, "pom", "xml");
     	    		Model projectModel = XmlParser.parsePom(pomFile);
     	    		project = new MavenProject(projectModel);
     	    		project.setPackaging((isRecursed) ? "jarinbundle" : projectModel.getPackaging());
     	    		projectsInPack.put(jarFileName, project);
     	    	}else if (jarEntry.getName().contains("pom.properties")){
     	    		File pomPropertiesFile = zipEntryToFile(jarFile, jarEntry, "pom", "properties");
     	    		pomProperties = readPomPropertiesFile(pomPropertiesFile);
     	    	}
     	    }
     	    
     	    if (pomProperties == null && project == null){
     	    	nonMavenjars.add(jarFileName); 
     	    }else if (pomProperties != null && project != null){
        		setProperties(project, pomProperties);
        	}
		}catch (Exception e) {
		       throw new MojoExecutionException(e.getMessage(), e);
	    }
	}
	
	public static File zipEntryToFile(ZipFile zipFile, ZipEntry entry, String filePrefix, String fileSuffix) throws IOException{
		File tempFile = File.createTempFile(filePrefix, fileSuffix);
 	    FileOutputStream tempOut = new FileOutputStream(tempFile); 
 	    IOUtils.copy(zipFile.getInputStream(new ZipEntry(entry.getName())), tempOut);
 	    tempOut.close();
		return tempFile;
	}
	
	private static Properties readPomPropertiesFile(File pomPropertiesFile){
		Properties properties = new Properties();
 	 	try {
	        FileReader reader = new FileReader(pomPropertiesFile);
	        bufferedReader = new BufferedReader(reader);
	        String line = null;
            while((line = bufferedReader.readLine()) != null) {
            	String[]propertyKeyValue = line.split("=");
            	if (propertyKeyValue.length == 2){
            		properties.setProperty(propertyKeyValue[0], propertyKeyValue[1]);
            	}
            }	
        } catch (Exception e) {
	       return null;
        }
 	 	return properties;
	}
	
	private static void setProperties(MavenProject project, Properties properties){
		if (project.getVersion().contains("$") && properties.get(POM_PROPERTIES_KEY_VERSION) != null){
			project.setVersion((String)properties.get(POM_PROPERTIES_KEY_VERSION));
		}
		
		if (project.getGroupId().contains("$") && properties.get(POM_PROPERTIES_KEY_GROUPID) != null){
			project.setGroupId((String)properties.get(POM_PROPERTIES_KEY_GROUPID));
		}
		
		if (project.getArtifactId().contains("$") && properties.get(POM_PROPERTIES_KEY_ARTIFACTID) != null){
			project.setArtifactId((String)properties.get(POM_PROPERTIES_KEY_ARTIFACTID));
		}
	}
	
	public static HashSet<String> getNonMavenJars(){
		return nonMavenjars;
	}
}
