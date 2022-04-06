package ua.od.acros.dualsimtrafficcounter.utils;

import android.annotation.TargetApi;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

class CallLogger {

    private static final String ENUM_PHONE_STATE = Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN ?
            "com.android.internal.telephony.PhoneConstants$State" :
            "com.android.internal.telephony.Phone$State";
    private static final String CLASS_CALLS_MANAGER = "com.android.server.telecom.CallsManager";
    private static final String CLASS_CALL = "com.android.server.telecom.Call";
    //private static final String CLASS_PHONE_UTILS = " com.android.phone.PhoneUtils";
    private static final int CALL_STATE_ACTIVE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? 3 : 2;
    private static final String ENUM_CALL_STATE = "com.android.internal.telephony.Call$State";
    private static final String CLASS_ASYNC_RESULT = "android.os.AsyncResult";
    private static final String CLASS_CALL_NOTIFIER = "com.android.phone.CallNotifier";
    private static final String CLASS_IN_CALL_PRESENTER = "com.android.incallui.InCallPresenter";
    private static final String ENUM_IN_CALL_STATE = "com.android.incallui.InCallPresenter$InCallState";
    private static final String CLASS_CALL_LIST = "com.android.incallui.CallList";
    private static Object mOutgoingCall;
    private static final Bundle mActiveCallStartList = new Bundle();
    private static final Bundle mActiveCallSimList = new Bundle();
    private static int mSimQuantity;
    private static ArrayList<String> mList;

    private CallLogger(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        createHooks(loadPackageParam);
    }

    static CallLogger init(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        return new CallLogger(loadPackageParam);
    }

    private void createHooks(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        String name = loadPackageParam.packageName;
        ClassLoader classLoader = loadPackageParam.classLoader;
        if (name.equals(Constants.PACKAGE_NAME)) {
            XSharedPreferences preferences = new XSharedPreferences(Constants.PACKAGE_NAME);
            mSimQuantity = preferences.getInt(Constants.PREF_OTHER[55], 1);
            mList = new ArrayList<>(Arrays.asList(preferences.getString(Constants.PREF_OTHER[56], "").split(";")));
            XposedBridge.log(String.valueOf(mSimQuantity));
            XposedBridge.log(mList.toString());
        }
        if (name.contains("phone") && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            final Class<?> classCallNotifier = XposedHelpers.findClass(CLASS_CALL_NOTIFIER, classLoader);
            final Class<? extends Enum> enumPhoneState = (Class<? extends Enum>) Class.forName(ENUM_PHONE_STATE);
            final Class<? extends Enum> enumCallState = (Class<? extends Enum>) Class.forName(ENUM_CALL_STATE);
            if (CustomApplication.isOldMtkDevice()) {
                XposedHelpers.findAndHookMethod(classCallNotifier, "onDisconnect",
                        CLASS_ASYNC_RESULT, int.class, onDisconnectHook);
            } else {
                XposedHelpers.findAndHookMethod(classCallNotifier, "onDisconnect",
                        CLASS_ASYNC_RESULT, onDisconnectHook);
            }
            XposedBridge.log("onDisconnect hooked");
            XposedHelpers.findAndHookMethod(classCallNotifier, "onPhoneStateChanged", CLASS_ASYNC_RESULT, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = AndroidAppHelper.currentApplication();
                    final Object cm = XposedHelpers.getObjectField(param.thisObject, "mCM");
                    final Object phoneState = XposedHelpers.callMethod(cm, "getState");
                    if (phoneState == Enum.valueOf(enumPhoneState, "OFFHOOK")) {
                        final Object fgPhone = XposedHelpers.callMethod(cm, "getFgPhone");
                        final Object activeCall = getCurrentCall(fgPhone);
                        final Object conn = getConnection(fgPhone, activeCall);
                        if (activeCall != null) {
                            final Object callState = XposedHelpers.callMethod(activeCall, "getState");
                            int sim = MobileUtils.getActiveSimForCall(context, mSimQuantity);
                            if (mOutgoingCall == null && (callState == Enum.valueOf(enumCallState, "DIALING") ||
                                    callState == Enum.valueOf(enumCallState, "ALERTING"))) {
                                mOutgoingCall = activeCall;
                                XposedBridge.log("Outgoing call started: " + sim);
                                Intent i = new Intent(Constants.OUTGOING_CALL_STARTED);
                                i.putExtra(Constants.SIM_ACTIVE, sim);
                                context.sendBroadcast(i);
                            }
                            if (activeCall == mOutgoingCall && callState == Enum.valueOf(enumCallState, "ACTIVE") &&
                                    !(Boolean) XposedHelpers.callMethod(conn, "isIncoming")) {
                                XposedBridge.log("Outgoing call answered: " + sim);
                                Intent i = new Intent(Constants.OUTGOING_CALL_ANSWERED);
                                i.putExtra(Constants.SIM_ACTIVE, sim);
                                context.sendBroadcast(i);
                            }
                        }
                    }
                }
            });
            XposedBridge.log("onPhoneStateChanged hooked");
        } else if (name.contains("telecom") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Class<?> clsCallsManager = XposedHelpers.findClass(CLASS_CALLS_MANAGER, classLoader);
                XposedBridge.log(CLASS_CALLS_MANAGER + " found!");
                XposedHelpers.findAndHookMethod(clsCallsManager, "addCall", CLASS_CALL, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        onCallAdded(param.args[0]);
                    }
                });
                XposedBridge.log("addCall hooked");
                XposedHelpers.findAndHookMethod(clsCallsManager, "setCallState", CLASS_CALL, int.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        onCallStateChanged(param.args[0], (int) param.args[1]);
                    }
                });
                XposedBridge.log("setCallState hooked");
            } catch (Throwable t) {
                XposedBridge.log(CLASS_CALLS_MANAGER + " not found!");
            }
        } else if (name.contains("dialer") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                Class<?> mClassInCallPresenter = XposedHelpers.findClass(CLASS_IN_CALL_PRESENTER, classLoader);
                XposedBridge.log(CLASS_IN_CALL_PRESENTER + " found!");
                final Class<? extends Enum> enumInCallState = (Class<? extends Enum>) XposedHelpers.findClass(ENUM_IN_CALL_STATE,
                        classLoader);
                XposedBridge.hookAllMethods(mClassInCallPresenter, "setUp", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mOutgoingCall = null;
                    }
                });
                XposedBridge.log("setUp hooked");
                XposedBridge.hookAllMethods(mClassInCallPresenter, "onDisconnect", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object activeCall = param.args[0];
                        String key = (String) XposedHelpers.callMethod(activeCall, "getId");
                        long start;
                        int sim;
                        if (activeCall == mOutgoingCall) {
                            sim = mActiveCallSimList.getInt(key);
                            start = mActiveCallStartList.getLong(key);
                            long durationMillis = System.currentTimeMillis() - start;
                            XposedBridge.log(sim + " - Outgoing call ended: " + durationMillis / 1000 + "s");
                            Context context = AndroidAppHelper.currentApplication();
                            Intent i = new Intent(Constants.OUTGOING_CALL_ENDED);
                            i.putExtra(Constants.SIM_ACTIVE, sim);
                            i.putExtra(Constants.CALL_DURATION, durationMillis);
                            context.sendBroadcast(i);
                        }
                    }
                });
                XposedBridge.log("onDisconnect hooked");
                XposedHelpers.findAndHookMethod(mClassInCallPresenter, "getPotentialStateFromCallList",
                        CLASS_CALL_LIST, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                long start;
                                Context context = AndroidAppHelper.currentApplication();
                                Object state = param.getResult();
                                Object activeCall;
                                String key;
                                int sim = MobileUtils.getActiveSimForCall(context, mSimQuantity);
                                if (state == Enum.valueOf(enumInCallState, "OUTGOING") && mOutgoingCall == null) {
                                    activeCall = XposedHelpers.callMethod(param.args[0], "getOutgoingCall");
                                    if (activeCall != null && mOutgoingCall == null) {
                                        mOutgoingCall = activeCall;
                                        key = (String) XposedHelpers.callMethod(activeCall, "getId");
                                        mActiveCallSimList.putInt(key, sim);
                                        XposedBridge.log("Outgoing call started: " + sim);
                                        Intent i = new Intent(Constants.OUTGOING_CALL_STARTED);
                                        i.putExtra(Constants.SIM_ACTIVE, sim);
                                        context.sendBroadcast(i);
                                    }
                                }
                                if (state == Enum.valueOf(enumInCallState, "INCALL")) {
                                    activeCall = XposedHelpers.callMethod(param.args[0], "getActiveCall");
                                    if (activeCall != null) {
                                        key = (String) XposedHelpers.callMethod(activeCall, "getId");
                                        final int callState = (Integer) XposedHelpers.callMethod(activeCall, "getState");
                                        if (callState == CALL_STATE_ACTIVE && !mActiveCallStartList.containsKey(key) &&
                                                activeCall == mOutgoingCall) {
                                            start = System.currentTimeMillis();
                                            mActiveCallStartList.putLong(key, start);
                                            XposedBridge.log("Outgoing call answered: " + sim);
                                            Intent i = new Intent(Constants.OUTGOING_CALL_ANSWERED);
                                            i.putExtra(Constants.SIM_ACTIVE, sim);
                                            context.sendBroadcast(i);
                                        }
                                    }
                                }
                            }
                        });
                XposedBridge.log("getPotentialStateFromCallList hooked");
            } catch (Throwable t) {
                XposedBridge.log(CLASS_IN_CALL_PRESENTER + " not found!");
            }
        }
    }

    private void onCallAdded(Object call) {
        int state = (int) XposedHelpers.callMethod(call, "getState");
        onCallStateChanged(call, state);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void onCallStateChanged(Object call, int state) {
        Context context = AndroidAppHelper.currentApplication();
        XposedBridge.log(context.toString());
        String key = (String) XposedHelpers.callMethod(call, "getId");
        XposedBridge.log(key);
        String id = "null";
        PhoneAccountHandle phoneAccountHandle = (PhoneAccountHandle) XposedHelpers.callMethod(call, "getConnectionManagerPhoneAccount");
        if (phoneAccountHandle != null)
            id = phoneAccountHandle.getId();
        XposedBridge.log(id);
        TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        ArrayList<String> ids = new ArrayList<>();
        if (telecomManager != null) {
            for (PhoneAccountHandle pah : telecomManager.getCallCapablePhoneAccounts()) {
                if (pah != null)
                    ids.add(pah.getId());
            }
            XposedBridge.log(ids.toString());
        }
        int sim = ids.indexOf(id);
        long start;
        //int sim = MobileUtils.getActiveSimForCallM(context, mSimQuantity, mList);
        // register outgoing call
        if (state == CallState.DIALING && mOutgoingCall == null) {
            mOutgoingCall = call;
            mActiveCallSimList.putInt(key, sim);
            XposedBridge.log("Outgoing call started: " + sim);
            Intent i = new Intent(Constants.OUTGOING_CALL_STARTED);
            i.putExtra(Constants.SIM_ACTIVE, sim);
            context.sendBroadcast(i);
        }
        // outgoing call connected
        if (state == CallState.ACTIVE && call == mOutgoingCall && !mActiveCallStartList.containsKey(key)) {
            start = System.currentTimeMillis();
            mActiveCallStartList.putLong(key, start);
            XposedBridge.log("Outgoing call answered: " + sim);
            Intent i = new Intent(Constants.OUTGOING_CALL_ANSWERED);
            i.putExtra(Constants.SIM_ACTIVE, sim);
            context.sendBroadcast(i);
        }
        // handle call disconnected
        if (state == CallState.DISCONNECTED && call == mOutgoingCall) {
            sim = mActiveCallSimList.getInt(key);
            start = mActiveCallStartList.getLong(key);
            if (start == 0)
                XposedBridge.log(sim + " - Outgoing call ended without answer");
            else {
                long finish = System.currentTimeMillis();
                long durationMillis = finish - start;
                XposedBridge.log(sim + " - Outgoing call ended: " + durationMillis / 1000 + "s");
                Intent i = new Intent(Constants.OUTGOING_CALL_ENDED);
                i.putExtra(Constants.SIM_ACTIVE, sim);
                i.putExtra(Constants.CALL_DURATION, durationMillis);
                context.sendBroadcast(i);
            }
            mOutgoingCall = null;
        }
    }

    private final XC_MethodHook onDisconnectHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            try {
                Context context = AndroidAppHelper.currentApplication();
                Object conn = XposedHelpers.getObjectField(param.args[0], "result");
                if (conn != null && !(Boolean) XposedHelpers.callMethod(conn, "isIncoming")) {
                    Object call = XposedHelpers.callMethod(conn, "getCall");
                    if (call == mOutgoingCall) {
                        mOutgoingCall = null;
                        Object phone = XposedHelpers.callMethod(call, "getPhone");
                        String imei = (String) XposedHelpers.callMethod(phone, "getDeviceId");
                        XposedBridge.log(imei + "\n");
                        ArrayList<String> id = MobileUtils.getDeviceIds(context, mSimQuantity);
                        XposedBridge.log(id.toString() + "\n");
                        int sim = Constants.DISABLED;
                        for (int i = 0; i < id.size(); i++) {
                            if (imei.equals(id.get(i)))
                                sim = i;
                        }
                        long durationMillis = (long) XposedHelpers.callMethod(conn, "getDurationMillis");
                        XposedBridge.log(sim + " - Outgoing call ended: " + durationMillis / 1000 + "s");
                        Intent i = new Intent(Constants.OUTGOING_CALL_ENDED);
                        i.putExtra(Constants.SIM_ACTIVE, sim);
                        i.putExtra(Constants.CALL_DURATION, durationMillis);
                        context.sendBroadcast(i);
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    };

    private static Object getCurrentCall(Object phone) {
        try {
            Object ringing = XposedHelpers.callMethod(phone, "getRingingCall");
            Object fg = XposedHelpers.callMethod(phone, "getForegroundCall");
            Object bg = XposedHelpers.callMethod(phone, "getBackgroundCall");
            if (!(Boolean) XposedHelpers.callMethod(ringing, "isIdle")) {
                return ringing;
            }
            if (!(Boolean) XposedHelpers.callMethod(fg, "isIdle")) {
                return fg;
            }
            if (!(Boolean) XposedHelpers.callMethod(bg, "isIdle")) {
                return bg;
            }
            return fg;
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }

    private static Object getConnection(Object phone, Object call) {
        if (call == null)
            return null;
        try {
            if ((Integer) XposedHelpers.callMethod(phone, "getPhoneType") ==
                    TelephonyManager.PHONE_TYPE_CDMA) {
                return XposedHelpers.callMethod(call, "getLatestConnection");
            }
            return XposedHelpers.callMethod(call, "getEarliestConnection");
        } catch (Throwable t) {
            XposedBridge.log(t);
            return null;
        }
    }

    private static class CallState {
        private CallState() {}

        /**
         * Indicates that a call is new and not connected. This is used as the default state internally
         * within Telecom and should not be used between Telecom and call services. Call services are
         * not expected to ever interact with NEW calls, but {@link android.telecom.InCallService}s will
         * see calls in this state.
         */
        static final int NEW = 0;

        /**
         * The initial state of an outgoing {@code Call}.
         * Common transitions are to {@link #DIALING} state for a successful call or
         * {@link #DISCONNECTED} if it failed.
         */
        static final int CONNECTING = 1;

        /**
         * The state of an outgoing {@code Call} when waiting on user to select a
         * {@link android.telecom.PhoneAccount} through which to place the call.
         */
        static final int SELECT_PHONE_ACCOUNT = 2;

        /**
         * Indicates that a call is outgoing and in the dialing state. A call transitions to this state
         * once an outgoing call has begun (e.g., user presses the dial button in Dialer). Calls in this
         * state usually transition to {@link #ACTIVE} if the call was answered or {@link #DISCONNECTED}
         * if the call was disconnected somehow (e.g., failure or cancellation of the call by the user).
         */
        static final int DIALING = 3;

        /**
         * Indicates that a call is incoming and the user still has the option of answering, rejecting,
         * or doing nothing with the call. This state is usually associated with some type of audible
         * ringtone. Normal transitions are to {@link #ACTIVE} if answered or {@link #DISCONNECTED}
         * otherwise.
         */
        static final int RINGING = 4;

        /**
         * Indicates that a call is currently connected to another party and a communication channel is
         * open between them. The normal transition to this state is by the user answering a
         * {@link #DIALING} call or a {@link #RINGING} call being answered by the other party.
         */
        static final int ACTIVE = 5;

        /**
         * Indicates that the call is currently on hold. In this state, the call is not terminated
         * but no communication is allowed until the call is no longer on hold. The typical transition
         * to this state is by the user putting an {@link #ACTIVE} call on hold by explicitly performing
         * an action, such as clicking the hold button.
         */
        static final int ON_HOLD = 6;

        /**
         * Indicates that a call is currently disconnected. All states can transition to this state
         * by the call service giving notice that the connection has been severed. When the user
         * explicitly ends a call, it will not transition to this state until the call service confirms
         * the disconnection or communication was lost to the call service currently responsible for
         * this call (e.g., call service crashes).
         */
        static final int DISCONNECTED = 7;

        /**
         * Indicates that the call was attempted (mostly in the context of outgoing, at least at the
         * time of writing) but cancelled before it was successfully connected.
         */
        static final int ABORTED = 8;

        /**
         * Indicates that the call is in the process of being disconnected and will transition next
         * to a {@link #DISCONNECTED} state.
         * <p>
         * This state is not expected to be communicated from the Telephony layer, but will be reported
         * to the InCall UI for calls where disconnection has been initiated by the user but the
         * ConnectionService has confirmed the call as disconnected.
         */
        static final int DISCONNECTING = 9;

        public String toString(int callState) {
            switch (callState) {
                case NEW:
                    return "NEW";
                case CONNECTING:
                    return "CONNECTING";
                case SELECT_PHONE_ACCOUNT:
                    return "SELECT_PHONE_ACCOUNT";
                case DIALING:
                    return "DIALING";
                case RINGING:
                    return "RINGING";
                case ACTIVE:
                    return "ACTIVE";
                case ON_HOLD:
                    return "ON_HOLD";
                case DISCONNECTED:
                    return "DISCONNECTED";
                case ABORTED:
                    return "ABORTED";
                case DISCONNECTING:
                    return "DISCONNECTING";
                default:
                    return "UNKNOWN";
            }
        }
    }
}

