package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ShowSimDialog extends DialogFragment {

    private AppCompatButton bOK;
    private static boolean[] mSim = new boolean[3];
    private static WeakReference<Activity> myActivity;
    private String mActivity;
    private Context mContext;
    private String[] mOperatorNames;
    private static int mSimQuantity;

    public static ShowSimDialog newInstance(String activity, boolean[] sim) {
        ShowSimDialog f = new ShowSimDialog();
        Bundle b = new Bundle();
        b.putBooleanArray("sim", sim);
        b.putString("activity", activity);
        f.setArguments(b);
        return f;
    }

    public interface ShowSimDialogClosedListener {
        void OnDialogClosed(String activity, boolean[] sim);
    }

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = CustomApplication.getAppContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        myActivity = new WeakReference<>(getActivity());
        if (getArguments() != null) {
            mActivity = getArguments().getString("activity");
            mSim = getArguments().getBooleanArray("sim");
        }
        mOperatorNames = new String[] {MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        mSimQuantity = prefs.getInt(Constants.PREF_OTHER[55], 1);
    }

    @NonNull
    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<Item> items = new ArrayList<>();
        for (int i = 0; i < mSim.length; i++) {
            items.add(new Item(mOperatorNames[i], mSim[i]));
        }
        final CustomListAdapter adapter = new CustomListAdapter(mContext, R.layout.showsim_list_row, items);
        final AlertDialog dialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity()))
                .setTitle(R.string.choose_sim)
                .setAdapter(adapter, null)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog1, id) -> dialog1.cancel())
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            bOK = (AppCompatButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            bOK.setOnClickListener(view -> {
                ShowSimDialogClosedListener listener = (ShowSimDialogClosedListener) getActivity();
                for (Item item : adapter.getList()) {
                    mSim[adapter.getList().indexOf(item)] = item.isChecked();
                }
                if (listener != null) {
                    listener.OnDialogClosed(mActivity, mSim);
                }
                dismiss();
            });
        });
        return dialog;
    }

    private static class Item {
        private final String name;
        private boolean checked;

        Item(String name, boolean checked) {
            this.name = name;
            this.checked = checked;
        }

        public final String getName() {
            return name;
        }

        public final boolean isChecked() {
            return checked;
        }

        public final void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    private static class CustomListAdapter extends ArrayAdapter<Item> {

        private ViewHolder holder;
        private final List<Item> list;
        private final int layout;

        CustomListAdapter(Context context, int layout, List<Item> list) {
            super(context, layout, list);
            this.list = list;
            this.layout = layout;
        }

        private static class ViewHolder {
            AppCompatCheckBox item;
        }

        @NonNull
        public final View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null ) {
                holder = new ViewHolder();
                convertView = myActivity.get().getLayoutInflater().inflate(layout, null);
                convertView.setTag(holder);
                holder.item = convertView.findViewById(R.id.checkBox);
            }
            else
                holder = (ViewHolder) convertView.getTag();
            holder.item.setEnabled(true);
            holder.item.setTag(list.get(position));
            holder.item.setText(list.get(position).getName());
            holder.item.setChecked(list.get(position).isChecked());
            holder.item.setOnCheckedChangeListener((buttonView, isChecked) -> ((Item) buttonView.getTag()).setChecked(isChecked));
            if (position >= mSimQuantity) {
                holder.item.setEnabled(false);
                holder.item.setChecked(false);
            }
            return convertView;
        }

        public final List<Item> getList() {
            return list;
        }
    }
}
