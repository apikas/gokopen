<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<jsp:include page="include_metadata.jsp" flush="false"></jsp:include>
<title>Spara importerade patruller</title>
</head>
<body>
	<div class="form-general">
		<div>
			<a href="${backurl}">Tillbaka</a>
		</div>
		<c:if test="${not empty savepatrolmodel.errormsg }">
			<div class="errorblock">${savepatrolmodel.errormsg}</div>
		</c:if>
		<c:if test="${not empty savepatrolmodel.info }">
			<h1>Importmeddelanden :</h1>
			<c:forEach items="${savepatrolmodel.info}" var="infotxt"
				varStatus="status">
				<div class="text">${infotxt}</div>
			</c:forEach>
		</c:if>
		<c:if test="${empty savepatrolmodel.errormsg }">
			<h1>Spara importerade patruller</h1>
			<form:form commandName="savepatrolmodel" method="post"
				action="${pageContext.request.contextPath}/admin/importpatrolsave"
				cssClass="form-general" enctype="multipart/form-data">
				<div class="form-box">
					Om en patrull med samma namn och kår redan finns så står det i
					importkommentaren, och då ersätts den gamla. <br /> Markera
					patruller som ska importeras: <br />
					<table>
						<tr>
							<th>Impor-<br />tera
							</th>
							<th>Åtgärd</th>
							<th>Importkommentar</th>
							<th>Kår</th>
							<th>Patrullnamn</th>
							<th>Klass</th>
							<th>Kommentar</th>
						</tr>

						<c:forEach items="${savepatrolmodel.patrols}" var="patrol"
							varStatus="status">
							<c:set var='trclass' value=''></c:set>
							<c:choose>
								<c:when test="${(status.index)%2 eq 1}">
									<c:set var='trclass' value='class="odd"'></c:set>
								</c:when>
							</c:choose>
							<tr ${trclass}>
								<form:hidden path="patrols[${status.index}].patrolName" />
								<form:hidden path="patrols[${status.index}].troop" />
								<form:hidden path="patrols[${status.index}].trackName" />
								<form:hidden path="patrols[${status.index}].leaderContact" />
								<form:hidden path="patrols[${status.index}].note" />
								<td><form:checkbox
										path="patrols[${status.index}].shouldImport" /></td>
								<td>${patrol.action }</td>
								<td>${patrol.importComment }</td>
								<td>${patrol.troop }</td>
								<td>${patrol.patrolName }</td>
								<td>${patrol.trackName }</td>
								<td>${patrol.note }</td>
							</tr>
						</c:forEach>
					</table>
					<div class="submit-area">
						<input type="submit" value="Importera" /> | <a href="${backurl}">Avbryt</a>
					</div>
				</div>
			</form:form>
		</c:if>
	</div>
</body>
</html>