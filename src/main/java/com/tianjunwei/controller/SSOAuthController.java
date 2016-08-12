package com.tianjunwei.controller;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tianjunwei.bean.Ticket;
import com.tianjunwei.util.DESUtils;

@Controller
@RequestMapping("/sso")
public class SSOAuthController {
	
	@RequestMapping("/preLogin")
	public String preLogin(HttpServletRequest request,HttpServletResponse response){
		Cookie ticket = null;
		Cookie[] cookies = request.getCookies();
		if(cookies != null)
			for(Cookie cookie : cookies) {
				if(cookie.getName().equals(TicketsUtils.cookieName)) {
					ticket = cookie;
					break;
				}
			}
		if(ticket == null) {
			//request.getRequestDispatcher("login.jsp").forward(request, response);
			return "login";
		} else {
			String encodedTicket = ticket.getValue();
			String decodedTicket = DESUtils.decrypt(encodedTicket, TicketsUtils.secretKey);
			if(TicketsUtils.tickets.containsKey(decodedTicket)) {
				String setCookieURL = request.getParameter("setCookieURL");
				String gotoURL = request.getParameter("gotoURL");
				if(setCookieURL != null)
					return "redirect:"+(setCookieURL + "?ticket=" + encodedTicket + "&expiry=" + ticket.getMaxAge() + "&gotoURL=" + gotoURL);
			} 
		}
		return "login";
	}
	
	@RequestMapping("/authTicket")
	@ResponseBody
	public String authTicket(HttpServletRequest request){
		StringBuilder result = new StringBuilder("{");
		String encodedTicket = request.getParameter("cookieName");
		if(encodedTicket == null) {
			result.append("\"error\":true,\"errorInfo\":\"Ticket can not be empty!\"");
		} else {
			String decodedTicket = DESUtils.decrypt(encodedTicket, TicketsUtils.secretKey);
			if(TicketsUtils.tickets.containsKey(decodedTicket))
				result.append("\"error\":false,\"username\":").append(TicketsUtils.tickets.get(decodedTicket).getUsername());
			else
				result.append("\"error\":true,\"errorInfo\":\"Ticket is not found!\"");
		}
		result.append("}");
		return result.toString();
	}
	
	
	@RequestMapping("/login")
	public String login(HttpServletRequest request, HttpServletResponse response,Model model){
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		if(username == "admin"){
			String ticketKey = UUID.randomUUID().toString().replace("-", "");
			String encodedticketKey = DESUtils.encrypt(ticketKey, TicketsUtils.secretKey);
			
			Timestamp createTime = new Timestamp(System.currentTimeMillis());
			Calendar cal = Calendar.getInstance();
			cal.setTime(createTime);
			cal.add(Calendar.MINUTE, TicketsUtils.ticketTimeout);
			Timestamp recoverTime = new Timestamp(cal.getTimeInMillis());
			Ticket ticket = new Ticket(username, createTime, recoverTime);
			
			TicketsUtils.tickets.put(ticketKey, ticket);

			String[] checks = request.getParameterValues("autoAuth");
			int expiry = -1;
			if(checks != null && "1".equals(checks[0]))
				expiry = 7 * 24 * 3600;
			Cookie cookie = new Cookie(TicketsUtils.cookieName, encodedticketKey);
			cookie.setSecure(TicketsUtils.secure);// 为true时用于https
			cookie.setMaxAge(expiry);
			cookie.setPath("/");
			response.addCookie(cookie);

			String setCookieURL = request.getParameter("setCookieURL");
			String gotoURL = request.getParameter("gotoURL");
			
			return "redirect:"+setCookieURL+"?gotoURL="+gotoURL+"&ticket="+encodedticketKey+"&expiry="+expiry;
			
		}else {
			model.addAttribute("errorInfo", "message.login.error");
			return "loginerror";
		}
	}
	
	@RequestMapping("/logout")
	@ResponseBody
	public String logout(HttpServletRequest request){
		StringBuilder result = new StringBuilder("{");
		String encodedTicket = request.getParameter("cookieName");
		if(encodedTicket == null) {
			result.append("\"error\":true,\"errorInfo\":\"Ticket can not be empty!\"");
		} else {
			String decodedTicket = DESUtils.decrypt(encodedTicket, TicketsUtils.secretKey);
			TicketsUtils.tickets.remove(decodedTicket);
			result.append("\"error\":false");
		}
		result.append("}");
		
		return result.toString();
	}
}
