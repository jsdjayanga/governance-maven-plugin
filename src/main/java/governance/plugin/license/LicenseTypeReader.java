package governance.plugin.license;

import governance.plugin.rxt.AssetCreatorUtil;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;



public class LicenseTypeReader extends AbstractLicenseDetailReader{

	private static String OUTPUT_FILE = "target" +  File.separatorChar + "generated-sources" + File.separatorChar + "license" + File.separatorChar + "THIRD-PARTY.txt";
	private static String MAVNEN_LICENSE_COMMAND = "mvn license:add-third-party";
	private static String MAVNEN_AGGREGATE_LICENSE_COMMAND = "mvn license:aggregate-add-third-party";
	
	private static final int GROUP_ID_COLUMN_INDEX = 0;
	private static final int ARTIFACT_ID_COLUMN_INDEX = 1;
	private static final int VERSION_COLUMN_INDEX = 2;
	
	
	public LicenseTypeReader(Log logger) throws MojoExecutionException {
	    super(OUTPUT_FILE, MAVNEN_LICENSE_COMMAND, MAVNEN_AGGREGATE_LICENSE_COMMAND, logger);
	    LicenseMetaData.loadLicenseMetaData();
    }
	
	@Override
    public void decodeAndPopulateInfo(String informationLine){
		// decoding license information in following format,
		// (<license-name>) <project-name> <groupId>:<artifactId>:<version> - <project-url>
		try {
    			if (informationLine.contains("(")){
        		String licenseType = informationLine.substring(informationLine.indexOf("(") + 1, informationLine.indexOf(")"));
        		if (licenseType.contains("Unknown") == false){
        			
            		String [] dependencyInformation = decodeDependencyInformation(informationLine);
            		
            		String normalizedLicenseType = LicenseMetaData.normlizeLicenseType(licenseType);
            		String key = AssetCreatorUtil.getKey(dependencyInformation[GROUP_ID_COLUMN_INDEX], 
            		                                                 dependencyInformation[ARTIFACT_ID_COLUMN_INDEX], dependencyInformation[VERSION_COLUMN_INDEX]);
            		
            		if (normalizedLicenseType == null || normalizedLicenseType.isEmpty()){
            			normalizedLicenseType = "{" + licenseType + "}";
            			logger.warn("Canno't map '" + licenseType + "' to a license type. Please update LicenseType.xml to have '" + licenseType + "' pattern");
            		}else{
            			logger.debug("Normalizing " + licenseType + " to " + normalizedLicenseType);
            		}
            		logger.debug("License of " + key + " => " + normalizedLicenseType);
            		super.information.put(key, normalizedLicenseType);
        		}
    		}
        }catch (Exception ex){
        	logger.warn("Cound not decode lincese information in : " + informationLine);
        }
	}
	
	private String [] decodeDependencyInformation(String information){
		int lastIndex = information.lastIndexOf(")");
		boolean isMatching = true;
		int matchingIndex = -1;
        for (int i = lastIndex - 1; i > 0; i--){
        	if (information.charAt(i) == ')'){
        		isMatching = false;
        	}else if (information.charAt(i) == '('){
        		if (isMatching){
        			matchingIndex = i;
        			break;
        		}else{
        			isMatching = true;
        		}
        	}
        }
        String dependencyDetails = information.substring(matchingIndex + 1, lastIndex);
        return dependencyDetails.split(" ")[0].trim().split(":");
	}
}

