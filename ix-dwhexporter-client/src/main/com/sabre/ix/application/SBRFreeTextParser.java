package com.sabre.ix.application;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: SG0211638 Halldor Gylfason
 * Date: 17.10.2011
 */
public class SBRFreeTextParser {

    private String freeText;
    private Map<String, String> pairs = new HashMap<String, String>();

    public SBRFreeTextParser(String freeText) {
        this.freeText = freeText;
        parse();
    }

    public String get(String key) {
        return pairs.get(key);
    }

    private void parse(){
        Pattern p = Pattern.compile("\\[(.*?):(.*?)\\]");
        Matcher m = p.matcher(this.freeText);

        while(m.find()){
            pairs.put(m.group(1), m.group(2));
        }
    }
}
