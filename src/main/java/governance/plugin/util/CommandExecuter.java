package governance.plugin.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.apache.maven.plugin.MojoExecutionException;

public class CommandExecuter {

	public static StringBuffer executeCommand(String command, File location, boolean isVerbose) throws MojoExecutionException{
		Process commandProcess;
        try {
	        commandProcess = Runtime.getRuntime().exec(command, null,location);
	        
	        StringBuffer buffer = new StringBuffer();
	        String currentInput = null;
	    	BufferedReader input = new BufferedReader(new InputStreamReader(commandProcess.getInputStream()));
	    	while ((currentInput = input.readLine()) != null) {
	    		if (isVerbose){
	    			System.out.println(currentInput);
	    		}
	    		buffer.append(currentInput);
	    	}
	    	
	    	commandProcess.waitFor();
	    	if (commandProcess.exitValue() != 0){
            	throw new MojoExecutionException("Cannot execute '" +  command + "'! @ the location " + location.getAbsolutePath());
            }
            
            commandProcess.destroy();
            return buffer; 
	    	 
        } catch (Exception e) {
	        throw new MojoExecutionException(e.getMessage(), e);
        }	
	}
}
