package governance.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EffectivePom {

	String pomFileLocation;
	MavenProject effectiveProject;
	Properties properties;
	private static final String EFFECTIVE_POM_FILE ="effective-pom.xml";
	private static final String MAVEN_COMMAND = "mvn help:effective-pom -Doutput=" + EFFECTIVE_POM_FILE;
	private static final String MAVEN_PROPERTY_PLACE_HOLDER = "$";
	private static final String MAVEN_PROJECT_GROUPID_PROPERTY = "${project.groupId}";
	//comment	
	private static Map<String, String> resolvedVersions = new HashMap<String, String>();
	
	public EffectivePom(File pomFile) throws MojoExecutionException{
		this.pomFileLocation = pomFile.getAbsolutePath().substring(0, pomFile.getAbsolutePath().lastIndexOf(File.separatorChar));
		runEffectivePomCommand();
		createEffectiveProject();
		indexDependencies(); 
		this.properties = effectiveProject.getProperties();
    }
	
	private void indexDependencies() {
	   List<Dependency> dependencies = effectiveProject.getDependencies();
	   for (Dependency dependecny: dependencies){
		   resolvedVersions.put(makeKey(dependecny.getGroupId(), dependecny.getArtifactId()), dependecny.getVersion());
	   }
    }

	private String makeKey(String groupId, String artifactId){		
		return groupId + "@" + artifactId;
	}
	
	private void createEffectiveProject() throws MojoExecutionException {
		try{
    	    File effectivePomXml =  new File(pomFileLocation + File.separatorChar + EFFECTIVE_POM_FILE);
    	    
    	    Document pomDoc = XmlParser.parseXmlFile(effectivePomXml);
    	    NodeList projectNodes = pomDoc.getElementsByTagName("project");
    	    Model model = null;
    	    if (projectNodes.getLength() > 1){
    	    	Node projectNode = projectNodes.item(0);//Only interested about the first project node
    	    	DOMSource source = new DOMSource(projectNode);
    	    	StreamResult result = new StreamResult(new StringWriter());
    	    	
    	    	Transformer transformer = TransformerFactory.newInstance().newTransformer();
    	    	transformer.transform(source, result);
    	    	
    	    	String xmlString = result.getWriter().toString();
    	    	model = XmlParser.parsePomFromXmlString(xmlString);
    	    }
    	    else{
    	    	 model = XmlParser.parsePom(effectivePomXml);
    	    }

    	    effectiveProject = new MavenProject(model);
    	    effectivePomXml.delete();
		}catch(Exception e){
			throw new MojoExecutionException(e.getMessage());
		}
    }

	private void runEffectivePomCommand() throws MojoExecutionException {
		Process mavenCommandProcess;
        try {
        	System.out.println("Running " + MAVEN_COMMAND + " on " + pomFileLocation + ". This might take few minutes....");
	        mavenCommandProcess = Runtime.getRuntime().exec(MAVEN_COMMAND, null,new File(pomFileLocation));
	        
	        String currentInput = null;
	    	BufferedReader input = new BufferedReader(new InputStreamReader(mavenCommandProcess.getInputStream()));
	    	while ((currentInput = input.readLine()) != null) {//Draining output
	    	}
	    	
	    	mavenCommandProcess.waitFor();
	    	if (mavenCommandProcess.exitValue() != 0){
            	System.out.println("------------------------------------------------------------------------------------------------");
            	System.out.println("ERROR! running command '" + MAVEN_COMMAND + "' on '" + pomFileLocation +"' FAILED!");
            	System.out.println("------------------------------------------------------------------------------------------------");
            	throw new MojoExecutionException("Cannot execute '" +  MAVEN_COMMAND + "'! Please make sure there are no build faiilures in the maven project @ " + pomFileLocation);
            }
            
            mavenCommandProcess.destroy();
	    	 
        } catch (Exception e) {
        	 e.printStackTrace();
	        throw new MojoExecutionException(e.getMessage());
        }	
    }
	
	private String resolveProperties(String field){
		String returnValue = null;
		if (field.equals(MAVEN_PROJECT_GROUPID_PROPERTY)){
			returnValue = effectiveProject.getGroupId();
		}else{
			String propertyValue = field.substring(field.indexOf('{') + 1, field.indexOf('}'));
			returnValue = properties.getProperty(propertyValue);
		}
		return returnValue;
	}
	
	public MavenProject fillChildProject(MavenProject childProject) throws MojoExecutionException{
		childProject.setVersion(effectiveProject.getVersion());
		List<Dependency> dependencies = childProject.getDependencies();
		for (Dependency dependency: dependencies){
			if (dependency.getVersion() == null || dependency.getVersion().isEmpty() || 
					dependency.getVersion().contains(MAVEN_PROPERTY_PLACE_HOLDER)){
				
				if (dependency.getGroupId().contains(MAVEN_PROPERTY_PLACE_HOLDER)){
					dependency.setGroupId(resolveProperties(dependency.getGroupId()));
				}
				
				if (dependency.getArtifactId().contains(MAVEN_PROPERTY_PLACE_HOLDER)){
					dependency.setArtifactId(resolveProperties(dependency.getArtifactId()));
				}
				
    		    String resolvedVersion = resolvedVersions.get(makeKey(dependency.getGroupId(), dependency.getArtifactId()));
    		    if (resolvedVersion == null){
    			   throw new MojoExecutionException("Could not resolve the version of " + dependency.getGroupId() + ":" + dependency.getArtifactId());
    		    }
    		    dependency.setVersion(resolvedVersion);
			}
		}
		return childProject;
	}
}
