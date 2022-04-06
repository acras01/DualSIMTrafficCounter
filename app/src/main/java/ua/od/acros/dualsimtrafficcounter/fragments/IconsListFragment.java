package ua.od.acros.dualsimtrafficcounter.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import android.widget.ListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import ua.od.acros.dualsimtrafficcounter.R;

public class IconsListFragment extends DialogFragment implements AdapterView.OnItemClickListener {

    private String[] mListItems;
    private ListView listView;
    private String mLogo;

    private OnCompleteListener mListener;

    private static final String ID = "id";

    public static IconsListFragment newInstance(String id) {
        IconsListFragment f = new IconsListFragment();
        Bundle args = new Bundle();
        args.putString(ID, id);
        f.setArguments(args);
        return f;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get back arguments
        if (getArguments() != null) {
            mLogo = getArguments().getString(ID, "");
        }
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListItems = getResources().getStringArray(R.array.icons);
        View view = inflater.inflate(R.layout.icons_list_layout, container, false);
        listView = view.findViewById(R.id.list);
        Window w = getDialog().getWindow();
        if (w != null) {
            w.requestFeature(Window.FEATURE_NO_TITLE);
        }
        return view;
    }

    @Override
    public final void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, mListItems);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(this);
        }
    }

    @Override
    public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        dismiss();
        this.mListener.onComplete(position, mLogo);
    }

    public interface OnCompleteListener {
        void onComplete(int position, String logo);
    }

    public final void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = null;
        if (context instanceof Activity)
            activity = (Activity) context;
        try {
            mListener = (OnCompleteListener) activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnCompleteListener");
        }
    }

}