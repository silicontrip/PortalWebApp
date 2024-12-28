package net.silicontrip.ingress;


import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.*;

import com.yahoo.platform.yui.compressor.*;
import org.mozilla.javascript.ErrorReporter;

import java.util.logging.Level;
import java.util.logging.Logger;


public class JSServlet extends HttpServlet {
		
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
	{
		ServletContext context = getServletContext();
		

		String userName = req.getParameter("agent");
		String apiKey = req.getParameter("apikey");
		if (apiKey!=null) apiKey = apiKey.toLowerCase();
		try {
			resp.setContentType("text/javascript");
			resp.setCharacterEncoding("UTF-8");
			resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");

			Logger.getLogger(JSServlet.class.getName()).log(Level.INFO, "user: " + userName + " key: "+apiKey);

			// authentication code
			req.login(userName,apiKey);
			// authentication code ends.  move to method? or catch login exception lower down?

			System.out.println("user: " + userName + " key: "+apiKey);
					
			
			String[] jsPath = new String[]{
				"/WEB-INF/db_portal.js",
				"/WEB-INF/db_edge.js",
				"/WEB-INF/db_region.js",
				"/WEB-INF/db_layer.js",
			};

			String baseUrl = "https://quadrant.silicontrip.net/portalApi/";
			if (req.getRemoteAddr().startsWith("10.101.222.")) 
				baseUrl = "https://quadrant.silicontrip.net:8181/portalApi/";
			if (req.getRemoteAddr().startsWith("127.")) 
				baseUrl = "http://localhost:8080/portalApi/";
			if (req.getRemoteAddr().startsWith("0:0:0:0:0:0:0:1"))
				baseUrl = "http://localhost:8080/portalApi/";
			if (req.getRemoteAddr().startsWith("192.168.0.")) 
				baseUrl = "https://192.168.0.20:8181/portalApi/";



			PrintWriter writer = resp.getWriter();
			writer.print("window.silicontrip_ingress_debug_remote_addr=\""+req.getRemoteAddr()+"\";");
			writer.print("window.silicontrip_ingress_url=\""+baseUrl+"\";");
			
			for (String file : jsPath)
			{
				InputStreamReader in = new InputStreamReader(context.getResourceAsStream(file));
				JavaScriptCompressor compressor = new JavaScriptCompressor(in, new YuiCompressorErrorReporter());
				compressor.compress(writer,-1,true,true,false,false);
			}
			writer.close();
		} catch (ServletException e) {

			try {
				resp.setStatus(403);
				PrintWriter writer = resp.getWriter();
				writer.println("alert (\"" + e.getMessage()+ " ("+e.getClass().getName()+") for " + userName+"\");");
				writer.close();
			} catch (IOException e2) {
				// :-P
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				resp.setStatus(500);
				PrintWriter writer = resp.getWriter();
				writer.println("alert (\"" + e.toString() +"\");");
				writer.close();
			} catch (Exception e2) {
				// sending the error caused an error.
				// is there anything else we can do?
				e2.printStackTrace();
			}
		}
	}
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp){
		doPost(req,resp);
	}
	
}
