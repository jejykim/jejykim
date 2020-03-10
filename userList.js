/*=======================================================================
Content  : FormLoad
========================================================================*/
$(document).ready(function(){
	try{
		userList.PageLoad();
	}
	catch(e){ console.log(e.message); }
	
});


/*=======================================================================
Content  : FormBeforeUnLoad
========================================================================*/
function FormBeforeUnLoad() {
    try {
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
userList Class 명세 시작 (상수(변수)>>속성>>메서드)
========================================================================*/
//userList Class
var userList = {};

//userList Const

//userList Variable
userList.page = 1;

//userList
var Properties = {};
userList.Properties = Properties;

//userList Method
userList.PageLoad = function () { };  //메인 페이지 로드 공통 함수
userList.SetEvent = function () { };  //메인 페이지 이벤트 바인딩
/*=======================================================================
userList Class 명세 끝
========================================================================*/

/*=======================================================================
내      용  : 메인 페이지 로드(PageLoad)
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
userList.PageLoad = function () {
    try {
        userList.Init();
        userList.SetEvent();
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 메인 페이지 초기화
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
userList.Init = function () {
    try {
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 이벤트 바인딩(SetEvent)
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
userList.SetEvent = function () {
    try {
    	
    	$("#btnDeleteUser").on('click', function(e)
		{
    		e.preventDefault();
    		var flag = confirm("해당 관리자를 정말 삭제 하시겠습니까?");
        	if(flag == true)
        	{
	    		var arrUserId = [];
	    		$("input[name=cbUser]:checked").each(function(){
	    			arrUserId.push($(this).val());
	    		});
	    		
	    		$("#user_id").val(arrUserId);
	    		$("#fmDeleteUser").submit();
        	}
		});
    	
    	// 목록 row 값 변경
        $("#page_per_data").change(function (e) {
            e.preventDefault();
            $("#now_page").val(1);
            document.searchForm.submit();
        });
        
        // checkbox 전체 선택
        $(".check_all").click(function(e){
    		$(".cbUser").prop("checked", this.checked);
        });
        
        // 사용자 삭제
        $("#btnDeleteDevice").click(function(e){
        	var flag = confirm("해당 단말을 정말 삭제 하시겠습니까?");
        	if(flag == true){
        		var arrUserId = [];
        		$(".cbDevice:checked").each(function(){
        			arrUserId.push($(this).val());
        		});
        		
        		var form = document.createElement("form");
        		form.setAttribute("charset","UTF-8");
        		form.setAttribute("method","Post");
        		form.setAttribute("action","delete.do");
        		
        		var hiddenField = document.createElement("input");
        		hiddenField.setAttribute("type","hidden");
        		hiddenField.setAttribute("name","user_id");
        		hiddenField.setAttribute("value",arrUserId);
        		form.appendChild(hiddenField);
        		
        		document.body.appendChild(form);
        		form.submit();
        	}
        });
        
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 사용자 상세 페이지 이동
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
userList.UserInfo = function (user_id) {
    try {
    	if(user_id){
    		var form = document.createElement("form");
    		form.setAttribute("charset","UTF-8");
    		form.setAttribute("method","Post");
    		form.setAttribute("action","show.do");

    		var hiddenField = document.createElement("input");
    		hiddenField.setAttribute("type","hidden");
    		hiddenField.setAttribute("name","user_id");
    		hiddenField.setAttribute("value",user_id);
    		form.appendChild(hiddenField);
    		
    		document.body.appendChild(form);
    		form.submit();
    	}else{
    		alert("잘못된 경로 입니다");
    	}
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 사용자 페이징
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
userList.Paging = function (page) {
    try {
    	if(page > 0){
    		$("#now_page").val(page);
    		document.searchForm.submit();
    	}else{
    		alert("잘못된 경로 입니다");
    	}
    }
    catch (e) { console.log(e.message); }
}


