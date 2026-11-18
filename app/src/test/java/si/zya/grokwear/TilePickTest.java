package si.zya.grokwear;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TilePickTest {
    static JSONObject roster(String main,String... pins)throws Exception{
        JSONArray bots=new JSONArray();for(String id:new String[]{"main","a","b","c"})bots.put(new JSONObject().put("id",id).put("name","Bot "+id));
        JSONArray p=new JSONArray();for(String id:pins)p.put(id);
        return new JSONObject().put("present",true).put("bots",bots).put("preferences",new JSONObject().put("mainAgentId",main).put("pinnedAgentIds",p));
    }

    @Test public void primaryAndFirstTwoPinsSkippingThePrimaryAndGoneBots()throws Exception{
        JSONObject t=GrokTileService.pick(roster("main","main","gone","a","b","c"));
        assertTrue(t.getBoolean("signedIn"));
        assertEquals("main",t.getJSONObject("primary").getString("id"));
        JSONArray pinned=t.getJSONArray("pinned");
        assertEquals(2,pinned.length());assertEquals("a",pinned.getJSONObject(0).getString("id"));assertEquals("Bot b",pinned.getJSONObject(1).getString("name"));
    }

    @Test public void signedOutAndEmptyRosters()throws Exception{
        assertFalse(GrokTileService.pick(null).getBoolean("signedIn"));
        JSONObject none=GrokTileService.pick(new JSONObject().put("present",false));
        assertTrue(none.getBoolean("signedIn"));assertFalse(none.has("primary"));assertEquals(0,none.getJSONArray("pinned").length());
        assertFalse(GrokTileService.pick(roster("missing")).has("primary"));
    }

    @Test public void namesAreTidiedAndCapped(){
        assertEquals("Bot",GrokTileService.name("  \n "));
        assertEquals("Two words",GrokTileService.name(" Two \n words "));
        StringBuilder longName=new StringBuilder();for(int i=0;i<60;i++)longName.append('n');
        assertEquals(GrokTileService.NAME_MAX,GrokTileService.name(longName.toString()).length());
        assertEquals("University",GrokTileService.shorten("University",GrokTileService.PINNED_SHORT));
        assertEquals("Universiti…",GrokTileService.shorten("Universities X",GrokTileService.PINNED_SHORT));
    }
}
