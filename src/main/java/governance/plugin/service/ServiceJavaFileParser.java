package governance.plugin.service;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jayanga on 2/9/14.
 */
public class ServiceJavaFileParser {
    private static ASTParser parser = ASTParser.newParser(AST.JLS3);

    public static List<Object> parse(File file) throws IOException {
        List<Object> serviceInfoList = new LinkedList();

        String stringFile = readFileToString(file);

        parser.setSource(stringFile.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        ServiceAnnotationVisitor av = new ServiceAnnotationVisitor(serviceInfoList);

        cu.accept(av);

        return serviceInfoList;
    }

    private static String readFileToString(File file) throws IOException {
        FileInputStream fis = null;

        fis = new FileInputStream(file);
        byte[] data = new byte[(int)file.length()];

        fis.read(data);
        fis.close();
        String s = new String(data, "UTF-8");

        return s;
    }
}
