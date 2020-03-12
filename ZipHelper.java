package com.decision.v2x.era.util.convert;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.decision.v2x.era.VO.CertificationVO;
import com.decision.v2x.era.VO.DeviceVO;
import com.decision.v2x.era.service.impl.CertificationService;
import com.decision.v2x.era.service.impl.DeviceService;
import com.decision.v2x.era.util.auth.PermissionManagement;
import com.decision.v2x.era.util.http.RestApi;
import com.decision.v2x.era.util.log.LogGenerator.Common;
import com.decision.v2x.era.util.log.LogGenerator.Location;


public class ZipHelper {

	private final int MAX_SIZE = 1024;
	
	// 부트스트랩 다운로드
	public JSONObject dlBootstrap(HttpServletRequest req, String request_hash) throws Exception {
		
		RestApi api = PermissionManagement.getApi(req);
		
		return api.downloadBootstrap(request_hash);
	}
	
	// private key 다운로드
	public JSONObject dlPrivateKey(HttpServletRequest req, String request_hash) throws Exception {
		
		RestApi api = PermissionManagement.getApi(req);
		
		return api.privateKeyDownload(request_hash);
	}
	
	// 배치 업로드 zip 파일
	public byte[] makeZip(HttpServletRequest req, JSONArray jsonDevice_sn) throws NumberFormatException, Exception {
		JSONParser jp = new JSONParser();
		byte[] result = null;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ZipOutputStream outputStream = null;
        ZipEntry entry = null;
        
		JSONArray ja = new JSONArray();
		JSONObject dl = new JSONObject();
		
		for(Object obj : jsonDevice_sn) {
			JSONObject jobj = (JSONObject) jp.parse(obj.toString());
			
			// 단말 고유번호로 zip 생성
	        byte[] buf = new byte[MAX_SIZE];
	        
			String bootstrapName = null;
			byte[] bootstrapData = null;
			
			String privateKeyName = null;
			byte[] privateKeyData = null;
	        
			// 등록인증서
			if(jobj.get("certType").equals("enrol")) {
				String request_hash = jobj.get("request_hash").toString();
				
				// 부트스트랩 파일 다운로드
				JSONObject bootFile = dlBootstrap(req, request_hash);
				if(bootFile.get("success").equals("Y")) {
					bootstrapName = bootFile.get("fileName").toString();
					bootstrapData = (byte[])bootFile.get("fileData");
					
					// private key 다운로드
					JSONObject pkFile = dlPrivateKey(req, request_hash);
					if(pkFile.get("success").equals("Y")) {
						privateKeyName = pkFile.get("fileName").toString();
						privateKeyData = (byte[])pkFile.get("fileData");
						
						dl.put("fileData", makeEnrZip(bootstrapName, bootstrapData, privateKeyName, privateKeyData, jobj.get("device_sn").toString()));
						dl.put("fileName", jobj.get("device_sn").toString());
						
						ja.add(dl);
					}else {
						// private key 다운로드 실패
						
					}
				}else {
					// 부트 스트랩 파일 다운로드 실패
					
				}
				
			}
			
			// 보안인증서
			if(jobj.get("certType").equals("enrol")) {
				
				// 보안인증서 파일 다운로드
			}else {
				// 보안인증서 파일 다운로드 실패
			}

		}
		
		outputStream = new ZipOutputStream(baos);
		
		for(Object obj : ja) {
			JSONObject jobj = (JSONObject) jp.parse(obj.toString());
			

			byte[] input = (byte[])jobj.get("fileData");
			
			entry = new ZipEntry(jobj.get("fileName").toString());
            entry.setSize(input.length);
            outputStream.putNextEntry(entry);
            outputStream.closeEntry();
		}
		
		outputStream.close();
		
		return baos.toByteArray();
	}

	public byte[] makeEnrZip(String bootstrapName, byte[] bootstrapData, String pkName, byte[] pkData, String device_sn) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ZipOutputStream outputStream = null;
        ZipEntry entry = null;
        try {
            outputStream = new ZipOutputStream(baos);

            // 부트스트랩 파일
            entry = new ZipEntry(bootstrapName);
            entry.setSize(bootstrapData.length);
            outputStream.putNextEntry(entry);
            outputStream.closeEntry();
            
            // private key 파일
            entry = new ZipEntry(pkName);
            entry.setSize(pkData.length);
            outputStream.putNextEntry(entry);
            outputStream.closeEntry();
                
            outputStream.close();
        } catch (IOException e) {
            // Exception Handling
        } finally {
            try {
                outputStream.closeEntry();
                outputStream.close();
            } catch (IOException e) {
                // Exception Handling
            }
        }
        
        return baos.toByteArray();
    }

	
}
