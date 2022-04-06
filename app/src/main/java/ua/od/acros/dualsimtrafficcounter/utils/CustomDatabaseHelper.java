package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import androidx.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class CustomDatabaseHelper extends SQLiteOpenHelper {

    private static final String WHITE_LIST_1 = "list1";
    private static final String WHITE_LIST_2 = "list2";
    private static final String WHITE_LIST_3 = "list3";
    private static final String BLACK_LIST_1 = "list1_b";
    private static final String BLACK_LIST_2 = "list2_b";
    private static final String BLACK_LIST_3 = "list3_b";
    private static final String UID_1 = "uid_list1";
    private static final String UID_2 = "uid_list2";
    private static final String UID_3 = "uid_list3";
    private static SQLiteDatabase mSqLiteDatabase;
    private static CustomDatabaseHelper mInstance;

    public static CustomDatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new CustomDatabaseHelper(context);
        }
        return mInstance;
    }

    private CustomDatabaseHelper(Context context) {
        super(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
    }

    private CustomDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                                 int version) {
        super(context, name, factory, version);
    }

    private CustomDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                                 int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String DATABASE_CREATE_SCRIPT = "create table "
                + Constants.TRAFFIC + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                + " text not null, " + Constants.LAST_ACTIVE_SIM + " integer, "
                + Constants.LAST_RX + " long, " + Constants.LAST_TX + " long, "
                + Constants.SIM1RX + " long, " + Constants.SIM1TX + " long, "
                + Constants.TOTAL1 + " long, " + Constants.SIM2RX + " long, "
                + Constants.SIM2TX + " long, " + Constants.TOTAL2 + " long, "
                + Constants.SIM3RX + " long, " + Constants.SIM3TX + " long, "
                + Constants.TOTAL3 + " long, " + Constants.PERIOD1 + " integer,"
                + Constants.PERIOD2 + " integer, " + Constants.PERIOD3 + " integer, "
                + Constants.SIM1RX_N + " long, "
                + Constants.SIM1TX_N + " long, " + Constants.TOTAL1_N + " long, "
                + Constants.SIM2RX_N + " long, " + Constants.SIM2TX_N + " long, "
                + Constants.TOTAL2_N + " long, " + Constants.SIM3RX_N + " long,"
                + Constants.SIM3TX_N + " long, " + Constants.TOTAL3_N + " long);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + Constants.CALLS + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                + " text not null, " +  Constants.CALLS1 + " long, "
                + Constants.CALLS1_EX + " long, " + Constants.CALLS2 + " long, "
                + Constants.CALLS2_EX + " long, " + Constants.CALLS3 + " long, "
                + Constants.CALLS3_EX + " long, " + Constants.PERIOD1 + " integer,"
                + Constants.PERIOD2 + " integer, " + Constants.PERIOD3 + " integer);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + WHITE_LIST_1 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + WHITE_LIST_2 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + WHITE_LIST_3 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + BLACK_LIST_1 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + BLACK_LIST_2 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
        DATABASE_CREATE_SCRIPT = "create table "
                + BLACK_LIST_3 + " (" + Constants.NUMBER + " text not null);";
        db.execSQL(DATABASE_CREATE_SCRIPT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String ALTER_TBL;
        if (oldVersion < Constants.DATABASE_VERSION) {
            if (oldVersion <= 1) {
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM3RX + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM3TX + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL3 + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.PERIOD1 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.PERIOD2 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.PERIOD3 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM1RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM1TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL1_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM2RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM2TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL2_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM3RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM3TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL3_N + " long;";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 2) {
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.PERIOD1 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.PERIOD2 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.PERIOD3 + " integer;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM1RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM1TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL1_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM2RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM2TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL2_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM3RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM3TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL3_N + " long;";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 3) {
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM1RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM1TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL1_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM2RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM2TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL2_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM3RX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.SIM3TX_N + " long;";
                db.execSQL(ALTER_TBL);
                ALTER_TBL =
                        "ALTER TABLE " + Constants.TRAFFIC +
                                " ADD COLUMN " + Constants.TOTAL3_N + " long;";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 4) {
                ALTER_TBL = "create table "
                        + Constants.CALLS + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                        + " text not null, " + Constants.CALLS1 + " long, "
                        + Constants.CALLS1_EX + " long, " + Constants.CALLS2 + " long, "
                        + Constants.CALLS2_EX + " long, " + Constants.CALLS3 + " long, "
                        + Constants.CALLS3_EX + " long, " + Constants.PERIOD1 + " integer,"
                        + Constants.PERIOD2 + " integer, " + Constants.PERIOD3 + " integer);";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 6) {
                db.execSQL("DROP TABLE IF EXISTS " + Constants.CALLS);
                ALTER_TBL = "create table "
                        + Constants.CALLS + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                        + " text not null, " + Constants.CALLS1 + " long, "
                        + Constants.CALLS1_EX + " long, " + Constants.CALLS2 + " long, "
                        + Constants.CALLS2_EX + " long, " + Constants.CALLS3 + " long, "
                        + Constants.CALLS3_EX + " long, " + Constants.PERIOD1 + " integer,"
                        + Constants.PERIOD2 + " integer, " + Constants.PERIOD3 + " integer);";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 7) {
                ALTER_TBL = "create table "
                        + WHITE_LIST_1 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
                ALTER_TBL = "create table "
                        + WHITE_LIST_2 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
                ALTER_TBL = "create table "
                        + WHITE_LIST_3 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
            }
            if (oldVersion <= 8) {
                ALTER_TBL = "create table "
                        + BLACK_LIST_1 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
                ALTER_TBL = "create table "
                        + BLACK_LIST_2 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
                ALTER_TBL = "create table "
                        + BLACK_LIST_3 + " (" + Constants.NUMBER + " text not null);";
                db.execSQL(ALTER_TBL);
            }
        }
    }

    private static void createTable(CustomDatabaseHelper dbHelper, String name) {
        String DATABASE_CREATE_SCRIPT;
        if (name.contains(Constants.TRAFFIC))
            DATABASE_CREATE_SCRIPT = "CREATE TABLE IF NOT EXISTS "
                    + name + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                    + " text not null, " + "rx" + " long, " + "tx" + " long, "
                    + "total" + " long, " + "period" + " integer,"
                    + "rx_n" + " long, " + "tx_n" + " long, " + "total_n" + " long);";
        else
            DATABASE_CREATE_SCRIPT = "CREATE TABLE IF NOT EXISTS "
                + name + " (" + Constants.LAST_DATE + " text not null, " + Constants.LAST_TIME
                + " text not null, " + "calls" + " long, " + "calls_ex" + " long, " + "period" + " integer);";
        dbHelper.getWritableDatabase().execSQL(DATABASE_CREATE_SCRIPT);
    }

    public static ContentValues readTrafficDataForSim(CustomDatabaseHelper dbHelper, String name) {
        ContentValues cv = new ContentValues();
        String dbName = Constants.TRAFFIC + "_" + name;
        createTable(dbHelper, dbName);
        mSqLiteDatabase = dbHelper.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(dbName, null, null, null, null, null, null);
        if (cursor.moveToLast()) {
            cv.put("rx", cursor.getLong(cursor.getColumnIndex("rx")));
            cv.put("tx", cursor.getLong(cursor.getColumnIndex("tx")));
            cv.put("total", cursor.getLong(cursor.getColumnIndex("total")));
            cv.put("rx_n", cursor.getLong(cursor.getColumnIndex("rx_n")));
            cv.put("tx_n", cursor.getLong(cursor.getColumnIndex("tx_n")));
            cv.put("total_n", cursor.getLong(cursor.getColumnIndex("total_n")));
            cv.put(Constants.LAST_TIME, cursor.getString(cursor.getColumnIndex(Constants.LAST_TIME)));
            cv.put(Constants.LAST_DATE, cursor.getString(cursor.getColumnIndex(Constants.LAST_DATE)));
            cv.put("period", cursor.getInt(cursor.getColumnIndex("period")));
        } else {
            cv.put("rx", 0L);
            cv.put("tx", 0L);
            cv.put("total", 0L);
            cv.put("rx_n", 0L);
            cv.put("tx_n", 0L);
            cv.put("total_n", 0L);
            cv.put(Constants.LAST_TIME, "");
            cv.put(Constants.LAST_DATE, "");
            cv.put("period", 0);
        }
        cursor.close();
        return cv;
    }

    public static ContentValues readCallsDataForSim(CustomDatabaseHelper dbHelper, String name) {
        ContentValues cv = new ContentValues();
        String dbName = Constants.CALLS + "_" + name;
        createTable(dbHelper, dbName);
        mSqLiteDatabase = dbHelper.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(dbName, null, null, null, null, null, null);
        if (cursor.moveToLast()) {
            cv.put("calls", cursor.getLong(cursor.getColumnIndex("calls")));
            cv.put("calls_ex", cursor.getLong(cursor.getColumnIndex("calls_ex")));
            cv.put(Constants.LAST_TIME, cursor.getString(cursor.getColumnIndex(Constants.LAST_TIME)));
            cv.put(Constants.LAST_DATE, cursor.getString(cursor.getColumnIndex(Constants.LAST_DATE)));
            cv.put("period", cursor.getInt(cursor.getColumnIndex("period")));
        } else {
            cv.put("calls", 0L);
            cv.put("calls_ex", 0L);
            cv.put(Constants.LAST_TIME, "");
            cv.put(Constants.LAST_DATE, "");
            cv.put("period", 0);
        }
        cursor.close();
        return cv;
    }

    public static ContentValues readTrafficData(CustomDatabaseHelper dbHelper) {
        ContentValues cv = new ContentValues();
        mSqLiteDatabase = dbHelper.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(Constants.TRAFFIC, null, null, null, null, null, null);
        if (cursor.moveToLast()) {
            cv.put(Constants.SIM1RX, cursor.getLong(cursor.getColumnIndex(Constants.SIM1RX)));
            cv.put(Constants.SIM2RX, cursor.getLong(cursor.getColumnIndex(Constants.SIM2RX)));
            cv.put(Constants.SIM3RX, cursor.getLong(cursor.getColumnIndex(Constants.SIM3RX)));
            cv.put(Constants.SIM1TX, cursor.getLong(cursor.getColumnIndex(Constants.SIM1TX)));
            cv.put(Constants.SIM2TX, cursor.getLong(cursor.getColumnIndex(Constants.SIM2TX)));
            cv.put(Constants.SIM3TX, cursor.getLong(cursor.getColumnIndex(Constants.SIM3TX)));
            cv.put(Constants.TOTAL1, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL1)));
            cv.put(Constants.TOTAL2, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL2)));
            cv.put(Constants.TOTAL3, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL3)));
            cv.put(Constants.LAST_ACTIVE_SIM, cursor.getInt(cursor.getColumnIndex(Constants.LAST_ACTIVE_SIM)));
            cv.put(Constants.LAST_RX, cursor.getLong(cursor.getColumnIndex(Constants.LAST_RX)));
            cv.put(Constants.LAST_TX, cursor.getLong(cursor.getColumnIndex(Constants.LAST_TX)));
            cv.put(Constants.LAST_TIME, cursor.getString(cursor.getColumnIndex(Constants.LAST_TIME)));
            cv.put(Constants.LAST_DATE, cursor.getString(cursor.getColumnIndex(Constants.LAST_DATE)));
            cv.put(Constants.PERIOD1, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD1)));
            cv.put(Constants.PERIOD2, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD2)));
            cv.put(Constants.PERIOD3, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD3)));
            cv.put(Constants.SIM1RX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM1RX_N)));
            cv.put(Constants.SIM2RX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM2RX_N)));
            cv.put(Constants.SIM3RX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM3RX_N)));
            cv.put(Constants.SIM1TX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM1TX_N)));
            cv.put(Constants.SIM2TX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM2TX_N)));
            cv.put(Constants.SIM3TX_N, cursor.getLong(cursor.getColumnIndex(Constants.SIM3TX_N)));
            cv.put(Constants.TOTAL1_N, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL1_N)));
            cv.put(Constants.TOTAL2_N, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL2_N)));
            cv.put(Constants.TOTAL3_N, cursor.getLong(cursor.getColumnIndex(Constants.TOTAL3_N)));
        } else {
            cv.put(Constants.SIM1RX, 0L);
            cv.put(Constants.SIM2RX, 0L);
            cv.put(Constants.SIM3RX, 0L);
            cv.put(Constants.SIM1TX, 0L);
            cv.put(Constants.SIM2TX, 0L);
            cv.put(Constants.SIM3TX, 0L);
            cv.put(Constants.TOTAL1, 0L);
            cv.put(Constants.TOTAL2, 0L);
            cv.put(Constants.TOTAL3, 0L);
            cv.put(Constants.LAST_ACTIVE_SIM, 0);
            cv.put(Constants.LAST_RX, 0L);
            cv.put(Constants.LAST_TX, 0L);
            cv.put(Constants.LAST_TIME, "");
            cv.put(Constants.LAST_DATE, "");
            cv.put(Constants.PERIOD1, 0);
            cv.put(Constants.PERIOD2, 0);
            cv.put(Constants.PERIOD3, 0);
            cv.put(Constants.SIM1RX_N, 0L);
            cv.put(Constants.SIM2RX_N, 0L);
            cv.put(Constants.SIM3RX_N, 0L);
            cv.put(Constants.SIM1TX_N, 0L);
            cv.put(Constants.SIM2TX_N, 0L);
            cv.put(Constants.SIM3TX_N, 0L);
            cv.put(Constants.TOTAL1_N, 0L);
            cv.put(Constants.TOTAL2_N, 0L);
            cv.put(Constants.TOTAL3_N, 0L);
        }
        cursor.close();
        return cv;
    }

    public static void writeData(ContentValues cv, CustomDatabaseHelper dbHelper, String table) {
        createTable(dbHelper, table);
        mSqLiteDatabase = dbHelper.getWritableDatabase();
        String filter = Constants.LAST_DATE + "='" + cv.get(Constants.LAST_DATE) + "'";
        int id = mSqLiteDatabase.update(table, cv, filter, null);
        if (id == 0)
            mSqLiteDatabase.insert(table, null, cv);
    }

    public static boolean isTableEmpty(CustomDatabaseHelper dbHelper, String name, boolean type) {
        try {
            boolean result;
            Cursor cursor = dbHelper.getReadableDatabase().query(name, null, null, null, null, null, null);
            if (type)
                result = cursor == null || cursor.getCount() == 0;
            else
                result = cursor == null;
            if (cursor != null) {
                cursor.close();
            }
            return result;
        } catch (Exception e) {
            return true;
        }
    }

    @Nullable
    public static Bundle getDataForDate(CustomDatabaseHelper dbHelper, String date, int sim,
                                        SharedPreferences prefs, ArrayList<String> imsi) {

        ContentValues cv1 = new ContentValues();
        ContentValues cv2 = new ContentValues();
        Bundle out = new Bundle();

        mSqLiteDatabase = dbHelper.getReadableDatabase();

        LocalDateTime queried = Constants.DATE_FORMATTER.parseLocalDateTime(date);
        LocalDateTime now = DateTime.now().toLocalDateTime();

        if (queried.isAfter(now))
            return null;

        String dayBeforeDate = queried.minusDays(1).toString(Constants.DATE_FORMATTER);

        Cursor cursorToDate, cursorToDayBeforeDate;
        if (imsi == null) {
            cursorToDate = mSqLiteDatabase.query(Constants.TRAFFIC, null, Constants.LAST_DATE + " = ?", new String[]{queried.toString(Constants.DATE_FORMATTER)}, null, null, null);
            if (cursorToDate.moveToLast()) {
                switch (sim) {
                    case Constants.SIM1:
                        cv1.put("rx", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM1RX)));
                        cv1.put("tx", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM1TX)));
                        cv1.put("total", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL1)));
                        cv1.put("period", cursorToDate.getInt(cursorToDate.getColumnIndex(Constants.PERIOD1)));
                        cv1.put("rx_n", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM1RX_N)));
                        cv1.put("tx_n", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM1TX_N)));
                        cv1.put("total_n", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL1_N)));
                        break;
                    case Constants.SIM2:
                        cv1.put("rx", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM2RX)));
                        cv1.put("tx", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM2TX)));
                        cv1.put("total", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL2)));
                        cv1.put("period", cursorToDate.getInt(cursorToDate.getColumnIndex(Constants.PERIOD2)));
                        cv1.put("rx_n", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM2RX_N)));
                        cv1.put("tx_n", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM2TX_N)));
                        cv1.put("total_n", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL2_N)));
                        break;
                    case Constants.SIM3:
                        cv1.put("rx", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM3RX)));
                        cv1.put("tx", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM3TX)));
                        cv1.put("total", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL3)));
                        cv1.put("period", cursorToDate.getInt(cursorToDate.getColumnIndex(Constants.PERIOD3)));
                        cv1.put("rx_n", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM3RX_N)));
                        cv1.put("tx_n", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.SIM3TX_N)));
                        cv1.put("total_n", cursorToDate.getLong(cursorToDate.getColumnIndex(Constants.TOTAL3_N)));
                        break;
                }
            }
            cursorToDayBeforeDate = mSqLiteDatabase.query(Constants.TRAFFIC, new String[]{
                    Constants.SIM1RX, Constants.SIM1TX, Constants.TOTAL1, Constants.SIM2RX, Constants.SIM2TX,
                    Constants.TOTAL2, Constants.SIM3RX, Constants.SIM3TX, Constants.TOTAL3, Constants.PERIOD1,
                    Constants.PERIOD2, Constants.PERIOD3, Constants.SIM1RX_N, Constants.SIM1TX_N, Constants.TOTAL1_N,
                    Constants.SIM2RX_N, Constants.SIM2TX_N, Constants.TOTAL2_N, Constants.SIM3RX_N, Constants.SIM3TX_N,
                    Constants.TOTAL3_N}, Constants.LAST_DATE + " = ?", new String[]{dayBeforeDate}, null, null, null);
            if (cursorToDayBeforeDate.moveToLast()) {
                switch (sim) {
                    case Constants.SIM1:
                        cv2.put("rx", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM1RX)));
                        cv2.put("tx", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM1TX)));
                        cv2.put("total", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL1)));
                        cv2.put("period", cursorToDayBeforeDate.getInt(cursorToDayBeforeDate.getColumnIndex(Constants.PERIOD1)));
                        cv2.put("rx_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM1RX_N)));
                        cv2.put("tx_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM1TX_N)));
                        cv2.put("total_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL1_N)));
                        break;
                    case Constants.SIM2:
                        cv2.put("rx", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM2RX)));
                        cv2.put("tx", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM2TX)));
                        cv2.put("total", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL2)));
                        cv2.put("period", cursorToDayBeforeDate.getInt(cursorToDayBeforeDate.getColumnIndex(Constants.PERIOD2)));
                        cv2.put("rx_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM2RX_N)));
                        cv2.put("tx_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM2TX_N)));
                        cv2.put("total_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL2_N)));
                        break;
                    case Constants.SIM3:
                        cv2.put("rx", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM3RX)));
                        cv2.put("tx", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM3TX)));
                        cv2.put("total", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL3)));
                        cv2.put("period", cursorToDayBeforeDate.getInt(cursorToDayBeforeDate.getColumnIndex(Constants.PERIOD3)));
                        cv2.put("rx_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM3RX_N)));
                        cv2.put("tx_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.SIM3TX_N)));
                        cv2.put("total_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex(Constants.TOTAL3_N)));
                        break;
                }
            }
        } else {
            cursorToDate = mSqLiteDatabase.query("data_" + imsi.get(sim), new String[]{
                            "rx", "tx", "total", "period", "rx_n", "tx_n", "total_n"}, Constants.LAST_DATE + " = ?",
                    new String[]{queried.toString(Constants.DATE_FORMATTER)}, null, null, null);
            if (cursorToDate.moveToLast()) {
                cv1.put("rx", cursorToDate.getLong(cursorToDate.getColumnIndex("rx")));
                cv1.put("tx", cursorToDate.getLong(cursorToDate.getColumnIndex("tx")));
                cv1.put("total", cursorToDate.getLong(cursorToDate.getColumnIndex("total")));
                cv1.put("period", cursorToDate.getInt(cursorToDate.getColumnIndex("period")));
                cv1.put("rx_n", cursorToDate.getLong(cursorToDate.getColumnIndex("rx_n")));
                cv1.put("tx_n", cursorToDate.getLong(cursorToDate.getColumnIndex("tx_n")));
                cv1.put("total_n", cursorToDate.getLong(cursorToDate.getColumnIndex("total_n")));
            }
            cursorToDayBeforeDate = mSqLiteDatabase.query("data_" + imsi.get(sim), new String[]{
                            "rx", "tx", "total", "period", "rx_n", "tx_n", "total_n"}, Constants.LAST_DATE + " = ?",
                    new String[]{dayBeforeDate}, null, null, null);
            if (cursorToDayBeforeDate.moveToLast()) {
                cv2.put("rx", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex("rx")));
                cv2.put("tx", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex("tx")));
                cv2.put("total", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex("total")));
                cv2.put("period", cursorToDayBeforeDate.getInt(cursorToDayBeforeDate.getColumnIndex("period")));
                cv2.put("rx_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex("rx_n")));
                cv2.put("tx_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex("tx_n")));
                cv2.put("total_n", cursorToDayBeforeDate.getLong(cursorToDayBeforeDate.getColumnIndex("total_n")));
            }
        }
        String[] prefSim = new String[Constants.PREF_SIM1.length];
        switch (sim) {
            case Constants.SIM1:
                prefSim = Constants.PREF_SIM1;
                break;
            case Constants.SIM2:
                prefSim = Constants.PREF_SIM2;
                break;
            case Constants.SIM3:
                prefSim = Constants.PREF_SIM3;
                break;
        }
        int choice = 0;
        switch (prefs.getString(prefSim[3], "0")) {
            case "0":
                if (cv1.size() > 0)
                    choice = 1;
                else
                    return null;
                break;
            case "1":
                if (queried.getDayOfMonth() != Integer.valueOf(prefs.getString(prefSim[10], "1")))
                    if (cv1.size() > 0 && cv2.size() > 0)
                        choice = 2;
                    else if (cv1.size() > 0 && cv2.size() == 0)
                        choice = 1;
                    else
                        return null;
                else {
                    if (cv1.size() > 0)
                        choice = 1;
                    else
                        return null;
                }
                break;
            case "2":
                if ((int) cv1.get("period") == 0)
                    if (cv1.size() > 0)
                        choice = 1;
                    else
                        return null;
                else {
                    if (cv1.size() > 0 && cv2.size() > 0)
                        choice = 2;
                    else if (cv1.size() > 0 && cv2.size() == 0)
                        choice = 1;
                    else
                        return null;
                }
                break;
        }
        switch (choice) {
            case 1:
                out.putLong("rx", (long) cv1.get("rx"));
                out.putLong("tx", (long) cv1.get("tx"));
                out.putLong("tot", (long) cv1.get("total"));
                out.putLong("rx_n", (long) cv1.get("rx_n"));
                out.putLong("tx_n", (long) cv1.get("tx_n"));
                out.putLong("tot_n", (long) cv1.get("total_n"));
                break;
            case 2:
                out.putLong("rx", ((long) cv1.get("rx") - (long) cv2.get("rx")) >= 0 ?
                        (long) cv1.get("rx") - (long) cv2.get("rx") : (long) cv1.get("rx"));
                out.putLong("tx", ((long) cv1.get("tx") - (long) cv2.get("tx")) >= 0 ?
                        (long) cv1.get("tx") - (long) cv2.get("tx") : (long) cv1.get("tx"));
                out.putLong("tot", ((long) cv1.get("total") - (long) cv2.get("total")) >= 0 ?
                        (long) cv1.get("total") - (long) cv2.get("total") : (long) cv1.get("total"));
                out.putLong("rx_n", ((long) cv1.get("rx_n") - (long) cv2.get("rx_n")) >= 0 ?
                        (long) cv1.get("rx_n") - (long) cv2.get("rx_n") : (long) cv1.get("rx_n"));
                out.putLong("tx_n", ((long) cv1.get("tx_n") - (long) cv2.get("tx_n")) >= 0 ?
                        (long) cv1.get("tx_n") - (long) cv2.get("tx_n") : (long) cv1.get("tx_n"));
                out.putLong("tot_n", ((long) cv1.get("total_n") - (long) cv2.get("total_n")) >= 0 ?
                        (long) cv1.get("total_n") - (long) cv2.get("total_n") : (long) cv1.get("total_n"));
                break;
        }
        cursorToDate.close();
        cursorToDayBeforeDate.close();
        return out;
    }

    public static ContentValues readCallsData(CustomDatabaseHelper dbHelper) {
        ContentValues cv = new ContentValues();
        mSqLiteDatabase = dbHelper.getReadableDatabase();
        Cursor cursor = mSqLiteDatabase.query(Constants.CALLS, null, null, null, null, null, null);
        if (cursor.moveToLast()) {
            cv.put(Constants.CALLS1, cursor.getLong(cursor.getColumnIndex(Constants.CALLS1)));
            cv.put(Constants.CALLS1_EX, cursor.getLong(cursor.getColumnIndex(Constants.CALLS1_EX)));
            cv.put(Constants.CALLS2, cursor.getLong(cursor.getColumnIndex(Constants.CALLS2)));
            cv.put(Constants.CALLS2_EX, cursor.getLong(cursor.getColumnIndex(Constants.CALLS2_EX)));
            cv.put(Constants.CALLS3, cursor.getLong(cursor.getColumnIndex(Constants.CALLS3)));
            cv.put(Constants.CALLS3_EX, cursor.getLong(cursor.getColumnIndex(Constants.CALLS3_EX)));
            cv.put(Constants.LAST_TIME, cursor.getString(cursor.getColumnIndex(Constants.LAST_TIME)));
            cv.put(Constants.LAST_DATE, cursor.getString(cursor.getColumnIndex(Constants.LAST_DATE)));
            cv.put(Constants.PERIOD1, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD1)));
            cv.put(Constants.PERIOD2, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD2)));
            cv.put(Constants.PERIOD3, cursor.getInt(cursor.getColumnIndex(Constants.PERIOD3)));
        } else {
            cv.put(Constants.CALLS1, 0L);
            cv.put(Constants.CALLS1_EX, 0L);
            cv.put(Constants.CALLS2, 0L);
            cv.put(Constants.CALLS2_EX, 0L);
            cv.put(Constants.CALLS3, 0L);
            cv.put(Constants.CALLS3_EX, 0L);
            cv.put(Constants.LAST_TIME, "");
            cv.put(Constants.LAST_DATE, "");
            cv.put(Constants.PERIOD1, 0);
            cv.put(Constants.PERIOD2, 0);
            cv.put(Constants.PERIOD3, 0);
        }
        cursor.close();
        return cv;
    }

    public static void writeList(int sim, ArrayList<String> list, CustomDatabaseHelper dbHelper, ArrayList<String> imsi, String name) {
        String table = getTableName(sim, imsi, name);
        if (sim >= 0) {
            mSqLiteDatabase = dbHelper.getReadableDatabase();
            try {
                mSqLiteDatabase.query(table, null, null, null, null, null, null);
            } catch (Exception e) {
                String DATABASE_CREATE_SCRIPT = "create table "
                        + table + " (" + Constants.NUMBER + " text not null);";
                mSqLiteDatabase = dbHelper.getWritableDatabase();
                mSqLiteDatabase.execSQL(DATABASE_CREATE_SCRIPT);
            }
            mSqLiteDatabase.delete(table, null, null);
            for (String s : list) {
                ContentValues cv = new ContentValues();
                cv.put(Constants.NUMBER, s);
                mSqLiteDatabase.insert(table, null, cv);
            }
            CustomApplication.getAppContext().getContentResolver().notifyChange(Constants.UID_URI, null);
        }
    }

    public static ArrayList<String> readList(int sim, CustomDatabaseHelper dbHelper, ArrayList<String> imsi, String name) {
        String table = getTableName(sim, imsi, name);
        ArrayList<String> list = new ArrayList<>();
        if (sim >= 0) {
            mSqLiteDatabase = dbHelper.getReadableDatabase();
            try {
                mSqLiteDatabase.query(table, null, null, null, null, null, null);
            } catch (Exception e) {
                String DATABASE_CREATE_SCRIPT = "create table "
                        + table + " (" + Constants.NUMBER + " text not null);";
                dbHelper.getWritableDatabase().execSQL(DATABASE_CREATE_SCRIPT);
                mSqLiteDatabase = dbHelper.getReadableDatabase();
            }
            try {
                Cursor cursor = mSqLiteDatabase.query(table, new String[]{Constants.NUMBER}, null, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        list.add(cursor.getString(cursor.getColumnIndex(Constants.NUMBER)));
                        cursor.moveToNext();
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    private static String getTableName(int sim, ArrayList<String> imsi, String name) {
        String table = "";
        switch (sim) {
            case Constants.SIM1:
                if (imsi != null)
                    table = name + "_" + imsi.get(0);
                else {
                    if (name.contains("h"))
                        table = WHITE_LIST_1;
                    else if (name.contains("b"))
                        table = BLACK_LIST_1;
                    else
                        table = UID_1;
                }
                break;
            case Constants.SIM2:
                if (imsi != null)
                    table = name + "_" + imsi.get(1);
                else {
                    if (name.contains("h"))
                        table = WHITE_LIST_2;
                    else if (name.contains("b"))
                        table = BLACK_LIST_2;
                    else
                        table = UID_2;
                }
                break;
            case Constants.SIM3:
                if (imsi != null)
                    table = name + "_" + imsi.get(2);
                else {
                    if (name.contains("h"))
                        table = WHITE_LIST_3;
                    else if (name.contains("b"))
                        table = BLACK_LIST_3;
                    else
                        table = UID_3;
                }
                break;
        }
        return table;
    }

    public static void deleteListTables(CustomDatabaseHelper dbHelper, ArrayList<String> imsi) {
        if (imsi != null) {
            int i = 1;
            for (String name : imsi) {
                try {
                    mSqLiteDatabase = dbHelper.getReadableDatabase();
                    Cursor cursor = mSqLiteDatabase.query("list" + i, null, null, null, null, null, null);
                    mSqLiteDatabase = dbHelper.getWritableDatabase();
                    if (cursor.getColumnCount() > 1) {
                        String DELETE = "DROP TABLE IF EXISTS list" + i;
                        mSqLiteDatabase.execSQL(DELETE);
                        String DATABASE_CREATE_SCRIPT = "create table list"
                                + i + " (" + Constants.NUMBER + " text not null);";
                        mSqLiteDatabase.execSQL(DATABASE_CREATE_SCRIPT);
                    } else
                        mSqLiteDatabase.delete("list" + i, null, null);
                    cursor.close();
                    String COPY = "INSERT INTO list" + i + " SELECT * FROM white_" + name;
                    mSqLiteDatabase.execSQL(COPY);
                    String DELETE = "DROP TABLE IF EXISTS white_" + name;
                    mSqLiteDatabase.execSQL(DELETE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    mSqLiteDatabase = dbHelper.getWritableDatabase();
                    mSqLiteDatabase.delete("list" + i + "_b", null, null);
                    String COPY = "INSERT INTO list" + i + "_b SELECT * FROM black_" + name;
                    mSqLiteDatabase.execSQL(COPY);
                    String DELETE = "DROP TABLE IF EXISTS black_" + name;
                    mSqLiteDatabase.execSQL(DELETE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
    }

    public static void deleteDataTable(CustomDatabaseHelper dbHelper, ArrayList<String> imsi, String table) {
        for (String name : imsi) {
            try {
                mSqLiteDatabase = dbHelper.getWritableDatabase();
                String DELETE = "DROP TABLE IF EXISTS " + table + "_" + name;
                mSqLiteDatabase.execSQL(DELETE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*public static void deleteInstance() {
        mInstance.close();
    }*/
}