<%@page import="com.decision.v2x.era.VO.DeviceVO"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c"      uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form"   uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ui"     uri="http://egovframework.gov/ctl/ui"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ page import="org.json.simple.JSONArray" %>
<%@ page import="org.json.simple.JSONObject" %>

<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.GregorianCalendar" %>

<%
	Object objList = request.getAttribute("infraList");
	Object objTotalCnt = request.getAttribute("infraTotalCnt");
	Object objMsg = request.getAttribute("msg");

	JSONArray list = objList == null ? new JSONArray() : (JSONArray)objList;
	String totalCnt = objTotalCnt == null ? "0" : objTotalCnt.toString();
	String msg = objMsg == null ? null : objMsg.toString();
%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>인프라 인증서</title>
	<link rel="stylesheet" href="<%=request.getContextPath() %>/css/contents.css" >
	<jsp:include page="../include/common.jsp"/>
	<script type="text/javascript" src="<%=request.getContextPath() %>/js/certifications/infraList.js"></script>
	
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
		<strong class="sub_title">인프라 인증서</strong>
		<ul class="board_top">
			<li><span class="number_span">Total : <%=totalCnt %></span></li>
			<li>
				<input type="button" class="btn" id="btnCreateInfra" value="인프라 인증서 추가" />
			</li>
		</ul>
		<table class="sub_table">
			<thead>
				<tr>
					<th width="15%">문서번호</th>
					<th width="10%">발급일자</th>
					<th width="10%">만료일자</th>
					<th width="15%">생성자</th>
					<th width="8%">적용여부</th>
					<th width="15%">적용일자</th>
					<th width="8%">적용</th>
					<th width="8%">CSR</th>
					<th width="10%">인프라 인증서</th>
				</tr>
			</thead>
			
			<tbody>
			
			<%
			if(objList != null)
			{	
				Calendar cal = new GregorianCalendar();
				SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				final int addHour = 9;

				//Date to = transFormat.parse(from);
				//cal.setTime(to);
				for(int i = 0; i<list.size(); i++)
				{
					JSONObject item = (JSONObject)list.get(i);
					String startTime = "";
					String expireTime = "";
					String applyTime = "-";
					
					cal.setTime(transFormat.parse(item.get("start_time").toString()));
					cal.add(Calendar.HOUR, addHour);
					startTime = transFormat.format(cal.getTime());
					
					cal.setTime(transFormat.parse(item.get("expire_time").toString()));
					cal.add(Calendar.HOUR, addHour);
					expireTime = transFormat.format(cal.getTime());
					
					try
					{
						cal.setTime(transFormat.parse(item.get("apply_time").toString()));
						cal.add(Calendar.HOUR, addHour);
						applyTime = transFormat.format(cal.getTime());
					}
					catch(Exception e)
					{
						applyTime = "-";
					}
			%>
				<tr>
       				<td><%=item.get("doc_no") %></td>
					<td><%=startTime %></td>
       				<td><%=expireTime %></td>
					<td><%=item.get("create_id") %></td>
					<td><%=item.get("apply_yn") %></td>
					<td><%=applyTime %></td>
					<td>
						<form method="POST" name="infraVO" method="POST" enctype="multipart/form-data" action="<%=request.getContextPath() %>/cert/infra/apply.do">
							<input type="hidden" name="docNo" value="<%=item.get("doc_no") %>" />
							<input type="button" name="submitInfraApply" class="btn" value="적용하기" />
							<input type="hidden" name="config" value="A" />
						</form>
					</td>
					<td>
						<form method="POST" name="infraVO" method="POST" enctype="multipart/form-data" action="<%=request.getContextPath() %>/cert/infra/download/csr.do">
							<input type="hidden" name="docNo" value="<%=item.get("doc_no") %>" />
							<input type="submit" class="btn" value="다운로드" />
						</form>
					</td>
					<td>
						<form name="infraVO" method="POST" enctype="multipart/form-data" action="<%=request.getContextPath() %>/cert/infra/upload.do">
							<div>
								<div class="personal_text" style="display:none;">
										<div class="login_box intro_box" style="width:50%">
											<table class="login_table intro_table" style="width: 100%">
												<tbody class="basic_tb">
													<tr>
														<th>파일</th>
														<td>
															<div class="fileBox">
																<input type="file" name="file" id="file" class="file_btn" style="width:100%">
															</div>
														</td>
													</tr>
												</tbody>
											</table>
											<div class="login_btn file_btn_b">
												<input type="submit" class="btn" value="확인" />
												<button type="button"  name="popupInfraUploadClose" class="btn" value="취소" >취소</button>
											</div>
										</div>
										<input type="hidden" name="docNo" value="<%=item.get("doc_no") %>" />
									
								</div>
								<button type="button" name="btnInfraUpload" class="btn" value="업로드" >업로드</button>
							</div>
						</form>
					</td>
       			</tr>
       		<%
       			}
			}
			else
			{
				String result = msg == null ? "목록이 없습니다." : msg;
				%>
				
				<tr>
       				<td colspan="9">
       					<%=result %>
       				</td>
       			</tr>
				
				<%
			}
       		%>
			
				<!-- sample 
				<tr>
       				<td></td>
					<td></td>
       				<td></td>
					<td></td>
					<td></td>
					<td></td>
					<td><button class="btn" onclick="">적용하기</button></td>
       			</tr>
       			-->
       			
				<%-- <c:forEach var="list" items="${ }" varStatus="status">
	       			<tr>
	       				<td></td>
						<td></td>
	       				<td></td>
						<td></td>
						<td></td>
						<td></td>
						<td><button class="btn" onclick="">적용하기</button></td>
	       			</tr>
	   			</c:forEach> --%>
   			</tbody>
			
   			<%-- <c:if test="${전체row수 eq 0}">
   				<tr>
       				<td colspan="9">
       					<spring:message code="search.noResult" />
       				</td>
       			</tr>
   			</c:if> --%>
		</table>

<%--
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
					<!-- <em>1</em>
					<a>2</a>
					<a>3</a>
					<a>4</a>
					<a>5</a>
					<a class="end" href="#">···</a> -->
				</span>
				<a class="next" id="next_page" onclick="deviceList.Paging(${pagingVO.next_page })">next page</a>
				<a class="last" id="last_page" onclick="deviceList.Paging(${pagingVO.end_page })">last page</a>
			</div>
		</c:if>
		 --%>
	</div>
</div>


<form name="infraVO" method="POST" enctype="multipart/form-data" action="<%=request.getContextPath() %>/cert/infra/create.do">
	<div>
		<div id="modalInfraCreate" class="personal_text" style="display: none;">
				<div class="login_box intro_box" style="width:50%">
					<table class="login_table intro_table" style="width: 100%">
						<tbody class="basic_tb">
							<tr>
								<th>호스트 이름</th>
								<td colspan="2">
									<input type="text" name="hostname" id="hostname" style="width:100%" />
								</td>
							</tr>
							
							<tr>
								<th>Duration</th>
								<td>
									<select name="durationUnit" id="durationUnit">
										<option value="YEARS">년</option>
										<option value="SIXTYHOURS">60 시간</option>
										<option value="HOURS">시간</option>
										<option value="MINUTES">분</option>
										<option value="SECONDS">초</option>
										<option value="MILLISECONDS">밀리초</option>
										<option value="MICROSECONDS">마이크로초</option>
									</select>
								</td>
								<td>
									<input type="number" name="durationValue" id="durationValue" style="width:100%"	/>
								</td>
							</tr>
							
							<tr>
								<th>
									KMS Key
								</th>
								<td colspan="2">
									<input type="text" name="kmsKey" id="kmsKey" style="width:100%"	/>
								</td>
							</tr>
							
							<tr>
								<th>
									인프라 인증서 효력 발생일
								</th>
								<td colspan="2">
									<input type="text" name="stateDate" id="stateDate" style="width:100%"	/>
								</td>
							</tr>
						</tbody>
					</table>
					<div class="login_btn file_btn_b">
						<input type="submit" class="btn" value="확인">
						<button type="button" id="popupInfraCreateClose" class="btn" value="취소">취소</button>
					</div>
				</div>			
		</div>
	</div>
</form>






<!-- modal start -->
<jsp:include page="../include/modal.jsp"/>
<!-- modal end -->





</body>
</html>