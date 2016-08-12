package com.tianjunwei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tianjunwei.bean.Ticket;

public class RecoverTicket implements Runnable {
	
	private Map<String, Ticket> tickets;
	
	public RecoverTicket(Map<String, Ticket> tickets) {
		super();
		this.tickets = tickets;
	}

	@Override
	public void run() {
		List<String> ticketKeys = new ArrayList<String>();
		for(Entry<String, Ticket> entry : tickets.entrySet()) {
			if(entry.getValue().getRecoverTime().getTime() < System.currentTimeMillis())
				ticketKeys.add(entry.getKey());
		}
		for(String ticketKey : ticketKeys) {
			tickets.remove(ticketKey);
			System.out.println("ticket[" + ticketKey + "]过期已删除！");
		}
	}

}
