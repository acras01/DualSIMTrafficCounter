package ua.od.acros.dualsimtrafficcounter.preferences;

import android.os.Bundle;
import androidx.preference.PreferenceDialogFragmentCompat;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;

public class EditTextPreferenceDialogFragmentCompatFix extends PreferenceDialogFragmentCompat {
    private EditText mEditText;

    public EditTextPreferenceDialogFragmentCompatFix() {
    }

    public static EditTextPreferenceDialogFragmentCompatFix newInstance(String key) {
        EditTextPreferenceDialogFragmentCompatFix fragment = new EditTextPreferenceDialogFragmentCompatFix();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        this.mEditText = getEditTextPreference().getEditText();
        this.mEditText.setText(this.getEditTextPreference().getText());

        Editable text = mEditText.getText();
        if (text != null) {
            mEditText.setSelection(text.length(), text.length());
        }

        ViewParent oldParent = this.mEditText.getParent();
        if (oldParent != view) {
            if (oldParent != null) {
                ((ViewGroup) oldParent).removeView(this.mEditText);
            }

            this.onAddEditTextToDialogView(view, this.mEditText);
        }
    }

    private TwoLineEditTextPreference getEditTextPreference() {
        return (TwoLineEditTextPreference) this.getPreference();
    }

    protected boolean needInputMethod() {
        return true;
    }

    private void onAddEditTextToDialogView(View dialogView, EditText editText) {
        //ViewGroup container = (ViewGroup) dialogView.findViewById(android.support.v7.preference.R.id.edittext_container);
        View oldEditText = dialogView.findViewById(android.R.id.edit);
        if (oldEditText != null) {
            ViewGroup container = (ViewGroup) (oldEditText.getParent());
            if (container != null) {
                container.removeView(oldEditText);
                container.addView(editText, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = this.mEditText.getText().toString();
            if (this.getEditTextPreference().callChangeListener(value)) {
                this.getEditTextPreference().setText(value);
            }
        }

    }
}
