package si.zya.grokwear;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SidebarSectionsTest {
    static JSONObject section(String id,String name,String... ids)throws Exception{
        JSONObject s=new JSONObject().put("id",id).put("name",name);
        if(ids.length>0){JSONArray a=new JSONArray();for(String i:ids)a.put(i);s.put("agentIds",a);}
        return s;
    }
    static JSONObject settings(JSONObject... sections)throws Exception{
        JSONArray list=new JSONArray();for(JSONObject s:sections)list.put(s);
        return new JSONObject().put("settings",new JSONObject().put("mainAgentId","main")
            .put("pinnedAgents",new JSONObject().put("agentIds",new JSONArray().put("a")))
            .put("sidebarSections",new JSONObject().put("sections",list)));
    }

    @Test public void keepsSectionsInOrderWithTheOtherSection()throws Exception{
        JSONObject prefs=GrokCore.botPreferences(settings(section("s1","Work","a","b"),section(GrokCore.OTHER_SECTION,"Bots"),section("s2","Home","c")));
        JSONArray s=prefs.getJSONArray("sections");
        assertEquals(3,s.length());
        assertEquals("Work",s.getJSONObject(0).getString("name"));
        assertEquals(GrokCore.OTHER_SECTION,s.getJSONObject(1).getString("id"));
        assertEquals(0,s.getJSONObject(1).getJSONArray("agentIds").length());
        assertEquals("c",s.getJSONObject(2).getJSONArray("agentIds").getString(0));
        // Pins and the Primary Bot still parse alongside the sections.
        assertEquals("main",prefs.getString("mainAgentId"));
        assertEquals("a",prefs.getJSONArray("pinnedAgentIds").getString(0));
    }

    @Test public void skipsBadSectionsAloneAndDedupes()throws Exception{
        JSONObject bad=new JSONObject().put("name","No id").put("agentIds",new JSONArray().put("x"));
        JSONObject mixed=new JSONObject().put("id","s1").put("name","Mixed").put("agentIds",new JSONArray().put("a").put(7).put("").put("a").put("b"));
        JSONObject prefs=GrokCore.botPreferences(settings(bad,section("empty","Empty"),mixed,section("s1","Duplicate id","z")));
        JSONArray s=prefs.getJSONArray("sections");
        assertEquals(1,s.length());
        JSONArray ids=s.getJSONObject(0).getJSONArray("agentIds");
        assertEquals(2,ids.length());assertEquals("a",ids.getString(0));assertEquals("b",ids.getString(1));
        assertEquals("main",prefs.getString("mainAgentId"));
    }

    @Test public void noSectionsLeavesPreferencesAsBefore()throws Exception{
        JSONObject prefs=GrokCore.botPreferences(new JSONObject().put("settings",new JSONObject().put("mainAgentId","main")));
        assertFalse(prefs.has("sections"));
        assertFalse(GrokCore.botPreferences(new JSONObject().put("settings",new JSONObject().put("sidebarSections","oops"))).has("sections"));
    }

    @Test public void capsLongNames()throws Exception{
        StringBuilder longName=new StringBuilder();for(int i=0;i<200;i++)longName.append('n');
        JSONObject prefs=GrokCore.botPreferences(settings(section("s1",longName.toString(),"a")));
        assertEquals(80,prefs.getJSONArray("sections").getJSONObject(0).getString("name").length());
    }
}
