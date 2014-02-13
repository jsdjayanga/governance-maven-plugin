package governance.plugin;

import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;

public class Configurations{

    private MavenProject project;
    
    private Settings settings;
    
    private String repositoryLocation;
    
    private String gregServiceUrl;
    
    private String gregUserName;
    
    private String gregPassword;
    
	private String gregHome;
    
    
    private static String MAVEN_PROFILE_NAME = "governance-plugin-profile";
    private static String SETTINGS_ELEMENT_GREG_URL = "greg.url";
    private static String SETTINGS_ELEMENT_GREG_USER_NAME = "greg.username";
    private static String SETTINGS_ELEMENT_GREG_PASSWORD = "greg.password";
    private static String SETTINGS_ELEMENT_GREG_HOME = "greg.home";
    private static String SETTINGS_ELEMENT_REPO_LOCATION = "repo.location";
    
    Properties properties = null;
    
    public Configurations(MavenProject project, Settings settings, String repoLocation, String gregServiceUrl, 
                          String gregUserName, String gregPassword, String gregHome) throws MojoExecutionException{
    	this.project = project;
    	this.settings = settings;
    	this.repositoryLocation = repoLocation;
    	this.gregServiceUrl = gregServiceUrl;
    	this.gregUserName = gregUserName;
    	this.gregPassword = gregPassword;
    	this.gregHome = gregHome;
    	
    	initialize();
    }
    
    public void initialize() throws MojoExecutionException{
    	Map<String, Profile> profileMap = settings.getProfilesAsMap();
    	Profile profile = profileMap.get(MAVEN_PROFILE_NAME);
    	
    	if (profile != null)
    		properties = profile.getProperties();
    	
    	gregServiceUrl = readParameter(gregServiceUrl, SETTINGS_ELEMENT_GREG_URL, "gregServiceUrl", "https://localhost:9443/services/");
    	gregUserName = readParameter(gregUserName, SETTINGS_ELEMENT_GREG_USER_NAME, "gregUsername", "admin");
    	gregPassword = readParameter(gregPassword, SETTINGS_ELEMENT_GREG_PASSWORD, "gregPassword", "admin");
    	repositoryLocation = readParameter(repositoryLocation, SETTINGS_ELEMENT_REPO_LOCATION, "location", project.getBasedir().getPath());
    	gregHome = readParameter(gregHome, SETTINGS_ELEMENT_GREG_HOME, "gregHome", null);
    }
    
  
   
    public String readParameter(String parameter, String settingFileElementName, String propertName, String defaultValue) throws MojoExecutionException{
    	if (isNullOrEmpty(parameter)){
    		parameter = getFromProperties(settingFileElementName);
    		if (isNullOrEmpty(parameter)){
    			if (defaultValue == null){
    			throw new MojoExecutionException("Configuration not provided in settings.xml propertties(<"+ settingFileElementName
    			                                 +">) or commandline (-D" + propertName + ")");
    			}else{
    				parameter = defaultValue;
    			}
    		}
    	}
    	System.out.println("Setting value to parameter "+propertName+ "=" + parameter);
    	return parameter;
    }
    
    private boolean isNullOrEmpty(String reference){
    	return (reference == null || reference.isEmpty());
    }
    
    private String getFromProperties(String settingFileElementName){
    	String returnValue = null;
    	if (properties!= null){
    		returnValue = properties.getProperty(settingFileElementName);
    	}
    	return returnValue;
    }
    
    public String getGergServiceUrl(){
    	return gregServiceUrl;
    }
    
    public String getGregUserName(){
    	return gregUserName;
    }
    
    public String getGregPassword(){
    	return gregPassword;
    }
    
    public String getGregHome(){
    	return gregHome;
    }
    
    public String getRepoLocation(){
    	return repositoryLocation;
    }
}
