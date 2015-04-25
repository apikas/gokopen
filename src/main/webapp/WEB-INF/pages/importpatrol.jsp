<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<jsp:include page="include_metadata.jsp" flush="false"></jsp:include>
<title>Importera patruller</title>
</head>
<body>
	<div class="nav-box">
		<a href="${pageContext.request.contextPath}/">Tillbaka</a>
	</div>
	<h1>Importera patruller</h1>
	Välj en fil med patruller i CSV-format. När filen lästs så visas en
	lista över patruller som kan importeras. Filen kan ha någon av
	teckenkodningarna UTF8, ISO8859-1 (Windows, Latin-1), MacRoman, och
	många fler. Såväl kommatecken som semikolon kan användas som
	kolumnseparator.
	<br />
	<c:if test="${not empty importpatrolmodel.fieldDescriptions }">
		Första raden i filen ska innehålla rubriker, t.ex. följande:<br />
		<div class="code">Kår, Tävling, Patrull, Antal scouter,
			Kontaktperson, E-post, Mobilnr, Åtgärd, Kommentar</div>
		<br />
		Följande rubriker känns igen:
		<div>
			<ul class="small-text">
				<c:forEach items="${importpatrolmodel.fieldDescriptions}"
					var="description" varStatus="status">
					<li>${description}</li>
				</c:forEach>
			</ul>
		</div>
	</c:if>
	Följande tävlingsklasser är definierade:
	<div>
		<ul class="small-text">
			<c:forEach items="${tracks }" var="track">
				<li>${track.trackName }</li>
			</c:forEach>
		</ul>
	</div>
	<form:form commandName="importpatrolmodel" method="post"
		action="${pageContext.request.contextPath}/admin/importpatrol"
		cssClass="form-general" enctype="multipart/form-data">
		<div class="form-box">
			Välj en fil med patruller i CVS-format att importera : <br />
			<form:input path="csvFile" type="file" name="file" id="file" />
			<div class="submit-area">
				<input type="submit" value="Läs fil" /> | <a
					href="${pageContext.request.contextPath}/">Avbryt</a>
			</div>
		</div>
	</form:form>
</body>
</html>