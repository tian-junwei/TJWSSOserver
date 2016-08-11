<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%
	Cookie cookie = new Cookie("test_key", "test_value");
	cookie.setPath("/");
	cookie.setDomain(".ghsau.com");
	response.addCookie(cookie);
%>