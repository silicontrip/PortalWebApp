package net.silicontrip.ingress;


import javax.jms.*;
import javax.ejb.*;

import com.google.common.geometry.S2CellId;

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

		@Override
		public void onMessage(Message message) {
			TextMessage textMessage = (TextMessage) message;
			System.out.println("Queue: " + textMessage.toString());
			S2CellId pcell = S2CellId.fromToken(textMessage.toString());
			fieldBean.processCell(pcell);
	}
}
