package ua.od.acros.dualsimtrafficcounter.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

/**
 * This implementation provides multiple windows. You may extend this class or
 * use it as a reference for a basic foundation for your own windows.
 * 
 * <p>
 * Functionality includes system window decorators, moveable, resizeable,
 * hideable, closeable, and bring-to-frontable.
 * 
 * <p>
 * The persistent notification creates new windows. The hidden notifications
 * restores previously hidden windows.
 * 
 * @author Mark Wei <markwei@gmail.com>
 * 
 */
public class FloatingWindowService extends StandOutWindow {
    private Context mContext;
    private SharedPreferences mPrefs;
    private int mY;
    private float mX;

    @Override
	public final String getAppName() {
		return getString(R.string.app_name);
	}

	@Override
	public final Bitmap getAppIcon() {
		return BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
	}

    /**
     * Return the icon resource for every window in this implementation. The
     * icon will appear in the default implementations of notifications.
     *
     * @return The icon.
     */
    @Override
    public final int getNotificationIcon() {
        return R.drawable.ic_launcher_small;
    }

    @Override
	public final String getTitle(int id) {
		return getAppName() + " " + id;
	}

    @Override
    public final void onCreate() {
        super.onCreate();
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        if (mPrefs == null)
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Timer timer = new Timer();
        final Handler handler = new Handler();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    try {
                        CheckServiceRunning performBackgroundTask = new CheckServiceRunning();
                        performBackgroundTask.execute(mContext, mPrefs);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 60000);
    }

	@Override
	public final void createAndAttachView(int id, FrameLayout frame) {
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        if (mPrefs == null)
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        // create a new layout from .xml
        //LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = View.inflate(mContext, R.layout.floating_window, frame);
        TextView status = view.findViewById(R.id.tv);
        String changedText = DataFormat.formatData(mContext, 0L);
        status.setTextSize(Integer.valueOf(Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[33], "10"))));
        status.setBackgroundColor(mPrefs.getInt(Constants.PREF_OTHER[35], ContextCompat.getColor(mContext, android.R.color.transparent)));
        status.setTextColor(mPrefs.getInt(Constants.PREF_OTHER[34], ContextCompat.getColor(mContext, R.color.widget_text)));
        status.setText(changedText);
    }

    @Override
	public final Animation getShowAnimation(int id) {
		if (isExistingId(id)) {
			// restore
			return AnimationUtils.loadAnimation(this,
					android.R.anim.slide_in_left);
		} else {
			// show
			return super.getShowAnimation(id);
		}
	}

	@Override
	public final Animation getHideAnimation(int id) {
		return AnimationUtils.loadAnimation(this,
				android.R.anim.slide_out_right);
	}

    @Override
    public final void onMove(int id, Window window, View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            //case MotionEvent.ACTION_MOVE:
                int newId = mPrefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID);
                if (newId == id) {
                    int[] location = new int[2];
                    window.getLocationOnScreen(location);
                    mY = location[1];
                    mX = getWindowPositionRelative(location[0]);
                    mPrefs.edit()
                            .putInt(Constants.PREF_OTHER[36], -2)
                            .putFloat(Constants.PREF_OTHER[54], mX)
                            .putInt(Constants.PREF_OTHER[37], mY)
                            .apply();
                }
                break;
        }
    }

	@Override
	public final void onReceiveData(int id, int requestCode, Bundle data,
                                    Class<? extends StandOutWindow> fromCls, int fromId) {
		// receive data from WidgetsWindow's button press
		// to show off the data sending framework
		switch (requestCode) {
            case Constants.FLOATING_WINDOW:
                Window window = getWindow(id);
                if (window == null) {
                    String errorText = String.format(Locale.US,
                            "%s received data but Window id: %d is not open.",
                            getAppName(), id);
                    Toast.makeText(mContext, errorText, Toast.LENGTH_SHORT).show();
                    return;
                }

                int newId = mPrefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID);
                if (newId == id) {
                    mPrefs.edit()
                            .putInt(Constants.PREF_OTHER[36], -2)
                            .putFloat(Constants.PREF_OTHER[54], mX)
                            .putInt(Constants.PREF_OTHER[37], mY)
                            .apply();
                }
                long seconds = System.currentTimeMillis() / 1000L;
                String changedText = "";
                int textSize = Integer.valueOf(Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[33], "10")));
                String choice = mPrefs.getString(Constants.PREF_OTHER[53], "0");
                switch (Objects.requireNonNull(choice)) {
                    case "0":
                        if (data.getLong("total") == -1)
                            changedText = mContext.getString(R.string.not_set);
                        else
                            changedText = DataFormat.formatData(mContext, data.getLong("total"));
                        break;
                    case "1":
                        changedText = String.format(getResources().getString(R.string.speed),
                                DataFormat.formatData(mContext, data.getLong(Constants.SPEEDRX, 0L))) +
                                "\n" + String.format(getResources().getString(R.string.speed),
                                DataFormat.formatData(mContext, data.getLong(Constants.SPEEDTX, 0L)));
                        textSize = (int) ((double) textSize * 0.5);
                        break;
                    case "2":
                        if (seconds % 3 == 0) {
                            changedText = String.format(getResources().getString(R.string.speed),
                                    DataFormat.formatData(mContext, data.getLong(Constants.SPEEDRX, 0L))) +
                                    "\n" + String.format(getResources().getString(R.string.speed),
                                    DataFormat.formatData(mContext, data.getLong(Constants.SPEEDTX, 0L)));
                            textSize = (int) ((double) textSize * 0.5);
                        } else {
                            if (data.getLong("total") == -1)
                                changedText = mContext.getString(R.string.not_set);
                            else
                                changedText = DataFormat.formatData(mContext, data.getLong("total"));
                        }
                        break;
                }
                TextView status = window.findViewById(R.id.tv);
                status.setTextSize(textSize);
                int textColor = mPrefs.getInt(Constants.PREF_OTHER[34], ContextCompat.getColor(mContext, R.color.widget_text));
                if (mPrefs.getBoolean(Constants.PREF_OTHER[52], true) && data.getBoolean("flash", false) && seconds % 2 == 0) {
                    String alpha = Integer.toHexString(textColor).substring(0, 2);
                    String color = Integer.toHexString(textColor).substring(2);
                    textColor = 0xFFFFFF - Integer.parseInt(color, 16);
                    color = alpha + Integer.toHexString(textColor);
                    textColor = (int) Long.parseLong(color, 16);
                }
                status.setTextColor(textColor);
                status.setBackgroundColor(mPrefs.getInt(Constants.PREF_OTHER[35], ContextCompat.getColor(mContext, android.R.color.transparent)));
                status.setText(changedText);
                break;
            default:
                Log.d("MultiWindow", "Unexpected data received.");
                break;
        }
	}

    @Override
    public final StandOutLayoutParams getParams(int id, Window window) {
        int x = mPrefs.getInt(Constants.PREF_OTHER[36], -1);
        if (x == -2) {
            mX = mPrefs.getFloat(Constants.PREF_OTHER[54], -1.0f);
            if (mX < 0)
                mX = 0.5f;
            x = getWindowPosition(mX);
        }
        else if (x == -1)
            x = StandOutLayoutParams.CENTER;
        mY = mPrefs.getInt(Constants.PREF_OTHER[37], -1);
        if (mY < 0)
            mY = StandOutLayoutParams.CENTER;
        return new StandOutLayoutParams(id, StandOutLayoutParams.WRAP_CONTENT, StandOutLayoutParams.WRAP_CONTENT, x, mY);
    }

    @Override
    public final int getFlags(int id) {
        return StandOutFlags.FLAG_BODY_MOVE_ENABLE
                | StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE
                | StandOutFlags.FLAG_WINDOW_HIDE_ENABLE
                | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
                | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE ;
    }

    @Override
    public final boolean onClose(int id, Window window) {
        int newId = mPrefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID);
        if (newId == id) {
            mPrefs.edit()
                    .putInt(Constants.PREF_OTHER[36], -2)
                    .putFloat(Constants.PREF_OTHER[54], mX)
                    .putInt(Constants.PREF_OTHER[37], mY)
                    .apply();
        }
        return false;
    }

    @Override
    public final void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean autoLoad = mPrefs.getBoolean(Constants.PREF_OTHER[47], false);
        boolean floatingWindow = mPrefs.getBoolean(Constants.PREF_OTHER[32], false);
        boolean alwaysShow = !mPrefs.getBoolean(Constants.PREF_OTHER[41], false);
        boolean mobileData = MobileUtils.isMobileDataActive(mContext);
        boolean bool = (autoLoad && mobileData) || (!autoLoad && (alwaysShow || mobileData));
        if (floatingWindow && bool)
            FloatingWindowService.showFloatingWindow(mContext, mPrefs);
    }

    public static void showFloatingWindow(Context context, SharedPreferences preferences) {
        closeFloatingWindow(context, preferences);
        int id = Math.abs(new SecureRandom().nextInt());
        preferences.edit()
                .putInt(Constants.PREF_OTHER[38], id)
                .apply();
        StandOutWindow.show(context, FloatingWindowService.class, id);
    }

    public static void closeFloatingWindow(Context context, SharedPreferences preferences) {
        int id = preferences.getInt(Constants.PREF_OTHER[38], -1);
        if (id >= 0)
            StandOutWindow.close(context, FloatingWindowService.class, id);
        else
            StandOutWindow.closeAll(context, FloatingWindowService.class);
    }

    private float getWindowPositionRelative(int x) {
        int width = getDisplayMetrics().widthPixels;
        return (float) x / width;
    }

    private int getWindowPosition(float x) {
        int width = getDisplayMetrics().widthPixels;
        return (int) (x * width);
    }

    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        }
        return displayMetrics;
    }

    private static class CheckServiceRunning extends AsyncTask<Object, Void, Void> {

        @Override
        protected final Void doInBackground(Object... params) {
            Context context = (Context) params[0];
            SharedPreferences prefs = (SharedPreferences) params[1];
            boolean floatingWindow = prefs.getBoolean(Constants.PREF_OTHER[32], false);
            boolean alwaysShow = !prefs.getBoolean(Constants.PREF_OTHER[41], false) && !prefs.getBoolean(Constants.PREF_OTHER[47], false);
            boolean bool = floatingWindow && !alwaysShow;
            if (bool && !CustomApplication.isMyServiceRunning(TrafficCountService.class))
                closeFloatingWindow(context, prefs);
            return null;
        }
    }
}
