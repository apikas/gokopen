<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
	<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<jsp:include page="include_metadata.jsp" flush="false"></jsp:include>
<title>Rapportera poäng</title>
</head>
<body>
<c:if test="${not empty oldPatr }">
<div class="statusrow">
Sparat ${oldScore.scorePoint} + ${oldScore.stylePoint} poäng till ${oldPatr.patrolName }.
</div>
</c:if>
<c:if test="${not empty errormsg }">
<div class="errorblock">
${errormsg}
</div>
</c:if>
	<h1>Rapportera poäng</h1>
	<c:if test="${empty score.station }">
	<form:form commandName="score" method="post" action="${pageContext.request.contextPath}/score/selectstation" cssClass="form-general">
		<form:hidden path="scoreId" id="scoreId" />
		<div class="form-box">
		<fieldset>
			<label for="station">Kontroll:</label>
			<form:select path="station" id="station">
				<option value="-1">-- Välj kontroll --</option>
				<form:options items="${stations}" itemLabel="stationName" />
			</form:select>
		</fieldset>
		</div>
		<div class="submit-area">
		<input type="submit" name="saveStation" value="Fortsätt"/> | <a href="${pageContext.request.contextPath}/">Avbryt</a>
		</div>
		
	</form:form>
</c:if>
<c:if test="${not empty score.station }">
	<form:form commandName="score" method="post" action="${pageContext.request.contextPath}/score/savescore" cssClass="form-general">
	<form:hidden path="scoreId" id="scoreId" />
	<fieldset>
Vald kontroll: ${score.station.stationName }
<form:hidden path="station.stationId" id="station.stationId" />
<div class="form-box">
<label for="patrol">Patrull:</label>
<form:select path="patrol" id="patrol">
<option value="-1">-- Välj patrull --</option>
<form:options items="${patrols}" itemLabel="patrolInfo"/>
</form:select>
</div>
</fieldset>
<div class="form-box">
<fieldset>
<label for="scorePoint">Poäng: </label>
<form:select path="scorePoint" id="scorePoint">
<c:forEach var="j" begin="${score.station.minScore}" end="${score.station.maxScore}">
		<c:if test="${score.scorePoint==j }"><option selected="selected" value="${j}">${j}</option></c:if>
		<c:if test="${score.scorePoint!=j }"><option value="${j}">${j}</option></c:if>
</c:forEach>
</form:select>
</fieldset>
</div>
<div class="form-box">
<fieldset>
<label for="stylePoint">Stilpoäng: </label>
<form:select path="stylePoint" id="stylePoint">
	<c:forEach var="i" begin="${score.station.minStyleScore}" end="${score.station.maxStyleScore}">
		<c:if test="${score.stylePoint==i }"><option selected="selected" value="${i}">${i}</option></c:if>
		<c:if test="${score.stylePoint!=i }"><option value="${i}">${i}</option></c:if>
	</c:forEach>
</form:select>
</fieldset>
</div>
<div class="submit-area">
<input type="submit" name="saveScore" value="Spara"/> | <a href="${pageContext.request.contextPath}/">Avbryt</a>
</div>
</form:form>
</c:if>
</body>
</html>