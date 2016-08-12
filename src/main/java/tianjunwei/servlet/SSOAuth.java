package tianjunwei.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tianjunwei.RecoverTicket;
import tianjunwei.bean.Ticket;
import tianjunwei.util.DESUtils;

/**
 * Servlet implementation class SSOAuth
 */
public class SSOAuth extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    
	/** 账户信息 */
	private static Map<String, String> accounts;
	
	/** 单点登录标记 */
	private static Map<String, Ticket> tickets;
	
	/** cookie名称 */
	private String cookieName;
	
	/** 是否安全协议 */
	private boolean secure;
	
	/** 密钥 */
	private String secretKey;
	
	/** ticket有效时间 */
	private int ticketTimeout;
	
	/** 回收ticket线程池 */
	private ScheduledExecutorService schedulePool;
	
	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		accounts = new HashMap<String, String>();
		accounts.put("admin", "admin");
		accounts.put("tian", "tian");
		
		tickets = new ConcurrentHashMap<String, Ticket>();
		
		cookieName = config.getInitParameter("cookieName");
		secure = Boolean.parseBoolean(config.getInitParameter("secure"));
		secretKey = config.getInitParameter("secretKey");
		ticketTimeout = Integer.parseInt(config.getInitParameter("ticketTimeout"));
		
		schedulePool = Executors.newScheduledThreadPool(1);
		schedulePool.scheduleAtFixedRate(new RecoverTicket(tickets), ticketTimeout * 60, 1, TimeUnit.MINUTES);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		String action = request.getParameter("action");
		if("preLogin".equals(action)) {
			preLogin(request, response);
		} else if("login".equals(action)) {
			doLogin(request, response);
		} else if("logout".equals(action)) {
			doLogout(request, response);
		} else if("authTicket".equals(action)) {
			authTicket(request, response);
		} else {
			System.err.println("指令错误");
			out.print("Action can not be empty！");
		}
		out.close();
	}

	@Override
	public void destroy() {
		if(schedulePool != null)    schedulePool.shutdown();
	}

	private void preLogin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Cookie ticket = null;
		Cookie[] cookies = request.getCookies();
		if(cookies != null)
			for(Cookie cookie : cookies) {
				if(cookie.getName().equals(cookieName)) {
					ticket = cookie;
					break;
				}
			}
		if(ticket == null) {
			request.getRequestDispatcher("login.jsp").forward(request, response);
		} else {
			String encodedTicket = ticket.getValue();
			String decodedTicket = DESUtils.decrypt(encodedTicket, secretKey);
			if(tickets.containsKey(decodedTicket)) {
				String setCookieURL = request.getParameter("setCookieURL");
				String gotoURL = request.getParameter("gotoURL");
				if(setCookieURL != null)
	                response.sendRedirect(setCookieURL + "?ticket=" + encodedTicket + "&expiry=" + ticket.getMaxAge() + "&gotoURL=" + gotoURL);
			} else {
				request.getRequestDispatcher("login.jsp").forward(request, response);
			}
		}
	}

	private void authTicket(HttpServletRequest request, HttpServletResponse response) throws IOException {
		StringBuilder result = new StringBuilder("{");
		PrintWriter out = response.getWriter();
		String encodedTicket = request.getParameter("cookieName");
		if(encodedTicket == null) {
			result.append("\"error\":true,\"errorInfo\":\"Ticket can not be empty!\"");
		} else {
			String decodedTicket = DESUtils.decrypt(encodedTicket, secretKey);
			if(tickets.containsKey(decodedTicket))
				result.append("\"error\":false,\"username\":").append(tickets.get(decodedTicket).getUsername());
			else
				result.append("\"error\":true,\"errorInfo\":\"Ticket is not found!\"");
		}
		result.append("}");
		out.print(result);
	}

	private void doLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		StringBuilder result = new StringBuilder("{");
		PrintWriter out = response.getWriter();
		String encodedTicket = request.getParameter("cookieName");
		if(encodedTicket == null) {
			result.append("\"error\":true,\"errorInfo\":\"Ticket can not be empty!\"");
		} else {
			String decodedTicket = DESUtils.decrypt(encodedTicket, secretKey);
			tickets.remove(decodedTicket);
			result.append("\"error\":false");
		}
		result.append("}");
		out.print(result);
	}

	private void doLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		String pass = accounts.get(username);
		if(pass == null || !pass.equals(password)) {
			request.getRequestDispatcher("login.jsp?errorInfo=username or password is wrong!").forward(request, response);
		} else {
			String ticketKey = UUID.randomUUID().toString().replace("-", "");
			String encodedticketKey = DESUtils.encrypt(ticketKey, secretKey);
			
			Timestamp createTime = new Timestamp(System.currentTimeMillis());
			Calendar cal = Calendar.getInstance();
			cal.setTime(createTime);
			cal.add(Calendar.MINUTE, ticketTimeout);
			Timestamp recoverTime = new Timestamp(cal.getTimeInMillis());
			Ticket ticket = new Ticket(username, createTime, recoverTime);
			
			tickets.put(ticketKey, ticket);

			String[] checks = request.getParameterValues("autoAuth");
			int expiry = -1;
			if(checks != null && "1".equals(checks[0]))
				expiry = 7 * 24 * 3600;
			Cookie cookie = new Cookie(cookieName, encodedticketKey);
			cookie.setSecure(secure);// 为true时用于https
			cookie.setMaxAge(expiry);
			cookie.setPath("/");
			response.addCookie(cookie);

			String setCookieURL = request.getParameter("setCookieURL");
			String gotoURL = request.getParameter("gotoURL");
			
			PrintWriter out = response.getWriter();
			out.print("<script type='text/javascript'>");
			out.print("document.write(\"<form id='url' method='post' action='" + setCookieURL + "'>\");");
			out.print("document.write(\"<input type='hidden' name='gotoURL' value='" + gotoURL + "' />\");");
			out.print("document.write(\"<input type='hidden' name='ticket' value='" + encodedticketKey + "' />\");");
			out.print("document.write(\"<input type='hidden' name='expiry' value='" + expiry + "' />\");");
			out.print("document.write('</form>');");
			out.print("document.getElementById('url').submit();");
			out.print("</script>");
		}
	}

}
