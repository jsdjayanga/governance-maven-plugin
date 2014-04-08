package governance.plugin.license;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.TreeSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class LicenseFile {
	private PrintWriter licenseFileWriter;
	
	public static final String UNKOWN_ELEMENT = "{?}";
	private static final int NAME_COLUMN_LENGTH = 80;
	private static final int TYPE_COLUMN_LENGTH = 15;
	private static final int LICENSE_COLUMN_LENGTH = 10;
	
	private static final String header = "This product is licensed by WSO2 Inc. under Apache License 2.0.\n"+
        								 "The license can be downloaded from the following locations:\n" +
                                         "\thttp://www.apache.org/licenses/LICENSE-2.0.html\n" + 
                                         "\thttp://www.apache.org/licenses/LICENSE-2.0.txt\n\n" +
                                         "This product also contains software under different licenses. This table below\n" + 
                                         "all the contained libraries (jar files) and the license under which they are\n" + 
                                         "provided to you.\n\n" +
                                         "At the bottom of this file is a table that shows what each license indicated\n" +
                                         "below is and where the actual text of the license can be found.\n\n";	
	
	private final String licenseDetailHeader = "The license types used by the above libraries and their information is given below:\n\n";
	
	private  static String  dependencyLicenseEntriesHeader = null;
	
	Log logger;
	
	private TreeSet<String> usedLicense = new TreeSet<String>(new SortIgnoreCase());
	private TreeSet<String> jarEntires = new TreeSet<String>(new SortIgnoreCase());
	
	public LicenseFile(Log logger) throws MojoExecutionException{
		this.logger = logger;  
		try {
			createLicentEntriesHeader();
			LicenseMetaData.loadLicenseMetaData();
	        licenseFileWriter = new PrintWriter("LICENSE.txt", "UTF-8");
        } catch (Exception e) {
	        throw new MojoExecutionException(e.getMessage(), e);
        } 
	}
	
	public void generate(){
		licenseFileWriter.println(header);
		licenseFileWriter.println(dependencyLicenseEntriesHeader);
		
		for (String jarLicenseEntry : jarEntires){
			licenseFileWriter.println(jarLicenseEntry);
		}
		
		licenseFileWriter.println("\n");
		licenseFileWriter.println(licenseDetailHeader);
		
		for (String license : usedLicense){
			LicenseMetaData licenseDetials = LicenseMetaData.getLicenseMetaData(license);
			
			if (licenseDetials != null){
				licenseFileWriter.println(formatStringElement(license, 15, " ") + licenseDetials.getName());
				licenseFileWriter.println(formatStringElement(" ", 15, " ") + licenseDetials.getUrl());
			}else{
				licenseFileWriter.println(formatStringElement(license, 15, " ") + "License Name Unknown");
				licenseFileWriter.println(formatStringElement(" ", 15, " ") + "License URL Unknown");
				logger.warn("Canno't find " + license + "in LicenseType.xml");
			}
		}
		licenseFileWriter.close();
	}
	
	private static void createLicentEntriesHeader(){
		String columnName = formatStringElement("Name", NAME_COLUMN_LENGTH, " ");
		String columnType = formatStringElement("Type", TYPE_COLUMN_LENGTH, " ");
		String columnLicense = formatStringElement("License", LICENSE_COLUMN_LENGTH, " ");
		String dotedLine = formatStringElement("-", NAME_COLUMN_LENGTH + TYPE_COLUMN_LENGTH + LICENSE_COLUMN_LENGTH, "-");
		dependencyLicenseEntriesHeader = columnName + columnType + columnLicense + "\n" + dotedLine;
	}

	private static String formatStringElement(String element, int expectedLength, String appendingChar){
		int startingElementLenght = element.length();
		
		for (int i = 0; i < (expectedLength - startingElementLenght); i++){
			element = element.concat(appendingChar);
		}
		
		return element;
	}
	
	public void addLicenseEntry(String jarName, String license, String  packagingType, String dependencyKey) throws MojoExecutionException{
		if (jarName.equals(UNKOWN_ELEMENT)){
			jarName = "{" + dependencyKey + "}";
		}
		
		if (!license.equals(UNKOWN_ELEMENT)){
			usedLicense.add(license);
			
			if (license.contains("{")){
				logger.warn("Canno't map '" + license + "' to a license type of " + (jarName.contains(dependencyKey) ? "{Unknown jar name}" : jarName)
				            + "(" + dependencyKey + "). Please update LicenseType.xml with the mapping.");
			}
		}
		
		String licenseEntry = formatStringElement(jarName, NAME_COLUMN_LENGTH, " ") +  formatStringElement(packagingType, TYPE_COLUMN_LENGTH, " ") +
				 				formatStringElement(license, LICENSE_COLUMN_LENGTH, " ");
		
		if (licenseEntry.contains(UNKOWN_ELEMENT)){
			logger.warn("Some information are not filled in artifact at " +  (jarName.contains(dependencyKey) ? "{Unknown jar name}" : jarName) + "(" + dependencyKey + ")");
		}
		
		jarEntires.add(licenseEntry);
	}
	
	public static String createLicenseEntryColumn(Document doc, String elementName){
		String elementValue;
		NodeList nodes = doc.getElementsByTagName(elementName);
		
		if (nodes.getLength() == 0 || nodes.item(0).getTextContent().isEmpty()){
			elementValue = UNKOWN_ELEMENT;
		}else{
			elementValue =  nodes.item(0).getTextContent();
		}
		
		return elementValue;
	}
}

class SortIgnoreCase implements Comparator<Object> {
    public int compare(Object o1, Object o2) {
        String s1 = (String) o1;
        String s2 = (String) o2;
        return s1.toLowerCase().compareTo(s2.toLowerCase());
    }
}
