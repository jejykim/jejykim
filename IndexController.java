package com.decision.v2x.era.web;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.decision.v2x.era.VO.UserVO;
import com.decision.v2x.era.service.impl.CodeService;
import com.decision.v2x.era.service.impl.UserService;
import com.decision.v2x.era.util.http.RestApi;

@Controller
public class IndexController 
{

	private static final Logger LOGGER = LoggerFactory.getLogger(IndexController.class);
	
	@RequestMapping(value = "/index.do")
	public String selectSampleList(ModelMap model, HttpServletRequest req, HttpServletResponse resp) 
	{
		try
		{
			
		}
		catch(Exception exp)
		{
			//System.out.println(exp.toString());			
		}
		
		return "login/login";
	}
	
	
}
