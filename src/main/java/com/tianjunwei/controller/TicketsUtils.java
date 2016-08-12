package com.tianjunwei.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tianjunwei.bean.Ticket;

public class TicketsUtils {
	/** 单点登录标记 */
	public static Map<String, Ticket> tickets = new ConcurrentHashMap<String, Ticket>();
	
	/** 密钥 */
	public static String secretKey;
	
	public static String cookieName;
	
	public static int ticketTimeout;
	
	public static boolean secure;
	
}
