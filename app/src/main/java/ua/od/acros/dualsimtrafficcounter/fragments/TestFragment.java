package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class TestFragment extends Fragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{

    private String mSimChecked = "";
    private boolean mAlternative = false;
    private SharedPreferences.Editor mEdit;
    private String[] mOperatorNames = new String[3];
    private OnFragmentInteractionListener mListener;
    private Context mContext;


    public static TestFragment newInstance() {
        return new TestFragment();
    }

    public TestFragment() {
        // Required empty public constructor
    }

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        View view = inflater.inflate(R.layout.test_fragment, container, false);
        RadioGroup radioGroup = view.findViewById(R.id.sim_group);
        AppCompatRadioButton sim1rb = view.findViewById(R.id.sim1RB);
        sim1rb.setText(mOperatorNames[0]);
        AppCompatRadioButton sim2rb = view.findViewById(R.id.sim2RB);
        sim2rb.setText(mOperatorNames[1]);
        AppCompatRadioButton sim3rb = view.findViewById(R.id.sim3RB);
        sim3rb.setText(mOperatorNames[2]);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mEdit = prefs.edit();
        int simQuantity = prefs.getInt(Constants.PREF_OTHER[55], 1);
        if (simQuantity == 1) {
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        if (simQuantity == 2)
            sim3rb.setEnabled(false);
        radioGroup.setOnCheckedChangeListener(this);
        view.findViewById(R.id.buttonOK).setOnClickListener(this);
        view.findViewById(R.id.test).setOnClickListener(this);
        return view;
    }

    @Override
    public final void onResume(){
        super.onResume();
        ((Toolbar) Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar)).setSubtitle(R.string.action_show_test);
    }

    @Override
    public final void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonOK:
                mEdit.putBoolean(Constants.PREF_OTHER[20], mAlternative);
                mEdit.apply();
                Objects.requireNonNull(getActivity()).onBackPressed();
                break;
            case R.id.test:
                if (!mSimChecked.equals("")) {
                    int sim = Constants.DISABLED;
                    try {
                        sim = (int) Settings.System.getLong(mContext.getContentResolver(), "gprs_connection_sim_setting");
                        mEdit.putInt(mSimChecked, sim);
                        mAlternative = true;
                    } catch (Settings.SettingNotFoundException e0) {
                        e0.printStackTrace();
                        try {
                            sim = (int) Settings.System.getLong(mContext.getContentResolver(), "gprs_connection_setting");
                            mEdit.putInt(mSimChecked, sim);
                            mAlternative = true;
                        } catch (Settings.SettingNotFoundException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (mAlternative)
                        Toast.makeText(mContext, mSimChecked + ": " + sim, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(mContext, R.string.error, Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(mContext, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public final void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sim1RB:
                mSimChecked = "sim1";
                break;
            case R.id.sim2RB:
                mSimChecked = "sim2";
                break;
            case R.id.sim3RB:
                mSimChecked = "sim3";
                break;
        }
    }

    public interface OnFragmentInteractionListener {
        void onTestFragmentInteraction(Uri uri);
    }

    public final void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onTestFragmentInteraction(uri);
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
}
