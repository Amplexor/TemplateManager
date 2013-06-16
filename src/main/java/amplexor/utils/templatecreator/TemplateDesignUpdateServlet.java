package amplexor.utils.templatecreator;

import java.io.IOException;
import java.util.logging.Level;
import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.lang.StringUtils;
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
    @Property(name = "sling.servlet.paths", value = "/bin/templatemanager/updatedesign"),
    @Property(name = "service.description", value = "Servlet to update the configurable parsys items for a specfic template"),
    @Property(name = "service.vendor", value = "Amplexor"),
    @Property(name = "sling.servlet.methods", value = {"GET"})
})
public class TemplateDesignUpdateServlet extends SlingAllMethodsServlet {

    private final static Logger logger = LoggerFactory.getLogger(TemplateDesignUpdateServlet.class);
    private ResourceResolver resourceResolver;
    private Session session;
    private final String SPLIT_TOKEN = "%%%";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        String template = TemplateUtils.getParameterValue("template", request);
        String config = TemplateUtils.getParameterValue("config", request);

        boolean success = false;

        if (template != null && config != null) {
            resourceResolver = request.getResourceResolver();
            session = resourceResolver.adaptTo(Session.class);

            String[] configArray = parseConfig(config);

            if (configArray != null) {
                success = createDesignNodes(template, configArray);
            }

        }

        response.getWriter().write("{\"success\":" + success + "}");
    }


    private String[] parseConfig(String config) {

        if (config != null && !StringUtils.isEmpty(config)) {
            return config.split(SPLIT_TOKEN);
        }

        return null;
    }

    private Node createNodesIfNotExist(Node parentNode, String path) {

        if (path != null) {
            String[] nodeNames = path.split("/");
            Node parent = parentNode;
            for (int i = 0; i < nodeNames.length; i++) {
                String currentName = nodeNames[i];
                try {
                    if (parent != null && !parent.hasNode(currentName)) {
                        parent = parent.addNode(currentName);
                    } else {
                        parent = parent.getNode(currentName);
                    }
                } catch (RepositoryException ex) {
                    logger.error("Unable to get node with name {}", currentName, ex);
                }
            }

            return parent;
        }
        return null;
    }

    private boolean createDesignNodes(String template, String[] configArray) {
        String designPath = TemplateUtils.getDesignPath(template, resourceResolver);

        if (designPath != null) {
            String[] templateArray = designPath.split("/jcr:content/");

            if (template != null) {
                Resource templateResource = resourceResolver.getResource(templateArray[0]);

                if (templateResource != null) {
                    Node templateNode = templateResource.adaptTo(Node.class);

                    if (templateNode != null) {
                        try {

                            boolean newNode = false;

                            for (int i = 0; i < configArray.length; i++) {
                                String configPath = configArray[i];
                                
                                String path = "jcr:content/" + templateArray[1] + "/" + configPath;
                                
                                Node configNode = createNodesIfNotExist(templateNode, path);

                                if(!configNode.hasProperty("sling:resourceType"))
                                    configNode.setProperty("sling:resourceType", "foundation/components/parsys");
                                
                                if(!configNode.hasProperty("components"))
                                    configNode.setProperty("components", new String[0]);

                                newNode = true;
                            }

                            if (newNode) {
                                TemplateUtils.saveNode(templateNode);
                                session.save();
                            }
                            return true;
                        } catch (VersionException ex) {
                            java.util.logging.Logger.getLogger(TemplateDesignUpdateServlet.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (LockException ex) {
                            java.util.logging.Logger.getLogger(TemplateDesignUpdateServlet.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ConstraintViolationException ex) {
                            java.util.logging.Logger.getLogger(TemplateDesignUpdateServlet.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (AccessDeniedException ex) {
                            java.util.logging.Logger.getLogger(TemplateDesignUpdateServlet.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (RepositoryException ex) {
                            java.util.logging.Logger.getLogger(TemplateDesignUpdateServlet.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            session.logout();
                        }
                    }
                }
            }
        }
        return false;
    }
}
