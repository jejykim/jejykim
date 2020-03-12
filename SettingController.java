package com.decision.v2x.era.web;

import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.decision.v2x.era.VO.CertSettingVO;
import com.decision.v2x.era.VO.CodeGroupVO;
import com.decision.v2x.era.VO.CodeVO;
import com.decision.v2x.era.VO.DeviceVO;
import com.decision.v2x.era.VO.PagingVO;
import com.decision.v2x.era.service.impl.CertSettingService;
import com.decision.v2x.era.service.impl.CodeService;
import com.decision.v2x.era.service.impl.LogService;
import com.decision.v2x.era.util.paging.PagingControl;

@Controller
public class SettingController {
	
	@Resource(name = "logService")
	private LogService logService;
	
	@Resource(name = "codeService")
	private CodeService codeService;
	
	@Resource(name = "certSettingService")
	private CertSettingService certSettingService;
	
	// 인증서 설정
	@RequestMapping(value = "setting/cert.do")
	public String settingCert(Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		String strReturn = "settings/cert";
		
		// 로그인, 권한 확인 class 호출
		if(req.getSession().getAttribute("user_id") == null) 
		{
			strReturn = "redirect:/login.do";
		}else {
			List<?> infraPsidList = codeService.getInfraPsidList();
			List<?> infraSspList = codeService.getInfraSspList();
			List<?> infraSubjectPermissionList = codeService.getInfraSubjectPermissionList();
			List<?> infraSSPRangeList = codeService.getInfraSSPRangeList();
			List<?> durationTypeList = codeService.getDurationTypeList();
			List<?> countryList = codeService.getCountryList();
			List<?> obuPsidList = codeService.getObuPsidList();
			List<?> rsuPsidList = codeService.getRsuPsidList();
			
			// 인프라 인증서
			CertSettingVO infraVO = new CertSettingVO();
			infraVO.setCert_type_code("5");
			infraVO.setCert_type_code_group("ECG028");
			int infraResult = certSettingService.checkSetting(infraVO);
			if(infraResult > 0) {
				infraVO = certSettingService.getSetting(infraVO);
				model.addAttribute("infraSetting", infraVO.getSetting());
			}
			
			// 익명 인증서
			CertSettingVO pseVO = new CertSettingVO();
			pseVO.setCert_type_code("1");
			pseVO.setCert_type_code_group("ECG028");
			int pseResult = certSettingService.checkSetting(pseVO);
			if(pseResult > 0) {
				pseVO = certSettingService.getSetting(pseVO);
				model.addAttribute("pseSetting", pseVO.getSetting());
			}
			
			// 실명 인증서
			CertSettingVO ideVO = new CertSettingVO();
			ideVO.setCert_type_code("2");
			ideVO.setCert_type_code_group("ECG028");
			int ideResult = certSettingService.checkSetting(ideVO);
			if(ideResult > 0) {
				ideVO = certSettingService.getSetting(ideVO);
				model.addAttribute("ideSetting", ideVO.getSetting());
			}
			
			// 기지국 인증서
			CertSettingVO appVO = new CertSettingVO();
			appVO.setCert_type_code("4");
			appVO.setCert_type_code_group("ECG028");
			int appResult = certSettingService.checkSetting(appVO);
			if(appResult > 0) {
				appVO = certSettingService.getSetting(appVO);
				model.addAttribute("appSetting", appVO.getSetting());
			}
			
			// 기지국 인증서
			CertSettingVO enrVO = new CertSettingVO();
			enrVO.setCert_type_code("3");
			enrVO.setCert_type_code_group("ECG028");
			int enrResult = certSettingService.checkSetting(enrVO);
			if(enrResult > 0) {
				enrVO = certSettingService.getSetting(enrVO);
				model.addAttribute("enrSetting", enrVO.getSetting());
			}
			
			model.addAttribute("infraPsidList", infraPsidList);
			model.addAttribute("infraSspList", infraSspList);
			model.addAttribute("infraSubjectPermissionList", infraSubjectPermissionList);
			model.addAttribute("infraSSPRangeList", infraSSPRangeList);
			model.addAttribute("durationTypeList", durationTypeList);
			model.addAttribute("countryList", countryList);
			model.addAttribute("obuPsidList", obuPsidList);
			model.addAttribute("rsuPsidList", rsuPsidList);
		}
		
		return strReturn;
	}
	
	// 인프라 인증서 설정 저장
	@ResponseBody
	@RequestMapping(value = "setting/infra.do", produces="application/text;charset=utf8;")
	public String settinginfra(@RequestParam("data") String data, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		JSONObject json = new JSONObject();
		boolean flag = false;
		String msg = "";
		data = data.replaceAll("&quot;", "\"");
		
		if(true/*권한 체크*/) {
			CertSettingVO vo = new CertSettingVO();
			vo.setCert_type_code("5");
			vo.setCert_type_code_group("ECG028");
			vo.setSetting(data);
			vo.setCreate_id(req.getSession().getAttribute("user_id").toString());
			vo.setModify_id(req.getSession().getAttribute("user_id").toString());
			int iResult = certSettingService.checkSetting(vo);
			
			// 인프라 인증서 설정 생성
			if(iResult == 0) {
				try {
					certSettingService.insertSetting(vo);
					
					msg = "인프라 인증서 설정 완료";
					flag = true;
					// 인프라 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "인프라 인증서 설정 생성 실패";
					// 인프라 인증서 설정 저장 실패 로그
				}
			// 인프라 인증서 설정 수정
			}else if(iResult > 0) {
				try {
					certSettingService.updateSetting(vo);
					
					msg = "인프라 인증서 설정 완료";
					flag = true;
					// 인프라 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "인프라 인증서 설정 수정 실패";
					// 인프라 인증서 설정 저장 실패 로그
				}
			}
		}
		
		json.put("flag", flag);
		json.put("msg", msg);
		
		return json.toString();
	}
	
	// 익명 인증서 설정 저장
	@ResponseBody
	@RequestMapping(value = "setting/pse.do", produces="application/text;charset=utf8;")
	public String settingPse(@RequestParam("data") String data, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		JSONObject json = new JSONObject();
		boolean flag = false;
		String msg = "";
		data = data.replaceAll("&quot;", "\"");
		
		if(true/*권한 체크*/) {
			CertSettingVO vo = new CertSettingVO();
			vo.setCert_type_code("1");
			vo.setCert_type_code_group("ECG028");
			vo.setSetting(data);
			vo.setCreate_id(req.getSession().getAttribute("user_id").toString());
			vo.setModify_id(req.getSession().getAttribute("user_id").toString());
			int iResult = certSettingService.checkSetting(vo);
			
			// 익명 인증서 설정 생성
			if(iResult == 0) {
				try {
					certSettingService.insertSetting(vo);
					
					msg = "익명 인증서 설정 완료";
					flag = true;
					// 익명 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "익명 인증서 설정 생성 실패";
					// 익명 인증서 설정 저장 실패 로그
				}
			// 익명 인증서 설정 수정
			}else if(iResult > 0) {
				try {
					certSettingService.updateSetting(vo);
					
					msg = "익명 인증서 설정 완료";
					flag = true;
					// 익명 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "익명 인증서 설정 수정 실패";
					// 익명 인증서 설정 저장 실패 로그
				}
			}
		}
		
		json.put("flag", flag);
		json.put("msg", msg);
		
		return json.toString();
	}
	
	// 실명 인증서 설정 저장
	@ResponseBody
	@RequestMapping(value = "setting/ide.do", produces="application/text;charset=utf8;")
	public String settingIde(@RequestParam("data") String data, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		JSONObject json = new JSONObject();
		boolean flag = false;
		String msg = "";
		data = data.replaceAll("&quot;", "\"");
		
		if(true/*권한 체크*/) {
			CertSettingVO vo = new CertSettingVO();
			vo.setCert_type_code("2");
			vo.setCert_type_code_group("ECG028");
			vo.setSetting(data);
			vo.setCreate_id(req.getSession().getAttribute("user_id").toString());
			vo.setModify_id(req.getSession().getAttribute("user_id").toString());
			int iResult = certSettingService.checkSetting(vo);
			
			// 실명 인증서 설정 생성
			if(iResult == 0) {
				try {
					certSettingService.insertSetting(vo);
					
					msg = "실명 인증서 설정 완료";
					flag = true;
					// 실명 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "실명 인증서 설정 생성 실패";
					// 실명 인증서 설정 저장 실패 로그
				}
			// 실명 인증서 설정 수정
			}else if(iResult > 0) {
				try {
					certSettingService.updateSetting(vo);
					
					msg = "실명 인증서 설정 완료";
					flag = true;
					// 실명 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "실명 인증서 설정 수정 실패";
					// 실명 인증서 설정 저장 실패 로그
				}
			}
		}
		
		json.put("flag", flag);
		json.put("msg", msg);
		
		return json.toString();
	}
	
	// 기지국 인증서 설정 저장
	@ResponseBody
	@RequestMapping(value = "setting/app.do", produces="application/text;charset=utf8;")
	public String settingApp(@RequestParam("data") String data, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		JSONObject json = new JSONObject();
		boolean flag = false;
		String msg = "";
		data = data.replaceAll("&quot;", "\"");
		
		if(true/*권한 체크*/) {
			CertSettingVO vo = new CertSettingVO();
			vo.setCert_type_code("4");
			vo.setCert_type_code_group("ECG028");
			vo.setSetting(data);
			vo.setCreate_id(req.getSession().getAttribute("user_id").toString());
			vo.setModify_id(req.getSession().getAttribute("user_id").toString());
			int iResult = certSettingService.checkSetting(vo);
			
			// 기지국 인증서 설정 생성
			if(iResult == 0) {
				try {
					certSettingService.insertSetting(vo);
					
					msg = "보안 인증서 설정 완료";
					flag = true;
					// 기지국 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "기지국 인증서 설정 생성 실패";
					// 기지국 인증서 설정 저장 실패 로그
				}
			// 실명 인증서 설정 수정
			}else if(iResult > 0) {
				try {
					certSettingService.updateSetting(vo);
					
					msg = "보안 인증서 설정 완료";
					flag = true;
					// 기지국 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "기지국 인증서 설정 수정 실패";
					// 기지국 인증서 설정 저장 실패 로그
				}
			}
		}
		
		json.put("flag", flag);
		json.put("msg", msg);
		
		return json.toString();
	}
	
	// 등록 인증서 설정 저장
	@ResponseBody
	@RequestMapping(value = "setting/enr.do", produces="application/text;charset=utf8;")
	public String settingEnr(@RequestParam("data") String data, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		JSONObject json = new JSONObject();
		boolean flag = false;
		String msg = "";
		data = data.replaceAll("&quot;", "\"");
		
		if(true/*권한 체크*/) {
			CertSettingVO vo = new CertSettingVO();
			vo.setCert_type_code("3");
			vo.setCert_type_code_group("ECG028");
			vo.setSetting(data);
			vo.setCreate_id(req.getSession().getAttribute("user_id").toString());
			vo.setModify_id(req.getSession().getAttribute("user_id").toString());
			int iResult = certSettingService.checkSetting(vo);
			
			// 등록 인증서 설정 생성
			if(iResult == 0) {
				try {
					certSettingService.insertSetting(vo);
					
					msg = "등록 인증서 설정 완료";
					flag = true;
					// 기지국 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "등록 인증서 설정 생성 실패";
					// 등록 인증서 설정 저장 실패 로그
				}
			// 등록 인증서 설정 수정
			}else if(iResult > 0) {
				try {
					certSettingService.updateSetting(vo);
					
					msg = "등록 인증서 설정 완료";
					flag = true;
					// 등록 인증서 설정 저장 성공 로그
				}catch (Exception e) {
					
					msg = "등록 인증서 설정 수정 실패";
					// 등록 인증서 설정 저장 실패 로그
				}
			}
		}
		
		json.put("flag", flag);
		json.put("msg", msg);
		
		return json.toString();
	}
	
	// 코드 목록
	@RequestMapping(value = "setting/code.do")
	public String settingCode(@ModelAttribute("codeVO") CodeVO codeVO, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		String strReturn = "settings/code";
		
		// 로그인, 권한 확인 class 호출
		if(req.getSession().getAttribute("user_id") == null) {
			strReturn = "redirect:/login.do";
		}else {
			
			if(codeVO.getNow_page() != 1) {
				codeVO.setPage((codeVO.getNow_page()-1)*codeVO.getPage_per_data());
			}
			
			List<?> codeList = codeService.selectCodeList(codeVO);
			int codeListCount = codeService.selectCodeListCount(codeVO);
			
			PagingControl pc = new PagingControl();
			PagingVO pagingVO = pc.paging(codeListCount, codeVO.getNow_page(), codeVO.getPage_per_data());
			
			model.addAttribute("codeList", codeList);
			model.addAttribute("codeListCount", codeListCount);
			model.addAttribute("pagingVO", pagingVO);
			model.addAttribute("codeVO", codeVO);
		}
		
		return strReturn;
	}
	
	// 코드 추가
	@RequestMapping(value = "setting/addCode.do")
	public String addCode(Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		String strReturn = "settings/addCode";
		
		// 로그인, 권한 확인 class 호출
		if(req.getSession().getAttribute("user_id") == null) {
			strReturn = "redirect:/login.do";
		}else {
			CodeGroupVO codeGroupVO = new CodeGroupVO();
			codeGroupVO.setPage_per_data(99);
			List<?> codeGroupList = codeService.selectCodeGroupList(codeGroupVO);
			
			model.addAttribute("codeGroupList", codeGroupList);
		}
		
		return strReturn;
	}
	
	// 코드 추가 OK
	@RequestMapping(value = "setting/addCodeOK.do")
	public String addCodeOK(@ModelAttribute("codeVO") CodeVO codeVO, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		String strReturn = "redirect:/setting/code.do";
		
		// 로그인, 권한 확인 class 호출
		if(req.getSession().getAttribute("user_id") == null) {
			strReturn = "redirect:/login.do";
		}else {
			try {
				codeVO.setCreate_id(req.getSession().getAttribute("user_id").toString());
				codeService.insertCode(codeVO);
				// 코드 추가 성공 로그
			}catch (Exception e) {
				// 코드 추가 실패 로그
				System.out.println("실패 :" + e);
			}
			
		}
		
		return strReturn;
	}
	
	// 코드 삭제
	@ResponseBody
	@RequestMapping(value = "setting/deleteCode.do", produces="application/text;charset=utf8;")
	public String deleteCode(@ModelAttribute("codeVO") CodeVO codeVO, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		JSONObject json = new JSONObject();
		
		if(true/*권한 체크*/) {
			try {
				codeService.deleteCode(codeVO);
				// 코드 삭제 성공 로그
				
				json.put("result", "Y");
				json.put("msg", "삭제 되었습니다.");
			}catch (Exception e) {
				// 코드 삭제 실패 로그
				
				json.put("result", "N");
				json.put("msg", "삭제 오류 발생");
			}
			
		}
		
		return json.toString();
	}
	
	// 코드 그룹 목록
	@RequestMapping(value = "setting/codeGroup.do")
	public String settingCodeGroup(@ModelAttribute("codeGroupVO") CodeGroupVO codeGroupVO, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		String strReturn = "settings/codeGroup";
		
		// 로그인, 권한 확인 class 호출
		if(req.getSession().getAttribute("user_id") == null) {
			strReturn = "redirect:/login.do";
		}else {
			
			if(codeGroupVO.getNow_page() != 1) {
				codeGroupVO.setPage((codeGroupVO.getNow_page()-1)*codeGroupVO.getPage_per_data());
			}
			
			List<?> codeGroupList = codeService.selectCodeGroupList(codeGroupVO);
			int codeGroupListCount = codeService.selectCodeGroupListCount(codeGroupVO);
			
			PagingControl pc = new PagingControl();
			PagingVO pagingVO = pc.paging(codeGroupListCount, codeGroupVO.getNow_page(), codeGroupVO.getPage_per_data());
			
			model.addAttribute("codeGroupList", codeGroupList);
			model.addAttribute("codeGroupListCount", codeGroupListCount);
			model.addAttribute("pagingVO", pagingVO);
			model.addAttribute("codeGroupVO", codeGroupVO);
		}
		
		return strReturn;
	}
	
	// 코드 그룹 추가
	@RequestMapping(value = "setting/addCodeGroup.do")
	public String addCodeGroup(Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		String strReturn = "settings/addCodeGroup";
		
		// 로그인, 권한 확인 class 호출
		if(req.getSession().getAttribute("user_id") == null) {
			strReturn = "redirect:/login.do";
		}else {
			// 권한 체크
			if(true/*권한 체크*/) {
				
			}else {
				strReturn = "redirect:/main/dashboard.do";
			}
			
		}
		
		return strReturn;
	}
	
	// 코드 그룹 추가 OK
	@RequestMapping(value = "setting/addCodeGroupOK.do")
	public String addCodeGroupOK(@ModelAttribute("codeGroupVO") CodeGroupVO codeGroupVO, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		String strReturn = "redirect:/setting/codeGroup.do";
		
		// 로그인, 권한 확인 class 호출
		if(req.getSession().getAttribute("user_id") == null) {
			strReturn = "redirect:/login.do";
		}else {
			try {
				codeGroupVO.setCreate_id(req.getSession().getAttribute("user_id").toString());
				codeService.insertCodeGroup(codeGroupVO);
				// 코드 추가 성공 로그
			}catch (Exception e) {
				// 코드 추가 실패 로그
				System.out.println("실패 :" + e);
			}
			
		}
		
		return strReturn;
	}
	
	// 코드 그룹 삭제
	@ResponseBody
	@RequestMapping(value = "setting/deleteCodeGroup.do", produces="application/text;charset=utf8;")
	public String deleteCodeGroup(@ModelAttribute("codeGroupVO") CodeGroupVO codeGroupVO, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		JSONObject json = new JSONObject();
		
		if(true/*권한 체크*/) {
			try {
				// 하위 코드 삭제
				codeService.deleteCodeForCodeGroup(codeGroupVO);
				// 코드 그룹 삭제
				codeService.deleteCodeGroup(codeGroupVO);
				
				// 코드 삭제 성공 로그
				
				json.put("result", "Y");
				json.put("msg", "삭제 되었습니다.");
			}catch (Exception e) {
				// 코드 삭제 실패 로그
				
				json.put("result", "N");
				json.put("msg", "삭제 오류 발생");
			}
			
		}
		
		return json.toString();
	}
	
	// 코드 중복 체크
	@ResponseBody
	@RequestMapping(value = "setting/checkCode.do", produces="application/text;charset=utf8;")
	public String checkCode(@ModelAttribute("codeVO") CodeVO codeVO, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		JSONObject json = new JSONObject();
		
		try {
			int iResult = codeService.checkCode(codeVO);
			
			if(iResult > 0) {
				json.put("result", "N");
			}else {
				json.put("result", "Y");
			}
			
		}catch (Exception e) {
			json.put("result", "E");
		}
			
		return json.toString();
	}
	
	// 코드 그룹 중복 체크
	@ResponseBody
	@RequestMapping(value = "setting/checkCodeGroup.do", produces="application/text;charset=utf8;")
	public String checkCodeGroup(@ModelAttribute("codeGroupVO") CodeGroupVO codeGroupVO, Locale locale, ModelMap model, HttpServletRequest req, HttpServletResponse resp) throws Exception 
	{
		JSONObject json = new JSONObject();
		
		try {
			int iResult = codeService.checkCodeGroup(codeGroupVO);
			
			if(iResult > 0) {
				json.put("result", "N");
			}else {
				json.put("result", "Y");
			}
			
		}catch (Exception e) {
			json.put("result", "E");
		}
		
		return json.toString();
	}
}
