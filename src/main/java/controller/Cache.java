package controller;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Cache {
    static JSONObject jsonObject = null;
    static JSONParser jsonParser = new JSONParser();
    static int showItems = 5;
    static JSONArray jsonArray = new JSONArray();
    static String filePath="";
    final static Logger logger = Logger.getLogger(Controller.class);


    public static Boolean assignJsonObject() throws Exception {
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("window"))
            filePath=System.getenv("LOCALAPPDATA") + "/AARAM/cache.json";
        else if(osName.contains("mac"))
            filePath = System.getProperty("user.home") + "/Library/Application Support/AARAM/cache.json";
        Reader content = new FileReader(filePath);
        if (content.read() == -1) {
            return false;
        } else
            jsonObject = (JSONObject) jsonParser.parse(new FileReader(filePath));
        return true;
    }

    public static List<String> readingFile() throws Exception {
        if (assignJsonObject()) {
            List<String> urlList = new ArrayList<>();
            jsonArray = (JSONArray) jsonObject.get("list of url");
            int startIndex = jsonArray.size() > showItems ? jsonArray.size() - showItems : 0;
            Iterator iterator = jsonArray.subList(startIndex, jsonArray.size()).iterator();
            while (iterator.hasNext()) {
                JSONObject obj = (JSONObject) iterator.next();
                urlList.add(obj.get("searchitem").toString());
                System.out.println(obj.get("searchitem").toString());
            }

            return urlList;
        }
        return null;
    }


    public static void writeToFile(String recievedUrl) throws Exception{

        FileWriter fileWriter = null;
        String urllist = "list of url";
        Date datetime = new Date();
        SimpleDateFormat timeformat = new SimpleDateFormat(" hh:mm:ss a");
        String ontime = timeformat.format(datetime);
        JSONObject tempobject = new JSONObject();

        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }

        else{
            jsonArray = (JSONArray) jsonObject.get(urllist);
            Iterator iterator = jsonArray.iterator();
            while (iterator.hasNext()) {
                JSONObject tempobj = (JSONObject) iterator.next();
                if (recievedUrl.equals(tempobj.get("searchitem").toString())) {
                    return;
                }
            }
            jsonArray = (JSONArray) jsonObject.get("list of url");
        }

        tempobject.put("searchitem", recievedUrl);
        tempobject.put("searchon", ontime);
        jsonArray.add(tempobject);
        jsonObject.put(urllist, jsonArray);

        try {
            fileWriter = new FileWriter(new File(filePath));
            fileWriter.write(jsonObject.toJSONString());
            fileWriter.flush();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            fileWriter.close();
        }
    }
}
