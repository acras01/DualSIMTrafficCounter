package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.OnOffTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class OnOffDialog extends DialogFragment {

    private int mSimChecked = Constants.NULL;
    private AppCompatButton bOK;
    private AppCompatCheckBox chbClose;
    private Context mContext;


    public static OnOffDialog newInstance() {
        return new OnOffDialog();
    }

    @NonNull
    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {

        mContext = CustomApplication.getAppContext();
        String[] operatorNames = new String[] {MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};

        View view = View.inflate(getActivity(), R.layout.onoff_dialog, null);
        chbClose = view.findViewById(R.id.checkBox);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        AppCompatRadioButton sim1rb = view.findViewById(R.id.sim1RB);
        sim1rb.setText(operatorNames[0]);
        AppCompatRadioButton sim2rb = view.findViewById(R.id.sim2RB);
        sim2rb.setText(operatorNames[1]);
        AppCompatRadioButton sim3rb = view.findViewById(R.id.sim3RB);
        sim3rb.setText(operatorNames[2]);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (CustomApplication.canToggleOff()) {
            int simQuantity = prefs.getInt(Constants.PREF_OTHER[55], 1);
            if (simQuantity >= 1)
                sim1rb.setEnabled(true);
            if (simQuantity >= 2)
                sim2rb.setEnabled(true);
            if (simQuantity == 3)
                sim3rb.setEnabled(true);
        }
        final ColorStateList[] textColor = {ColorStateList.valueOf(ContextCompat.getColor(Objects.requireNonNull(getActivity()), R.color.colorAccent))};
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            chbClose.setEnabled(true);
            bOK.setEnabled(true);
            bOK.setTextColor(textColor[0]);
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
                case R.id.offRB:
                    mSimChecked = Constants.DISABLED;
                    break;
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.choose_sim)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog1, id) -> dialog1.cancel())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            chbClose.setEnabled(false);
            chbClose.setChecked(false);
            bOK = (AppCompatButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            textColor[0] = bOK.getTextColors();
            bOK.setEnabled(false);
            bOK.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getActivity()), R.color.colorPrimaryDark));
            bOK.setOnClickListener(view1 -> {
                if (mSimChecked != Constants.NULL) {
                    dialog.dismiss();
                    EventBus.getDefault().post(new OnOffTrafficEvent(mSimChecked, chbClose.isChecked()));
                } else
                    Toast.makeText(mContext, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            });
        });
        return dialog;
    }
}
