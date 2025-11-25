package net.silicontrip.ingress;

import jakarta.ejb.EJB;

import jakarta.servlet.http.*;
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

public class JMXServlet extends HttpServlet {

	public void init () throws ServletException {
	}
	
	public void destroy () {
		;
	}

	private String callCellDBManager(String call, Object[] par, String[] sig) throws MalformedObjectNameException, InstanceNotFoundException, MBeanException, ReflectionException, SQLException
	{
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName("net.silicontrip.ingress:type=CellDBManager");
		return (String)mbs.invoke(name, call, par, sig);
	}
	
	String[] getAuth(String authHeader) 
	{
		String[] parts = new String[2];
		if (authHeader != null && authHeader.toLowerCase().startsWith("basic ")) {
			String base64Credentials = authHeader.substring(6);
			byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
			String credentials = new String(decodedBytes);
			parts = credentials.split(":", 2);
		}
		return parts;
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
	{
		resp.setCharacterEncoding("UTF-8");

		try {
			PrintWriter writer = resp.getWriter();
			
			String authHeader = req.getHeader("Authorization");
			String[] up = getAuth(authHeader);
			req.login(up[0],up[1]);

			// writer.println("<h1>OK</H1>");
			String action = req.getParameter("action");
			resp.setContentType("text/plain");

			if (action.equals("best")) {
				resp.setContentType("text/plain");
				String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
				String filename = "field_export_" + timestamp + ".txt";
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
				writer.println(callCellDBManager("exportBestFields",null,null));
				return;
			} else if (action.equals("backup")) {
				String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
				String filename = "field_export_" + timestamp + ".txt";
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
				writer.println(callCellDBManager("exportAllFields",null,null));
				return;
			}

			resp.setContentType("text/html");
			
			writer.println("<html>");
			writer.println("<head><link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"/icons/favicon32.png\"></head>");
			writer.println("<body>");

			// now our code will look just like celldbtool
			if (action.equals("trace")) {
				String cellid = req.getParameter("cellid");
				writer.println("<pre>Tracing cellid: " + cellid + "</pre>");
				Object[] par = { cellid };
				String[] sig = { "java.lang.String" };
				String result = callCellDBManager("traceCell", par, sig);
				writer.println("<pre>" + result + "</pre>");
			} else if (action.equals("refine")) {
				writer.println("<pre>Refining cell model.</pre>");
				String result = callCellDBManager("refineCells",null,null);
				writer.println("<pre>" + result + "</pre>");
			} else if (action.equals("build")) {
				writer.println("<pre>Rebuilding cell model.</pre>");
				String result = callCellDBManager("rebuildCells",null,null);
				writer.println("<pre>" + result + "</pre>");
			} else if (action.equals("rebuild")) {
				writer.println("<pre>Rebuilding Field-Cell table.</pre>");
				String result = callCellDBManager("rebuildFieldCells",null,null);
				writer.println("<pre>" + result + "</pre>");
			} else if (action.equals("invalidate")) {
				String fieldguid = req.getParameter("fieldguid");
				writer.println("<pre>Invalidating field: " + fieldguid + "</pre>");
				Object[] par = { fieldguid };
				String[] sig = { "java.lang.String" };
				String result = callCellDBManager("invalidateField",par,sig);
				writer.println("<pre>" + result + "</pre>");
			} else {
				writer.println("<pre>this is not the action you are looking for.</pre>");
			}
			
			writer.println("</body>");
			writer.println("</html>");

		} catch (Exception e) {
			
			try {
				resp.setStatus(401);
				PrintWriter writer = resp.getWriter();
				writer.println("<h1>Authentication Required</h1>");
				writer.close();
			} catch (IOException e2) {
				// :-P
			}
			
		}

	}
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	{
		//doPost(req,resp);
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/html");


		try {
			PrintWriter writer = resp.getWriter();

			String authHeader = req.getHeader("Authorization");
			String[] up = getAuth(authHeader);
			req.login(up[0],up[1]);
			
			writer.println("<html>");
			writer.println("<head>");
			writer.println("<link rel=\"icon\" type=\"image/png\" sizes=\"32x32\" href=\"favicon_32.png\">");
			writer.println("<link rel=\"stylesheet\" href=\"api-stylesheet-grn.css\">");
			writer.println("</head>");
			writer.println("<body>");

			writer.println("<div class=\"title\">");
			writer.println("<H1>cell database tools</h1>");
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
			writer.println("<p>Enter a field GUID which has an invalid MU to mark is as invalid in the database and not used for cell calculations.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='text' name='fieldguid'>");
			writer.println("<input type='hidden' name='action' value='invalidate'>");
			writer.println("<input type='submit' value='Invalidate'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>REFINE</h1>");
			writer.println("<p>Update calculation of all cells based on database fields.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='hidden' name='action' value='refine'>");
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
			writer.println("<h1>REBUILD CELLS</h1>");
			writer.println("<p>Build Cell model from scratch based on database fields.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='hidden' name='action' value='build'>");
			writer.println("<input type='submit' value='Build'>");
			writer.println("</form>");
			writer.println("</div>");

			writer.println("<div class=\"control\">");
			writer.println("<h1>REBUILD FIELD CELLS</h1>");
			writer.println("<p>Rebuild the MUCELL-FIELDS joining table if the field table has been modified.</p>");
			writer.println("<form method='POST' action='jarvis'>");
			writer.println("<input type='hidden' name='action' value='rebuild'>");
			writer.println("<input type='submit' value='Rebuild'>");
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

			writer.println("</body>");
			writer.println("</html>");


		} catch (Exception e) {
			
			try {
				resp.setHeader("WWW-Authenticate", "Basic realm=\"portalApi/jarvis\"");
				resp.setStatus(401);
				PrintWriter writer = resp.getWriter();
				writer.println("<h1>Authentication Required</h1>");
				writer.close();
			} catch (IOException e2) {
				// :-P
			}
			
		}

	}

}
