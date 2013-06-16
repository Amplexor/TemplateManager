package amplexor.utils.templatecreator;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.cq.search.QueryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

@Component
@Service(Servlet.class)
@Properties({
        @Property(name = "sling.servlet.paths", value = "/bin/templatemanager/replace"),
        @Property(name = "service.description", value = "Servlet to create replace allowed component configuration with new configuration"),
        @Property(name = "service.vendor", value = "Amplexor"),
        @Property(name = "sling.servlet.methods", value = {"POST"})
})
public class TemplateDesignReplaceServlet extends SlingAllMethodsServlet {
	private final static Logger logger = LoggerFactory.getLogger(TemplateDesignReplaceServlet.class);

    private final int ERROR_INVALID_PARAMS = 1;
    private final String SPLIT_TOKEN_CAT = "#";
    private final String SPLIT_TOKEN_COMP = ":";
    private final String SPLIT_TOKEN_PATH = ",";
    private final String SPLIT_TOKEN_VAL = "@";
    
    private List errors;
    
	@Reference
    private QueryBuilder queryBuilder;
    
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        
        String templatePath = TemplateUtils.getParameterValue("template",request);
        String parsysName = TemplateUtils.getParameterValue("parsys",request);
        String configuredComponents = TemplateUtils.getParameterValue("components",request);
        
        errors = new ArrayList();
        
        if(!StringUtils.isEmpty(templatePath) && !StringUtils.isEmpty(parsysName)) {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Session session = resourceResolver.adaptTo(Session.class);
            
            Resource template = resourceResolver.getResource(templatePath); 
            if(template != null) {
                try {
                    Node templateNode = template.adaptTo(Node.class);
                    String[] allowedComponents = new String[0];
                    if(configuredComponents != null)
                        allowedComponents = parseComponentsToStringArray(configuredComponents);
                    
                    templateNode.setProperty("components", allowedComponents);
                    TemplateUtils.saveNode(templateNode);
                    session.save();
                }
                catch (RepositoryException ex) {
                    logger.error("Problem getting property components from node", ex);
                }
                finally {
                    session.logout();
                }
            }
            
        }
        else {
            addErrorCode(ERROR_INVALID_PARAMS);
        }
        
        String errorCodes = getErrorCodes();
        if(errorCodes != null)
            response.getWriter().write(errorCodes);
        else
            response.getWriter().write("0");
    }
    
    
    private void addErrorCode(int errorCode) {
        if(errorCode != -1)
            errors.add(errorCode);
    }
    
    
    private String getErrorCodes() {
        
        if(errors != null && errors.size() > 0) {
            String errorCodes = "";
            for(int i = 0; i < errors.size(); i++) {
                int currentCode = (Integer)errors.get(i);
                
                errorCodes += currentCode;
                
                if(i+1 < errors.size()) {
                    errorCodes += "|";
                }
            }
            
            return errorCodes;
            
        }
        else {
            return null;
        }
    }
    
    
    private String[] parseComponentsToStringArray(String configuredComponents) {
        List allowedComponents = new ArrayList<String>();
        
        String[] componentCatsArray = configuredComponents.split(SPLIT_TOKEN_CAT);
        for(int i = 0; i < componentCatsArray.length; i++) {
            String currentCat = componentCatsArray[i];
            
            if(StringUtils.isNotEmpty(currentCat)) {
               if(currentCat.startsWith("group:")) {
                   allowedComponents.add(currentCat.substring(0, currentCat.lastIndexOf(":")));
               }
               else {
                String[] catComponentsArray = currentCat.split(SPLIT_TOKEN_COMP);

                if(catComponentsArray.length > 1) {
                    String currentComponent = catComponentsArray[1];

                    if(StringUtils.isNotEmpty(currentComponent)) {
                            String[] currentComponentArray = currentComponent.split(SPLIT_TOKEN_PATH);

                            for(int j = 0; j < currentComponentArray.length; j++) {
                                String currentComponentPath = currentComponentArray[j];

                                if(StringUtils.isNotEmpty(currentComponentPath) && currentComponentPath.contains(SPLIT_TOKEN_VAL)) {
                                    String[] currentCompPath = currentComponentPath.split(SPLIT_TOKEN_VAL);
                                    if(currentCompPath.length > 1) {
                                        allowedComponents.add(currentCompPath[1]);
                                    }
                                }
                            }

                    }

                }
               }
            }
            
        }
        
        String[] strResult=new String[allowedComponents.size()];  
        return (String[]) allowedComponents.toArray(strResult);  

    }
    
    
    
}
