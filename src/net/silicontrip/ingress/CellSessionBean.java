/**
* The CellSessionBean contains all code for working with fields and cells
*
* @author  Silicon Tripper
* @version 1.0
* @since   2019-03-31
*/
package net.silicontrip.ingress;

import com.google.common.geometry.*;

import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import net.silicontrip.UniformDistribution;

@Stateless
public class CellSessionBean {

	@PersistenceContext(unitName="net.silicontrip.ingress.persistence")
	EntityManager em;


	@EJB
	private MUSessionBean muBean;
	
	//public void createCellsForField (S2Polygon field)
	public void createCellsForField (Field field)
	{
		S2CellUnion cells = field.getCells();
		for (S2CellId cellid : cells)
		{
			System.out.println("CHECK FOR: " + cellid.toToken());
			CellMUEntity cmu = em.find(CellMUEntity.class, cellid.toToken());
			if (cmu == null)
			{
				try {
					cmu = new CellMUEntity(cellid.toToken());
					em.persist(cmu);
					em.flush();
				} catch (PersistenceException e) {
					// dc;dn  don't care, do nothing.
				}
			}
		}
	}
	
}
