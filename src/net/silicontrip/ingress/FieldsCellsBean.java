/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.silicontrip.ingress;

import com.google.common.geometry.*;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;


import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author mark
 */
@Startup
@Singleton
@LocalBean
public class FieldsCellsBean {

	@EJB
	private SQLEntityDAO dao;

    @PersistenceContext(unitName="net.silicontrip.ingress.persistence")
	private EntityManager em;

	//private HashMap<S2CellId,HashSet<String>>fieldCellCache;

/*
	@PostConstruct
	public void init() {
		//fieldCellCache = new HashMap<S2CellId,HashSet<String>>();
		// foreach all fields
		int fieldCount = 0;
		try {
			dao.deleteFieldGuidCellsAll();
			//ArrayList<Field>allFields = dao.getFieldAll();
			Query queryAll = em.createNamedQuery("Mufields.findAll");
			Collection<Mufields> allFields = queryAll.getResultList();
			for (Mufields field : allFields) {


			//    getCells for field
				S2CellUnion fieldCells = field.getCells();
					//for (S2CellId cellid : fieldCells)
					//{
					//	insertCellsForField(field.getGuid(), cellid);
					//}
				dao.insertCellsForField(field.getGuid(),fieldCells);
				fieldCount++;
			if (fieldCount % 1000 == 0) 
				Logger.getLogger(FieldsCellsBean.class.getName()).log(Level.INFO, "Initialising. " + fieldCount +" fields.");
			}
			//int cellSize = fieldCellCache.size();
			//int fieldSize = allFields.size();
			Logger.getLogger(FieldsCellsBean.class.getName()).log(Level.INFO, "Initialised. " + fieldCount +" fields.");


		} catch (EntityDAOException e) {
			Logger.getLogger(FieldsCellsBean.class.getName()).log(Level.SEVERE, null, e);
		}
	}
	*/
	public ArrayList<String> fieldGuidsForCell(S2CellId cell)  {

		try {
			return dao.fieldGuidsForCell(cell);
		} catch (EntityDAOException e) {
			Logger.getLogger(FieldsCellsBean.class.getName()).log(Level.SEVERE, null, e);
		}
		return new ArrayList<String>();
	}

// wondering if I should get the field and calculate the cells myself
	@Lock(LockType.WRITE)
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) 
	public void insertCellsForField(String guid, S2CellUnion cells) {
		// foreach cells
		try {
			dao.deleteFieldGuidCells(guid);
			dao.insertCellsForField(guid,cells);
		} catch (EntityDAOException e) {
			Logger.getLogger(FieldsCellsBean.class.getName()).log(Level.SEVERE, null, e);
		}

	}
	
}
