package amplexor.utils.templatecreator;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.jcr.vault.fs.api.PathFilterSet;
import com.day.jcr.vault.fs.config.DefaultWorkspaceFilter;
import com.day.jcr.vault.packaging.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype=true)
@Service(Servlet.class)
@Properties({
        @Property(name = "sling.servlet.paths", value = "/bin/templatemanager/backup"),
        @Property(name = "process.label", value = "TemplateManager Backup Process"),
        @Property(name = "service.description", value = "Servlet to create a backup of all templates within the CQ application"),
        @Property(name = "service.vendor", value = "Amplexor"),
        @Property(name = "sling.servlet.methods", value = {"GET"})
})
public class TemplatePackageBackupServlet extends SlingAllMethodsServlet {
	private final static Logger logger = LoggerFactory.getLogger(TemplatePackageBackupServlet.class);
    
    private ResourceResolver resourceResolver;
    private JcrPackageManager packMgr;
    
    @Reference
    SlingRepository repository;
    
    private int maxBackupsToKeep = 5; 
    
    private StringBuilder debugOutput; 
    
    @Property(label="maxbackups", description="Maximum amount of Template backups to store", intValue={5})
    private static final String MAX_BACKUPS_TO_KEEP = "templatemanager.maxbackups";
    
    
    @SuppressWarnings("unused")
	@Activate
	@Modified
	protected void update(ComponentContext componentContext) {
		Dictionary<?, ?> properties = componentContext.getProperties();
		if (properties != null) {
			Object propertyObj = properties.get(MAX_BACKUPS_TO_KEEP);
			if (propertyObj instanceof Integer) {
				maxBackupsToKeep = ((Integer) propertyObj).intValue();
			}
		}
	}
    
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        try {
            resourceResolver = request.getResourceResolver();
            Session session = repository.loginAdministrative(null);
            packMgr = PackagingService.getPackageManager(session);
            debugOutput = new StringBuilder();
            
            boolean success = createTemplateBackupPackage(session);
            
            // write results as json 
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");

            JsonWriter jsonWriter = null;

            try {
                jsonWriter = new JsonWriter(response.getWriter());
                writeTemplateValuesAsJson(success, debugOutput.toString(), jsonWriter);
            } finally {
                if (jsonWriter != null) {
                    jsonWriter.close();
                }
            }

            response.getWriter().flush();
            
        } catch (RepositoryException ex) {
            logger.error("Error while creating backup",ex);
            response.getWriter().println("Error while creating backup!");
        }
    }
    
    
    private List<Node> getTemplateDesignPaths() {
        try {
            List<Node> templatePathList = new ArrayList<Node>();
                    
            Resource appResource = resourceResolver.getResource("/apps/");
            Iterator<Resource> children = appResource.listChildren();
            
            while(children.hasNext()) { 
                Resource childApp = children.next();
                
                // get all apps except for the templatemanager itself
                if(childApp != null && childApp.getResourceType() != null && "nt:folder".equals(childApp.getResourceType().toString()) && !childApp.getName().equals("templatemanager")) { 
                    // check if template folder
                    String appName = childApp.getName();
                    
                    String templateDesignPath = "/etc/designs/" + appName;
                    
                    Resource templatePathResource = resourceResolver.getResource(templateDesignPath);
                    
                    if(templatePathResource != null) {
                        Node templatePathNode = templatePathResource.adaptTo(Node.class);
                        templatePathList.add(templatePathNode);
                    }
                }
            }
            
            return templatePathList;
        }
        catch(Exception e) {
            logger.error("Unable to get the templates design path for different applications from the repository", e);
        }
        return null;
    }
    
    
    private List<Node> getApplicationTemplatePaths() {
        try {
            List<Node> templatePathList = new ArrayList<Node>();
                    
            Resource appResource = resourceResolver.getResource("/apps/");
            Iterator<Resource> children = appResource.listChildren();
            
            while(children.hasNext()) { 
                Resource childApp = children.next();
                
                // get all apps except for the templatemanager itself
                if(childApp != null && childApp.getResourceType() != null && "nt:folder".equals(childApp.getResourceType().toString()) && !childApp.getName().equals("templatemanager")) { 
                    // check if template folder
                    String templatesPath = childApp.getPath() + "/templates"; 
                    Resource templatePathResource = resourceResolver.getResource(templatesPath);
                    
                    if(templatePathResource != null) {
                        Node templatePathNode = templatePathResource.adaptTo(Node.class);
                        templatePathList.add(templatePathNode);
                    }
                }
            }
            
            return templatePathList;
        }
        catch(Exception e) {
            logger.error("Unable to get the templates path for different applications from the repository", e);
        }
        return null;
    }
    
    
    private List<Hit> orderPackagesByDate(String path) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put("path", path);
            map.put("type", "nt:file");
            map.put("orderby", "@jcr:created");  
            map.put("p.offset", "0"); 
            map.put("p.limit", "999"); 

            Session session = resourceResolver.adaptTo(Session.class); 
            QueryBuilder builder = resourceResolver.adaptTo(QueryBuilder.class); 
            Query query = builder.createQuery(PredicateGroup.create(map), session);
            SearchResult result = query.getResult();

            return result.getHits();
        }
        catch(Exception e) {
            logger.error("Unable to list packages from repository for path " + path, e);
            return null;
        }
    }
    
    
    private boolean createTemplateBackupPackage(Session session) throws IOException, RepositoryException {
        try {
            
            String packageGroupName = "template_manager_backups";
            String packageName = "templatesBackup_" + (new Date()).getTime();
            String packPath = packageGroupName + "/" + packageName + ".zip";
            
            debugOutput.append("Creating a new backup package - path : ").append(packPath).append("<br/>");
            JcrPackage pack = packMgr.create(packageGroupName,packageName,null);
            DefaultWorkspaceFilter filters = new DefaultWorkspaceFilter();
            
            // add filters
            List<Node> designs = getTemplateDesignPaths();
            List<Node> templates = getApplicationTemplatePaths();
            templates.addAll(designs);
            for(int i = 0; i < templates.size(); i++) {
                Node template = templates.get(i);
                filters.add(new PathFilterSet(template.getPath()));
            }
            
            JcrPackageDefinition jcrPackageDefinition = pack.getDefinition();
            jcrPackageDefinition.setFilter(filters, true);
            jcrPackageDefinition.set("acHandling","overwrite",false);
            
            packMgr.assemble(pack, null);
            
            removeOldPackages(packPath,packageGroupName);
            
            debugOutput.append("<br/><br/><strong>Succesfully created new backup of templates!</strong>");
            
            return true;
            
        } catch (PackageException ex) {
            debugOutput.append(ex.getMessage());
            debugOutput.append("<br/><br/><strong>Failed to create a new backup of all templates...</strong>");
            return false;
        }
    }
    
    
    private void removeOldPackages(String packPath, String packageGroupName) {
        try {
            Node packageFolder = packMgr.getPackageRoot();
            
            // remove packages that are to much
            String templateBackupPath = packageFolder.getPath() + "/" + packageGroupName;
            List<Hit> packages = orderPackagesByDate(templateBackupPath);

            int amountToDelete = packages.size() - maxBackupsToKeep;
            if(amountToDelete > 0) {
                debugOutput.append("Removing ").append(amountToDelete).append(" backup package(s)<br/>");

                for(int i = 0; i < packages.size() && i < amountToDelete; i++) {
                    Node currentPackage = packages.get(i).getNode();    
                    debugOutput.append("-> Removed old backup package - '").append(currentPackage.getPath()).append("'<br/>");
                    currentPackage.remove();
                    currentPackage.getSession().save();

                }
            }
            
        } catch (RepositoryException ex) {
            debugOutput.append("Failed to delete old backup packages...");
            logger.error("Failed to delete old backup packages...", ex);
        }
    }

    private void writeTemplateValuesAsJson(boolean success, String debugOutput, JsonWriter jsonWriter) {
        
        Map templateMap = new HashMap<String, String>();
        
        if(success)
            templateMap.put("status", "1");
        else
            templateMap.put("status", "0");
        
        templateMap.put("debug", debugOutput);
        
        GsonBuilder gsonBuilder = new GsonBuilder();
		Gson gson = gsonBuilder.create();
        Type jsonType = new TypeToken<Map<String, String>>(){}.getType();
        gson.toJson(templateMap,jsonType,jsonWriter);
    }
    
}
