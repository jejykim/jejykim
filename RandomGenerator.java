package com.decision.v2x.dcm.util.random;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RandomGenerator 
{
	public String Shuffle(String item)
	{
		List<Character> characters = new ArrayList<Character>();
        for(char c:item.toCharArray())
        {
            characters.add(c);
        }
        
        StringBuilder output = new StringBuilder(item.length());
        while(characters.size() != 0)
        {
            int randPicker = (int)(Math.random() * characters.size());
            output.append(characters.remove(randPicker));
        }

		return output.toString();
	}
	
	public String getUUID()
	{
		String retval = "";
		String uuid = UUID.randomUUID().toString().replace("-", "");
		String sp = "!@$%&";
		String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random rand = new Random(System.currentTimeMillis());
		//int addSpIndex = rand.nextInt(sp.length() + 1);
		//int addUpperIndex = rand.nextInt(upper.length() + 1);
		//int rmUuidIndex = rand.nextInt(uuid.length() + 1);

		for(int i = 0; i<4; i++)
		{
			int rmUuidIndex = rand.nextInt(uuid.length() - 1);
			uuid = uuid.substring(0, rmUuidIndex + 1) + uuid.substring(rmUuidIndex, uuid.length());			
		}
		
		for(int i = 0; i<2; i++)
		{
			int addSpIndex = rand.nextInt(sp.length());
			int addUpperIndex = rand.nextInt(upper.length());
			uuid += sp.substring(addSpIndex, addSpIndex +1) + upper.substring(addUpperIndex, addUpperIndex+1);			
		}
		
	
		return Shuffle(uuid);
	}
	
	public String getID(String email)
	{
		try
		{
			int st = 0;
			int end = email.indexOf("@") - 1;
		
			return email.substring(st, end);
		}
		catch(Exception e)
		{
			return email;
		}
	}
	public String getHostname(String email)
	{
		try
		{
			int st = email.indexOf("@") + 1;
			int end = email.length();
			
			return email.substring(st, end);
		}
		catch(Exception e)
		{
			return "";
		}
	}
}
