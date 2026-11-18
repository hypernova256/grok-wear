package si.zya.grokwear;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.wear.protolayout.ActionBuilders;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.DimensionBuilders;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ModifiersBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.TimelineBuilders;
import androidx.wear.protolayout.material.Chip;
import androidx.wear.protolayout.material.ChipColors;
import androidx.wear.protolayout.material.Colors;
import androidx.wear.protolayout.material.CompactChip;
import androidx.wear.protolayout.material.Text;
import androidx.wear.protolayout.material.Typography;
import androidx.wear.protolayout.material.layouts.PrimaryLayout;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileService;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Tile beside the watch face: Talk to the Primary Bot, open a pinned one, or the whole list. It reads only the
 * roster the app already saved on the watch, never the network, so it shows what the app last saw; the app asks
 * for a redraw when that changes.
 */
public final class GrokTileService extends TileService {
    static final String TALK="tile_talk";
    private static final String MIC="mic",RESOURCES="1";
    // Pinned Bots past this are left to the app: a round tile fits two chips beside the Talk one.
    static final int MAX_PINNED=2;static final int NAME_MAX=40,PINNED_SHORT=11;
    private static final int BLUE=0xff006ceb,CHIP=0xff2c2c2e,WHITE=0xffffffff,SECONDARY=0xff8d8d93;
    private static final ExecutorService READ=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"grok-tile");t.setDaemon(true);return t;});

    /** The app's roster or pins changed: redraw the Tile if it's on the watch. */
    static void refresh(Context context){try{getUpdater(context).requestUpdate(GrokTileService.class);}catch(RuntimeException ignored){}}

    /** {signedIn, primary?:{id,name}, pinned:[{id,name}]} from the saved roster ({present, bots, preferences}). */
    static JSONObject pick(JSONObject roster)throws Exception{
        JSONObject out=new JSONObject().put("signedIn",roster!=null).put("pinned",new JSONArray());
        if(roster==null||!roster.optBoolean("present"))return out;
        JSONArray bots=roster.optJSONArray("bots");JSONObject prefs=roster.optJSONObject("preferences");
        if(bots==null||prefs==null)return out;
        String main=prefs.optString("mainAgentId","");JSONObject primary=main.isEmpty()?null:row(bots,main);
        if(primary!=null)out.put("primary",primary);
        JSONArray pins=prefs.optJSONArray("pinnedAgentIds");if(pins==null)return out;
        for(int i=0;i<pins.length()&&out.getJSONArray("pinned").length()<MAX_PINNED;i++){
            String id=pins.optString(i,"");if(id.isEmpty()||id.equals(main))continue;
            JSONObject b=row(bots,id);if(b!=null)out.getJSONArray("pinned").put(b);
        }
        return out;
    }
    private static JSONObject row(JSONArray bots,String id)throws Exception{
        for(int i=0;i<bots.length();i++){JSONObject b=bots.optJSONObject(i);
            if(b!=null&&id.equals(b.optString("id"))&&id.length()<=512)return new JSONObject().put("id",id).put("name",name(b.optString("name","")));}
        return null;
    }
    static String name(String raw){String n=raw.replaceAll("\\s+"," ").trim();return n.isEmpty()?"Bot":shorten(n,NAME_MAX);}
    static String shorten(String n,int max){return n.codePointCount(0,n.length())<=max?n:n.substring(0,n.offsetByCodePoints(0,max-1)).trim()+"…";}

    @NonNull @Override protected ListenableFuture<TileBuilders.Tile> onTileRequest(@NonNull RequestBuilders.TileRequest request){
        final DeviceParameters device=request.getDeviceConfiguration();
        return CallbackToFutureAdapter.getFuture(done->{READ.execute(()->{
            JSONObject picked;
            // Signed out (or the saved sign-in can't be read): the tile just opens the app.
            try{picked=pick(new BotAccount(getApplicationContext()).cachedRoster());}
            catch(Exception unread){try{picked=pick(null);}catch(Exception never){done.setException(never);return;}}
            try{done.set(new TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES)
                // The app pushes changes; this is only a backstop for a redraw it missed.
                .setFreshnessIntervalMillis(30*60_000L)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(this,device,picked))).build());}
            catch(Exception failed){done.setException(failed);}
        });return "grok-tile";});
    }
    @NonNull @Override protected ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(@NonNull RequestBuilders.ResourcesRequest request){
        return CallbackToFutureAdapter.getFuture(done->{done.set(new ResourceBuilders.Resources.Builder().setVersion(RESOURCES)
            .addIdToImageMapping(MIC,new ResourceBuilders.ImageResource.Builder().setAndroidResourceByResId(
                new ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(R.drawable.ic_tile_mic).build()).build()).build());return "grok-tile-resources";});
    }

    static LayoutElementBuilders.LayoutElement layout(Context context,DeviceParameters device,JSONObject picked)throws Exception{
        Colors colors=new Colors(BLUE,WHITE,CHIP,WHITE);
        ChipColors primary=ChipColors.primaryChipColors(colors),secondary=ChipColors.secondaryChipColors(colors);
        JSONObject main=picked.optJSONObject("primary");JSONArray pinned=picked.getJSONArray("pinned");
        LayoutElementBuilders.Column.Builder column=new LayoutElementBuilders.Column.Builder().setWidth(DimensionBuilders.expand());
        List<LayoutElementBuilders.LayoutElement> chips=new ArrayList<>();
        if(!picked.optBoolean("signedIn"))
            chips.add(new Text.Builder(context,"Sign in on the watch app").setTypography(Typography.TYPOGRAPHY_BODY1).setColor(argb(SECONDARY)).setMaxLines(2).build());
        else{
            if(main!=null)chips.add(new Chip.Builder(context,open(context,"talk",null),device).setPrimaryLabelContent("Talk").setSecondaryLabelContent(main.getString("name"))
                .setIconContent(MIC).setChipColors(primary).setContentDescription("Talk to "+main.getString("name")).setWidth(DimensionBuilders.expand()).build());
            // Pinned Bots share one row of small chips: two full chips don't fit above All Bots on a round screen.
            if(pinned.length()>0){LayoutElementBuilders.Row.Builder row=new LayoutElementBuilders.Row.Builder();int cap=pinned.length()>1?PINNED_SHORT:NAME_MAX;
                for(int i=0;i<pinned.length();i++){JSONObject b=pinned.getJSONObject(i);if(i>0)row.addContent(new LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(4)).build());
                    row.addContent(new CompactChip.Builder(context,shorten(b.getString("name"),cap),open(context,"bot:"+b.getString("id"),b.getString("id")),device).setChipColors(secondary).build());}
                chips.add(row.build());}
            if(chips.isEmpty())chips.add(new Text.Builder(context,"Pick a Primary Bot or pin Bots in the app").setTypography(Typography.TYPOGRAPHY_BODY2).setColor(argb(SECONDARY)).setMaxLines(3).build());
        }
        for(int i=0;i<chips.size();i++){if(i>0)column.addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(6)).build());column.addContent(chips.get(i));}
        return new PrimaryLayout.Builder(device).setResponsiveContentInsetEnabled(true)
            .setPrimaryLabelTextContent(new Text.Builder(context,"Grok").setTypography(Typography.TYPOGRAPHY_CAPTION1).setColor(argb(SECONDARY)).build())
            .setContent(column.build())
            .setPrimaryChipContent(new CompactChip.Builder(context,picked.optBoolean("signedIn")?"All Bots":"Open",open(context,"list",null),device).setChipColors(secondary).build())
            .build();
    }
    /** Opens the app: on its Bot list, a Bot's page (bot), or a call with the Primary Bot (talk). */
    private static ModifiersBuilders.Clickable open(Context context,String id,String bot){
        ActionBuilders.AndroidActivity.Builder activity=new ActionBuilders.AndroidActivity.Builder().setPackageName(context.getPackageName()).setClassName(MainActivity.class.getName());
        if(bot!=null)activity.addKeyToExtraMapping(ReplyAlertService.OPEN_BOT,new ActionBuilders.AndroidStringExtra.Builder().setValue(bot).build());
        if("talk".equals(id))activity.addKeyToExtraMapping(TALK,new ActionBuilders.AndroidBooleanExtra.Builder().setValue(true).build());
        return new ModifiersBuilders.Clickable.Builder().setId(id.length()>64?id.substring(0,64):id)
            .setOnClick(new ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity.build()).build()).build();
    }
    private static androidx.wear.protolayout.ColorBuilders.ColorProp argb(int color){return androidx.wear.protolayout.ColorBuilders.argb(color);}
}
