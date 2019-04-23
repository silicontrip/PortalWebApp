/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.silicontrip.ingress;

import javax.ejb.Singleton;
import javax.ejb.LocalBean;
import java.util.HashSet;


/**
 *
 * @author mark
 */
@Singleton
@LocalBean
public class FieldProcessCache {

	private HashSet<String>fieldCache;

	public FieldProcessCache() 
	{ 
		fieldCache = new HashSet<String>();
	}
	
	public void addFieldGuid(String guid)
	{
		fieldCache.add(guid);
	}
	public void removeFieldGuid(String guid)
	{
		fieldCache.remove(guid);
	}
	
	public String nextFieldGuid()
	{
		if (fieldCache.iterator().hasNext())
			return fieldCache.iterator().next();
		return null;
	}
	
}
