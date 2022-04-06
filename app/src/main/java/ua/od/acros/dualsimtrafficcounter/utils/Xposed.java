package ua.od.acros.dualsimtrafficcounter.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Xposed implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static final List<String> PACKAGE_NAMES = new ArrayList<>(Arrays.asList(
            "com.google.android.dialer", "com.android.dialer", "com.android.phone", "com.android.server.telecom"));

    private XSharedPreferences prefs;

    /**
     * Called very early during startup of Zygote.
     *
     * @param startupParam Details about the module itself and the started process.
     * @throws Throwable everything is caught, but will prevent further initialization of the module.
     */
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(Constants.PACKAGE_NAME);
    }

    /**
     * This method is called when an app is loaded. It's called very early, even before
     * {@link Application#onCreate} is called.
     * Modules can set up their app-specific hooks here.
     *
     * @param lpparam Information about the app.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android") && lpparam.processName.equals("android")) {
            XposedBridge.log("ModileUtils loaded");
            MobileUtils.initAndroid(lpparam.classLoader);
        }
        String name = lpparam.packageName;
        if (PACKAGE_NAMES.contains(name) || name.equals(Constants.PACKAGE_NAME)) {
            XposedBridge.log("Loaded app: " + name);
            CallLogger.init(lpparam);
        }
    }
}
