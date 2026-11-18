package si.zya.grokwear;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * List items shrink a little as they near the round screen's top and bottom, as Wear OS's transforming lists do, so
 * a row never runs into the bezel at full width. Rows are the page's direct children, and the children of a list
 * nested in it (tag "list" or "chat"); a view tagged "hero" keeps its size.
 */
final class EdgeScale {
    private EdgeScale(){}
    /** Fraction of the screen height, at each end, over which a row shrinks. */
    static final float ZONE=.24f;
    /** Smallest scale, at the very edge. */
    static final float MIN=.82f;

    /** Scale for a row whose centre is at y (0 top … 1 bottom of the screen). */
    static float scale(float y){
        float edge=Math.min(y,1-y);if(edge>=ZONE)return 1;if(edge<=0)return MIN;
        float t=1-edge/ZONE;t=t*t*(3-2*t);return 1-(1-MIN)*t;
    }

    static void apply(ScrollView scroll,LinearLayout body){
        if(body==null||scroll.getHeight()==0)return;
        if(!scroll.getResources().getConfiguration().isScreenRound())return;
        int[] at=new int[2];scroll.getLocationOnScreen(at);
        float screen=scroll.getRootView().getHeight();if(screen<=0)return;
        each(body,at[1]-scroll.getScrollY(),screen);
    }
    private static void each(ViewGroup group,int originY,float screen){
        for(int i=0;i<group.getChildCount();i++){View row=group.getChildAt(i);
            if("hero".equals(row.getTag())||row.getVisibility()!=View.VISIBLE)continue;
            if(row instanceof LinearLayout&&("list".equals(row.getTag())||"chat".equals(row.getTag()))){each((ViewGroup)row,originY+row.getTop(),screen);continue;}
            if(row.getHeight()==0)continue;
            float centre=originY+row.getTop()+row.getHeight()/2f;
            float s=scale(centre/screen);
            if(Math.abs(row.getScaleX()-s)>.002f){row.setPivotX(row.getWidth()/2f);row.setPivotY(row.getHeight()/2f);row.setScaleX(s);row.setScaleY(s);}
        }
    }
}
