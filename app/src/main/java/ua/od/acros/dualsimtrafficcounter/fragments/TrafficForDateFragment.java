package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.lang.ref.WeakReference;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;


public class TrafficForDateFragment extends Fragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{

    private int mYear, mMonth, mDay;
    private static int mSimChecked = Constants.NULL;
    private static int mSimQuantity;
    private static WeakReference<AppCompatButton> bSetDate, bOK;
    private static WeakReference<ProgressBar> pb;
    private static WeakReference<RadioGroup> radioGroup;

    private OnFragmentInteractionListener mListener;

    private TextView RX, TX, RXN, TXN, TOT, TOTN, day, night;
    private static WeakReference<TextView> wRX, wTX, wRXN, wTXN, wTOT, wTOTN, wDay, wNight;
    private String[] mOperatorNames = new String[3];
    private Context mContext;
    private static SharedPreferences mPrefs;
    private static boolean mIsDetached;

    public static TrafficForDateFragment newInstance() {
        return new TrafficForDateFragment();
    }

    public TrafficForDateFragment() {
        // Required empty public constructor
    }

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDay = DateTime.now().toLocalDate().getDayOfMonth();
        mMonth = DateTime.now().toLocalDate().getMonthOfYear();
        mYear = DateTime.now().toLocalDate().getYear();
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        View view = inflater.inflate(R.layout.traffic_for_date_fragment, container, false);
        ProgressBar pBar = view.findViewById(R.id.progressBar);
        pb = new WeakReference<>(pBar);
        pBar.setVisibility(View.GONE);
        RadioGroup rGroup = view.findViewById(R.id.sim_group);
        radioGroup = new WeakReference<>(rGroup);
        AppCompatButton bDate = view.findViewById(R.id.setdate);
        bSetDate = new WeakReference<>(bDate);
        bDate.setOnClickListener(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSimQuantity = mPrefs.getInt(Constants.PREF_OTHER[55], 1);
        AppCompatRadioButton sim1rb = view.findViewById(R.id.sim1RB);
        sim1rb.setText(mOperatorNames[0]);
        AppCompatRadioButton sim2rb = view.findViewById(R.id.sim2RB);
        sim2rb.setText(mOperatorNames[1]);
        AppCompatRadioButton sim3rb = view.findViewById(R.id.sim3RB);
        sim3rb.setText(mOperatorNames[2]);
        if (mSimQuantity == 1) {
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        if (mSimQuantity == 2)
            sim3rb.setEnabled(false);
        rGroup.setOnCheckedChangeListener(this);
        RX = view.findViewById(R.id.rx);
        wRX = new WeakReference<>(RX);
        TX = view.findViewById(R.id.tx);
        wTX = new WeakReference<>(TX);
        RXN = view.findViewById(R.id.rxnight);
        wRXN = new WeakReference<>(RXN);
        TXN = view.findViewById(R.id.txnight);
        wTXN = new WeakReference<>(TXN);
        TOT = view.findViewById(R.id.total);
        wTOT = new WeakReference<>(TOT);
        TOTN = view.findViewById(R.id.totalnight);
        wTOTN = new WeakReference<>(TOTN);
        day = view.findViewById(R.id.day);
        wDay = new WeakReference<>(day);
        night = view.findViewById(R.id.night);
        wNight = new WeakReference<>(night);
        AppCompatButton butOK = view.findViewById(R.id.buttonOK);
        bOK = new WeakReference<>(butOK);
        butOK.setOnClickListener(this);
        butOK.setEnabled(false);
        bDate.setOnClickListener(this);
        bDate.setEnabled(false);
        RXN.setVisibility(View.GONE);
        TXN.setVisibility(View.GONE);
        TOTN.setVisibility(View.GONE);
        night.setVisibility(View.GONE);
        if (savedInstanceState != null) {
            int sim = savedInstanceState.getInt("sim");
            switch (sim) {
                case Constants.SIM1:
                    sim1rb.setChecked(true);
                    break;
                case Constants.SIM2:
                    sim2rb.setChecked(true);
                    break;
                case Constants.SIM3:
                    sim3rb.setChecked(true);
                    break;
            }
            if (sim >= 0) {
                day.setText(savedInstanceState.getString("day"));
                night.setText(savedInstanceState.getString("night"));
                RX.setText(savedInstanceState.getString("rx"));
                RXN.setText(savedInstanceState.getString("rxn"));
                TX.setText(savedInstanceState.getString("tx"));
                TXN.setText(savedInstanceState.getString("txn"));
                TOT.setText(savedInstanceState.getString("tot"));
                TOTN.setText(savedInstanceState.getString("totn"));
                bDate.setText(savedInstanceState.getString("set"));
                bDate.setEnabled(true);
                butOK.setEnabled(true);
            }
        }
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public final void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isVisible()) {
            outState.putInt("sim", mSimChecked);
            outState.putString("day", day.getText().toString());
            outState.putString("night", night.getText().toString());
            outState.putString("rx", RX.getText().toString());
            outState.putString("tx", TX.getText().toString());
            outState.putString("tot", TOT.getText().toString());
            outState.putString("rxn", RXN.getText().toString());
            outState.putString("txn", TXN.getText().toString());
            outState.putString("totn", TOTN.getText().toString());
            outState.putString("set", bSetDate.get().getText().toString());
        }
    }

    @Override
    public final void onResume(){
        super.onResume();
        ((Toolbar) Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar)).setSubtitle(R.string.action_show_history);
    }

    public final void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onTrafficForDateFragmentInteraction(uri);
        }
    }

    @Override
    public final void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = null;
        if (context instanceof Activity)
            activity = (Activity) context;
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public final void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public final void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sim1RB:
                mSimChecked = Constants.SIM1;
                break;
            case R.id.sim2RB:
                mSimChecked = Constants.SIM2;
                break;
            case R.id.sim3RB:
                mSimChecked = Constants.SIM3;
                break;
        }
        bOK.get().setEnabled(true);
        bSetDate.get().setEnabled(true);
    }

    public interface OnFragmentInteractionListener {
        void onTrafficForDateFragmentInteraction(Uri uri);
    }

    static class GetTask extends AsyncTask<Integer, Void, Bundle> {

        @Override
        protected final void onPreExecute() {
            super.onPreExecute();
            bOK.get().setEnabled(false);
            radioGroup.get().setEnabled(false);
            for (int i = 0; i < radioGroup.get().getChildCount(); i++) {
                radioGroup.get().getChildAt(i).setEnabled(false);
            }
            bSetDate.get().setEnabled(false);
            pb.get().setVisibility(View.VISIBLE);
        }

        @Override
        protected final Bundle doInBackground(Integer... params) {
            Context ctx = CustomApplication.getAppContext();
            String date = params[0] + "-" + params[1] + "-" + params[2];
            if (isCancelled())
                return null;
            else {
                ArrayList<String> imsi = null;
                if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false))
                    imsi = MobileUtils.getSimIds(ctx);
                return CustomDatabaseHelper.getDataForDate(CustomDatabaseHelper.getInstance(ctx),
                        date, params[3], mPrefs, imsi);
            }
        }

        @Override
        protected final void onPostExecute(Bundle result) {
            super.onPostExecute(result);
            Context ctx = CustomApplication.getAppContext();
            if (!mIsDetached) {
                pb.get().setVisibility(View.GONE);
                bOK.get().setEnabled(true);
                radioGroup.get().setEnabled(true);
                for (int i = 0; i < radioGroup.get().getChildCount(); i++) {
                    if (i < mSimQuantity)
                        radioGroup.get().getChildAt(i).setEnabled(true);
                }
                bSetDate.get().setEnabled(true);
                if (result != null && mSimChecked != Constants.NULL) {
                    String[] prefsConst = new String[Constants.PREF_SIM1.length];
                    switch (mSimChecked) {
                        case Constants.SIM1:
                            prefsConst = Constants.PREF_SIM1;
                            break;
                        case Constants.SIM2:
                            prefsConst = Constants.PREF_SIM2;
                            break;
                        case Constants.SIM3:
                            prefsConst = Constants.PREF_SIM3;
                            break;
                    }
                    String opName = MobileUtils.getName(ctx, prefsConst[5], prefsConst[6], mSimChecked);
                    wDay.get().setText(opName);
                    wNight.get().setText(String.format(ctx.getResources().getString(R.string.night), opName));

                    wRX.get().setText(DataFormat.formatData(ctx, result.getLong("rx")));
                    wTX.get().setText(DataFormat.formatData(ctx, result.getLong("tx")));
                    wTOT.get().setText(DataFormat.formatData(ctx, result.getLong("tot")));

                    if (mPrefs.getBoolean(prefsConst[17], false)) {
                        wRXN.get().setVisibility(View.VISIBLE);
                        wTXN.get().setVisibility(View.VISIBLE);
                        wTOTN.get().setVisibility(View.VISIBLE);
                        wNight.get().setVisibility(View.VISIBLE);
                        wRXN.get().setText(DataFormat.formatData(ctx, result.getLong("rx_n")));
                        wTXN.get().setText(DataFormat.formatData(ctx, result.getLong("tx_n")));
                        wTOTN.get().setText(DataFormat.formatData(ctx, result.getLong("tot_n")));
                    }
                } else
                    Toast.makeText(ctx, R.string.date_incorrect_or_data_missing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public final void onClick(View v) {
        switch (v.getId()) {
            case R.id.setdate:
                DatePickerDialog tpd = new DatePickerDialog(Objects.requireNonNull(getActivity()), mCallBack, mYear, mMonth - 1, mDay);
                tpd.show();
                break;
            case R.id.buttonOK:
                mIsDetached = isDetached();
                new GetTask().execute(mYear, mMonth, mDay, mSimChecked);
        }

    }

    private final DatePickerDialog.OnDateSetListener mCallBack = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear + 1;
            mDay = dayOfMonth;

            DateTime date = Constants.DATE_FORMATTER.parseDateTime(mYear + "-" + mMonth + "-" + mDay);

            Format dateFormat = DateFormat.getDateFormat(mContext);
            String pattern = ((SimpleDateFormat) dateFormat).toLocalizedPattern();

            bSetDate.get().setText(new SimpleDateFormat(pattern).format(date.toDate()));
        }
    };

}
