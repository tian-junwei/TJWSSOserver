<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%
	// 输出cookies，过滤掉JSESSIONID
	Cookie[] cookies = request.getCookies();
	if(cookies != null)
		for(Cookie cookie : cookies) {
			if(cookie.getName().equals("JSESSIONID"))    continue;
			out.println(cookie.getName() + "-" + cookie.getValue());
		}
%>