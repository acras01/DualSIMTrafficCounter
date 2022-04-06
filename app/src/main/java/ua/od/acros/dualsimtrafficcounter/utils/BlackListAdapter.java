package ua.od.acros.dualsimtrafficcounter.utils;

import android.graphics.Paint;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;

public class BlackListAdapter extends RecyclerView.Adapter<BlackListAdapter.ViewHolder> {

    private List<ListItem> mList;

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
        final AppCompatCheckBox checkBox;
        final TextView textView;

        ViewHolder(View v) {
            super(v);
            checkBox = v.findViewById(R.id.checkBox);
            textView = v.findViewById(R.id.textView);
            checkBox.setOnCheckedChangeListener(this);
            textView.setOnClickListener(this);
        }

        @Override
        public final void onClick(View view) {
            boolean isChecked = ((ListItem) view.getTag()).isChecked();
            ((ListItem) view.getTag()).setChecked(!isChecked);
            checkBox.setChecked(!isChecked);
            if (!isChecked)
                textView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            else
                textView.setPaintFlags(0);
        }

        @Override
        public final void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            ((ListItem) compoundButton.getTag()).setChecked(isChecked);
            if (isChecked)
                textView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            else
                textView.setPaintFlags(0);
        }
    }

    public BlackListAdapter(List<ListItem> list) {
        this.mList = list;
    }

    @Override
    public final BlackListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.black_list_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public final void onBindViewHolder(BlackListAdapter.ViewHolder holder, int position) {
        holder.textView.setText(mList.get(position).getNumber());
        holder.textView.setTag(mList.get(position));
        boolean checked = mList.get(position).isChecked();
        holder.checkBox.setTag(mList.get(position));
        holder.checkBox.setChecked(checked);
        if (checked)
            holder.textView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        else
            holder.textView.setPaintFlags(0);
    }

    @Override
    public final int getItemCount() {
        if (mList != null)
            return mList.size();
        else
            return 0;
    }

    public final ArrayList<String> getCheckedItems(){
        ArrayList<String> list = new ArrayList<>();
        for (ListItem item : mList)
        if (item.isChecked())
            list.add(item.getNumber());
        return list;
    }

    public final Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public final long getItemId(int position) {
        return position;
    }

    public final void swapItems(List<ListItem> list) {
        if (list != null)
            this.mList = list;
        notifyDataSetChanged();
    }
}