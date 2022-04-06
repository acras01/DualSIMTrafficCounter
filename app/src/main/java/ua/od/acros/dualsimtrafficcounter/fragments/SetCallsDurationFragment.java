package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.SetCallsEvent;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class SetCallsDurationFragment extends Fragment implements RadioGroup.OnCheckedChangeListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener {

    private EditText duration;
    private int mSimChecked = Constants.DISABLED;
    private String[] mOperatorNames;
    private OnFragmentInteractionListener mListener;
    private int mSpinnerSel;
    private Context mContext;
    private AppCompatButton buttonOk;
    private AppCompatSpinner spinner;

    public static SetCallsDurationFragment newInstance() {
        return new SetCallsDurationFragment();
    }

    public SetCallsDurationFragment() {
        // Required empty public constructor
    }

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        mOperatorNames = new String[] {MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        View view = inflater.inflate(R.layout.duration_fragment, container, false);
        duration = view.findViewById(R.id.duration);
        duration.setEnabled(false);
        spinner = view.findViewById(R.id.spinner);
        RadioGroup radioGroup = view.findViewById(R.id.sim_group);
        AppCompatRadioButton sim1rb = view.findViewById(R.id.sim1RB);
        sim1rb.setText(mOperatorNames[0]);
        AppCompatRadioButton sim2rb = view.findViewById(R.id.sim2RB);
        sim2rb.setText(mOperatorNames[1]);
        AppCompatRadioButton sim3rb = view.findViewById(R.id.sim3RB);
        sim3rb.setText(mOperatorNames[2]);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int simQuantity = prefs.getInt(Constants.PREF_OTHER[55], 1);
        if (simQuantity == 1) {
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        if (simQuantity == 2)
            sim3rb.setEnabled(false);
        radioGroup.setOnCheckedChangeListener(this);
        spinner.setOnItemSelectedListener(this);
        spinner.setEnabled(false);
        buttonOk = view.findViewById(R.id.buttonOK);
        buttonOk.setOnClickListener(this);
        buttonOk.setEnabled(false);
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
                duration.setText(savedInstanceState.getString("duration"));
                duration.setEnabled(true);
                spinner.setSelection(savedInstanceState.getInt("spinner"));
                spinner.setEnabled(true);
                buttonOk.setEnabled(true);
            }
        }
        return view;
    }

    @Override
    public final void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isVisible()) {
            outState.putInt("sim", mSimChecked);
            outState.putInt("spinner", mSpinnerSel);
            outState.putString("duration", duration.getText().toString());
        }
    }

    @Override
    public final void onResume(){
        super.onResume();
        ((Toolbar) Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar)).setSubtitle(R.string.action_set_duration);
    }

    @Override
    public final void onClick(View view) {
        if (mSimChecked != Constants.DISABLED && !duration.getText().toString().equals("")) {
            boolean service = CustomApplication.isMyServiceRunning(CallLoggerService.class);
            if (!service)
                mContext.startService(new Intent(mContext, CallLoggerService.class));
            SetCallsEvent event = new SetCallsEvent(mSimChecked, duration.getText().toString(), mSpinnerSel);
            EventBus.getDefault().postSticky(event);
            Objects.requireNonNull(getActivity()).onBackPressed();
        } else
            Toast.makeText(mContext, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
    }

    @Override
    public final void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sim1RB:
                mSimChecked =  Constants.SIM1;
                break;
            case R.id.sim2RB:
                mSimChecked =  Constants.SIM2;
                break;
            case R.id.sim3RB:
                mSimChecked =  Constants.SIM3;
                break;
        }
        buttonOk.setEnabled(true);
        duration.setEnabled(true);
        spinner.setEnabled(true);
    }

    @Override
    public final void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case (R.id.spinner):
                mSpinnerSel = parent.getSelectedItemPosition();
                break;
        }
    }

    @Override
    public final void onNothingSelected(AdapterView<?> parent) {

    }

    public interface OnFragmentInteractionListener {
        void onSetDurationFragmentInteraction(Uri uri);
    }

    public final void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onSetDurationFragmentInteraction(uri);
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
