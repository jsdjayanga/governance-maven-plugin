package governance.plugin.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jayanga on 2/20/14.
 */
public class POMFileCache {
    private static Map<String, File> pomMap = new HashMap<String, File>();

    public static void put(String path, File file){
        pomMap.put(path, file);
    }

    public static File getNearestPOM(File file){
        while (true){

            File pomFile = pomMap.get(file.getParent());
            if (pomFile != null){
                return pomFile;
            }
            file = file.getParentFile();
        }
    }
}
