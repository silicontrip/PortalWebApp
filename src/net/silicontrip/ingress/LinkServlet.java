package net.silicontrip.ingress;

import org.json.*;
import com.google.common.geometry.*;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.*;
import java.sql.*;
import javax.sql.*;
import javax.naming.*;

import jakarta.ejb.EJB;

public class LinkServlet extends HttpServlet {

	@EJB
	private EntitySessionBean entBean;
	
	public void init () throws ServletException {
	}
	
	public void destroy () {
		;
	}

	private S2LatLng fromE6String(String s)
	{
		if (s==null)
			return null;
		if (s.matches("(\\+|-)?([0-9]+(\\.[0-9]+)),(\\+|-)?([0-9]+(\\.[0-9]+))"))
		{
			String[] ll = s.split(",");
			Double lat = Double.parseDouble(ll[0]);
			Double lng = Double.parseDouble(ll[1]);
			//long latE6 = Math.round(lat * 1000000);
			//long lngE6 = Math.round(lng * 1000000);  
			return S2LatLng.fromDegrees(lat,lng);
		}
		return null;
	}

  public void doPost(HttpServletRequest req, HttpServletResponse resp){
	try {
		resp.setContentType("text/json");
		resp.setCharacterEncoding("UTF-8");
		resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");
		PrintWriter writer = resp.getWriter();

		S2LatLng p1 =  entBean.getPortalLocation(req.getParameter("ll"));
		S2LatLng p2 = fromE6String(req.getParameter("l2"));
                String pr =  req.getParameter("rr");

                S2LatLngRect searchRegion = null;

                if (pr != null && p1 != null)
                {
                        double radius = Double.parseDouble(pr) ;
                        S1Angle rangeAngle =  S1Angle.radians(radius / 6367.0);

			// can't have a point area...
                        //if (radius<0.0001) radius=0.0001; // because 0 doesn't work
			// has to be a rectangle.
                        searchRegion = (S2Cap.fromAxisAngle(p1.toPoint(),rangeAngle)).getRectBound();
                }

	//	if (p1 != null && p2 != null)
	//		System.out.println("query: " + req.getQueryString() +" Search area: " + p1.toStringDegrees() + " - " +p2.toStringDegrees());
		

		if (p1 !=null && p2 !=null)
		{
			searchRegion = S2LatLngRect.fromPointPair(p1,p2);
			System.out.println("<- links: " + searchRegion);
		}

		Collection<Link> linkList; // no not that kind of link list

		if (searchRegion != null) 
			linkList = entBean.getLinkInRect(searchRegion);
		else
			linkList = entBean.getLinkAll();
			
		JSONObject jsonResponse = new JSONObject();
		for (Link li : linkList)
		{
			JSONObject jsonLink = new JSONObject();
			jsonLink.put("guid",li.getGuid());
			jsonLink.put("team",li.getTeam());
			jsonLink.put("dguid",li.getdGuid());
			jsonLink.put("dlat",li.getdLatE6()); // would love to move these to the E6 naming
			jsonLink.put("dlng",li.getdLngE6()); // E6

			jsonLink.put("oguid",li.getoGuid());
			jsonLink.put("olat",li.getoLatE6()); // would love to move these to the E6 naming
			jsonLink.put("olng",li.getoLngE6()); // E6
			jsonResponse.put(li.getGuid(),jsonLink);	
		}
		writer.println(jsonResponse.toString());
		writer.close(); 
		
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
			e2.printStackTrace();
		}
	}  
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp){
	doPost(req,resp);
  }
}

