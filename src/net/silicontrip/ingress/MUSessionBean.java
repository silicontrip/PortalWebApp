package net.silicontrip.ingress;

import com.google.common.geometry.S2CellId;
import net.silicontrip.UniformDistribution;
import net.silicontrip.UniformDistributionException;

import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Lock;
import javax.ejb.LockType;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Startup
@Singleton
public class MUSessionBean {  

	@PersistenceContext(unitName="net.silicontrip.ingress.persistence")
	private EntityManager em;

	private HashMap<S2CellId,CellMUEntity> cells;

	@PostConstruct
	void init() {
		System.out.println ("MUSessionBean:: init");
		Query queryAll = em.createNamedQuery("CellMUEntity.findAll");
		List<CellMUEntity> allCells = queryAll.getResultList();
		this.cells = new HashMap<>();
		for (CellMUEntity cell : allCells)
			this.cells.put(S2CellId.fromToken(cell.getId()),cell);

	}
	
	public UniformDistribution getMU(S2CellId id)  { 
		if (cells.get(id) != null)
			return cells.get(id).getDistribution(); 
		return null;
	}
	public CellMUEntity getMUEntity(S2CellId id)  { return cells.get(id); }
	
	public boolean refineMU(S2CellId id, UniformDistribution ud) throws UniformDistributionException {
		//System.out.println(">>> refineMU");
		boolean updated = false;
		if (cells.containsKey(id)) 
		{
			updated =  cells.get(id).refine(ud);
			em.flush();
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
		if (cells.containsKey(id))
			cells.get(id).refine(ud);
		else
        {
			CellMUEntity newMU = new CellMUEntity(id,ud);
			em.persist(newMU);
			cells.put(id,newMU);
		}
		System.out.println("<<< createMU");

	}

 

}


