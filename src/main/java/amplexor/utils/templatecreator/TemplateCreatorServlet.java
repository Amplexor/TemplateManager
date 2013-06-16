package amplexor.utils.templatecreator;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.search.QueryBuilder;
import com.day.cq.wcm.api.Page;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.List;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(Servlet.class)
@Properties({
        @Property(name = "sling.servlet.paths", value = "/bin/templatecreator"),
        @Property(name = "service.description", value = "Servlet to create a template based on posted data"),
        @Property(name = "service.vendor", value = "Amplexor"),
        @Property(name = "sling.servlet.methods", value = {"POST"})
})
public class TemplateCreatorServlet extends SlingAllMethodsServlet {
	private final static Logger logger = LoggerFactory.getLogger(TemplateCreatorServlet.class);

    private final int ERROR_ALREADY_EXISTS = 1;
    private final int ERROR_UNABLE_TO_COPY = 2;
    private final int ERROR_INVALID_PARAMS = 3;
    private final int ERROR_THUMBNAIL_FAILED = 4;
    private final int ERROR_APPLICATION_NOT_FOUND = 5;
    private final int ERROR_UNABLE_TO_CREATE_COMPONENT = 6;
    
    private final long DEFAULT_RANKING = 0;
    private final String SPLIT_TOKEN = "$$";
    private final String COMPONENTS_FOLDER_NAME = "components";
    private final String DEFAULT_CUSTOM_COMPONENTS_FOLDER_NAME = "custom_generated";
    
    private List errors;
    
	@Reference
    private QueryBuilder queryBuilder;
    
    private ResourceResolver resourceResolver;
    
    private String customComponentsFolderName = "custom_generated";
    
    @Property(label="customComponentsFolderName", description="Name of the folder where new components (used as sling:resourceType of new templates) are stored", value={"custom_generated"})
    private static final String CUSTOM_COMPONENTS_FOLDER_NAME = "templatedesigner.customComponentsFolderName";
    
    
    @SuppressWarnings("unused")
	@Activate
	@Modified
	protected void update(ComponentContext componentContext) {
		Dictionary<?, ?> properties = componentContext.getProperties();
		if (properties != null) {
			Object propertyObj = properties.get(CUSTOM_COMPONENTS_FOLDER_NAME);
			if (propertyObj instanceof String) {
				customComponentsFolderName = ((String) propertyObj).toString();
			}
		}
	}
    
    
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        
        String newTemplateName = TemplateUtils.getParameterValue("templatename",request);
        String newTemplateDescription = TemplateUtils.getParameterValue("templatedescription",request);
        String applicationTemplatesPath = TemplateUtils.getParameterValue("application",request);
        String path = TemplateUtils.getParameterValue("examplepath",request);
        String allowedPath = TemplateUtils.getParameterValue("allowedpaths",request);
        String inheritResourceType = TemplateUtils.getParameterValue("inheritresourcetype",request);
        
        errors = new ArrayList();
        
        if(!StringUtils.isEmpty(newTemplateName) && !StringUtils.isEmpty(applicationTemplatesPath) && !StringUtils.isEmpty(path)) {

            resourceResolver = request.getResourceResolver();
            
            // template page
            Resource templatePageResource = resourceResolver.getResource(path);
            Page templatePage = templatePageResource.adaptTo(Page.class);
            
            // 'templates' resource
            Resource applicationPathResource = resourceResolver.getResource(applicationTemplatesPath);
            
            if(templatePage != null && applicationPathResource != null) {
                Resource jcrContentResource = templatePage.getContentResource();                
                Resource templateChild = applicationPathResource.getChild(newTemplateName);
                if(templateChild == null) {
                    
                    // create the new template
                    Node applicationTemplatesNode = applicationPathResource.adaptTo(Node.class);
                    Node newTemplateNode = createTemplateNode(applicationTemplatesNode,newTemplateName);
                    
                    // set the properties
                    if(newTemplateNode != null) {
                        createNewTemplate(newTemplateName, newTemplateNode, newTemplateDescription, allowedPath, inheritResourceType, jcrContentResource, request);
                    }
                    else {
                        addErrorCode(ERROR_ALREADY_EXISTS);
                    }
                }
                else {
                    addErrorCode(ERROR_ALREADY_EXISTS);
                }
            }
            else {
                if(templatePage == null)
                    addErrorCode(ERROR_UNABLE_TO_COPY);
                if(applicationPathResource == null)
                    addErrorCode(ERROR_APPLICATION_NOT_FOUND);
            }
            
        }
        else {
            addErrorCode(ERROR_INVALID_PARAMS);
        }
        
        redirectToPage(request,response, newTemplateName, newTemplateDescription, applicationTemplatesPath, path, allowedPath, inheritResourceType);
    }

    
    private void createNewTemplate(String newTemplateName, Node newTemplateNode, String newTemplateDescription, String allowedPath, String inheritResourceType, Resource jcrContentResource, SlingHttpServletRequest request) {
        TemplateUtils.setNodeProperty("jcr:title", newTemplateName, newTemplateNode);
        TemplateUtils.setNodeProperty("jcr:description", newTemplateDescription, newTemplateNode);
        TemplateUtils.setNodeProperty("ranking", DEFAULT_RANKING, newTemplateNode);
    
        if(allowedPath != null) {
            String[] allowedPaths = getAllowedPathsArray(allowedPath);
            TemplateUtils.setNodeProperty("allowedPaths", allowedPaths, newTemplateNode);
        }

        // copy the jcr:contents of the original template page
        copyNode(jcrContentResource, newTemplateNode, newTemplateName, newTemplateDescription, inheritResourceType);

        // create the thumbnail
        setThumbnailImageForNode(newTemplateNode, request);

        TemplateUtils.saveNode(newTemplateNode);
    }
    
    
    private void addErrorCode(int errorCode) {
        if(errorCode != -1)
            errors.add(errorCode);
    }

    private Node getFolderOrCreateNew(Node parentNode, String folderName) throws RepositoryException {
        Node customTemplateComponentsNode = null;
        if(!parentNode.hasNode(folderName)) {                
            customTemplateComponentsNode = createFolder(parentNode, folderName);
        }
        else {
            customTemplateComponentsNode = parentNode.getNode(folderName);
        }
        return customTemplateComponentsNode;
    }
    
    
    private void redirectToPage(SlingHttpServletRequest request, SlingHttpServletResponse response, String name, String description, String appPath, String path, String allowedPath, String newResourceType) {
        
        if(request.getHeader("referer") != null) {
            try {
                String redirectUrl = request.getHeader("referer").toString();
                String queryString = "";
                
                // remove queryString if available
                if(redirectUrl.contains("?")) {
                    redirectUrl = redirectUrl.substring(0, redirectUrl.indexOf("?"));
                }
                

                String errorCodes = getErrorCodes();
                if(errorCodes != null) {
                    queryString = addToQuerystring(queryString, "wcmmode","disabled",true);
                    queryString = addToQuerystring(queryString, "name",name,false);
                    queryString = addToQuerystring(queryString, "description",description,false);
                    queryString = addToQuerystring(queryString, "appPath",appPath,false);
                    queryString = addToQuerystring(queryString, "path",path,false);
                    queryString = addToQuerystring(queryString, "inheritresourcetype",newResourceType,false);
                    queryString = addToQuerystring(queryString, "allowedPath",allowedPath.replaceAll("\\?", "%3F"),false);
                    queryString = addToQuerystring(queryString, "errorCodes",errorCodes,false);
                    
                    if(request.getParameter("templateimage") != null && request.getParameter("templateimage").length() > 0) {
                        queryString = addToQuerystring(queryString, "thumbnail","1",false);
                    }
                    
                    redirectUrl += queryString;
                }
                else {
                    redirectUrl += "?wcmmode=disabled&success=1";
                }

                response.sendRedirect(redirectUrl);
            } catch (IOException ex) {
                logger.error("Error redirecting...");
            }
        }
    }
    
    private String addToQuerystring(String queryString, String key, String value, boolean first) {
        if(first)
            queryString = "?";
        else 
            queryString += "&";
        
        queryString += key + "=" + value;
        return queryString;
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
    
    private boolean copyNode(Resource jcrContentResource, Node newTemplateNode, String templateName, String templateDescription, String inheritResourceType) {
        Node jcrContentNode = jcrContentResource.adaptTo(Node.class);
        if(jcrContentNode != null) {
            try {
                String resourceType = getResouceTypeFromNode(jcrContentNode, "");
                if(resourceType == null || "".equals(resourceType) || (inheritResourceType == null || !"yes".equals(inheritResourceType))) {
                    resourceType = createNewPageComponent(jcrContentResource, newTemplateNode, templateName, templateDescription);
                }
                
                if(resourceType == null || "".equals(resourceType)) {
                    addErrorCode(ERROR_UNABLE_TO_CREATE_COMPONENT);
                    return false;
                }
                
                Node copiedNode = JcrUtil.copy(jcrContentNode, newTemplateNode, "jcr:content");
                copiedNode.setProperty("cq:template", newTemplateNode.getPath());
                copiedNode.setProperty("sling:resourceType", resourceType);
                
                return true;
            } catch (RepositoryException ex) {
                logger.error("Unable to copy the content of the page to the newly created template");
            }
            
            addErrorCode(ERROR_UNABLE_TO_COPY);
        }
        return false;
    }
    
    
    /***
     * Based on the template, create a new component (with a reference to the original resourceType using sling:resourceSuperType)
     * @return resourceType of the new component
     */
    private String createNewPageComponent(Resource originalContentResource, Node newTemplateNode, String templateName, String templateDescription) {
        try {
            Node originalContentNode = originalContentResource.adaptTo(Node.class);
            if(originalContentNode != null) { 
                Node applicationNode = newTemplateNode.getParent().getParent();
                
                // create /components folder (if not exists)
                Node componentsNode = getFolderOrCreateNew(applicationNode, COMPONENTS_FOLDER_NAME);
                
                // create a 'custom' folder (which will contain all components that are generated by the TemplateManager)
                String customFolderName = customComponentsFolderName;
                if(customFolderName == null || "".equals(customFolderName))
                    customFolderName = DEFAULT_CUSTOM_COMPONENTS_FOLDER_NAME;
                
                Node customTemplateComponentsNode = getFolderOrCreateNew(componentsNode, customFolderName);

                if(customTemplateComponentsNode != null) {
                    // create the new component and return it as a resourceType
                    String fallbackResourceType = transformToResourceType(componentsNode.getPath() + "/" + customFolderName + "/" + newTemplateNode.getName());
                    String resourceType = getResouceTypeFromNode(originalContentNode, fallbackResourceType);

                    Node newComponent = createResourceSuperTypeComponent(customTemplateComponentsNode, newTemplateNode.getName(), templateName, templateDescription, resourceType);
                    return transformToResourceType(newComponent.getPath());
                }
            }
            
        } catch (RepositoryException ex) {
            logger.error("Problem creating page component..." ,ex);
        }
        
        return null;
    }
    
    
    private String getClassNameProperty(String resourceTypeComponent) {
        Resource component = resourceResolver.getResource(resourceTypeComponent);
        if(component != null) {
            try {
                Node componentNode = component.adaptTo(Node.class);
                
                if(componentNode != null && componentNode.hasProperty("className")) {
                    return componentNode.getProperty("className").getString();
                }
            } catch (RepositoryException ex) {
                logger.error("Unable to get the className property from the component {}", resourceTypeComponent, ex);
            }
        }
        return null;
    }
    
    private String transformToResourceType(String path) {
        if(path != null) {
            return path.replace("/apps/","");
        }
        
        return path;
    }
    
    private String getResouceTypeFromNode(Node originalContentNode, String fallBackResourceType) {
        try {
            if(originalContentNode.hasProperty("sling:resourceType")) {
                return originalContentNode.getProperty("sling:resourceType").getString();
            }
            
        } catch (RepositoryException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return fallBackResourceType;
    }
    
    
    private Node createResourceSuperTypeComponent(Node parentNode, String name, String title, String description, String resourceSuperType) {
        try {
            // if the node already exists, return this one
            if(parentNode.hasNode(name)) {
                return parentNode.getNode(name);
            }
            
            // create components folder if not exists
            Node createdNode = parentNode.addNode(name,"cq:Component");
            createdNode.setProperty("jcr:title", title);
            createdNode.setProperty("jcr:description", description);
            createdNode.setProperty("sling:resourceSuperType", resourceSuperType);
            createdNode.setProperty("componentGroup", ".hidden");
            
            String className = getClassNameProperty(resourceSuperType);
            if(className != null && !"".equals(className))
                createdNode.setProperty("className", className);
            
            TemplateUtils.saveNode(parentNode);
            return createdNode;
        } catch (ItemExistsException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (PathNotFoundException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchNodeTypeException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LockException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VersionException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConstraintViolationException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    private Node createFolder(Node parentNode, String name) {
        try {
            // create components folder if not exists
            Node createdNode = parentNode.addNode(name,"nt:folder");
            TemplateUtils.saveNode(parentNode);
            return createdNode;
        } catch (ItemExistsException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (PathNotFoundException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchNodeTypeException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LockException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VersionException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConstraintViolationException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    private boolean setThumbnailImageForNode(Node node, SlingHttpServletRequest request) {
        // get the thumbnail image from the request
        if(request.getRequestParameter("templateimage") != null && request.getParameter("templateimage").length() > 0) {
            
            try {
                Node thumbnailNode = node.addNode("thumbnail.png", "nt:file");
                Node contentNode = thumbnailNode.addNode("jcr:content", "nt:resource");
                
                String contentType = request.getRequestParameter("templateimage").getContentType();
                InputStream imageStream = request.getRequestParameter("templateimage").getInputStream();
                Image image = (Image) ImageIO.read(imageStream); 
                
                BufferedImage bufferedImage = createResizedCopy(image,128,100);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage,"png", os); 
                InputStream fis = new ByteArrayInputStream(os.toByteArray());
                
                contentNode.setProperty("jcr:data", fis);                
                contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
                contentNode.setProperty("jcr:mimeType", "image/png");
                
                return true;
                
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ItemExistsException ex) {
                java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (PathNotFoundException ex) {
                java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchNodeTypeException ex) {
                java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (LockException ex) {
                java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (VersionException ex) {
                java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ConstraintViolationException ex) {
                java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RepositoryException ex) {
                java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
            } 
            
            addErrorCode(ERROR_THUMBNAIL_FAILED);
        }

        return false;
    }
    
    private BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight) {
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledBI.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
    }
    
    
    private String[] getAllowedPathsArray(String allowedPathParam) {
        if(allowedPathParam != null && allowedPathParam.contains(SPLIT_TOKEN)) {
            String[] allowedPaths = allowedPathParam.split(SPLIT_TOKEN);
            return allowedPaths;
        }
        return null;
    }
    
    private Node createTemplateNode(Node applicationTemplatesNode, String newTemplateName) {
        try {
            return applicationTemplatesNode.addNode(JcrUtil.createValidName(newTemplateName), "cq:Template");
            
        } catch (ItemExistsException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (PathNotFoundException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchNodeTypeException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LockException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (VersionException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConstraintViolationException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RepositoryException ex) {
            java.util.logging.Logger.getLogger(TemplateCreatorServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    
    
}
