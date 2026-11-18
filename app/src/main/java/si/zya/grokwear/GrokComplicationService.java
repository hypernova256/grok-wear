package si.zya.grokwear;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.MonochromaticImage;
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.data.SmallImage;
import androidx.wear.watchface.complications.data.SmallImageComplicationData;
import androidx.wear.watchface.complications.data.SmallImageType;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A watch-face complication: one tap talks to the Primary Bot, like the Tile's Talk chip. Short text shows the
 * Bot's name beside a mic; the icon types show the mic alone. It reads only the roster the app saved.
 */
public final class GrokComplicationService extends ComplicationDataSourceService {
    /** Short text holds about seven characters on most faces. */
    static final int SHORT=7;
    private static final ExecutorService READ=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"grok-complication");t.setDaemon(true);return t;});

    @Override public void onComplicationRequest(ComplicationRequest request,ComplicationRequestListener listener){
        READ.execute(()->{String name="";
            try{JSONObject primary=GrokTileService.pick(new BotAccount(getApplicationContext()).cachedRoster()).optJSONObject("primary");if(primary!=null)name=primary.optString("name","");}
            catch(Exception signedOutOrUnread){}
            try{listener.onComplicationData(data(request.getComplicationType(),name));}catch(Exception gone){}
        });
    }
    @Override public ComplicationData getPreviewData(ComplicationType type){return data(type,"Grok");}

    private ComplicationData data(ComplicationType type,String name){
        Icon mic=Icon.createWithResource(this,R.drawable.ic_tile_mic);
        ComplicationText says=new PlainComplicationText.Builder(name.isEmpty()?"Open Grok Wear":"Talk to "+name).build();
        PendingIntent tap=PendingIntent.getActivity(this,7,new Intent(this,MainActivity.class).setAction("complication:talk").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(GrokTileService.TALK,!name.isEmpty()),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        switch(type){
            case SHORT_TEXT:return new ShortTextComplicationData.Builder(new PlainComplicationText.Builder(name.isEmpty()?"Grok":GrokTileService.shorten(name,SHORT)).build(),says)
                .setMonochromaticImage(new MonochromaticImage.Builder(mic).build()).setTapAction(tap).build();
            case SMALL_IMAGE:return new SmallImageComplicationData.Builder(new SmallImage.Builder(mic,SmallImageType.ICON).build(),says).setTapAction(tap).build();
            case MONOCHROMATIC_IMAGE:return new MonochromaticImageComplicationData.Builder(new MonochromaticImage.Builder(mic).build(),says).setTapAction(tap).build();
            default:return null;
        }
    }
}
