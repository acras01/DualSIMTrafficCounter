package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.ContactsContract;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import com.squareup.picasso.Transformation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;

public class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.ViewHolder> {


    private List<ListItem> mList;
    private Picasso mPicasso;
    private int mDim;

    private static final String SCHEME_APP_ICON = "app_icon";
    private static final String SCHEME_CONTACT_PHOTO = "contact_photo";


    public MyListAdapter(List<ListItem> list) {
        if (list != null)
            this.mList = list;
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
        final AppCompatCheckBox checkBox;
        final TextView txtViewName;
        final TextView txtViewNumber;
        final ImageView imgIcon;


        ViewHolder(View view) {
            super(view);
            checkBox = view.findViewById(R.id.checkBox);
            txtViewName = view.findViewById(R.id.name);
            txtViewNumber = view.findViewById(R.id.number);
            imgIcon = view.findViewById(R.id.icon);
            checkBox.setOnCheckedChangeListener(this);
            txtViewName.setOnClickListener(this);
            txtViewNumber.setOnClickListener(this);
            imgIcon.setOnClickListener(this);
        }

        @Override
        public final void onClick(View view) {
            boolean isChecked = ((ListItem) view.getTag()).isChecked();
            ((ListItem) view.getTag()).setChecked(!isChecked);
            checkBox.setChecked(!isChecked);
        }

        @Override
        public final void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            ((ListItem) compoundButton.getTag()).setChecked(isChecked);
        }
    }

    @Override
    public final MyListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.my_list_row, parent, false);
        mDim = (int) context.getResources().getDimension(R.dimen.logo_size);
        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new CustomRequestHandler(context));
        mPicasso = builder.build();
        return new ViewHolder(view);
    }

    @Override
    public final void onBindViewHolder(ViewHolder holder, int position) {
        holder.txtViewName.setText(mList.get(position).getName());
        holder.txtViewName.setTag(mList.get(position));
        holder.txtViewNumber.setText(mList.get(position).getNumber());
        holder.txtViewNumber.setTag(mList.get(position));
        holder.checkBox.setTag(mList.get(position));
        holder.checkBox.setChecked(mList.get(position).isChecked());
        holder.imgIcon.setTag(mList.get(position));
        Uri icon = mList.get(position).getIcon();
        if (icon.toString().contains(SCHEME_CONTACT_PHOTO))
            mPicasso.load(icon)
                    .resize(mDim, mDim)
                    //.fit()
                    .transform(new CircularTransformation(0))
                    .centerInside()
                    //.error(R.drawable.ic_person_black_24dp)
                    .into(holder.imgIcon);
        else
            mPicasso.load(icon)
                    .resize(mDim, mDim)
                    .centerInside()
                    //.error(R.drawable.ic_android_black_24dp)
                    .into(holder.imgIcon);
        //holder.imgIcon.setImageDrawable(mList.get(position).getIcon());
    }

    public final ArrayList<String> getCheckedItems(){
        ArrayList<String> list = new ArrayList<>();
        for (ListItem item : mList)
            if (item.isChecked())
                list.add(item.getNumber());
        return list;
    }

    @Override
    public final int getItemCount() {
        if (mList != null)
            return mList.size();
        else
            return 0;
    }

    public final ListItem getItem(int position) {
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

    private static class CustomRequestHandler extends RequestHandler {

        private final PackageManager packageManager;
        private final Context context;

        CustomRequestHandler(Context context) {
            this.context = context;
            packageManager = context.getPackageManager();
        }

        @Override
        public final boolean canHandleRequest(Request data) {
            return Objects.requireNonNull(data.uri.getScheme()).contains(SCHEME_APP_ICON) || data.uri.getScheme().contains(SCHEME_CONTACT_PHOTO);
        }

        @Override
        public final Result load(Request request, int networkPolicy) throws IOException {
            Bitmap bmp = null;
            if (request.uri.toString().contains(SCHEME_APP_ICON)) {
                try {
                    bmp = ((BitmapDrawable) packageManager.getApplicationIcon(request.uri.toString().replace(SCHEME_APP_ICON + "://", ""))).getBitmap();
                } catch (Exception e) {
                }
                if (bmp != null)
                    return new Result(bmp, Picasso.LoadedFrom.DISK);
            } else if (request.uri.toString().contains(SCHEME_CONTACT_PHOTO)) {
                boolean choice;
                long contactID = Long.parseLong(request.uri.toString().replace(SCHEME_CONTACT_PHOTO + "://", ""));
                Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID);
                Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                Cursor cursor = context.getContentResolver().query(photoUri,
                        new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
                if (cursor != null)
                    try {
                        if (cursor.moveToFirst()) {
                            byte[] data = cursor.getBlob(0);
                            if (data != null)
                                return new Result(BitmapFactory.decodeStream(new ByteArrayInputStream(data)), Picasso.LoadedFrom.DISK);
                            else
                                choice = true;
                        } else
                            choice = true;
                    } finally {
                        cursor.close();
                    }
                else
                    choice = true;
                if (choice) {
                    bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);
                    return new Result(bmp, Picasso.LoadedFrom.DISK);
                }
            }
            return null;
        }
    }

    private static class CircularTransformation implements Transformation {

        private int radius = 10;

        CircularTransformation(final int radius) {
            this.radius = radius;
        }

        @Override
        public final Bitmap transform(final Bitmap source) {
            final Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            final Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(output);
            if (radius == 0)
                canvas.drawCircle(source.getWidth() / 2, source.getHeight() / 2, source.getWidth() / 2, paint);
            else
                canvas.drawCircle(source.getWidth() / 2, source.getHeight() / 2, radius, paint);
            if (source != output)
                source.recycle();
            return output;
        }

        @Override
        public final String key() {
            return "circular" + String.valueOf(radius);
        }
    }
}
