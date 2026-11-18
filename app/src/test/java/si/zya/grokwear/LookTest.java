package si.zya.grokwear;

import org.junit.Test;
import static org.junit.Assert.*;

public class LookTest {
    @Test public void baselineTextReadsOnItsContainers(){
        Look.baseline();
        // WCAG AA for body text: 4.5:1.
        assertTrue(Look.contrast(Look.onPrimary,Look.primary)>=4.5);
        assertTrue(Look.contrast(Look.onPrimaryContainer,Look.primaryContainer)>=4.5);
        assertTrue(Look.contrast(Look.onSurface,Look.surface)>=4.5);
        assertTrue(Look.contrast(Look.onSurfaceVariant,Look.surface)>=4.5);
        assertTrue(Look.contrast(Look.onSecondaryContainer,Look.secondaryContainer)>=4.5);
        assertTrue(Look.contrast(Look.error,Look.BACKGROUND)>=4.5);
    }
    @Test public void mixAndAlpha(){
        assertEquals(0xff000000,Look.mix(0xff000000,0xffffffff,0));
        assertEquals(0xffffffff,Look.mix(0xff000000,0xffffffff,1));
        assertEquals(0xff808080,Look.mix(0xff000000,0xffffffff,.5f)|0x000000);
        assertEquals(0x80123456,Look.alpha(0xff123456,128/255f));
    }
    @Test public void rowsShrinkOnlyNearTheEdges(){
        assertEquals(1f,EdgeScale.scale(.5f),0);
        assertEquals(1f,EdgeScale.scale(EdgeScale.ZONE),0);
        assertEquals(EdgeScale.MIN,EdgeScale.scale(0),0);
        assertEquals(EdgeScale.MIN,EdgeScale.scale(1),0);
        assertEquals(EdgeScale.MIN,EdgeScale.scale(-.3f),0);
        float a=EdgeScale.scale(.05f),b=EdgeScale.scale(.15f);
        assertTrue(a<b&&b<1&&a>EdgeScale.MIN);
        assertEquals(EdgeScale.scale(.1f),EdgeScale.scale(.9f),1e-6);
    }
}
