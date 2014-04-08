package governance.plugin.license;

import governance.plugin.common.XmlParser;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class LicenseMetaData{
	private String id;
	private String licenseName;
	private String url;
	private HashSet<Pattern> matchingPatterns = new HashSet<Pattern>();
	
	private static Map<String, LicenseMetaData> licensesMetaData = new HashMap<String, LicenseMetaData>();
	
	public static  void loadLicenseMetaData() throws MojoExecutionException{
		Document licenseInformation = XmlParser.parseXmlFile(new File("LicenseType.xml"));
		NodeList licenseTypes = licenseInformation.getElementsByTagName("licenseType");
		
		for (int i = 0;  i < licenseTypes.getLength(); i++){
			Element licenseType = (Element)licenseTypes.item(i);
			String id = licenseType.getAttribute("id");
			String name = licenseType.getAttribute("name");
			String description = licenseType.getAttribute("url");
			LicenseMetaData licenseMetaData = new LicenseMetaData(id, name, description);
	
			Element mattchingPatternsElement = (Element)licenseType.getElementsByTagName("matchingPatterns").item(0);
			NodeList mattchingPatterns = mattchingPatternsElement.getElementsByTagName("matchingPattern");
			
			for (int j = 0;  j < mattchingPatterns.getLength(); j++){
				Node mattchingPattern = mattchingPatterns.item(j);
				licenseMetaData.addMatchigPattern(mattchingPattern.getTextContent());
			}
			licensesMetaData.put(licenseMetaData.getId(), licenseMetaData);
		}
	}
	
	public static String normlizeLicenseType(String licenseType){
		String licenseName = null;
		for (LicenseMetaData licenseMetadata : licensesMetaData.values()){
			if (licenseMetadata.isMatchingLicenseType(licenseType)){
				licenseName = licenseMetadata.getId();
			}
		}
		return licenseName;
	}
	
	public static LicenseMetaData getLicenseMetaData(String normalizedLicenseName){
		return licensesMetaData.get(normalizedLicenseName);
	}
	
	public LicenseMetaData(String id, String licenseName, String description) {
	    this.id = id;
	    this.licenseName = licenseName;
	    this.url = description;
    }
	
	public String getId() {
		return id;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getName() {
		return licenseName;
	}
	
	public void addMatchigPattern(String matchingPattern){
		Pattern pattern = Pattern.compile(matchingPattern);
		matchingPatterns.add(pattern);
	}
	
	public boolean isMatchingLicenseType(String licenseName){
		for (Pattern pattern: matchingPatterns){
			Matcher matcher = pattern.matcher(licenseName);
			while (matcher.find()){
				return true;
			}
		}
		return false;
	}
}

