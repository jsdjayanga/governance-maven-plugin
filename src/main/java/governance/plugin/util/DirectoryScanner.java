package governance.plugin.util;

import java.io.File;

/**
 * Created by jayanga on 2/20/14.
 */
public class DirectoryScanner {

    public static File findFile(File directory, String name){

        if (directory != null && directory.isDirectory()){
            File[] files = directory.listFiles();
            if (files != null){
                File file = null;
                for (int index = 0; index < files.length; index++){
                    file = files[index];
                    if (file != null && file.isFile()){
                        if (file.getName().equals(name)){
                            return file;
                        }
                    }
                }
            }
        }

        return null;
    }
}
