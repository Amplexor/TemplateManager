package amplexor.utils.templatecreator;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
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
    @Property(name = "sling.servlet.paths", value = "/bin/templatemanager/save"),
    @Property(name = "service.description", value = "Servlet to save the list of templates"),
    @Property(name = "service.vendor", value = "Amplexor"),
    @Property(name = "sling.servlet.methods", value = {"POST"})
})
public class TemplateManagerSaveServlet extends SlingAllMethodsServlet {

    private final static Logger logger = LoggerFactory.getLogger(TemplateManagerSaveServlet.class);
    private ResourceResolver resourceResolver;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        resourceResolver = request.getResourceResolver();

        if (request.getParameter("templateList") != null) {
            try {
                String templateList = request.getParameter("templateList");

                if (templateList != null && !"".equals(templateList)) {
                    String[] templates = templateList.split("%%%");

                    for (int i = 0; i < templates.length; i++) {
                        String templateStr = templates[i];
                        String[] templateArray = templateStr.split("###");

                        String templatePath = templateArray[0];
                        String templateRank = templateArray[1];
                        if (templatePath != null) {
                            Resource templateResource = resourceResolver.getResource(templatePath);
                            if (templateResource != null) {
                                Node templateNode = templateResource.adaptTo(Node.class);
                                if (templateNode != null) {
                                    try {
                                        templateNode.setProperty("ranking", Long.parseLong(templateRank));
                                    } catch (ValueFormatException ex) {
                                        java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (VersionException ex) {
                                        java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (LockException ex) {
                                        java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (ConstraintViolationException ex) {
                                        java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (RepositoryException ex) {
                                        java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }
                        }
                    }
                }

                Session session = resourceResolver.adaptTo(Session.class);
                session.save();
            } catch (AccessDeniedException ex) {
                java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ItemExistsException ex) {
                java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ReferentialIntegrityException ex) {
                java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ConstraintViolationException ex) {
                java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidItemStateException ex) {
                java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (VersionException ex) {
                java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (LockException ex) {
                java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchNodeTypeException ex) {
                java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(TemplateManagerSaveServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
