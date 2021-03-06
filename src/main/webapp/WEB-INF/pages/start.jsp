<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<title>${config.name }</title>
<jsp:include page="include_metadata.jsp" flush="false"></jsp:include>
</head>
<body>

<div class="nav-box">
<c:if test="${not empty errormsg }">
<div class="errorblock">
${errormsg}
</div>
</c:if>
<h1>${config.name }</h1>
<ul>
<li class="nav-item"><a href="${pageContext.request.contextPath}/score/">Rapportera poäng</a></li>
<li class="nav-item"><a href="${pageContext.request.contextPath}/reports/patrols">Patruller</a></li>
<li class="nav-item"><a href="${pageContext.request.contextPath}/reports/bytrack">Resultat per klass</a></li>
<sec:authorize access="hasRole('ROLE_ADMIN')">
<li class="nav-item"><a href="${pageContext.request.contextPath}/print/start">Skriv ut listor</a></li>
<li class="nav-item"><a href="${pageContext.request.contextPath}/admin/">Administration</a></li>
</sec:authorize>
</ul>
Inloggad användare: ${username }
</div>
</body>
</html>