package net.silicontrip.ingress;

import org.json.*;
import com.google.common.geometry.*;

import java.util.ArrayList;

import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.*;
import java.sql.*;
import javax.sql.*;
import javax.naming.*;

import jakarta.ejb.EJB;

import java.util.logging.Level;
import java.util.logging.Logger;


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

		// As portal titles are not unique. I'd like to change this logic.
		String l1 = req.getParameter("ll");
		String l2 = req.getParameter("l2");
		String l3 = req.getParameter("l3");
		
		String pr =  req.getParameter("rr");
		String portalsString = req.getParameter("portals");

		S2Region searchRegion = null;

		if (pr != null && l1 != null)
		{
			S2LatLng p1 =  entBean.getPortalLocation(l1);
			double radius = Double.parseDouble(pr) ;
			//Logger.getLogger(PortalServlet.class.getName()).log(Level.INFO, "Radius: " + radius );

			//System.out.println("Radius: " + radius);
			if (radius<0.0001) radius=0.0001; // because 0 doesn't work
			S1Angle rangeAngle =  S1Angle.radians(radius / 6367.0);
			searchRegion = S2Cap.fromAxisAngle(p1.toPoint(),rangeAngle);
		}
		else if (l1 != null && l2 != null && l3 != null)
		{
			S2LatLng p1 =  entBean.getPortalLocation(l1);
			S2LatLng p2 =  entBean.getPortalLocation(l2);
			S2LatLng p3 =  entBean.getPortalLocation(l3);
			S2PolygonBuilder pb = new S2PolygonBuilder(S2PolygonBuilder.Options.UNDIRECTED_UNION);
			pb.addEdge(p1.toPoint(),p2.toPoint());
			pb.addEdge(p2.toPoint(),p3.toPoint());
			pb.addEdge(p3.toPoint(),p1.toPoint());
			searchRegion=pb.assemblePolygon();
		} else if (l1 !=null && l2 !=null) {
			S2LatLng p1 =  entBean.getPortalLocation(l1);
			S2LatLng p2 =  entBean.getPortalLocation(l2);
			searchRegion = S2LatLngRect.fromPointPair(p1,p2);
		} else if (l1 != null) {
			//S1Angle rangeAngle =  S1Angle.radians(0.0001 / 6367.0);
			//searchRegion = S2Cap.fromAxisAngle(p1.toPoint(),rangeAngle);
			// This condition is the only one where we are not concerned with ambigious titles.
			//portalsString = "[\"" + l1 + "\"]";
			JSONArray ja = new JSONArray();
			ja.put(l1);
			portalsString=ja.toString();
		}
		ArrayList<Portal> portalList;
		if (searchRegion != null) 
			portalList = entBean.getPortalInRegion(searchRegion);
		 else
		 {
		 	if (portalsString != null)
			{
				portalList = new ArrayList<Portal>();
				JSONArray portalsJSON = new JSONArray(portalsString);
				if (portalsJSON.length() == 1)
				{
					portalList = entBean.getPortals(portalsJSON.getString(0));
				} else {
					for (Object obj: portalsJSON)
					{
						String portalTitle = (String)obj;
						ArrayList<Portal> res = entBean.getPortals(portalTitle);
						if (res.size() > 1)
						{
							String message = "Ambiguous Portal Title: " + portalTitle;
							for (Portal p : res)
							{
								message = message + " " + (p.getLatE6() / 1000000.0) + "," + (p.getLngE6() / 1000000.0);
							}
							throw new ServletException(message);
						}

						portalList.add(res.get(0));
					}
				}
			} else {
				portalList = entBean.getPortalAll();
			}
		 }
		JSONObject jsonResponse = new JSONObject();
		for (Portal pt : portalList)
		{
			JSONObject jsonPortal = new JSONObject();
			jsonPortal.put("guid",pt.getGuid());
			jsonPortal.put("title",pt.getTitle());
			jsonPortal.put("lat",pt.getLatE6()); // would love to move these to the E6 naming
			jsonPortal.put("lng",pt.getLngE6()); // E6
			jsonPortal.put("health",pt.getHealth());
			jsonPortal.put("team",pt.getTeam());
			jsonPortal.put("level",pt.getLevel());
			jsonResponse.put(pt.getGuid(),jsonPortal);	
		}
		writer.println(jsonResponse.toString());
		writer.close(); 
			/*
		} else {
			resp.setStatus(400);
			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("error",  "null region");
			writer.println(jsonResponse.toString());
			writer.close(); 
		}
		*/
	}
	 catch (IOException e) {
		e.printStackTrace();
	} catch (Exception e) {
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
		//Logger.getLogger(PortalServlet.class.getName()).log(Level.SEVERE, null, e);

	} 
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp){
	doPost(req,resp);
  }
}

