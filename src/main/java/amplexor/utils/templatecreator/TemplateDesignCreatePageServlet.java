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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.cq.search.QueryBuilder;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.logging.Level;
import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

@Component
@Service(Servlet.class)
@Properties({
    @Property(name = "sling.servlet.paths", value = "/bin/templatemanager/createpage"),
    @Property(name = "service.description", value = "Creates a temporary page based on a specific template"),
    @Property(name = "service.vendor", value = "Amplexor"),
    @Property(name = "sling.servlet.methods", value = {"GET"})
})
public class TemplateDesignCreatePageServlet extends SlingAllMethodsServlet {

    private final static Logger logger = LoggerFactory.getLogger(TemplateDesignCreatePageServlet.class);
    private List errors;
    private ResourceResolver resourceResolver;
    private PageManager pageManager;
    private Session session;
    
    private final String TEMP_PAGE_NAME = "temp_page";
    private final String TEMP_PAGE_PARENTPATH = "/etc/designs/template-manager";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        
        resourceResolver = request.getResourceResolver();
        String templatePath = TemplateUtils.getParameterValue("template", request);
        
        String newPath = createPageBasedOnTemplate(templatePath);
        response.getWriter().println("{\"path\":\"" + newPath + "\"}");
    }
    
    
    private String createPageBasedOnTemplate(String templatePath) {
        try {
            
            pageManager = resourceResolver.adaptTo(PageManager.class);
            Page newPage = pageManager.create(TEMP_PAGE_PARENTPATH, TEMP_PAGE_NAME, templatePath, TEMP_PAGE_NAME);
            session = resourceResolver.adaptTo(Session.class);
            session.save();

            return newPage.getPath();

        } catch (AccessDeniedException ex) {
            logger.error("Problem saving session", ex);
        } catch (ItemExistsException ex) {
            logger.error("Problem saving session", ex);
        } catch (ReferentialIntegrityException ex) {
            logger.error("Problem saving session", ex);
        } catch (ConstraintViolationException ex) {
            logger.error("Problem saving session", ex);
        } catch (InvalidItemStateException ex) {
            logger.error("Problem saving session", ex);
        } catch (VersionException ex) {
            logger.error("Problem saving session", ex);
        } catch (LockException ex) {
            logger.error("Problem saving session", ex);
        } catch (NoSuchNodeTypeException ex) {
            logger.error("Problem saving session", ex);
        } catch (RepositoryException ex) {
            logger.error("Problem saving session", ex);
        } catch (WCMException ex) {
            logger.error("Problem  creating page with template ", ex);
        } finally {
            session.logout();
        }
        
        return null;
    }
}
