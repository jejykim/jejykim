<%@page import="com.decision.v2x.era.VO.DeviceVO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c"      uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form"   uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ui"     uri="http://egovframework.gov/ctl/ui"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title><spring:message code="title.devices" /></title>
	<link rel="stylesheet" href="../css/contents.css" >
	<jsp:include page="../include/common.jsp"/>
	<script type="text/javascript" src="../js/devices/deviceList.js"></script>
	
	<script>
		$(function(){
			
			var errMsg = '<%=session.getAttribute("errMsg") %>';
			if(errMsg !== 'null')
				alert(errMsg);
			
			
			var succMsg = '<%=session.getAttribute("succMsg") %>';
			if(succMsg !== 'null')
				alert(succMsg);
			

			<%
				session.removeAttribute("errMsg");
				session.removeAttribute("succMsg");
			%>
		});
	</script>
</head>
<body>
<!-- header start -->
<jsp:include page="../include/header.jsp"/>
<!-- header end -->

<div class="section">
	<div class="wrap">
		<strong class="sub_title"><spring:message code="title.devices" /></strong>
		<form name="searchForm" action="search.do" method="post">
		<table class="sub_table sub_table02">
			<tr>
				<th><spring:message code="search.regDate" /></th>
				<td><input type="text" id="daterangepicker"/></td>
				<input type="hidden" value="${searchStartDate }" name="start_time" id="start_time"/>
				<input type="hidden" value="${searchEndDate }" name="end_time" id="end_time"/>
				<th>단말 타입</th>
				<td>
					<select name="device_type">
						<option value=""><spring:message code="select.all" /></option>
						<c:forEach var="result" items="${deviceTypeList}" varStatus="status">
							<c:choose>
								<c:when test="${deviceVO.device_type eq result.codeId }">
									<option value="${result.codeId }:${result.codeGroupId}" selected="selected">${result.codeDescription }</option>
								</c:when>
								<c:otherwise>
									<option value="${result.codeId }:${result.codeGroupId}">${result.codeDescription }</option>
								</c:otherwise>
							</c:choose>
						</c:forEach>
					</select>
				</td>
				<td rowspan="3">
					<input type="submit" class="btn" id="btnSearch" value="<spring:message code='button.search' />">
				</td>
			</tr>
			<tr>
				<th>모델명</th>
				<td><input type="text" name="device_name" value="${deviceVO.device_name }"></td>
				<th>단말 고유번호 구분</th>
				<td>
					<select name="device_id_type_group">
						<option value=""><spring:message code="select.all" /></option>
						<c:forEach var="result" items="${deviceIdTypeList}" varStatus="status">
							<c:choose>
								<c:when test="${deviceVO.device_group_id eq result.codeId }">
									<option value="${result.codeId }:${result.codeGroupId}" selected="selected">${result.codeName }</option>
								</c:when>
								<c:otherwise>
									<option value="${result.codeId }:${result.codeGroupId}">${result.codeName }</option>
								</c:otherwise>
							</c:choose>
						</c:forEach>
					</select>
				</td>
			</tr>
			<tr>
				<th>단말 고유번호</th>
				<td colspan="3"><input type="text" name="device_sn" value="${deviceVO.device_sn }"></td>
			</tr>
		</table>

		<ul class="board_top">
			<li><span class="number_span">Total : ${deviceListCount }</span></li>
			<input type="hidden" id="totalCount" value="${deviceListCount }"/>
			<li>
			<%DeviceVO vo = (DeviceVO)request.getAttribute("deviceVO");%>
				<select name="page_per_data" id="page_per_data">
					<option value="10" <%=vo.getPage_per_data() == 10 ? "selected='selected'" : "" %>>10 <spring:message code="search.row"/></option>
					<option value="20" <%=vo.getPage_per_data() == 20 ? "selected='selected'" : "" %>>20 <spring:message code="search.row"/></option>
					<option value="50" <%=vo.getPage_per_data() == 50 ? "selected='selected'" : "" %>>50 <spring:message code="search.row"/></option>
					<option value="100" <%=vo.getPage_per_data() == 100 ? "selected='selected'" : "" %>>100 <spring:message code="search.row"/></option>
					<option value="1000" <%=vo.getPage_per_data() == 1000 ? "selected='selected'" : "" %>>1000 <spring:message code="search.row"/></option>
				</select>
				<button type="button" id="btnDeleteDevice" class="btn" style="background-color: red;">단말 삭제</button>
				<button type="button" class="btn" id="btnCreateEnrol">등록인증서 생성</button>
				<a href="add.do" class="btn"><spring:message code="button.device.add" /></a>
			</li>
		</ul>
		
		<input type="hidden" name="now_page" id="now_page" value="1"/>
		</form>
		
		<table class="sub_table">
			<thead>
				<tr>
					<th width="5%"><input type="checkbox" class="check_all"/></th>
					<th width="8%">단말 타입</th>
					<th width="10%">단말 고유번호 구분</th>
					<th width="20%">단말 고유번호</th>
					<th width="15%">제조사</th>
					<th>모델명</th>
					<th width="15%"><spring:message code="table.title.regDate" /></th>
					<th>등록자 ID</th>
					<th>인증서 발급 여부</th>
				</tr>
			</thead>
			
			<tbody>
				<c:forEach var="list" items="${deviceList }" varStatus="status">
	       			<tr style="cursor: pointer;">
	       				<td><input type="checkbox" id="cbDevice${list.get('deviceId')}" name="cbDevice" class="cbDevice" value="${list.get('deviceId')}" /></td>
						<td onclick="deviceList.DeviceDetail(${list.get('deviceId')})">${list.get("deviceTypeName") }</td>
	       				<td onclick="deviceList.DeviceDetail(${list.get('deviceId')})">${list.get("deviceIdTypeName") }</td>
						<td onclick="deviceList.DeviceDetail(${list.get('deviceId')})">${list.get("deviceSn") }</td>
						<td onclick="deviceList.DeviceDetail(${list.get('deviceId')})">${list.get("deviceMaker") }</td>
						<td onclick="deviceList.DeviceDetail(${list.get('deviceId')})">${list.get("deviceName") }</td>
						<td onclick="deviceList.DeviceDetail(${list.get('deviceId')})">${list.get("createDate") }</td>
						<td onclick="deviceList.DeviceDetail(${list.get('deviceId')})">${list.get("createId") }</td>
						<td>${list.certCondition }</td>
	       			</tr>
	   			</c:forEach>
   			</tbody>
			
   			<c:if test="${deviceListCount eq 0}">
   				<tr>
       				<td colspan="9">
       					<spring:message code="search.noResult" />
       				</td>
       			</tr>
   			</c:if>
		</table>

		<c:if test="${deviceListCount != 0}">
			<div class="webtong-paging">
				<a class="first" id="first_page" onclick="deviceList.Paging(${pagingVO.start_page })">first page</a>
				<a class="previous" id="pre_page" onclick="deviceList.Paging(${pagingVO.pre_page })">previous page</a>
				<span class="numbering">
					<c:forEach var="list" varStatus="status" begin="${pagingVO.first_page }" end="${pagingVO.last_page }">
						<c:choose>
								<c:when test="${pagingVO.now_page eq list }">
									<em>${list }</em>
								</c:when>
								<c:otherwise>
									<a onclick="deviceList.Paging(${list })">${list }</a>
								</c:otherwise>
							</c:choose>
					</c:forEach>
				</span>
				<a class="next" id="next_page" onclick="deviceList.Paging(${pagingVO.next_page })">next page</a>
				<a class="last" id="last_page" onclick="deviceList.Paging(${pagingVO.end_page })">last page</a>
			</div>
		</c:if>
		
	</div>
</div>

<!-- modal start -->
<jsp:include page="../include/modal.jsp"/>
<!-- modal end -->

</body>
</html>