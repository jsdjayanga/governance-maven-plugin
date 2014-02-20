package governance.plugin.service;

import governance.plugin.util.PathNameResolver;
import org.eclipse.jdt.core.dom.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jayanga on 2/9/14.
 */
public class ServiceAnnotationVisitor extends ASTVisitor {
    List<Object> serviceInfoList;

    public ServiceAnnotationVisitor(List<Object> serviceInfoList){
        this.serviceInfoList = serviceInfoList;
    }

    @Override
    public boolean visit(MarkerAnnotation node) {
        if (node.getTypeName().toString().equals("WebService")){

            ASTNode n = node;
            while(n != null && n.getNodeType() != ASTNode.TYPE_DECLARATION){
                n = n.getParent();
            }

            String serviceName = (n != null) ? ((TypeDeclaration) n).getName().getIdentifier(): "default";

            while(n != null && n.getNodeType() != ASTNode.COMPILATION_UNIT){
                n = n.getParent();
            }

            String packageName = "defaultpackage";
            if (n != null){
                PackageDeclaration pd = ((CompilationUnit) n).getPackage();
                if (pd != null){
                    packageName = pd.getName().getFullyQualifiedName();
                }
            }
            String namespace = PathNameResolver.PackageToNamespace(packageName.trim());

            //String[] serviceInfo = {serviceName, namespace, "1.0.0", "JAX-WS", ""};
            Map<String, String> serviceInfo = new HashMap<String, String>();
            serviceInfo.put("name", serviceName.trim());
            serviceInfo.put("namespace", namespace.trim());
            serviceInfo.put("version", "1.0.0");
            serviceInfo.put("type", "JAX-WS");

            serviceInfoList.add(serviceInfo);
        }

        return false;
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
        if (node.getTypeName().toString().equals("Path")){


            ASTNode n = node;
            while(n != null && n.getNodeType() != ASTNode.TYPE_DECLARATION){
                n = n.getParent();
            }

            String serviceName = (n != null) ? ((TypeDeclaration) n).getName().getIdentifier(): "default";

            while(n != null && n.getNodeType() != ASTNode.COMPILATION_UNIT){
                n = n.getParent();
            }

            String packageName = "defaultpackage";
            if (n != null){
                PackageDeclaration pd = ((CompilationUnit) n).getPackage();
                if (pd != null){
                    packageName = pd.getName().getFullyQualifiedName();
                }
            }

            String namespace = PathNameResolver.PackageToNamespace(packageName.trim());

            //String[] serviceInfo = {serviceName, "1.0.0", "JAX-RS", ""};
            Map<String, String> serviceInfo = new HashMap<String, String>();
            serviceInfo.put("name", serviceName.trim());
            serviceInfo.put("namespace", namespace.trim());
            serviceInfo.put("version", "1.0.0");
            serviceInfo.put("type", "JAX-RS");

            serviceInfoList.add(serviceInfo);
        }

        return false;
    }
}
