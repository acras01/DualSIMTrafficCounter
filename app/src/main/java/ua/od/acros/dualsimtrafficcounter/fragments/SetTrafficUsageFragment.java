package ua.od.acros.dualsimtrafficcounter.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.SetTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class SetTrafficUsageFragment extends Fragment implements CompoundButton.OnCheckedChangeListener,
        RadioGroup.OnCheckedChangeListener, AdapterView.OnItemSelectedListener, View.OnClickListener {

    private EditText txInput, rxInput;
    private int mTXSpinnerSel, mRXSpinnerSel;
    private int mSimChecked = Constants.DISABLED;
    private AppCompatSpinner rxSpinner, txSpinner;
    private String[] mOperatorNames;
    private AppCompatCheckBox total;
    private OnFragmentInteractionListener mListener;
    private Context mContext;
    private boolean mOnlyReceived;
    private SharedPreferences mPrefs;
    private AppCompatButton buttonOk;


    public static SetTrafficUsageFragment newInstance() {
        return new SetTrafficUsageFragment();
    }

    public SetTrafficUsageFragment() {
        // Required empty public constructor
    }

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        mOnlyReceived = false;
        mOperatorNames = new String[] {MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mContext == null)
            mContext = CustomApplication.getAppContext();
        View view = inflater.inflate(R.layout.usage_fragment, container, false);
        txInput = view.findViewById(R.id.txamount);
        txInput.setEnabled(false);
        rxInput = view.findViewById(R.id.rxamount);
        rxInput.setEnabled(false);
        txSpinner = view.findViewById(R.id.spinnertx);
        rxSpinner = view.findViewById(R.id.spinnerrx);
        RadioGroup radioGroup = view.findViewById(R.id.sim_group);
        AppCompatRadioButton sim1rb = view.findViewById(R.id.sim1RB);
        sim1rb.setText(mOperatorNames[0]);
        AppCompatRadioButton sim2rb = view.findViewById(R.id.sim2RB);
        sim2rb.setText(mOperatorNames[1]);
        AppCompatRadioButton sim3rb = view.findViewById(R.id.sim3RB);
        sim3rb.setText(mOperatorNames[2]);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int simQuantity = mPrefs.getInt(Constants.PREF_OTHER[55], 1);
        if (simQuantity == 1) {
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        if (simQuantity == 2)
            sim3rb.setEnabled(false);
        total = view.findViewById(R.id.checktotal);
        total.setChecked(false);
        total.setOnCheckedChangeListener(this);
        total.setEnabled(false);
        radioGroup.setOnCheckedChangeListener(this);
        txSpinner.setOnItemSelectedListener(this);
        txSpinner.setEnabled(false);
        rxSpinner.setOnItemSelectedListener(this);
        rxSpinner.setEnabled(false);
        buttonOk = view.findViewById(R.id.buttonOK);
        buttonOk.setOnClickListener(this);
        buttonOk.setEnabled(false);
        if (savedInstanceState != null) {
            mOnlyReceived = savedInstanceState.getBoolean("received");
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
                txInput.setText(savedInstanceState.getString("tx"));
                rxInput.setText(savedInstanceState.getString("rx"));
                total.setChecked(savedInstanceState.getBoolean("tot"));
                rxSpinner.setSelection(savedInstanceState.getInt("rxs"));
                txSpinner.setSelection(savedInstanceState.getInt("txs"));
                txInput.setEnabled(!mOnlyReceived);
                txSpinner.setEnabled(!mOnlyReceived);
                total.setEnabled(!mOnlyReceived);
                if (total.isEnabled()) {
                    rxInput.setEnabled(!total.isChecked());
                    rxSpinner.setEnabled(!total.isChecked());
                } else {
                    rxInput.setEnabled(mOnlyReceived);
                    rxSpinner.setEnabled(mOnlyReceived);
                }
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
            outState.putBoolean("received", mOnlyReceived);
            outState.putInt("rxs", mTXSpinnerSel);
            outState.putString("rx", rxInput.getText().toString());
            outState.putInt("txs", mRXSpinnerSel);
            outState.putString("tx", txInput.getText().toString());
            outState.putBoolean("tot", total.isChecked());
        }
    }

    @Override
    public final void onResume(){
        super.onResume();
        ((Toolbar) Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar)).setSubtitle(R.string.action_set_usage);
    }

    @Override
    public final void onClick(View view) {
        if ((mSimChecked != Constants.DISABLED && !rxInput.getText().toString().equals("") &&
                !txInput.getText().toString().equals("")) ||
                (mSimChecked != Constants.DISABLED && total.isChecked() && !txInput.getText().toString().equals(""))) {
            boolean service = CustomApplication.isMyServiceRunning(TrafficCountService.class);
            if (!service)
                mContext.startService(new Intent(mContext, TrafficCountService.class));
            String rx = "0";
            if (!total.isChecked())
                rx = rxInput.getText().toString();
            String tx = "0";
            if (!mOnlyReceived)
                tx = txInput.getText().toString();
            SetTrafficEvent event = new SetTrafficEvent(tx, rx, mSimChecked, mTXSpinnerSel, mRXSpinnerSel);
            EventBus.getDefault().postSticky(event);
            Objects.requireNonNull(getActivity()).onBackPressed();
        } else
            Toast.makeText(mContext, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();

    }

    @Override
    public final void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.sim1RB:
                mSimChecked = Constants.SIM1;
                mOnlyReceived = mPrefs.getBoolean(Constants.PREF_SIM1[32], false);
                break;
            case R.id.sim2RB:
                mSimChecked = Constants.SIM2;
                mOnlyReceived = mPrefs.getBoolean(Constants.PREF_SIM2[32], false);
                break;
            case R.id.sim3RB:
                mSimChecked = Constants.SIM3;
                mOnlyReceived = mPrefs.getBoolean(Constants.PREF_SIM3[32], false);
                break;
        }
        txInput.setEnabled(!mOnlyReceived);
        txSpinner.setEnabled(!mOnlyReceived);
        total.setEnabled(!mOnlyReceived);
        if (total.isEnabled()) {
            rxInput.setEnabled(!total.isChecked());
            rxSpinner.setEnabled(!total.isChecked());
        } else {
            rxInput.setEnabled(mOnlyReceived);
            rxSpinner.setEnabled(mOnlyReceived);
        }
        buttonOk.setEnabled(true);
    }

    @Override
    public final void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case (R.id.spinnertx):
                mTXSpinnerSel = parent.getSelectedItemPosition();
                break;
            case (R.id.spinnerrx):
                mRXSpinnerSel = parent.getSelectedItemPosition();
                break;
        }
    }

    @Override
    public final void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public final void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        rxSpinner.setEnabled(!isChecked);
        rxInput.setEnabled(!isChecked);
        if (isChecked)
            txInput.setHint(R.string.total);
        else
            txInput.setHint(R.string.transmitted);
    }

    public interface OnFragmentInteractionListener {
        void onSetUsageFragmentInteraction(Uri uri);
    }

    public final void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onSetUsageFragmentInteraction(uri);
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
