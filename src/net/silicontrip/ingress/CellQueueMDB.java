package net.silicontrip.ingress;


import javax.jms.*;
import javax.ejb.*;

import com.google.common.geometry.S2CellId;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(
mappedName = "jms/cellQueue",
activationConfig = { 
		@ActivationConfigProperty(
		  propertyName = "acknowledgeMode", 
		  propertyValue = "Auto-acknowledge"), 
		@ActivationConfigProperty(
		  propertyName = "destinationType", 
		  propertyValue = "javax.jms.Queue")
})

public class CellQueueMDB implements MessageListener {

		@EJB
		private FieldSessionBean fieldBean;

		@EJB
		private SQLEntityDAO dao;
		
		@Override
		public void onMessage(Message message) {
			TextMessage textMessage = (TextMessage) message;
			try {
				// System.out.println("Queue: " + textMessage.getText());
				//S2CellId pcell = S2CellId.fromToken(textMessage.getText());
				Field fi = dao.getField(textMessage.getText());
				fieldBean.processField(fi);

			} catch (JMSException ex) {
				Logger.getLogger(CellQueueMDB.class.getName()).log(Level.SEVERE, null, ex);
			}
	}
}
