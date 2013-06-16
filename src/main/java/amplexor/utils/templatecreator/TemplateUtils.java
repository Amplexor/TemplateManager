package amplexor.utils.templatecreator;

import org.apache.sling.api.SlingHttpServletRequest;
import java.util.logging.Level;
import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *TemplateUtils
 * @author ruben.thys
 */
public class TemplateUtils {
    private final static Logger logger = LoggerFactory.getLogger(TemplateUtils.class);
    
    public static String getParameterValue(String key, SlingHttpServletRequest request) {
        String value = null;
        
        if(request.getParameter(key) != null) {
               return (String) request.getParameter(key);
        }
        
        return null;
    }
    
    public static String getCellName(String appPath, ResourceResolver resourceResolver) {
        Resource app = resourceResolver.getResource(appPath);
        if(app != null) {
            try {
                Node appNode = app.adaptTo(Node.class);
                if(appNode.hasProperty("cq:cellName")) {
                    return appNode.getProperty("cq:cellName").getString();
                }
            } catch (RepositoryException ex) {
                logger.error("Unable to get cq:CellName from applicationComponent",ex);
            }
        }
        return null;
    }
    
    
    public static String getDesignPath(String templatePath, ResourceResolver resourceResolver) {
        String cellName = getCellName(templatePath, resourceResolver);
        String path = templatePath.replace("/apps/", "/etc/designs/");
        path = path.replace("/templates", "/jcr:content/");
        
        // remove all the folders that are between /components/ and the actual name of the resourceType
        if(path.contains("/components")) {
            String[] designPathArray = path.split("/components");
            
            if(designPathArray.length > 1) {
                String designPathPostFix = designPathArray[designPathArray.length-1];
                String[] designPathPostFixArray = designPathPostFix.split("/");
                
                if(cellName != null) {
                    path = designPathArray[0] + "/jcr:content/" + cellName;
                }
                else if(designPathPostFixArray.length > 0) {
                    path = designPathArray[0] + "/jcr:content/" + designPathPostFixArray[designPathPostFixArray.length-1];
                }
            }
        }
        
        return path;
    }
    
    public static boolean saveNode(Node node) {
        try {
            node.getSession().save();
            return true;
        } catch (AccessDeniedException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ItemExistsException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ReferentialIntegrityException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConstraintViolationException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidItemStateException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VersionException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LockException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchNodeTypeException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    
    public static boolean setNodeProperty(String key, String value, Node node) {
        try {
            if(node != null) {
                node.setProperty(key, value);
                return true;
            }
        } catch (ValueFormatException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VersionException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LockException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConstraintViolationException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return  false;
        
    }
    
    public static boolean setNodeProperty(String key, long value, Node node) {
        try {
            if(node != null) {
                node.setProperty(key, value);
                return true;
            }
        } catch (ValueFormatException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VersionException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LockException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConstraintViolationException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return  false;
        
    }
    
    public static boolean setNodeProperty(String key, String[] value, Node node) {
        try {
            if(node != null) {
                node.setProperty(key, value);
                return true;
            }
        } catch (ValueFormatException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VersionException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LockException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConstraintViolationException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return  false;
        
    }
}
