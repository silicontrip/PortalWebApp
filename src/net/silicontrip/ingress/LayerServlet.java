package net.silicontrip.ingress;

import org.json.*;
import com.google.common.geometry.*;

import java.util.ArrayList;

import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.*;

import jakarta.ejb.EJB;
import java.util.Base64;
import java.util.Base64.*;

public class LayerServlet extends HttpServlet {

	@EJB
        private EntitySessionBean entBean;


  public void init () throws ServletException { }

  public void destroy () { ; }

	private JSONArray latLng(S2LatLng l)
	{
		JSONArray response = new JSONArray();
		response.put(l.latDegrees());
		response.put(l.lngDegrees());
		return response;
	}

	private JSONObject getDefaultStyle() {

		JSONArray dash = new JSONArray();
		dash.put(6);
		dash.put(4);
		JSONObject defaultStyle = new JSONObject();

		defaultStyle.put("stroke", true);
		defaultStyle.put("opacity", 1);
		defaultStyle.put("weight", 1.0);
		defaultStyle.put("fill", true);
		defaultStyle.put("fillOpacity", 0.6);
		defaultStyle.put("color", "#ff4444");
	//	defaultStyle.put("dashArray", dash);

		return defaultStyle;
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp){
	try {
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/json");
		resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");
		PrintWriter writer = resp.getWriter();


		S2LatLng p1 =  entBean.getPortalLocation(req.getParameter("ll"));
		S2LatLng p2 =  entBean.getPortalLocation(req.getParameter("l2"));
		S2LatLng p3 =  entBean.getPortalLocation(req.getParameter("l3"));
		String pr =  req.getParameter("rr");
		
		String type=null;
		JSONArray ll=null;

		if (pr != null && p1 != null)
		{
			type = "circle";
			//defaultStyle.put("radius",Double.parseDouble(pr) * 1000); 
			ll = latLng(p1);
		}

		if (p1 != null && p2 != null && p3 != null)
		{
			type = "polygon";
			ll = new JSONArray();

			ll.put(latLng(p1));
			ll.put(latLng(p2));
			ll.put(latLng(p3));
		}

		if (p3 == null && p1 !=null && p2 !=null)
		{
			type="rectangle";
			
			ll = new JSONArray();
			ll.put(latLng(p1));
			ll.put(latLng(p2));
		}

		if (type != null) {
			
		JSONObject object = new JSONObject();
		JSONArray jsonResponse = new JSONArray();

			object.put("type", type);
			object.put("latLng", ll);
			object.put("options", getDefaultStyle());

			jsonResponse.put(object);

			// layer object
			// { type: polyline, polygon, rectangle, circle
			// { latLng: [array]
			// { options: { }

			writer.println(jsonResponse.toString());
			writer.close(); 
		} else {
			resp.setStatus(400);
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("error",  "null region");
			writer.println(jsonResponse.toString());
			writer.close(); 
		}
		
	}
	catch (EntityDAOException e) {
		JSONObject jsonResponse = new JSONObject();
		// e.printStackTrace();
		jsonResponse.put("error",  e.getMessage());
		jsonResponse.put("errorType",  e.getClass().getName());
		try {
			resp.setStatus(500);
			PrintWriter writer = resp.getWriter();
			writer.println(jsonResponse.toString());
			writer.close(); 
			//resp.sendError(500,jsonResponse.toString());  // I want this to be json valid
		} catch (Exception e2) {
			// sending the error caused an error.
			// is there anything else we can do?
			e2.printStackTrace();;
		}
	}  catch (IOException e) {
		e.printStackTrace();
	}
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp){
	//doPost(req,resp);

		if (req.getParameter("pls") != null)
		{
			DrawTools dt = DrawTools.fromIntelQuery(req.getParameter("pls"));
			//byte[] dtb = dt.asLinesPack();

			//Encoder b64enc = Base64.getUrlEncoder();
			try {
				PrintWriter writer = resp.getWriter();
				writer.println("<tt>");
				writer.println(dt.toString());
				
				writer.println("</tt>"); writer.println("<hr/>"); writer.println("<tt>");
				writer.println(dt.asIntelLink());
				writer.println("</tt>"); writer.println("<hr/>"); writer.println("<tt>");
				writer.println(dt.asLinesPack());
				writer.println("</tt>");
			} catch (IOException e) {
				// how can we print that an error occurred if an error occurred in the thing that prints the error.  I'm sure there's a Douglas Adams story in that.
				e.printStackTrace();
			}
			
		} else {
			try {
				//Decoder b64dec = Base64.getUrlDecoder();
				DrawTools dt = DrawTools.fromStringEncoding(req.getQueryString());
				//DrawTools dt = new DrawTools();
				try {
					PrintWriter writer = resp.getWriter();
					writer.println("<tt>");
					//writer.println(req.getQueryString());
					//writer.println("</pre>"); writer.println("<hr/>"); writer.println("<pre>");
						writer.println(dt.toString());
					writer.println("</tt>"); writer.println("<hr/>"); writer.println("<tt>");
					writer.println("<a href=\"" + dt.asIntelLink() + "\">INTEL</a>");
					writer.println("</tt>");
				} catch (IOException e) {
					// how can we print that an error occurred if an error occurred in the thing that prints the error.  I'm sure there's a Douglas Adams story in that.
					e.printStackTrace();
				}
			} catch (Exception e) {
				try {
					PrintWriter writer = resp.getWriter();
					writer.println("<pre>");
					writer.println(e.getMessage());
					writer.println("</pre>");
				} catch (IOException e2) {
					// how can we print that an error occurred if an error occurred in the thing that prints the error.  I'm sure there's a Douglas Adams story in that.
					e2.printStackTrace();
				}
			

			}
		}
		
  }
}

