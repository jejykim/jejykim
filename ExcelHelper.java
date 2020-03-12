package com.decision.v2x.era.util.convert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.web.multipart.MultipartFile;

import com.decision.v2x.era.VO.CertificatesKeyVO;
import com.decision.v2x.era.VO.CertificationVO;
import com.decision.v2x.era.VO.DeviceVO;
import com.decision.v2x.era.service.impl.CertificationService;
import com.decision.v2x.era.service.impl.DeviceService;
import com.decision.v2x.era.service.impl.LogService;
import com.decision.v2x.era.service.impl.PermissionService;
import com.decision.v2x.era.util.auth.PermissionManagement;
import com.decision.v2x.era.util.http.RestApi;
import com.decision.v2x.era.util.log.LogGenerator;
import com.decision.v2x.era.util.log.LogGenerator.Common;
import com.decision.v2x.era.util.log.LogGenerator.Location;
import com.decision.v2x.era.util.setting.SettingManagement;
import com.decision.v2x.era.web.LoginController;

public class ExcelHelper {
	
	PermissionService permissionService;
	CertificationService certService;
	LogService logService;
	DeviceService deviceService;
	
	public ExcelHelper(PermissionService permissionService, CertificationService certService,
			LogService logService, DeviceService deviceService) {
		this.permissionService = permissionService;
		this.certService = certService;
		this.logService = logService;
		this.deviceService = deviceService;
	}
	
	private PermissionManagement pm = null;
	private LogGenerator lg = null;
	private LogHelper lh = new LogHelper();
	
	@PostConstruct
	public void init() throws Exception
	{
		RestApi.disableSslVerification();
		pm = new PermissionManagement(permissionService);
		lg = new LogGenerator(logService);
	}
	
	public void batchUpload(MultipartFile mfile, HttpServletRequest req, HttpServletResponse resp) {
		try {
			SettingManagement setting = LoginController.setting;
			
			OutputStream os = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

	        ZipOutputStream outputStream = null;
	        ZipEntry entry = null;
	        JSONParser jp = new JSONParser();
			
			File file = new File(mfile.getOriginalFilename());
			mfile.transferTo(file);
			XSSFWorkbook workbook = new XSSFWorkbook(file);
 
            DeviceVO deviceVO = new DeviceVO();
            
            // 등록인증서 생성 여부
        	boolean enrolFlag = false;
        	// 보안인증서 생성 여부
        	boolean secFlag = false;
        	// 보안인증서 종류
        	String secCert = "";
        	
        	// 생성된 인증서의 단말 고유번호 array
        	JSONArray jsonDevice_sn = new JSONArray();
            
            int rowindex=0;
            int columnindex=0;
            //시트 수 (첫번째에만 존재하므로 0을 준다)
            //만약 각 시트를 읽기위해서는 FOR문을 한번더 돌려준다
            XSSFSheet sheet=workbook.getSheetAt(0);
            //행의 수
            int rows=sheet.getPhysicalNumberOfRows();
            for(rowindex=2;rowindex<rows;rowindex++){
                //행을읽는다
                XSSFRow row=sheet.getRow(rowindex);
                if(row !=null){

                    // 등록인증서 생성 여부
                	enrolFlag = false;
                	// 보안인증서 생성 여부
                	secFlag = false;
                	// 보안인증서 종류
                	secCert = "";
                	
                    //셀의 수
                    int cells=row.getPhysicalNumberOfCells();
                    for(columnindex=0; columnindex<=cells; columnindex++){
                        //셀값을 읽는다
                        XSSFCell cell=row.getCell(columnindex);
                        String value="";
                        //셀이 빈값일경우를 위한 널체크
                        if(cell==null){
                            continue;
                        }else{
                            //타입별로 내용 읽기
                            switch (cell.getCellType()){
                            case XSSFCell.CELL_TYPE_FORMULA:
                                value=cell.getCellFormula();
                                break;
                            case XSSFCell.CELL_TYPE_NUMERIC:
                                value=cell.getNumericCellValue()+"";
                                break;
                            case XSSFCell.CELL_TYPE_STRING:
                                value=cell.getStringCellValue()+"";
                                break;
                            case XSSFCell.CELL_TYPE_BLANK:
                                value=cell.getBooleanCellValue()+"";
                                break;
                            case XSSFCell.CELL_TYPE_ERROR:
                                value=cell.getErrorCellValue()+"";
                                break;
                            }
                        }

                        //System.out.println(rowindex+"번 행 : "+columnindex+"번 열 값은: "+value);
                        // 고유번호 구분
                        if(columnindex == 0) {
                        	if(value.length() > 0) {
                        		deviceVO.setDevice_id_type_group("ECG027");
                        		switch (value.toUpperCase()) {
								case "MAC":
									deviceVO.setDevice_id_type("1");
									break;

								case "IMEI":
									deviceVO.setDevice_id_type("2");						
									break;
									
								case "VIN":
									deviceVO.setDevice_id_type("3");
									break;
									
								case "CAR":
									deviceVO.setDevice_id_type("4");
									break;
									
								case "CS":
									deviceVO.setDevice_id_type("5");
									break;
	
								}
                        	}
                        }
                        
                        // 고유번호
                        else if(columnindex == 1) {
                        	if(value.length() > 0) {
                        		deviceVO.setDevice_sn(value);
                        	}
                        }
                        
                        // 단말 종류
                        else if(columnindex == 2) {
                        	if(value.length() > 0) {
                        		deviceVO.setDevice_type_group("CMM801");
                        		switch (value.toUpperCase()) {
								case "OBU":
									deviceVO.setDevice_type("obu_1");
									break;

								case "RSU":
									deviceVO.setDevice_type("rsu_2");
									break;
								}
                        	}
                        }
                        
                        // 단말 제조사
                        else if(columnindex == 3) {
                        	if(value.length() > 0) {
                        		deviceVO.setDevice_maker(value);
                        	}
                        }
                        
                        // 모델명
                        else if(columnindex == 4) {
                        	if(value.length() > 0) {
                        		deviceVO.setDevice_name(value);
                        	}
                        }
                        
                        // 모델 번호
                        else if(columnindex == 5) {
                        	if(value.length() > 0) {
                        		deviceVO.setDevice_number(value);
                        	}
                        }
                        
                        // 모델 버전
                        else if(columnindex == 6) {
                        	if(value.length() > 0) {
                        		deviceVO.setDevice_ver(value);
                        	}
                        }
                        
                        
                        // 등록인증서
                        else if(columnindex == 7) {
                        	if(value.length() > 0) {
                        		if(value.toUpperCase().equals("O")) {
                        			enrolFlag = true;
                        		}
                        	}
                        }
                        
                        // 보안인증서
                        else if(columnindex == 8) {
                        	if(value.length() > 0) {
                        		if(value.toUpperCase().equals("O")) {
                        			secFlag = true;
                        		}
                        	}
                        }
                        
                        // 보안인증서 종류
                        else if(columnindex == 9) {
                        	if(value.length() > 0) {
                        		secCert = value.toUpperCase();
                        	}
                        }
                    }
 
                }
                
                // 단말 등록 여부
                if(deviceService.selectCheckDeviceSn(deviceVO.getDevice_sn()) > 0) {
                	// 보안 인증서 생성 여부
                	if(secFlag) {
                		JSONObject resultJson = secCert(req, deviceVO, secCert);
                		JSONObject jobj = new JSONObject();
                		jobj.put("device_sn", deviceVO.getDevice_sn());
                		jobj.put("certType", "sec");
                		jobj.put("secType", secCert);
                		
                		jobj.put("request_hash", resultJson.get("request_hash"));
                		jobj.put("public_key", resultJson.get("public_key"));
                		
                		jsonDevice_sn.add(jobj);
                		
                	// 등록 인증서 생성 여부
                	}else if(enrolFlag) {
                		JSONObject resultJson = enrolCert(req, deviceVO);
                		JSONObject jobj = new JSONObject();
                		jobj.put("device_sn", deviceVO.getDevice_sn());
                		jobj.put("certType", "enrol");
                		
                		jobj.put("request_hash", resultJson.get("request_hash"));
                		jobj.put("public_key", resultJson.get("public_key"));
                		
                		jsonDevice_sn.add(jobj);
                	}
                }else {
                	// 생성자 ID
                	deviceVO.setCreate_id(req.getSession().getAttribute("user_id").toString());
                	// 단말 그룹 ID (도공, 서울, 제주)
                	deviceVO.setDevice_group_id(setting.getCompanyCode());
                	deviceVO.setDevice_group_id_group(setting.getCompanyGroup());
                	
                	try {
                		// 단말 등록
                    	int iResult = deviceService.insertDevice(deviceVO);
                    	
                    	/*단말 등록 성공 로그*/
                    	
                    	if(iResult > 0) {
                    		// 보안 인증서 생성 여부
                    		if(secFlag) {
                    			JSONObject resultJson = secCert(req, deviceVO, secCert);
                    			JSONObject jobj = new JSONObject();
                    			jobj.put("device_sn", deviceVO.getDevice_sn());
                    			jobj.put("certType", "sec");
                    			jobj.put("secType", secCert);
                    			
                    			jobj.put("request_hash", resultJson.get("request_hash"));
                        		jobj.put("public_key", resultJson.get("public_key"));
                    			
                    			jsonDevice_sn.add(jobj);
                    			
                    			// 등록 인증서 생성 여부
                    		}else if(enrolFlag) {
                    			JSONObject resultJson = enrolCert(req, deviceVO);
                    			JSONObject jobj = new JSONObject();
                    			jobj.put("device_sn", deviceVO.getDevice_sn());
                    			jobj.put("certType", "enrol");
                    			
                    			jobj.put("request_hash", resultJson.get("request_hash"));
                        		jobj.put("public_key", resultJson.get("public_key"));
                    			
                    			jsonDevice_sn.add(jobj);
                    		}
                    	}
                	}catch (Exception e) {
						// TODO: handle exception
					}
                	
                }
                
            }
 
            // zip 다운로드 (부트스트랩 & 부트스트랩+보안인증서)
            try {
            	os = resp.getOutputStream();
            	
            	byte[] batchFile = new ZipHelper().makeZip(req, jsonDevice_sn);
            	String batchName = "batch_file.zip";
            	
    			outputStream = new ZipOutputStream(baos);

    			entry = new ZipEntry(batchName);
                entry.setSize(batchFile.length);
                outputStream.putNextEntry(entry);
                outputStream.closeEntry();
        		outputStream.close();
        		
        		resp.setContentType("application/zip");
        		resp.setHeader("Content-Disposition", "attachment; filename=\"" + batchName + "\";");
        		resp.setHeader("Content-Transfer-Encoding", "binary");
        		
        		os.write(batchFile);
        		
        		os.flush();
    			os.close();
    			
            }catch (Exception e) {
            	// 다운로드 실패
            }
            
        }catch(Exception e) {
            e.printStackTrace();
        }
		
		
	}
	
	// 등록인증서 api
	public JSONObject enrolCert(HttpServletRequest req, DeviceVO deviceVO) throws Exception {
		
		RestApi api = PermissionManagement.getApi(req);
		DeviceVO tmp = deviceService.selectSN(deviceVO.getDevice_sn());
		HashMap<String, Object> enrolItem = null;
		JSONObject returnJson = new JSONObject();
		
		try
		{
        	String key = certService.selectId();
        	enrolItem = new HashMap<>();
        	Long deviceId = Long.parseLong(tmp.getDevice_id());
        	
        	enrolItem.put("success", "N");
        	enrolItem.put("title", key);
        	enrolItem.put("device_id", deviceId);
			
			//csr 생성 및 request hash, public key 얻기
        	String title = enrolItem.get("title").toString();
        	
        	/*============== 등록인증서 설정 값 불러오기 =============*/
        	int nCount =  1;
        	int[] arrPsid = new int[] {32, 82058, 38, 82050, 82051, 82052, 82054, 82055, 82056, 82057};
        	int[] arrContryCode = new int[] {410};
        	int[] arrCircularRegin = null;
        	String strDurationUint = "HOURS";
        	int nDurationValue = 4320;
        	/*============== 등록인증서 설정 값 불러오기 =============*/
        	
        	//*
        	JSONObject enrolResult = api.makeEnrol(title, nCount, arrPsid, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
        	if(enrolResult.get("success").equals("Y"))
        	{
        		enrolItem.put("success", "Y");
        		enrolItem.put("public_key", enrolResult.get("public_key").toString());
        		enrolItem.put("enrol_validity_end", enrolResult.get("enrol_validity_end").toString());
        		enrolItem.put("enrol_validity_start", enrolResult.get("enrol_validity_start").toString());
        		enrolItem.put("request_hash", enrolResult.get("request_hash").toString());
        		enrolItem.put("oer_title", enrolResult.get("oer_title").toString());
        		
        		// return
        		returnJson.put("request_hash", enrolResult.get("request_hash").toString());
        		returnJson.put("public_key", enrolResult.get("public_key").toString());
        	}
        	else
        	{
        		enrolItem.put("msg", enrolResult.get("msg").toString());
        	}
        	//*/
	        
	        ///DB에 등록
	        //*
        	if(enrolItem.get("success").equals("Y"))
        	{
	        	String enrolValidityEnd = (String)enrolItem.get("enrol_validity_end");
	        	String enrolValidityStart = (String)enrolItem.get("enrol_validity_start");
	        	
	        	CertificationVO vo  = new CertificationVO();
				CertificatesKeyVO kVo = new CertificatesKeyVO();
				DeviceVO dVo = new DeviceVO();
				
	        	vo.setCert_id(enrolItem.get("title").toString());
				vo.setDevice_id((long)enrolItem.get("device_id"));
				vo.setCreate_id(req.getSession().getAttribute("user_id").toString());
				vo.setCert_state("2");
				vo.setCert_state_group_id("ECG005");
				vo.setExpiry_date(enrolValidityEnd);
				vo.setIssue_date(enrolValidityStart);
				vo.setCert_type("3");
				vo.setCert_type_group("ECG028");
				
				kVo.setCert_enrol_id(enrolItem.get("title").toString());
				kVo.setCert_sec_id(null);
				kVo.setRequest_hash(enrolItem.get("request_hash").toString());
				kVo.setPublic_key(enrolItem.get("public_key").toString());	
				
				dVo.setDevice_id(deviceId.toString());
				dVo.setDevice_cert_type("U");
				
				this.certService.updateDeviceCertType(dVo);
				this.certService.insertCert(vo);
				this.certService.insertCertKey(kVo);

				JSONObject contents = lh.createEnrol(req, kVo.getSeq(), vo.getCert_id(), deviceId, "");
				lg.insertLog(Location.EnrolCreate, Common.Success, pm.getUserId(req), contents);
        	}
        	else
        	{
        		JSONObject contents = lh.createEnrol(req, null, null, deviceId, (String)enrolItem.get("msg"));
				lg.insertLog(Location.EnrolCreate, Common.Fail, pm.getUserId(req), contents);
        	}
	        //*/
	        
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
			
		return returnJson;
	}
	
	// 보안인증서 api
	public JSONObject secCert(HttpServletRequest req, DeviceVO deviceVO, String secCert) throws Exception {
		String userId = (String) req.getSession().getAttribute("user_id");
		JSONArray resultList = new JSONArray();
		JSONObject result = new JSONObject();
		JSONObject item = new JSONObject();
		
		JSONObject returnJson = new JSONObject();
		
		String deviceSn = deviceVO.getDevice_sn();
		String deviceID = deviceService.getDeviceId(deviceVO);
		
		/*============== 보안인증서 설정 값 불러오기 =============*/
		String strDurationUint = "HOURS";
		int nDurationValue = 4320;
		int[] arrContryCode = new int[] {410};
		int[] arrCircularRegin = null;
		int[] appPsids = new int[] {38, 135, 82049, 82053, 82054, 82055, 82056, 82057, 82059};
		int[] idePsids = new int[] {32, 82058, 38, 82050, 82051, 82052, 82054, 82055, 82056, 82057};
		int[] psePsids = new int[] {32, 82058, 38, 82050, 82051, 82052, 82054, 82055, 82056, 82057};
		/*============== 보안인증서 설정 값 불러오기 =============*/
		
		String certTypeId = "";
		String certTypeGroup = "";
		
		try
		{
			RestApi api = PermissionManagement.getApi(req);
			
			CertificatesKeyVO kVo = new CertificatesKeyVO();
			CertificationVO vo = new CertificationVO();
			String key = certService.selectId();
			
			switch(secCert)
			{
			case "APP":
				certTypeId = "4";
				certTypeGroup = "ECG028";
				result = api.createCertApp(deviceSn, 1, appPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
				break;
				
			case "PSE":
				certTypeId = "1";
				certTypeGroup = "ECG028";
				result = api.createCertPse(deviceSn, 1, psePsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
				break;
				
			case "IDE":
				certTypeId = "2";
				certTypeGroup = "ECG028";
				result = api.createCertIde(deviceSn, 1, idePsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue);
				break;
				
			default:
				throw new Exception("잘못된 Type입니다: " + secCert);
			}
			
			if(result.get("success").equals("Y"))
			{
				vo.setCert_id(key);
				vo.setDevice_id(Long.parseLong(deviceID));
				vo.setCreate_id(userId);
				vo.setCert_state("2");
				vo.setCert_state_group_id("ECG005");
				vo.setExpiry_date(result.get("enrol_validity_end").toString());
				vo.setIssue_date(result.get("enrol_validity_start").toString());
				vo.setCert_type(certTypeId);
				vo.setCert_type_group(certTypeGroup);
				
				kVo.setDevice_id(Long.parseLong(deviceID));
				kVo.setCert_enrol_id(key);
				kVo.setCert_sec_id(key);
				kVo.setRequest_hash(result.get("request_hash").toString());
				kVo.setPublic_key(result.get("public_key").toString());				
				
				// return
				returnJson.put("request_hash", result.get("request_hash").toString());
				returnJson.put("request_hash", result.get("public_key").toString());
			
				this.certService.insertCert(vo);
				this.certService.insertCertKey(kVo);
				
				item.put("deviceSN", deviceSn);
				item.put("success", "Y");
				item.put("msg", "");
				
				switch(secCert)
				{
				case "APP":
					lg.insertLog(Location.MakeApp, Common.Success, PermissionManagement.getUserId(req), 
							lh.makeCert(req, secCert, deviceSn, 1, appPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, ""));
					break;
					
				case "PSE":
					lg.insertLog(Location.MakePse, Common.Success, PermissionManagement.getUserId(req), 
							lh.makeCert(req, secCert, deviceSn, 1, psePsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, ""));
					break;
					
				case "IDE":
					lg.insertLog(Location.MakeIde, Common.Success, PermissionManagement.getUserId(req), 
							lh.makeCert(req, secCert, deviceSn, 1, idePsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, ""));
					break;
				}
			}
			else
			{
				item.put("deviceSN", deviceSn);
				item.put("success", "N");
				item.put("msg", result.get("msg").toString());
			}
		}
		catch(Exception e)
		{
			item.put("deviceSN", deviceSn);
			item.put("success", "N");
			item.put("msg", lh.error(e).toString());
			
			switch(secCert)
			{
			case "APP":
				lg.insertLog(Location.MakeApp, Common.Error, PermissionManagement.getUserId(req), 
						lh.makeCert(req, secCert, deviceSn, 1, appPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
						);
				break;
				
			case "PSE":
				lg.insertLog(Location.MakePse, Common.Error, PermissionManagement.getUserId(req), 
						lh.makeCert(req, secCert, deviceSn, 1, appPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
					);
				break;
				
			case "IDE":
				lg.insertLog(Location.MakeIde, Common.Error, PermissionManagement.getUserId(req), 
						lh.makeCert(req, secCert, deviceSn, 1, appPsids, arrContryCode, arrCircularRegin, strDurationUint, nDurationValue, lh.error(e).toString())
					);
				break;
			}
		}
		
		return returnJson;
	}
	
}
