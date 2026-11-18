package si.zya.grokwear;

import org.junit.Test;
import static org.junit.Assert.*;

public class EdgeButtonTest {
    /** Every point of the pill's two end caps stays clear inside the circle. */
    private static void assertInside(float radius,float w,float h,float gap,float clear){
        float r=h/2,capX=w/2-r,capY=radius-gap-r;
        assertTrue(Math.hypot(capX,capY)+r<=radius-clear+.01f);
    }
    @Test public void pillFitsInsideRoundScreens(){
        for(float radius:new float[]{96,102,114}){
            float w=EdgeScale.edgeWidth(radius,EdgeScale.EDGE_H,EdgeScale.EDGE_GAP,EdgeScale.EDGE_CLEAR);
            assertInside(radius,w,EdgeScale.EDGE_H,EdgeScale.EDGE_GAP,EdgeScale.EDGE_CLEAR);
            // Wide enough for an icon and "Talk" at 15sp.
            assertTrue(w>=100);
        }
        // A 408 px Pixel Watch at 2x: about 124 dp.
        assertEquals(124,EdgeScale.edgeWidth(102,52,16,6),1);
    }
    @Test public void tinyScreenFallsBackToACircle(){assertEquals(52,EdgeScale.edgeWidth(30,52,16,6),.01);}

    @Test public void silentWhenAlwaysOrFollowingAQuietWatch(){
        assertTrue(NativeAudio.repliesSilent(true,false,false));
        assertTrue(NativeAudio.repliesSilent(false,true,true));
        assertFalse(NativeAudio.repliesSilent(false,true,false));
        assertFalse(NativeAudio.repliesSilent(false,false,true));
    }
}
