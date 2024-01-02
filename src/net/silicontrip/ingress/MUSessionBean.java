package net.silicontrip.ingress;

import com.google.common.geometry.S2CellId;
import net.silicontrip.UniformDistribution;
import net.silicontrip.UniformDistributionException;

import java.util.HashMap;
import java.util.List;

import jakarta.annotation.PostConstruct;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Singleton
@LocalBean
public class MUSessionBean {  

	@PersistenceContext(unitName="net.silicontrip.ingress.persistence")
	private EntityManager em;

//	private HashMap<S2CellId,CellMUEntity> cells;

/*
	@PostConstruct
	void init() {
		System.out.println ("MUSessionBean:: init: " + em);
		Query queryAll = em.createNamedQuery("CellMUEntity.findAll");
		List<CellMUEntity> allCells = queryAll.getResultList();
		this.cells = new HashMap<>();
		int count=0;
		for (CellMUEntity cell : allCells)
		{
			count++;
			this.cells.put(S2CellId.fromToken(cell.getId()),cell);
		}
		System.out.println("MUSessionBean::init: items loaded: "+ count);
	}
*/
	
	public UniformDistribution getMU(S2CellId id)  { 
		CellMUEntity c = getMUEntity(id);
		if (c != null)
			return c.getDistribution(); 
		return null;
	}
	public CellMUEntity getMUEntity(S2CellId id)  { return em.find(CellMUEntity.class, id.toToken()); }
	
	public boolean refineMU(S2CellId id, UniformDistribution ud) throws UniformDistributionException {
		//System.out.println(">>> refineMU");
		CellMUEntity c = getMUEntity(id);
		boolean updated;
		if (c != null) 
		{
			//System.out.println ("" + id.toToken() + " managed: " + em.contains(c)); // tree, bark, right?

			updated =  c.refine(ud);
			if (updated)
				System.out.println(""+ id.toToken() + " -> " + c.getDistribution());
			//em.flush();
		}
		else
		{
			updated=true;
			createMU(id,ud);
		}

		return updated;
	}

	public void flush() { em.flush(); }

	// some sort of lock
	@Lock(LockType.WRITE)
	public void createMU (S2CellId id, UniformDistribution ud) throws UniformDistributionException {
		System.out.println(">>> createMU");
		
		CellMUEntity c = getMUEntity(id);
		if (c != null)
			c.refine(ud);
		else
		{
			CellMUEntity newMU = new CellMUEntity(id,ud);
			System.out.println("CREATE: " + id.toToken() + " -> " + ud);
			em.persist(newMU);
			//cells.put(id,newMU);
		}
		System.out.println("<<< createMU");

	}

 

}


