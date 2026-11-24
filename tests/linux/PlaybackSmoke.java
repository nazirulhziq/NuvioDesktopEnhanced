import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Frame;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import com.sun.jna.Native;

/** Exercise the packaged Kotlin/JNI pair without accounts or remote media. */
public class PlaybackSmoke {
    public static void main(String[] args) throws Exception {
        var bridge = Class.forName("com.nuvio.app.features.player.desktop.NativePlayerBridge");
        Object player = bridge.getField("INSTANCE").get(null);
        bridge.getMethod("ensureLoaded").invoke(player);
        if (!Boolean.TRUE.equals(bridge.getMethod("initGtkEarly").invoke(player))) throw new AssertionError("GTK init");
        Canvas canvas = new Canvas();
        EventQueue.invokeAndWait(() -> {
            Frame frame = new Frame("Nuvio playback smoke");
            frame.add(canvas); frame.setSize(640, 360); frame.setVisible(true);
        });
        Method create = Arrays.stream(bridge.getMethods()).filter(m -> m.getName().equals("create")).findFirst().orElseThrow();
        var types = create.getParameterTypes();
        var ready = new AtomicBoolean();
        Object sink = Proxy.newProxyInstance(bridge.getClassLoader(), new Class[]{types[types.length - 1]}, (p,m,a) -> {
            if (m.getName().equals("onPlayerEvent") && "controlsReady".equals(a[0])) ready.set(true);
            return null;
        });
        var parameters = new java.util.ArrayList<Object>();
        parameters.add(Native.getComponentID(canvas)); parameters.add(args[0]);
        if (types[2] == String.class) parameters.add(""); // fork accepts a separate audio URL
        parameters.add(new String[0]); parameters.add(true); parameters.add(0L);
        parameters.add(bridge.getMethod("getControlsPageUrl").invoke(player)); parameters.add(2); // software decoder on nested X11
        parameters.add(false); parameters.add(false); parameters.add(sink);
        long handle = (long) create.invoke(player, parameters.toArray());
        if (handle == 0) throw new AssertionError("create returned no player");
        var position = bridge.getMethod("positionMs", long.class);
        long ms = 0;
        for (int i = 0; i < 75; i++) {
            Thread.sleep(200); ms = (long) position.invoke(player, handle);
            if (ms >= 1000 && ready.get()) break;
        }
        System.out.printf("playback=%dms controlsReady=%s%n", ms, ready.get());
        bridge.getMethod("dispose", long.class).invoke(player, handle);
        System.exit(ms >= 1000 && ready.get() ? 0 : 1);
    }
}
