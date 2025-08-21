/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.silicontrip.ingress;

import jakarta.ejb.Singleton;
import java.util.HashSet;


/**
 *
 * @author mark
 */
@Singleton
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
	
	public boolean hasFieldGuid(String guid)
	{
		return fieldCache.contains(guid);
	}
	
	// I should've just made the HashSet Public (or a public getter)
	public boolean isEmpty()
	{
		return fieldCache.isEmpty();
	}
	
	public String nextFieldGuid()
	{
		if (fieldCache.iterator().hasNext())
			return fieldCache.iterator().next();
		return null;
	}
	
	public int size()
	{
		return fieldCache.size();
	}
}
