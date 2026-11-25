// From the repository root, with a JDK, native development headers and Xvfb:
// c++ -std=c++17 -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" scripts/linux/test-player-overlay-teardown.cpp $(pkg-config --cflags --libs webkit2gtk-4.1 mpv x11 xcomposite) -pthread -ldl -o "$TMPDIR/overlay-test"
// xvfb-run -a "$TMPDIR/overlay-test"
#include "../../composeApp/src/desktopMain/native/linux/player_bridge.cpp"

int main(int argc, char **argv) {
    gdk_set_allowed_backends("x11");
    gtk_init(&argc, &argv);
    auto *display = gdk_display_get_default();
    auto *x = GDK_DISPLAY_XDISPLAY(display);
    auto host = XCreateSimpleWindow(x, DefaultRootWindow(x), 0, 0, 160, 90, 0, 0, 0);
    XMapWindow(x, host);
    XSync(x, False);
    auto *hostWrapper = gdk_x11_window_foreign_new_for_display(display, host);
    auto initialHostRefs = G_OBJECT(hostWrapper)->ref_count;
    int destroyed = 0;
    auto watchManager = [&](WebKitWebView *view) {
        auto *manager = webkit_web_view_get_user_content_manager(view);
        g_object_weak_ref(G_OBJECT(manager), +[](gpointer count, GObject *) {
            ++*static_cast<int *>(count);
        }, &destroyed);
    };

    // Exercise a cold open, warm-view adoption/replenishment, and warm shutdown.
    constexpr int opens = 8;
    for (int i = 0; i < opens; ++i) {
        if (i == 1) warmupOnGtk(new std::string("about:blank"));
        Player player;
        gLivePlayers.insert(&player);
        createWebviewOnGtk(new WebviewSetup{&player, host, "about:blank"});
        watchManager(player.webview);
        gLivePlayers.erase(&player);
        destroyWebviewOnGtk(&player);
    }
    watchManager(gWarmupView);
    Java_com_nuvio_app_features_player_desktop_NativePlayerBridge_shutdownWebView2Warmup(nullptr, nullptr);
    auto deadline = g_get_monotonic_time() + 2 * G_TIME_SPAN_SECOND;
    while ((destroyed != opens + 1 || G_OBJECT(hostWrapper)->ref_count != initialHostRefs) &&
           g_get_monotonic_time() < deadline) {
        g_main_context_iteration(nullptr, FALSE);
        g_usleep(1000);
    }
    auto finalHostRefs = G_OBJECT(hostWrapper)->ref_count;
    printf("controls managers finalized: %d/%d; foreign host refs: %u -> %u\n",
           destroyed, opens + 1, initialHostRefs, finalHostRefs);
    g_object_unref(hostWrapper);
    XDestroyWindow(x, host);
    return destroyed == opens + 1 && initialHostRefs == finalHostRefs ? 0 : 1;
}
