package com.edg.crimeonthemove;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by egaebel on 11/26/15.
 */
public class Cache {

    private static final String TAG = "Cache";

    private static abstract class CacheQuery {

        protected Context mContext;

        public CacheQuery(Context context) {
            mContext = context;
        }

        public void finish() {
            mContext = null;
        }

        public abstract void execute() throws JSONException;
    }

    public static abstract class CountyQuery extends CacheQuery {

        public CountyQuery(Context context) {
            super(context);
        }

        public abstract void useData(String countyName, List<LatLng> countyOutline);

        public void execute() throws JSONException {
            DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(mContext);
            SQLiteDatabase database = accessPoint.getReadableDatabase();

            Cursor cursor = database.query(COUNTY_OUTLINE_TABLE_NAME, null, null, null, null, null, null, null);
            JSONArray jsonCountyOutline;
            List<LatLng> countyOutline;
            while (cursor.moveToNext()) {
                if (!cursor.isNull(0) && !cursor.isNull(1)) {
                    jsonCountyOutline = new JSONArray(cursor.getString(1));
                    countyOutline = new ArrayList<>(jsonCountyOutline.length());
                    Utils.parseArea(countyOutline, jsonCountyOutline);
                    useData(cursor.getString(0), countyOutline);
                } else {
                    Log.w(TAG, "THERE IS A NULL FIELD IN THE COUNTY OUTLINES TABLE!");
                }
            }
            cursor.close();
            database.close();
        }
    }

    public void runQuery(CacheQuery query) throws JSONException {
        query.execute();
        query.finish();
    }

    public void insertCountyData(Context context, JSONObject response) throws JSONException {

        DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(context);
        SQLiteDatabase database = accessPoint.getWritableDatabase();

        // Parse out arrays of points describing boundaries
        JSONObject areaOutlinesObject = response.getJSONObject("area_outline");
        Iterator<String> keyIt = areaOutlinesObject.keys();
        String key;
        JSONArray jsonAreaPoints;
        ContentValues contentValues;
        while (keyIt.hasNext()) {
            key = keyIt.next();
            jsonAreaPoints = areaOutlinesObject.getJSONArray(key);
            contentValues = new ContentValues();
            contentValues.put(COUNTY_OUTLINE_COUNTY_NAME_COLUMN, key);
            contentValues.put(COUNTY_OUTLINE_COUNTY_OUTLINE_COLUMN, jsonAreaPoints.toString());
            database.insert(COUNTY_OUTLINE_TABLE_NAME, null, contentValues);
        }
        database.close();
    }

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "CrimeOnTheMoveSQLiteCache";
    private static final String COUNTY_OUTLINE_TABLE_NAME = "county_outlines";
    private static final String COUNTY_OUTLINE_COUNTY_NAME_COLUMN = "county_name";
    private static final String COUNTY_OUTLINE_COUNTY_OUTLINE_COLUMN = "points_json";
    private static final String CREATE_COUNTY_OUTLINE_TABLE_SQL
            = "CREATE TABLE " + COUNTY_OUTLINE_TABLE_NAME
                + "("
                + COUNTY_OUTLINE_COUNTY_NAME_COLUMN + " TEXT, "
                + COUNTY_OUTLINE_COUNTY_OUTLINE_COLUMN + " TEXT"
                + ");";
    private static final String CREATE_NOVA_CRIME_DATA_TABLE_SQL
            = "CREATE TABLE";
    private static final String CREATE_DC_CRIME_DATA_TABLE_SQL
            = "CREATE TABLE";

    private static class DatabaseAccessPoint extends SQLiteOpenHelper {

        public DatabaseAccessPoint(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Called when the database is created for the first time. This is where the
         * creation of tables and the initial population of the tables should happen.
         *
         * @param db The database.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_COUNTY_OUTLINE_TABLE_SQL);
            // db.execSQL(CREATE_NOVA_CRIME_DATA_TABLE_SQL);
            // db.execSQL(CREATE_DC_CRIME_DATA_TABLE_SQL);
        }

        /**
         * Called when the database needs to be upgraded. The implementation
         * should use this method to drop tables, add tables, or do anything else it
         * needs to upgrade to the new schema version.
         * <p/>
         * <p>
         * The SQLite ALTER TABLE documentation can be found
         * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
         * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
         * you can use ALTER TABLE to rename the old table, then create the new table and then
         * populate the new table with the contents of the old table.
         * </p><p>
         * This method executes within a transaction.  If an exception is thrown, all changes
         * will automatically be rolled back.
         * </p>
         *
         * @param db         The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
