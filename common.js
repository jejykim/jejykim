/*=======================================================================
Content  : FormLoad
========================================================================*/
$(document).ready(function(){
	try{
		common.PageLoad();
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
common Class 명세 시작 (상수(변수)>>속성>>메서드)
========================================================================*/
//common Class
var common = {};

//common Const

//common Variable

//common
var Properties = {};
common.Properties = Properties;

//common Method
common.PageLoad = function () { };  //메인 페이지 로드 공통 함수
common.SetEvent = function () { };  //메인 페이지 이벤트 바인딩
/*=======================================================================
common Class 명세 끝
========================================================================*/

/*=======================================================================
내      용  : 메인 페이지 로드(PageLoad)
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
common.PageLoad = function () {
    try {
        common.Init();
        common.SetEvent();
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 메인 페이지 초기화
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
common.Init = function () {
    try {

    	
    	
    	// daterangepicker
    	$(function() {
			if($("#start_time").val() == "" && $("#end_time").val() == ""){
				$('#daterangepicker').daterangepicker({
					autoUpdateInput: false,
					timePicker: true,
					timePicker24Hour: true,
			    	locale: {
			    		cancelLabel: 'Clear',
			    		format: 'YYYY-MM-DD HH:mm:ss',
						daysOfWeek: ["일", "월", "화", "수", "목", "금", "토"],
						monthNames: ["1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"],
						applyLabel: "적용",
						cancelLabel: "취소"
			    	}
				});
				
				$('#daterangepicker').on('apply.daterangepicker', function(ev, picker) {
				      $(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:00') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:00'));
				      $("#start_time").val(picker.startDate.format('YYYY-MM-DD HH:mm:00'));
				      $("#end_time").val(picker.endDate.format('YYYY-MM-DD HH:mm:00'));
				  });
				
				$('#daterangepicker').on('cancel.daterangepicker', function(ev, picker) {
				      $(this).val("");
				      $("#start_time").val("");
				      $("#end_time").val("");
				  });
				
			}else {
				$('#daterangepicker').daterangepicker({
					timePicker: true,
					timePicker24Hour: true,
					startDate: moment($("#start_time").val()),
					endDate: moment($("#end_time").val()),
					locale: {
						format: 'YYYY-MM-DD HH:mm:ss',
						daysOfWeek: ["일", "월", "화", "수", "목", "금", "토"],
						monthNames: ["1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"],
						applyLabel: "적용",
						cancelLabel: "취소"
					}
				});
				
				$('#daterangepicker').on('apply.daterangepicker', function(ev, picker) {
				      $(this).val(picker.startDate.format('YYYY-MM-DD HH:mm:00') + ' - ' + picker.endDate.format('YYYY-MM-DD HH:mm:00'));
				      $("#start_time").val(picker.startDate.format('YYYY-MM-DD HH:mm:00'));
				      $("#end_time").val(picker.endDate.format('YYYY-MM-DD HH:mm:00'));
				  });
				
				$('#daterangepicker').on('cancel.daterangepicker', function(ev, picker) {
				      $(this).val("");
				      $("#start_time").val("");
				      $("#end_time").val("");
				  });
			}
		});
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 이벤트 바인딩(SetEvent)
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
common.SetEvent = function () {
    try {
    	// 단말 배치 업로드 모달 호출
        $("#aBatchUpload").click(function (e) {
            e.preventDefault();
            $("#modalDeivce").fadeIn(150);
        });
        $("#btnBatchUpload").click(function (e) {
        	e.preventDefault();
        	$("#modalDeivce").fadeIn(150);
        });
        
        // 단말 배치 업로드 모달 닫기
        $("#aMDClose").click(function (e) {
            e.preventDefault();
            $("#modalDeivce").fadeOut(150);
            var agent = navigator.userAgent.toLowerCase();
            if ((navigator.appName == 'Netscape' && navigator.userAgent.search('Trident') != -1) || (agent.indexOf("msie") != -1)) {
            	$("#inputExcelFile").replaceWith( $("#inputExcelFile").clone(true) );
            	//$("#inputZipFile").replaceWith( $("#inputZipFile").clone(true) );
            } else {
            	$("#inputExcelFile").val("");
            	//$("#inputZipFile").val("");
            }
            $("#inputExcelName").val("선택된 파일 없음");
            //$("#inputZipName").val("선택된 파일 없음");
        });
        
        // excel 파일 선택
        $("#inputExcelFile").change(function (e) {
            e.preventDefault();
            var excelName = $("#inputExcelFile")[0].files[0].name;
            $("#inputExcelName").val(excelName);
        });
        
        // csr 파일 선택
        /*$("#inputZipFile").change(function (e) {
            e.preventDefault();
            var zipName = $("#inputZipFile")[0].files[0].name;
            $("#inputZipName").val(zipName);
        });*/
        
        // excel format 다운로드
        $("#dlExcel").click(function(e){
        	common.DownloadFormat();
        });
        
        // 등록
        $("#aUploadBatch").click(function(e){
        	if(common.CheckParam() == true){
        		common.BatchUpload();
        	}
        });
        
        // excel 파일 확장자 체크
        $("#inputExcelFile").change(function(e){
        	// excel 파일 체크
        	var fileName = $("#inputExcelFile").val();
        	var check = fileName.substring(fileName.indexOf(".")+1, fileName.length);
        	if(check.toLowerCase() != "xlsx"){
        		alert("xlsx 확장자만 선택 해주세요");
        		
        		var agent = navigator.userAgent.toLowerCase();
    			if ( (navigator.appName == 'Netscape' && agent.indexOf('trident') != -1) || (agent.indexOf("msie") != -1)) {
    				$("#inputExcelFile").replaceWith( $("#inputExcelFile").clone(true) );
    			}else{
    				$("#inputExcelFile").val("");
    				$("#inputExcelName").val("선택된 파일 없음");
    			}
        	}
        });
        
        // csr.zip 파일 확장자 체크
        /*$("#inputZipFile").change(function(e){
        	// csr 파일 체크
        	var fileName = $("#inputZipFile").val();
        	var check = fileName.substring(fileName.indexOf(".")+1, fileName.length);
        	if(check.toLowerCase() != "zip"){
        		alert("zip 확장자만 선택 해주세요");
        		
        		var agent = navigator.userAgent.toLowerCase();
    			if ( (navigator.appName == 'Netscape' && agent.indexOf('trident') != -1) || (agent.indexOf("msie") != -1)) {
    				$("#inputZipFile").replaceWith( $("#inputZipFile").clone(true) );
    			}else{
    				$("#inputZipFile").val("");
    				$("#inputZipName").val("선택된 파일 없음");
    			}
        	}
        });*/
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 유효성 검사
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
common.CheckParam = function () {
    try {
    	return true;
    	// excel 파일
    	if($("#inputExcelFile").val() == ""){
    		alert("단말 excel 파일을 선택해주세요.");
    		return false;
    	}
    	
    }
    catch (e) { console.log(e.message); }
}

/*=======================================================================
내      용  : 배치 업로드
작  성  자  : 김진열
2019.12.06 - 최초생성
========================================================================*/
common.BatchUpload = function () {
    try {
    	var form = new FormData(document.getElementById('formBatch'));
    	
    	$.ajax({
    		type:"post",
    		url : "/ERA/batch/upload.do",
    		data : form,
    		processData: false,
    		contentType: false,
    		success : function(data){
    			alert("배치 업로드 되었습니다.");
    			location.reload();
    		},
    		error: function(request,status,error,data){
	    		alert("잘못된 접근 경로입니다.");
	    		return false;
    		}
		});
    }
    catch (e) { console.log(e.message); }
}
