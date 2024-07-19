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
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.logging.Level;
import java.util.logging.Logger;

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
	
	public UniformDistribution getMU(String tok)  { 
		CellMUEntity c = getMUEntity(tok);
		if (c != null)
			return c.getDistribution(); 
		return null;
	}
	public CellMUEntity getMUEntity(String tok)  { return em.find(CellMUEntity.class, tok); }
	public void deleteMUEntity(CellMUEntity cell) { em.remove(cell); }

	public void deleteMU(String s) {
		CellMUEntity cell = em.find(CellMUEntity.class, s);
		//em.getTransaction().begin();
		if (cell != null)
			em.remove(cell);
		//em.getTransaction().commit();

	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) 
	public boolean refineMU(String tok, UniformDistribution ud) throws UniformDistributionException {
		//System.out.println(">>> refineMU");
		CellMUEntity c = getMUEntity(tok);
		boolean updated;
		if (c != null) 
		{
			//System.out.println ("" + id.toToken() + " managed: " + em.contains(c)); // tree, bark, right?

			updated =  c.refine(ud);
			/*
			if (updated) {
				System.out.println(""+ id.toToken() + " -> " + c.getDistribution());
				Logger.getLogger(MUSessionBean.class.getName()).log(Level.INFO, "UPDATE: "+ id.toToken() + " -> " + c.getDistribution());

			}
			*/
			//em.flush();
		}
		else
		{
			updated=true;
			createMU(tok,ud);
		}

		return updated;
	}

	public void flush() { em.flush(); }

	// some sort of lock
	@Lock(LockType.WRITE)
	public void createMU (String tok, UniformDistribution ud) throws UniformDistributionException {
		//System.out.println(">>> createMU");
		
		CellMUEntity c = getMUEntity(tok);
		if (c != null)
			c.refine(ud);
		else
		{
			CellMUEntity newMU = new CellMUEntity(tok,ud);
			//System.out.println("CREATE: " + id.toToken() + " -> " + ud);
			em.persist(newMU);
			//cells.put(id,newMU);
		}
		//System.out.println("<<< createMU");

	}

 

}


