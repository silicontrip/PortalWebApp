package net.silicontrip.ingress;

import jakarta.ejb.EJB;

import jakarta.servlet.http.*;
import jakarta.servlet.http.Part;
import net.silicontrip.UniformDistribution;
import jakarta.servlet.*;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ReflectionException;
import javax.management.ObjectName;

import java.sql.SQLException;

import org.json.JSONObject;
import org.json.JSONArray;

@jakarta.servlet.annotation.MultipartConfig
public class JMXServlet extends HttpServlet {

	public void init() throws ServletException {
	}

	public void destroy() {
		;
	}

	private String callCellDBManager(String call, Object[] par, String[] sig) throws MalformedObjectNameException,
			InstanceNotFoundException, MBeanException, ReflectionException, SQLException {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName("net.silicontrip.ingress:type=CellDBManager");
		return (String) mbs.invoke(name, call, par, sig);
	}

	String[] getAuth(String authHeader) {
		String[] parts = new String[2];
		if (authHeader != null && authHeader.toLowerCase().startsWith("basic ")) {
			String base64Credentials = authHeader.substring(6);
			byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
			String credentials = new String(decodedBytes);
			parts = credentials.split(":", 2);
		}
		return parts;
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		resp.setCharacterEncoding("UTF-8");

		try {
			PrintWriter writer = resp.getWriter();

			String authHeader = req.getHeader("Authorization");
			String[] up = getAuth(authHeader);
			req.login(up[0], up[1]);

			String action = req.getParameter("action");
			resp.setContentType("text/plain");

			if (action.equals("best")) {
				resp.setContentType("text/plain");
				String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
				String filename = "field_export_" + timestamp + ".txt";
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
				writer.println(callCellDBManager("exportBestFields", null, null));
				return;
			} else if (action.equals("backup")) {
				String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
				String filename = "field_export_" + timestamp + ".txt";
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
				writer.println(callCellDBManager("exportAllFields", null, null));
				return;
			}

			resp.setContentType("text/html");

			writer.println("<html>");
			writer.println("<head>");
			writer.println("<link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"favicon_32.png\">");
			writer.println("<link rel=\"stylesheet\" href=\"api-stylesheet-grn.css\">");
			writer.println("</head>");
			writer.println("<body>");

			// now our code will look just like celldbtool
			if (action.equals("trace")) {
				String cellid = req.getParameter("cellid");
				writer.println("<div class=\"title\">");
				writer.println("<h1>Tracing cellid: " + cellid + "</h1>");
				writer.println("</div>");
				writer.println("<div class=\"control\">");
				Object[] par = { cellid };
				String[] sig = { "java.lang.String" };
				String result = callCellDBManager("traceCell", par, sig);
				JSONObject json = new JSONObject(result);
				// writer.println("<p>" + json.toString(2) + "</p>");
				writer.println("<p>" + json.getString("cellShapeDrawTools") + "</p>");
				writer.println("<p>" + json.getString("cellShapeIntel") + "</p>");
				UniformDistribution mu = new UniformDistribution(json.getString("mudb"));
				writer.println("<p>MU (db): " + mu + " : " + mu.toStringWithPrecision(3) + "</p>");
				mu = new UniformDistribution(json.getString("mucalc"));
				writer.println("<p>MU (calc): " + mu + " : " + mu.toStringWithPrecision(3) + "</p>");
				writer.println(
						"<p>Fields: " + json.getInt("numfields") + " All Valid: " + json.getBoolean("valid") + "</p>");
				writer.println("<h1>Fields</h1>");
				for (Object o : json.getJSONArray("fields")) {
					JSONObject fi = (JSONObject) o;
					// writer.println("<p>" + fi.toString(2) + "</p>");
					writer.println("<p>GUID: " + fi.getString("guid") + "</p>");
					writer.println("<p>Index: " + fi.getInt("index"));
					mu = new UniformDistribution(fi.getString("mu"));
					writer.println(
							" MU: " + fi.getInt("imu") + " Scaled MU: " + mu + " : " + mu.toStringWithPrecision(3));
					writer.println(" area: " + fi.getFloat("area") + " mukm: " + fi.getFloat("mukm") + "</p>");
					for (String c : fi.getJSONObject("contributions").keySet()) {
						mu = new UniformDistribution(fi.getJSONObject("contributions").getString(c));
						writer.println("<p>" + c + ": " + mu + " : " + mu.toStringWithPrecision(3) + "</p>");
					}
					writer.println("<p>" + fi.getString("drawtools") + "</p>");
					writer.println("<p>" + fi.getString("intel") + "</p>");
				}
				writer.println("</div>");
			} else if (action.equals("refine")) {
				writer.println("<div class=\"title\">");
				writer.println("<h1>Refining cell model.</h1>");
				writer.println("</div>");
				String result = callCellDBManager("refineCells", null, null);
				writer.println("<div class=\"control\">");
				writer.println("<p>" + result + "</p>");
				writer.println("</div>");

			} else if (action.equals("build-cells-background")) {
				// Start background build cells and show polling status page
				String result = callCellDBManager("startRebuildCellsBackground", null, null);
				writer.println("<div class=\"title\">");
				writer.println("<h1>Background Build Cells</h1>");
				writer.println("</div>");
				writer.println("<div class=\"control\">");
				writer.println("<p>" + result + "</p>");
				writer.println("<div id='status'><pre>Loading status...</pre></div>");
				writer.println("<button id='cancelBtn' onclick='cancelTask()'>Cancel Build</button>");
				writer.println("</div>");
				writer.println("<script>");
				writer.println("function updateStatus() {");
				writer.println("  fetch('jarvis?action=build-cells-status', {");
				writer.println("    method: 'POST',");
				writer.println("    headers: {");
				writer.println("      'Authorization': '" + req.getHeader("Authorization") + "'");
				writer.println("    }");
				writer.println("  })");
				writer.println("  .then(r => r.text())");
				writer.println("  .then(data => {");
				writer.println("    document.getElementById('status').innerHTML = '<pre>' + data + '</pre>';");
				writer.println("    if (data.includes('Status: RUNNING')) {");
				writer.println("      setTimeout(updateStatus, 2000);");
				writer.println("    } else {");
				writer.println("      document.getElementById('cancelBtn').disabled = true;");
				writer.println("    }");
				writer.println("  });");
				writer.println("}");
				writer.println("function cancelTask() {");
				writer.println("  fetch('jarvis?action=build-cells-cancel', {");
				writer.println("    method: 'POST',");
				writer.println("    headers: {");
				writer.println("      'Authorization': '" + req.getHeader("Authorization") + "'");
				writer.println("    }");
				writer.println("  })");
				writer.println("  .then(r => r.text())");
				writer.println("  .then(data => alert(data));");
				writer.println("}");
				writer.println("setTimeout(updateStatus, 1000);");
				writer.println("</script>");

			} else if (action.equals("rebuild-fields")) {
				writer.println("<div class=\"title\">");
				writer.println("<h1>Rebuilding Field Index.</h1>");
				writer.println("</div>");
				String result = callCellDBManager("rebuildFieldCells", null, null);
				writer.println("<div class=\"control\">");
				writer.println("<p>" + result + "</p>");
				writer.println("</div>");
			} else if (action.equals("invalidate")) {
				String fieldguid = req.getParameter("fieldguid");
				writer.println("<div class=\"title\">");
				writer.println("<h1>Invalidating field: " + fieldguid + "</h1>");
				writer.println("</div>");
				writer.println("<div class=\"control\">");
				Object[] par = { fieldguid };
				String[] sig = { "java.lang.String" };
				String result = callCellDBManager("invalidateField", par, sig);
				writer.println("<pre>" + result + "</pre>");
				writer.println("</div>");
			} else if (action.equals("refine-background")) {
				// Start background refinement and show polling status page
				String result = callCellDBManager("startRefineCellsBackground", null, null);
				writer.println("<div class=\"title\">");
				writer.println("<h1>Background Refinement</h1>");
				writer.println("</div>");
				writer.println("<div class=\"control\">");
				writer.println("<p>" + result + "</p>");
				writer.println("<div id='status'><pre>Loading status...</pre></div>");
				writer.println("<button id='cancelBtn' onclick='cancelTask()'>Cancel Refinement</button>");
				writer.println("</div>");
				writer.println("<script>");
				writer.println("function updateStatus() {");
				writer.println("  fetch('jarvis?action=refine-status', {");
				writer.println("    method: 'POST',");
				writer.println("    headers: {");
				writer.println("      'Authorization': '" + req.getHeader("Authorization") + "'");
				writer.println("    }");
				writer.println("  })");
				writer.println("  .then(r => r.text())");
				writer.println("  .then(data => {");
				writer.println("    document.getElementById('status').innerHTML = '<pre>' + data + '</pre>';");
				writer.println("    if (data.includes('Status: RUNNING')) {");
				writer.println("      setTimeout(updateStatus, 2000);");
				writer.println("    } else {");
				writer.println("      document.getElementById('cancelBtn').disabled = true;");
				writer.println("    }");
				writer.println("  });");
				writer.println("}");
				writer.println("function cancelTask() {");
				writer.println("  fetch('jarvis?action=refine-cancel', {");
				writer.println("    method: 'POST',");
				writer.println("    headers: {");
				writer.println("      'Authorization': '" + req.getHeader("Authorization") + "'");
				writer.println("    }");
				writer.println("  })");
				writer.println("  .then(r => r.text())");
				writer.println("  .then(data => alert(data));");
				writer.println("}");
				writer.println("setTimeout(updateStatus, 1000);");
				writer.println("</script>");
			} else if (action.equals("refine-status")) {
				// Return plain text status for polling
				resp.setContentType("text/plain");
				String result = callCellDBManager("refineCellsStatus", null, null);
				writer.println(result);
				return; // Skip HTML wrapper
			} else if (action.equals("refine-cancel")) {
				// Cancel running refinement
				resp.setContentType("text/plain");
				String result = callCellDBManager("cancelRefineCells", null, null);
				writer.println(result);
				return; // Skip HTML wrapper
			} else if (action.equals("build-cells-status")) {
				// Return plain text status for polling
				resp.setContentType("text/plain");
				String result = callCellDBManager("rebuildCellsStatus", null, null);
				writer.println(result);
				return; // Skip HTML wrapper
			} else if (action.equals("build-cells-cancel")) {
				// Cancel running build cells
				resp.setContentType("text/plain");
				String result = callCellDBManager("cancelRebuildCells", null, null);
				writer.println(result);
				return; // Skip HTML wrapper
			} else if (action.equals("merge")) {
				// Handle file upload for merge
				writer.println("<div class=\"title\">");
				writer.println("<h1>Merging Fields</h1>");
				writer.println("</div>");
				writer.println("<div class=\"control\">");

				try {
					Part filePart = req.getPart("fileToUpload");
					if (filePart != null && filePart.getSize() > 0) {
						// Read file content
						java.io.InputStream fileContent = filePart.getInputStream();
						java.io.BufferedReader reader = new java.io.BufferedReader(
								new java.io.InputStreamReader(fileContent, "UTF-8"));
						StringBuilder fileData = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) {
							fileData.append(line).append("\n");
						}
						reader.close();

						// Call JMX method
						Object[] par = { fileData.toString() };
						String[] sig = { "java.lang.String" };
						String result = callCellDBManager("importFieldsMerge", par, sig);
						writer.println("<pre>" + result + "</pre>");
					} else {
						writer.println("<p>Error: No file uploaded.</p>");
					}
				} catch (Exception e) {
					writer.println("<p>Error processing file: " + e.getMessage() + "</p>");
					e.printStackTrace(writer);
				}

				writer.println("</div>");
			} else if (action.equals("replace")) {
				// Handle file upload for replace
				writer.println("<div class=\"title\">");
				writer.println("<h1>Replacing Fields</h1>");
				writer.println("</div>");
				writer.println("<div class=\"control\">");

				try {
					Part filePart = req.getPart("fileToUpload");
					if (filePart != null && filePart.getSize() > 0) {
						// Read file content
						java.io.InputStream fileContent = filePart.getInputStream();
						java.io.BufferedReader reader = new java.io.BufferedReader(
								new java.io.InputStreamReader(fileContent, "UTF-8"));
						StringBuilder fileData = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) {
							fileData.append(line).append("\n");
						}
						reader.close();

						// Call JMX method
						Object[] par = { fileData.toString() };
						String[] sig = { "java.lang.String" };
						String result = callCellDBManager("importFieldsReplace", par, sig);
						writer.println("<pre>" + result + "</pre>");
					} else {
						writer.println("<p>Error: No file uploaded.</p>");
					}
				} catch (Exception e) {
					writer.println("<p>Error processing file: " + e.getMessage() + "</p>");
					e.printStackTrace(writer);
				}

				writer.println("</div>");
			} else if (action.equals("erase")) {
				writer.println("<div class=\"title\">");
				writer.println("<h1>Erasing cells.</h1>");
				writer.println("</div>");
				String result = callCellDBManager("eraseCells", null, null);
				writer.println("<div class=\"control\">");
				writer.println("<p>" + result + "</p>");
				writer.println("</div>");
			} else {
				writer.println("<div class=\"title\">");
				writer.println("<h1>?UNKNOWN ACTION.</h1>");
				writer.println("</div>");
			}

			writer.println("</body>");
			writer.println("</html>");

		} catch (ServletException e) {

			try {
				resp.setStatus(401);
				PrintWriter writer = resp.getWriter();
				resp.setContentType("text/html");
				writer.println("<html>");
				writer.println("<head>");
				writer.println("<link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"favicon_32.png\">");
				writer.println("<link rel=\"stylesheet\" href=\"api-stylesheet-grn.css\">");
				writer.println("</head>");
				writer.println("<body>");

				writer.println("<div class=\"title\">");
				writer.println("<h1>?Error Authentication Required</h1>");
				// writer.println("<h1>ready.</h1>");
				writer.println("</div>");
				writer.println("<div class=\"control\">");
				writer.println("<p>");
				writer.println(e.getClass().getName());
				writer.println("</p>");
				writer.println("<p>");
				writer.println(e.getMessage());
				writer.println("</p>");
				writer.println("</div>");

				writer.println("</body></html>");
				writer.close();
			} catch (IOException e2) {
				// :-P
				// I should put a logger in here
			}

		} catch (Exception e) {
			try {

				resp.setStatus(500);
				PrintWriter writer = resp.getWriter();
				resp.setContentType("text/html");
				writer.println("<html>");
				writer.println("<head>");
				writer.println("<link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"favicon_32.png\">");
				writer.println("<link rel=\"stylesheet\" href=\"api-stylesheet-grn.css\">");
				writer.println("</head>");
				writer.println("<body>");

				writer.println("<div class=\"title\">");
				writer.println("<h1>?Syntax Error</h1>");
				// writer.println("<h1>ready.</h1>");
				writer.println("</div>");
				writer.println("<div class=\"control\">");
				writer.println("<p>");
				writer.println(e.getClass().getName());
				writer.println("</p>");
				writer.println("<p>");
				writer.println(e.getMessage());
				writer.println("</p>");
				writer.println("<p>");
				e.printStackTrace(writer);
				writer.println("</p>");

				writer.println("</div>");

				writer.println("</body></html>");
				writer.close();
			} catch (IOException e2) {
				// :-P
			}

		}

	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		// doPost(req,resp);
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/html");

		try {
			PrintWriter writer = resp.getWriter();

			String authHeader = req.getHeader("Authorization");
			String[] up = getAuth(authHeader);
			req.login(up[0], up[1]);

			writer.println("<html>");
			writer.println("<head>");
			writer.println("<link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"favicon_32.png\">");
			writer.println("<link rel=\"stylesheet\" href=\"api-stylesheet-grn.css\">");
			writer.println("</head>");
			writer.println("<body>");

			writer.println("<div class=\"title\">");
			writer.println("<H1>cell database tools</h1>");
			writer.println("<h1>ready.</h1>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>trace</h1>");
			writer.println("<p>Enter a cellid to show the field or fields that contribute to its value.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='text' name='cellid'>");
			writer.println("<input type='hidden' name='action' value='trace'>");
			writer.println("<input type='submit' value='Trace'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>INVALIDATE FIELD</h1>");
			writer.println(
					"<p>Enter a field GUID which has an invalid MU to mark is as invalid in the database and not used for cell calculations.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='text' name='fieldguid'>");
			writer.println("<input type='hidden' name='action' value='invalidate'>");
			writer.println("<input type='submit' value='Invalidate'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>REFINE</h1>");
			writer.println("<p>Update calculation of all cells based on database fields.</p>");
			// writer.println("<form method='POST' action='jarvis'
			// style='display:inline;'>");
			// writer.println("<input type='hidden' name='action' value='refine'>");
			// writer.println("<input type='submit' value='Refine (Synchronous)'>");
			// writer.println("</form>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='hidden' name='action' value='refine-background'>");
			writer.println("<input type='submit' value='Refine'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>ERASE CELLS</h1>");
			writer.println("<p>Erase MUCELLS.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='hidden' name='action' value='erase'>");
			writer.println("<input type='submit' value='Erase'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>BUILD CELLS</h1>");
			writer.println("<p>Build cell model from scratch based on database fields.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='hidden' name='action' value='build-cells-background'>");
			writer.println("<input type='submit' value='Build Cell Model'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>REBUILD FIELD INDEX</h1>");
			writer.println("<p>Rebuild field index when fields have been added or modified.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='hidden' name='action' value='rebuild-fields'>");
			writer.println("<input type='submit' value='Rebuild Field Index'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>EXPORT FIELDS</h1>");
			writer.println("<p>Download All or the Best Fields for backup.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='hidden' name='action' value='best'>");
			writer.println("<input type='submit' value='Export Best'>");
			writer.println("</form><form method='POST' action='jarvis'>");
			writer.println("<input type='hidden' name='action' value='backup'>");
			writer.println("<input type='submit' value='Export All'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>Import and merge FIELDS</h1>");
			writer.println("<p>Import fields from a file and add them to the field table.</p>");
			writer.println("<form method='POST' action='jarvis' enctype=\"multipart/form-data\">");
			writer.println("<input type='hidden' name='action' value='merge'>");
			writer.println("<input type=\"file\" id=\"uploadFile\" name=\"fileToUpload\" required>");
			writer.println("<input type='submit' value='Import'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>Import and REPLACE FIELDS</h1>");
			writer.println(
					"<p><strong>WARNING:</strong> This will delete ALL existing fields and replace them with the uploaded file.</p>");
			writer.println("<form method='POST' action='jarvis' enctype=\"multipart/form-data\">");
			writer.println("<input type='hidden' name='action' value='replace'>");
			writer.println("<input type=\"file\" id=\"uploadFileReplace\" name=\"fileToUpload\" required>");
			writer.println("<input type='submit' value='Replace All Fields'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("</body>");
			writer.println("</html>");

		} catch (Exception e) {

			try {
				resp.setHeader("WWW-Authenticate", "Basic realm=\"portalApi/jarvis\"");
				resp.setStatus(401);
				PrintWriter writer = resp.getWriter();
				writer.println("<html>");
				writer.println("<head>");
				writer.println("<link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"favicon_32.png\">");
				writer.println("<link rel=\"stylesheet\" href=\"api-stylesheet-grn.css\">");
				writer.println("</head>");
				writer.println("<body>");

				writer.println("<div class=\"title\">");
				writer.println("<h1>Authentication Required</h1>");
				writer.println("<h1>ready.</h1>");
				writer.println("</div>");
				writer.println("</body></html>");
				writer.close();
			} catch (IOException e2) {
				// :-P
			}

		}

	}

}
