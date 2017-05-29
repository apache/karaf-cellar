/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.webconsole;

import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Set;

/**
 * WebConsole plugin for Cellar cluster groups.
 */
public class CellarPlugin extends AbstractWebConsolePlugin {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(CellarPlugin.class);

    public static final String NAME = "cellar";
    public static final String LABEL = "Cellar";
    private ClassLoader classLoader;
    private String cellarJs = "/cellar/res/ui/cellar.js";

    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private BundleContext bundleContext;

    public void start() {
        super.activate(bundleContext);
        this.classLoader = this.getClass().getClassLoader();
        this.LOGGER.info("{} plugin activated", LABEL);
    }

    public void stop() {
        this.LOGGER.info("{} plugin deactivated", LABEL);
        super.deactivate();
    }

    @Override
    public String getLabel() {
        return NAME;
    }

    @Override
    public String getTitle() {
        return LABEL;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean success = false;

        final String action = req.getParameter("action");
        final String node = req.getParameter("node");
        final String group = req.getParameter("group");
        final String id = req.getParameter("id");

        if (action == null) {
            success = true;
        } else if (action.equals("createGroup")) {
            groupManager.createGroup(group);
            success = true;
        } else if (action.equals("deleteGroup")) {
            groupManager.deleteGroup(group);
            success = true;
        }

        if (success) {
            // let's wait a little bit to give the framework time
            // to process our request
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                // ignore
            }
            this.renderJSON(resp, null);
        } else {
            super.doPost(req, resp);
        }
    }

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // get request info from request attribute
        final PrintWriter pw = response.getWriter();

        String appRoot = (String) request.getAttribute("org.apache.felix.webconsole.internal.servlet.OsgiManager.appRoot");
        final String featuresScriptTag = "<script src='" + appRoot + this.cellarJs
                + "' language='JavaScript'></script>";
        pw.println(featuresScriptTag);

        pw.println("<script type='text/javascript'>");
        pw.println("// <![CDATA[");
        pw.println("var imgRoot = '" + appRoot + "/res/imgs';");
        pw.println("// ]]>");
        pw.println("</script>");

        pw.println("<div id='plugin_content'/>");

        pw.println("<script type='text/javascript'>");
        pw.println("// <![CDATA[");
        pw.print("renderGroups( ");
        writeJSON(pw);
        pw.println(" )");
        pw.println("// ]]>");
        pw.println("</script>");
    }

    protected URL getResource(String path) {
        path = path.substring(NAME.length() + 1);
        if (path == null || path.isEmpty()) {
            return null;
        }
        URL url = this.classLoader.getResource(path);
        if (url != null) {
            InputStream ins = null;
            try {
                ins = url.openStream();
                if (ins == null) {
                    this.LOGGER.error("failed to open {}", url);
                    url = null;
                }
            } catch (IOException e) {
                this.LOGGER.error(e.getMessage(), e);
                url = null;
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                        this.LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        }
        return url;
    }

    private void renderJSON(final HttpServletResponse response, final String feature) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final PrintWriter pw = response.getWriter();
        writeJSON(pw);
    }

    private void writeJSON(final PrintWriter pw) throws IOException {
        final Set<Group> groups = groupManager.listAllGroups();
        final Set<Node> nodes = clusterManager.listNodes();

        final JSONWriter jw = new JSONWriter(pw);

        jw.object();
        jw.key("status");
        jw.value(getStatusLine(groups, nodes));
        jw.key("groups");
        jw.array();
        for (Group g : groups) {
            jw.object();
            jw.key("name");
            jw.value(g.getName());

            Set<Node> members = g.getNodes();
            jw.key("members");
            jw.array();
            if (nodes != null) {
                for (Node n : members) {
                    jw.object();
                    jw.key("id");
                    jw.value(n.getId());
                    jw.endObject();
                }
            }

            jw.endArray();
            jw.key("actions");
            jw.array();
            boolean enable = true;
            action(jw, enable, "removeNode", "Remove Node", "update");
            action(jw, enable, "deleteGroup", "Delete Group", "delete");
            jw.endArray();
            jw.endObject();
        }
        jw.endArray();
        jw.endObject();
    }

    private void action(JSONWriter jw, boolean enabled, String op, String title, String image) throws IOException {
        jw.object();
        jw.key("enabled").value(enabled);
        jw.key("op").value(op);
        jw.key("title").value(title);
        jw.key("image").value(image);
        jw.endObject();
    }

    private String getStatusLine(final Set<Group> groups, Set<Node> members) {
        int groupCount = 0;
        int memberCount = 0;

        if (groups != null) {
            groupCount = groups.size();
        }

        if (members != null) {
            memberCount = members.size();
        }

        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Found %s members in %s groups", memberCount, groupCount));
        return builder.toString();
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

}