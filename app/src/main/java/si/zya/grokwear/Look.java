package si.zya.grokwear;

import android.content.Context;
import android.os.Build;

/**
 * The app's Material 3 colours for Wear OS: the watch's own dynamic palette where Wear OS gives one (Wear OS 6, API 36+,
 * from the watch face), else Wear's baseline dark scheme. Read once per screen draw, so a palette change or the
 * Settings switch shows on the next screen.
 */
final class Look {
    private Look(){}
    static int primary,primaryDim,onPrimary,primaryContainer,onPrimaryContainer,
        secondary,secondaryContainer,onSecondaryContainer,tertiary,tertiaryContainer,onTertiaryContainer,
        surfaceLow,surface,surfaceHigh,onSurface,onSurfaceVariant,outline,outlineVariant,
        error,onError,errorContainer,onErrorContainer;
    static final int BACKGROUND=0xff000000;
    static boolean dynamic;
    static{baseline();}

    /** Wear OS's baseline dark scheme (Material 3 for Wear). */
    static void baseline(){
        dynamic=false;
        primary=0xffd3e3fd;primaryDim=0xffa8c7fa;onPrimary=0xff041e49;primaryContainer=0xff0842a0;onPrimaryContainer=0xffd3e3fd;
        secondary=0xffc2e7ff;secondaryContainer=0xff004a77;onSecondaryContainer=0xffc2e7ff;
        tertiary=0xffc4eed0;tertiaryContainer=0xff0f5223;onTertiaryContainer=0xffc4eed0;
        surfaceLow=0xff1b1b1b;surface=0xff252626;surfaceHigh=0xff303232;onSurface=0xffe3e3e3;onSurfaceVariant=0xffc4c7c5;outline=0xff8e918f;outlineVariant=0xff444746;
        error=0xfff2b8b5;onError=0xff601410;errorContainer=0xff8c1d18;onErrorContainer=0xfff9dedc;
    }

    static boolean watchColors(Context context){try{return context.getSharedPreferences("grokwear_ui",Context.MODE_PRIVATE).getBoolean("watch_colors",true);}catch(RuntimeException unavailable){return true;}}
    static void setWatchColors(Context context,boolean on){try{context.getSharedPreferences("grokwear_ui",Context.MODE_PRIVATE).edit().putBoolean("watch_colors",on).apply();}catch(RuntimeException ignored){}}
    /** Dynamic colour exists on phones from API 31, but Wear OS only fills it from the watch face from Wear OS 6. */
    static boolean dynamicAvailable(){return Build.VERSION.SDK_INT>=36;}

    static void load(Context context,boolean preview){
        baseline();
        if(preview||!dynamicAvailable()||!watchColors(context))return;
        try{
            primary=c(context,android.R.color.system_accent1_100);primaryDim=c(context,android.R.color.system_accent1_200);onPrimary=c(context,android.R.color.system_accent1_900);
            primaryContainer=c(context,android.R.color.system_accent1_700);onPrimaryContainer=c(context,android.R.color.system_accent1_100);
            secondary=c(context,android.R.color.system_accent2_100);secondaryContainer=c(context,android.R.color.system_accent2_700);onSecondaryContainer=c(context,android.R.color.system_accent2_100);
            tertiary=c(context,android.R.color.system_accent3_100);tertiaryContainer=c(context,android.R.color.system_accent3_700);onTertiaryContainer=c(context,android.R.color.system_accent3_100);
            surfaceLow=c(context,android.R.color.system_neutral1_900);surface=mix(c(context,android.R.color.system_neutral2_900),c(context,android.R.color.system_neutral2_800),.5f);
            surfaceHigh=c(context,android.R.color.system_neutral2_800);onSurface=c(context,android.R.color.system_neutral1_50);onSurfaceVariant=c(context,android.R.color.system_neutral2_200);
            outline=c(context,android.R.color.system_neutral2_400);outlineVariant=c(context,android.R.color.system_neutral2_700);
            dynamic=true;
        }catch(RuntimeException missing){baseline();}
    }
    private static int c(Context context,int id){return context.getColor(id)|0xff000000;}

    static int mix(int a,int b,float t){
        int ar=(a>>16)&255,ag=(a>>8)&255,ab=a&255,br=(b>>16)&255,bg=(b>>8)&255,bb=b&255;
        return 0xff000000|Math.round(ar+(br-ar)*t)<<16|Math.round(ag+(bg-ag)*t)<<8|Math.round(ab+(bb-ab)*t);
    }
    static int alpha(int color,float a){return (Math.round(a*255)<<24)|(color&0xffffff);}
    /** WCAG contrast ratio of two opaque colours. */
    static double contrast(int a,int b){double la=luminance(a),lb=luminance(b);return (Math.max(la,lb)+.05)/(Math.min(la,lb)+.05);}
    static double luminance(int color){
        double[] rgb={((color>>16)&255)/255d,((color>>8)&255)/255d,(color&255)/255d};
        for(int i=0;i<3;i++)rgb[i]=rgb[i]<=.04045?rgb[i]/12.92:Math.pow((rgb[i]+.055)/1.055,2.4);
        return .2126*rgb[0]+.7152*rgb[1]+.0722*rgb[2];
    }

    // Material 3 for Wear type scale, in sp.
    static final float DISPLAY_S=24,TITLE_L=20,TITLE_M=16,TITLE_S=14,LABEL_L=20,LABEL_M=15,LABEL_S=13,BODY_L=16,BODY_M=14,BODY_S=12,BODY_XS=10;
}
