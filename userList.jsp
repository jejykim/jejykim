<%@page import="com.decision.v2x.era.VO.UserVO"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c"      uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form"   uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ui"     uri="http://egovframework.gov/ctl/ui"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt"	uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title><spring:message code="title.users" /></title>
<link rel="stylesheet" href="../css/contents.css" >
<jsp:include page="../include/common.jsp"/>
<script type="text/javascript" src="../js/users/userList.js"></script>

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
		<strong class="sub_title"><spring:message code="title.users" /></strong>
		
		<form name="searchForm" action="list.do" method="post">
		<table class="sub_table sub_table02">
			<tr>
				<th><spring:message code="search.id" /></th>
				<td><input type="text" name="user_id" value="${userVO.user_id }"></td>
				<th><spring:message code="search.name" /></th>
				<td><input type="text" name="user_name" value="${userVO.user_name }"></td>
				<td rowspan="2">
					<input type="submit" class="btn" id="btnSearch" value="<spring:message code='button.search' />">
				</td>
			</tr>
			<tr>
				<th><spring:message code="search.regDate" /></th>
				<td colspan="3"><input type="text" id="daterangepicker"/></td>
				<input type="hidden" value="${searchStartDate }" name="start_time" id="start_time"/>
				<input type="hidden" value="${searchEndDate }" name="end_time" id="end_time"/>
			</tr>
		</table>
		
		<ul class="board_top">
			<li><span class="number_span">Total : ${userListCount }</span></li>
			<input type="hidden" id="totalCount" value="${userListCount }"/>
			<li>
			<%UserVO vo = (UserVO)request.getAttribute("userVO");%>
				<select name="page_per_data" id="page_per_data">
					<option value="10" <%=vo.getPage_per_data() == 10 ? "selected='selected'" : "" %>>10 <spring:message code="search.row"/></option>
					<option value="20" <%=vo.getPage_per_data() == 20 ? "selected='selected'" : "" %>>20 <spring:message code="search.row"/></option>
					<option value="50" <%=vo.getPage_per_data() == 50 ? "selected='selected'" : "" %>>50 <spring:message code="search.row"/></option>
					<option value="100" <%=vo.getPage_per_data() == 100 ? "selected='selected'" : "" %>>100 <spring:message code="search.row"/></option>
					<option value="1000" <%=vo.getPage_per_data() == 1000 ? "selected='selected'" : "" %>>1000 <spring:message code="search.row"/></option>
				</select>
				<button type="button" id="btnDeleteUser" class="btn" style="background-color: red;">관리자 삭제</button>
				<a href="add.do" class="btn personal_btn">관리자 추가</a>
			</li>
		</ul>
		
		<input type="hidden" name="now_page" id="now_page" value="1"/>
		</form>
		
		<table class="sub_table">
			<thead>
				<tr>
					<th style="width: 5%"><input type="checkbox" class="check_all"/></th>
					<th><spring:message code="search.id" /></th>
					<th><spring:message code="table.title.name" /></th>
					<th><spring:message code="table.title.dept" /></th>
					<th><spring:message code="table.title.position" /></th>
					<th><spring:message code="table.title.auth" /></th>
					<th><spring:message code="table.title.regDate" /></th>
					<th>상세</th>
				</tr>
			</thead>
			
			<tbody>
				<c:forEach var="list" items="${userList }" varStatus="status">
	       			<tr>
	       				<td><input type="checkbox" id="cbUser${list.get('userId')}" name="cbUser" class="cbUser" value="${list.get('userId')}" /></td>
						<td >${list.get("userId") }</td>
						<td>${list.get("userName") }</td>
						<td>
							<c:choose>
								<c:when test="${list.get('depart') eq null}">
									-
								</c:when>
								<c:otherwise>
									${list.get('depart') }
								</c:otherwise>
							</c:choose>
						</td>
						<td>
							<c:choose>
								<c:when test="${list.get('position') eq null}">
									-
								</c:when>
								<c:otherwise>
									${list.get('position') }
								</c:otherwise>
							</c:choose>
						</td>
						<td>
							<c:choose>
								<c:when test="${list.get('authorityDescription') eq null}">
									-
								</c:when>
								<c:otherwise>
									${list.get('authorityDescription') }
								</c:otherwise>
							</c:choose>
						</td>
						<td>${list.get("createDate") }</td>
						<td><button type="button" class="btn" onclick="userList.UserInfo('${list.get('userId')}')">상세보기</button></td>
	       			</tr>
	   			</c:forEach>
   			</tbody>
			
   			<c:if test="${userListCount eq 0}">
   				<tr>
       				<td colspan="8">
       					<spring:message code="search.noResult" />
       				</td>
       			</tr>
   			</c:if>
		</table>

		<c:if test="${userListCount != 0}">
			<div class="webtong-paging">
				<a class="first" id="first_page" onclick="userList.Paging(${pagingVO.start_page })">first page</a>
				<a class="previous" id="pre_page" onclick="userList.Paging(${pagingVO.pre_page })">previous page</a>
				<span class="numbering">
					<c:forEach var="list" varStatus="status" begin="${pagingVO.first_page }" end="${pagingVO.last_page }">
						<c:choose>
								<c:when test="${pagingVO.now_page eq list }">
									<em>${list }</em>
								</c:when>
								<c:otherwise>
									<a onclick="userList.Paging(${list })">${list }</a>
								</c:otherwise>
							</c:choose>
					</c:forEach>
				</span>
				<a class="next" id="next_page" onclick="userList.Paging(${pagingVO.next_page })">next page</a>
				<a class="last" id="last_page" onclick="userList.Paging(${pagingVO.end_page })">last page</a>
			</div>
		</c:if>
	</div>
</div>

<form name="fmDeleteUser" id="fmDeleteUser" method="POST" action="<%=request.getContextPath() %>/users/delete.do">
	<input type="hidden" name="user_id" id="user_id" />
</form>


<!-- modal start -->
<jsp:include page="../include/modal.jsp"/>
<!-- modal end -->

</body>
</html>