package net.silicontrip.ingress;

import org.json.*;
import com.google.common.geometry.*;

import java.util.ArrayList;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.sql.*;
import javax.sql.*;
import javax.naming.*;

import javax.ejb.EJB;

public class PortalServlet extends HttpServlet {

	@EJB
	private EntitySessionBean entBean;

  public void init () throws ServletException {
  }

  public void destroy () {
	;
  }

  public void doPost(HttpServletRequest req, HttpServletResponse resp){
	try {
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/json");
		resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");
		PrintWriter writer = resp.getWriter();


	//	PortalDAO pdao = new SQLPortalDAO();

		S2LatLng p1 =  entBean.getPortalLocation(req.getParameter("ll"));
		S2LatLng p2 =  entBean.getPortalLocation(req.getParameter("l2"));
		S2LatLng p3 =  entBean.getPortalLocation(req.getParameter("l3"));
		String pr =  req.getParameter("rr");
		
		S2Region searchRegion = null;

		if (pr != null && p1 != null)
		{
			S1Angle rangeAngle =  S1Angle.radians(Double.parseDouble(pr) / 6367.0);
			searchRegion = S2Cap.fromAxisAngle(p1.toPoint(),rangeAngle);
		}

		if (p1 != null && p2 != null && p3 != null)
		{
			S2PolygonBuilder pb = new S2PolygonBuilder(S2PolygonBuilder.Options.UNDIRECTED_UNION);
			pb.addEdge(p1.toPoint(),p2.toPoint());
			pb.addEdge(p2.toPoint(),p3.toPoint());
			pb.addEdge(p3.toPoint(),p1.toPoint());
			searchRegion=pb.assemblePolygon();
		}

		if (p1 !=null && p2 !=null)
			searchRegion = S2LatLngRect.fromPointPair(p1,p2);

		if (searchRegion != null) {
			ArrayList<Portal> portalList = entBean.getPortalInRegion(searchRegion);
			
			JSONObject jsonResponse = new JSONObject();
			for (Portal pt : portalList)
			{
				JSONObject jsonPortal = new JSONObject();
				jsonPortal.put("guid",pt.getGuid());
				jsonPortal.put("title",pt.getTitle());
				jsonPortal.put("lat",pt.getLatE6()); // would love to move these to the E6 naming
				jsonPortal.put("lng",pt.getLngE6()); // E6
				jsonResponse.put(pt.getGuid(),jsonPortal);	
			}
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
	catch (Exception e) {
		JSONObject jsonResponse = new JSONObject();
		e.printStackTrace();
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
			;
		}
	}  
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp){
	doPost(req,resp);
  }
}

