/*=======================================================================
Content  : FormLoad
========================================================================*/
$(document).ready(function(){
	try{
		deviceList.PageLoad();
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
deviceList Class 명세 시작 (상수(변수)>>속성>>메서드)
========================================================================*/
//deviceList Class
var deviceList = {};

//deviceList Const

//deviceList Variable
deviceList.page = 1;
deviceList.arrDeviceId = [];

//deviceList
var Properties = {};
deviceList.Properties = Properties;

//deviceList Method
deviceList.PageLoad = function () { };  //메인 페이지 로드 공통 함수
deviceList.SetEvent = function () { };  //메인 페이지 이벤트 바인딩
/*=======================================================================
deviceList Class 명세 끝
========================================================================*/

/*=======================================================================
내      용  : 메인 페이지 로드(PageLoad)
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
deviceList.PageLoad = function () {
    try {
        deviceList.Init();
        deviceList.SetEvent();
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 메인 페이지 초기화
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
deviceList.Init = function () {
    try {
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 이벤트 바인딩(SetEvent)
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
deviceList.SetEvent = function () {
    try {
    	
    	// 목록 row 값 변경
        $("#page_per_data").change(function (e) {
            e.preventDefault();
            $("#now_page").val(1);
            document.searchForm.submit();
        });
        
        // checkbox 전체 선택
        $(".check_all").click(function(e){
    		$(".cbDevice").prop("checked", this.checked);
        });
        
        // 단말 삭제
        $("#btnDeleteDevice").click(function(e){
        	var flag = confirm("해당 단말을 정말 삭제 하시겠습니까?");
        	if(flag == true){
        		var arrDeviceId = [];
        		$(".cbDevice:checked").each(function(){
        			arrDeviceId.push($(this).val());
        		});
        		
        		var form = document.createElement("form");
        		form.setAttribute("charset","UTF-8");
        		form.setAttribute("method","Post");
        		form.setAttribute("action","delete.do");
        		
        		var hiddenField = document.createElement("input");
        		hiddenField.setAttribute("type","hidden");
        		hiddenField.setAttribute("name","device_id");
        		hiddenField.setAttribute("value",arrDeviceId);
        		form.appendChild(hiddenField);
        		
        		document.body.appendChild(form);
        		form.submit();
        	}
        });
        
        $("#btnCreateEnrol").click(function(e) {
        	e.preventDefault();
        	var flag = confirm("등록 인증서를 생성하시겠습니까?");
        	if(flag == true){
        		deviceList.arrDeviceId = [];
        		$(".cbDevice:checked").each(function(){
        			deviceList.arrDeviceId.push($(this).val());
        		});
        		
        		if(arrDeviceId.length > 0){
        			// ajax 등록인증서 api
        			deviceList.RequestEnrolCert();
        		}else {
        			alert("선택된 단말 없습니다.");
        		}
        	}
        });
        
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 단말 상세 페이지 이동
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
deviceList.DeviceDetail = function (device_id) {
    try {
    	if(device_id){
    		var form = document.createElement("form");
    		form.setAttribute("charset","UTF-8");
    		form.setAttribute("method","Post");
    		form.setAttribute("action","show.do");

    		var hiddenField = document.createElement("input");
    		hiddenField.setAttribute("type","hidden");
    		hiddenField.setAttribute("name","device_id");
    		hiddenField.setAttribute("value",device_id);
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
내      용  : 단말 페이징
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
deviceList.Paging = function (page) {
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

/*=======================================================================
내      용  : 등록인증서 발급 api
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
deviceList.RequestEnrolCert = function () {
    try {
    	
    	//// 필요 파라미터 추가
    	//var ajaxData = { "arrDeviceId" , };
    	
    	$.ajax({
    		type:"post",
    		url : ".do",
    		data : ajaxData,
    		success : function(data){
    			var json = JSON.parse(data);
	    		if(json.result == "Y"){
		    		alert(json.msg);
	    		}
	    		if(json.result == "N"){
	    			alert(json.msg);
	    		}
    		},
    		error: function(request,status,error,data){
	    		alert("잘못된 접근 경로입니다.");
	    		addDevice.checkFlag = false;
	    		return false;
    		}
		});
    }
    catch (e) { console.log(e.message); }
}

