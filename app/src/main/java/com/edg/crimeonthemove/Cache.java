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
 * Provides access to a SQLite cache.
 * Data is added to it through methods taking JSON objects (where the raw json strings are cached).
 * Data is retrieved from it through overriding various CacheQuery objects and passing
 * them to runQuery.
 */
public class Cache {

    private static final String TAG = "Cache";

    /**
     * A class which defines a framework by which any query can be executed generically.
     * Requires a context object to access the database, the context is de-referenced in finish()
     * to avoid memory leaks.
     */
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

    /**
     * CacheQuery used to retrieve Nova counties from the cache.
     * The useData function should be overriden so that any type of object can use the data retrieved
     * in any way they wish.
     */
    public static abstract class NovaCountyQuery extends CacheQuery {

        public NovaCountyQuery(Context context) {
            super(context);
        }

        public abstract void useData(String countyName, List<LatLng> countyOutline);

        public void execute() throws JSONException {
            DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(mContext);
            SQLiteDatabase database = accessPoint.getReadableDatabase();

            Cursor cursor = database.query(NOVA_COUNTY_OUTLINE_TABLE_NAME, null, null, null, null, null, null, null);
            JSONArray jsonCountyOutline;
            List<LatLng> countyOutline;
            while (cursor.moveToNext()) {
                if (!cursor.isNull(0) && !cursor.isNull(1)) {
                    jsonCountyOutline = new JSONArray(cursor.getString(1));
                    countyOutline = new ArrayList<>(jsonCountyOutline.length());
                    Utils.parseArea(countyOutline, jsonCountyOutline);
                    useData(cursor.getString(0), countyOutline);
                } else {
                    Log.w(TAG, "THERE IS A NULL FIELD IN THE NOVA COUNTY OUTLINES TABLE!");
                }
            }
            cursor.close();
            database.close();
        }
    }

    /**
     * Takes an instance of a CacheQuery class and executes its operation.
     *
     * @param query a sub-class of the CacheQuery class which is to be overridden to provide
     *              specific query access to the cache followed by parsing (hence the JSONException).
     * @throws JSONException
     */
    public void runQuery(CacheQuery query) throws JSONException {
        query.execute();
        query.finish();
    }

    /**
     * Insert Nova crime data
     *
     * @param context
     */
    public void insertNovaCrimeData(Context context) {

    }

    /**
     * Insert DC crime data
     *
     * @param context
     */
    public void insertDCCrimeData(Context context) {

    }

    /**
     * Insert nova county data contained in the JSONObject.
     *
     * @param context application context used for database access.
     * @param response the JSONObject containing JSON representing a nova county outline.
     * @throws JSONException
     */
    public void insertNovaCountyData(Context context, JSONObject response) throws JSONException {
        Log.v(TAG, "insertNovaCountyData");
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
            contentValues.put(NOVA_COUNTY_OUTLINE_COUNTY_NAME_COLUMN, key);
            contentValues.put(NOVA_COUNTY_OUTLINE_COUNTY_OUTLINE_COLUMN, jsonAreaPoints.toString());
            database.insert(NOVA_COUNTY_OUTLINE_TABLE_NAME, null, contentValues);
        }
        database.close();

        database = accessPoint.getReadableDatabase();
        Log.d(TAG, "database insertion sanity check: ");
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + NOVA_COUNTY_OUTLINE_TABLE_NAME + ";", null);
        cursor.moveToFirst();
        Log.d(TAG, "Count: ||" + cursor.getString(0)  + "||");
        database.close();
    }

    /**
     * Performs a checksum on the number of rows in the nova county table.
     * @param context used for database access.
     * @return the number of rows in the nova county outlines table.
     */
    public int novaCountyRowsCheckSum(Context context) {
        int countyRowsCheckSum;
        DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(context);
        SQLiteDatabase database = accessPoint.getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + NOVA_COUNTY_OUTLINE_TABLE_NAME + ";", null);
        countyRowsCheckSum = cursor.getCount();
        cursor.close();
        database.close();
        return countyRowsCheckSum;
    }

    /**
     * Removes all rows from the nova county outline table.
     * Cache invalidation.
     * @param context used for database access.
     */
    public void wipeCountyRows(Context context) {
        Log.w(TAG, "wipeCountyRows");
        DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(context);
        SQLiteDatabase database = accessPoint.getWritableDatabase();
        database.delete(NOVA_COUNTY_OUTLINE_TABLE_NAME, null, null);
        database.close();
    }

    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "CrimeOnTheMoveSQLiteCache";
    private static final String NOVA_COUNTY_OUTLINE_TABLE_NAME = "nova_county_outlines";
    private static final String NOVA_COUNTY_OUTLINE_COUNTY_NAME_COLUMN = "county_name";
    private static final String NOVA_COUNTY_OUTLINE_COUNTY_OUTLINE_COLUMN = "points_json";
    private static final String CREATE_NOVA_COUNTY_OUTLINE_TABLE_SQL
            = "CREATE TABLE " + NOVA_COUNTY_OUTLINE_TABLE_NAME
                + "("
                + NOVA_COUNTY_OUTLINE_COUNTY_NAME_COLUMN + " TEXT PRIMARY KEY, "
                + NOVA_COUNTY_OUTLINE_COUNTY_OUTLINE_COLUMN + " TEXT"
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
            db.execSQL(CREATE_NOVA_COUNTY_OUTLINE_TABLE_SQL);
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
            Log.w(TAG, "onUpgrade Called!");
            // db.execSQL("DROP TABLE county_outlines;");
            // db.execSQL(CREATE_NOVA_COUNTY_OUTLINE_TABLE_SQL);
        }
    }
}
