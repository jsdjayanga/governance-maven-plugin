package governance.plugin.license;

import governance.plugin.rxt.AssetCreatorUtil;

import java.io.File;

import org.apache.maven.plugin.logging.Log;

public class JARFileNameReader extends AbstractLicenseDetailReader{

	private static String OUTPUT_FILE = "jar-names-output.txt";
	//private static String MAVNEN_DEPENDENCY_COMMAND = "mvn dependency:resolve -DexcludeTransitive=true -DoutputAbsoluteArtifactFilename=true -DoutputFile=" 
	//													+ OUTPUT_FILE;
	
	private static String MAVNEN_DEPENDENCY_COMMAND = "mvn dependency:resolve -DoutputAbsoluteArtifactFilename=true -DoutputFile=" 
			+ OUTPUT_FILE;

	private static final int VALID_LINE_COLUMN_COUNT = 6;
	private static final int ARTIFACT_ID_COLUMN_INDEX = 1;
	private static final int VERSION_COLUMN_INDEX = 3;
	private static final int GROUP_ID_COLUMN_INDEX = 0;
	private static final int JAR_FILE_NAME_COLUMN_INDEX = 5;
	
    public JARFileNameReader(Log logger) {
	    super(OUTPUT_FILE, MAVNEN_DEPENDENCY_COMMAND, MAVNEN_DEPENDENCY_COMMAND, logger);
    }
    
	public void decodeAndPopulateInfo(String informationLine) {
		String[] parts = informationLine.split(":");
		if (parts.length == VALID_LINE_COLUMN_COUNT){
			String filePath = parts[JAR_FILE_NAME_COLUMN_INDEX].trim();
			String JARFileName = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1);
			String key = AssetCreatorUtil.getKey(parts[GROUP_ID_COLUMN_INDEX].trim(), parts[ARTIFACT_ID_COLUMN_INDEX].trim(), parts[VERSION_COLUMN_INDEX].trim());
			
			logger.debug("JAR file of " + key + " => " + JARFileName);
			super.information.put(key, JARFileName);
		}
    }
}
