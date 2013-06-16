package amplexor.utils.templatecreator;

import java.io.IOException;
import java.util.logging.Level;
import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
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
    @Property(name = "sling.servlet.paths", value = "/bin/templatemanager/delete"),
    @Property(name = "service.description", value = "Servlet to delete a specific node"),
    @Property(name = "service.vendor", value = "Amplexor"),
    @Property(name = "sling.servlet.methods", value = {"GET"})
})
public class TemplateManagerDeleteServlet extends SlingAllMethodsServlet {

    private final static Logger logger = LoggerFactory.getLogger(TemplateManagerDeleteServlet.class);
    private ResourceResolver resourceResolver;
    private Session session; 

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String path = TemplateUtils.getParameterValue("path", request);
        boolean success = false;
        
        if(path != null) {
            resourceResolver = request.getResourceResolver();
            session = resourceResolver.adaptTo(Session.class);
            success = removeNode(path);
        }
        
        response.getWriter().write("{\"success\":" + success + "}");
    }
    
    
    private boolean removeNode(String path) {
        
        Resource templateResource = resourceResolver.getResource(path);
        if(templateResource != null) {
            Node nodeToDelete = templateResource.adaptTo(Node.class);

            if(nodeToDelete != null) {
                try {
                    nodeToDelete.remove();
                    TemplateUtils.saveNode(nodeToDelete);
                    session.save();
                    return true;
                } catch (VersionException ex) {
                    java.util.logging.Logger.getLogger(TemplateManagerDeleteServlet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (LockException ex) {
                    java.util.logging.Logger.getLogger(TemplateManagerDeleteServlet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ConstraintViolationException ex) {
                    java.util.logging.Logger.getLogger(TemplateManagerDeleteServlet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (AccessDeniedException ex) {
                    java.util.logging.Logger.getLogger(TemplateManagerDeleteServlet.class.getName()).log(Level.SEVERE, null, ex);
                } catch (RepositoryException ex) {
                    java.util.logging.Logger.getLogger(TemplateManagerDeleteServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
                finally {
                    session.logout();
                }
            }
        }
        return false;
    }
}
