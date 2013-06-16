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
import java.util.*;
import java.util.logging.Level;
import javax.jcr.*;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
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
        @Property(name = "sling.servlet.paths", value = "/bin/templatedesigner/list"),
        @Property(name = "service.description", value = "Servlet to list all allowed parsys and allowed components for a template"),
        @Property(name = "service.vendor", value = "Amplexor"),
        @Property(name = "sling.servlet.methods", value = {"GET"})
})
public class TemplateParSysOverviewServlet extends SlingAllMethodsServlet {
	private final static Logger logger = LoggerFactory.getLogger(TemplateParSysOverviewServlet.class);

	@Reference
    private QueryBuilder queryBuilder;
    
    private ResourceResolver resourceResolver;
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        resourceResolver = request.getResourceResolver();
        String path = null;
        if(request.getParameter("path") != null) {
            path = request.getParameter("path").toString();
        }
        
        List<Hit> parsysForTemplate = queryForParsys(path);
        
        // write results as json 
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        
        if (parsysForTemplate == null) {
            response.getWriter().println("[{}]");
        } else {
            JsonWriter jsonWriter = null;

            try {
                
                String templateName = getTemplateName(path);
                String resourceType = getResourceType(path);
                jsonWriter = new JsonWriter(response.getWriter());
                writeParsysAndAllowedComponents(templateName,resourceType, parsysForTemplate, jsonWriter);
            } finally {
                if (jsonWriter != null) {
                    jsonWriter.close();
                }
            }

        }
        response.getWriter().flush();
        
    }
    
    
    
    private String getResourceType(String templatePath) {
        if(templatePath != null)
            return templatePath.replace("/apps/","");
        return null;
    }
    
    
    
    private String getTemplateName(String templatePath) {
        
        Resource template = resourceResolver.getResource(templatePath);
        try {
            if (template != null) {
                Node templateNode = template.adaptTo(Node.class);
                if (templateNode != null && templateNode.hasProperty("jcr:title")) {
                    return templateNode.getProperty("jcr:title").getString();
                }
            }
        } catch (PathNotFoundException ex) {
            logger.error("Path of template not found", ex);
        } catch (RepositoryException ex) {
            logger.error("Problem getting the template from repository", ex);
        }
        
        return template.getName();
    }
    
    private List<Hit> queryForParsys(String templatePath) {
        try {
            String path = TemplateUtils.getDesignPath(templatePath,resourceResolver);
            
            Map<String, String> map = new HashMap<String, String>();
            map.put("path", path);
            map.put("type", "nt:unstructured");
            map.put("property", "sling:resourceType");
            map.put("property.1_value", "foundation/components/parsys");
            map.put("property.2_value", "foundation/components/iparsys"); 
            map.put("p.offset", "0"); 
            map.put("p.limit", "999");
            map.put("orderby", "nodename");

            Session session = resourceResolver.adaptTo(Session.class); 
            QueryBuilder builder = resourceResolver.adaptTo(QueryBuilder.class); 
            Query query = builder.createQuery(PredicateGroup.create(map), session);
            SearchResult result = query.getResult();
            return result.getHits();
        }
        catch(Exception e) {
            logger.error("Unable to get parsys overview for template '" + templatePath + "' repository", e);
            return null;
        }
    }

    private void writeParsysAndAllowedComponents(String templateName, String resourceType, List<Hit> parsysForTemplate, JsonWriter jsonWriter) {
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        
        for (Hit hit : parsysForTemplate) {
            Map templateMap = new HashMap<String, Object>();
            Map componentMap = new TreeMap<String, Object>();
            
            try {
                Node parsysNode = hit.getNode();
                
                if(parsysNode != null) {
                    templateMap.put("templateName", templateName);
                    templateMap.put("path", parsysNode.getPath());
                    templateMap.put("resourceType", resourceType);
                    
                    if(parsysNode.hasProperty("components")) {
                        Value[] components = null;
                                                
                        if(parsysNode.getProperty("components").isMultiple())
                            components = parsysNode.getProperty("components").getValues();
                        else {
                            components = new Value[1];
                            components[0] = parsysNode.getProperty("components").getValue();
                        }
                            
                        
                        if(components != null) {
                            for(int i = 0; i < components.length; i++) {
                                String componentPath = components[i].getString();
                                
                                // if config is a group
                                if(componentPath.startsWith("group:")) {
                                    componentMap.put(componentPath, new ArrayList<String[]>());
                                }
                                else {
                                
                                    Resource componentResource = resourceResolver.getResource(componentPath);
                                    if(componentResource != null) {
                                        Node componentNode = componentResource.adaptTo(Node.class);

                                        if(componentNode != null) { 
                                            String key = "";
                                            if(componentNode.hasProperty("componentGroup")) {
                                                String componentGroup = componentNode.getProperty("componentGroup").getString();
                                                key = componentGroup;
                                            }

                                            List componentList = null;

                                            // not contains key
                                            if(!componentMap.containsKey(key)) { 
                                                componentList = new ArrayList<String[]>();
                                                componentMap.put(key, componentList);
                                            }
                                            else {
                                                componentList = (List) componentMap.get(key);
                                            }

                                            // allowed component
                                            String[] item = new String[3];
                                            if(componentNode.hasProperty("jcr:title")) {
                                                String componentTitle = componentNode.getProperty("jcr:title").getString();
                                                item[0] = componentTitle;
                                            }
                                            if(componentNode.hasProperty("jcr:description")) {
                                                String componentDesc = componentNode.getProperty("jcr:description").getString();
                                                item[1] = componentDesc;
                                            }

                                            item[2] = componentPath;

                                            componentList.add(item);

                                            componentMap.put(key, componentList);
                                        }
                                    }
                                }
                            }
                        }
                        
                        templateMap.put("components", componentMap);
                    }
                    
                }
                
            } catch (RepositoryException ex) {
                logger.error("No node found for the template ", ex);
            }
            
            results.add(templateMap);
        }
        
        
        GsonBuilder gsonBuilder = new GsonBuilder();
		Gson gson = gsonBuilder.create();
        Type jsonType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        gson.toJson(results,jsonType,jsonWriter);
        
    }
    
}
