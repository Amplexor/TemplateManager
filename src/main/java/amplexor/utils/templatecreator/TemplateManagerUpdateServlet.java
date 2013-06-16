package amplexor.utils.templatecreator;

import java.io.IOException;
import javax.jcr.*;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(Servlet.class)
@Properties({
    @Property(name = "sling.servlet.paths", value = "/bin/templatemanager/update"),
    @Property(name = "service.description", value = "Servlet to update a specific template"),
    @Property(name = "service.vendor", value = "Amplexor"),
    @Property(name = "sling.servlet.methods", value = {"POST"})
})
public class TemplateManagerUpdateServlet extends SlingAllMethodsServlet {

    private final static Logger logger = LoggerFactory.getLogger(TemplateManagerUpdateServlet.class);
    private ResourceResolver resourceResolver;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String key = TemplateUtils.getParameterValue("name", request);
        String value = TemplateUtils.getParameterValue("value", request);
        String templatePath = TemplateUtils.getParameterValue("pk", request);
        
        if(templatePath != null && key != null && value != null) {
            resourceResolver = request.getResourceResolver();
            Resource templateResource = resourceResolver.getResource(templatePath);
            
            if(templateResource != null) {
                key = "jcr:" + key;
                Node templateNode = templateResource.adaptTo(Node.class);
                logger.debug("Updating the template '" + templatePath + "' for key '" + key + "' with value = '" + value + "'");
                TemplateUtils.setNodeProperty(key, value, templateNode);
                TemplateUtils.saveNode(templateNode);
            }
        }
    }
}
