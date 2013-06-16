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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(Servlet.class)
@Properties({
        @Property(name = "sling.servlet.paths", value = "/bin/templatemanager/list"),
        @Property(name = "service.description", value = "Servlet to list the template"),
        @Property(name = "service.vendor", value = "Amplexor"),
        @Property(name = "sling.servlet.methods", value = {"GET"})
})
public class TemplateManagerListServlet extends SlingAllMethodsServlet {
	private final static Logger logger = LoggerFactory.getLogger(TemplateManagerListServlet.class);

	@Reference
    private QueryBuilder queryBuilder;
    
    private ResourceResolver resourceResolver;
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        resourceResolver = request.getResourceResolver();
        
        String path = "/apps";
        if(request.getParameter("path") != null) {
            path = request.getParameter("path").toString();
        }
        
        List<Hit> templates = queryForTemplates(path);
        
        // write results as json 
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        
        if (templates == null) {
            response.getWriter().println("[{}]");
        } else {
            JsonWriter jsonWriter = null;

            try {
                jsonWriter = new JsonWriter(response.getWriter());
                writeTemplateValuesAsJson(templates, jsonWriter);
            } finally {
                if (jsonWriter != null) {
                    jsonWriter.close();
                }
            }

        }
        response.getWriter().flush();
        
    }
    
    private List<Hit> queryForTemplates(String path) {
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put("path", path);
            map.put("type", "cq:Template");
            map.put("orderby", "@ranking");  
            map.put("p.offset", "0"); 
            map.put("p.limit", "999"); 

            Session session = resourceResolver.adaptTo(Session.class); 
            QueryBuilder builder = resourceResolver.adaptTo(QueryBuilder.class); 
            Query query = builder.createQuery(PredicateGroup.create(map), session);
            SearchResult result = query.getResult();

            return result.getHits();
        }
        catch(Exception e) {
            logger.error("Unable to get the templates from the repository", e);
            return null;
        }
    }

    private void writeTemplateValuesAsJson(List<Hit> templates, JsonWriter jsonWriter) {
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        
        for (Hit hit : templates) {
            Map templateMap = new HashMap<String, String>();
            
            try {
                Node templateNode = hit.getNode();
                
                if(templateNode != null) {
                    templateMap.put("path", templateNode.getPath());
                    
                    if(templateNode.hasProperty("jcr:title")) {
                        templateMap.put("title", templateNode.getProperty("jcr:title").getString());
                    }
                    if(templateNode.hasProperty("jcr:description")) {
                        templateMap.put("description", templateNode.getProperty("jcr:description").getString());
                    }
                    if(templateNode.hasNode("jcr:content") && templateNode.getNode("jcr:content").hasProperty("sling:resourceType")) {
                        templateMap.put("resourceType", templateNode.getNode("jcr:content").getProperty("sling:resourceType").getString());
                    }
                    if(templateNode.hasNode("thumbnail.png")) {
                        templateMap.put("thumbnail",templateNode.getNode("thumbnail.png").getPath());
                    }
                    else { 
                        templateMap.put("thumbnail","/etc/designs/template-manager/clientlibs/img/noimage.jpg");
                    }
                }
                
            } catch (RepositoryException ex) {
                logger.error("No node found for the template ", ex);
            }
            
            results.add(templateMap);
        }
        
        
        GsonBuilder gsonBuilder = new GsonBuilder();
		Gson gson = gsonBuilder.create();
        Type jsonType = new TypeToken<List<Map<String, String>>>(){}.getType();
        gson.toJson(results,jsonType,jsonWriter);
        
    }
    
}
