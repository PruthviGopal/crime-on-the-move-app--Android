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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

        public abstract void useData(String countyName, List<LatLng> countyOutline,
                Map<String, String> countyStatistics);

        public void execute() throws JSONException {
            DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(mContext);
            SQLiteDatabase database = accessPoint.getReadableDatabase();

            Cursor cursor = database.query(NOVA_COUNTY_OUTLINE_TABLE_NAME, null, null, null, null, null, null, null);
            JSONArray jsonCountyOutline;
            List<LatLng> countyOutline;
            JSONObject jsonCountyStatistics;
            Map<String, String> countyStatistics;
            while (cursor.moveToNext()) {
                if (!cursor.isNull(0) && !cursor.isNull(1)) {
                    jsonCountyOutline = new JSONArray(cursor.getString(1));
                    countyOutline = new ArrayList<>(jsonCountyOutline.length());
                    Utils.parseArea(countyOutline, jsonCountyOutline);
                    jsonCountyStatistics = new JSONObject(cursor.getString(2));
                    countyStatistics = new HashMap<>(jsonCountyStatistics.length());
                    Utils.parseStatistics(countyStatistics, jsonCountyStatistics);
                    useData(cursor.getString(0), countyOutline, countyStatistics);
                } else {
                    Log.w(TAG, "THERE IS A NULL FIELD IN THE NOVA COUNTY OUTLINES TABLE!");
                }
            }
            cursor.close();
            database.close();
        }
    }

    public static abstract class NovaCrimeQuery extends CacheQuery {

        private String[] mCountyConstraints;

        public NovaCrimeQuery(Context context, String[] countyConstraints) {
            super(context);
            mCountyConstraints = countyConstraints;
        }

        public void execute() {
            DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(mContext);
            SQLiteDatabase database = accessPoint.getReadableDatabase();

            Cursor cursor;
            // If we only want certain counties
            if (mCountyConstraints != null) {
                StringBuilder build = new StringBuilder();
                for (int i = 0; i < mCountyConstraints.length; i++) {
                    String county = mCountyConstraints[i];
                    build.append(NOVA_CRIME_COUNTY).append("=").append("'").append(county).append("'");
                    if (i < (mCountyConstraints.length - 1)) {
                        build.append(" OR ");
                    }
                }
                String selection = build.toString();
                cursor = database.query(NOVA_CRIME_TABLE_NAME, null, selection, null, null, null, null, null);
            // Get all nova crime data
            } else {
                cursor = database.query(NOVA_CRIME_TABLE_NAME, null, null, null, null, null, null, null);
            }
            String address;
            double latitude;
            double longitude;
            while (cursor.moveToNext()) {
                address = cursor.getString(cursor.getColumnIndex(NOVA_CRIME_ADDRESS));
                longitude = cursor.getDouble(cursor.getColumnIndex(NOVA_CRIME_X_CORD));
                latitude = cursor.getDouble(cursor.getColumnIndex(NOVA_CRIME_Y_CORD));
                useData(latitude, longitude, address);
            }

            cursor.close();
            database.close();
        }

        public abstract void useData(double latitude, double longitude, String address);
    }

    public static abstract class DcCrimeQuery extends CacheQuery {

        public DcCrimeQuery(Context context) {
            super(context);
        }

        public void execute() {
            DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(mContext);
            SQLiteDatabase database = accessPoint.getReadableDatabase();

            Cursor cursor = database.query(DC_CRIME_TABLE_NAME, null, null, null, null, null, null, null);
            String address;
            double latitude;
            double longitude;
            while (cursor.moveToNext()) {
                address = cursor.getString(cursor.getColumnIndex(DC_CRIME_ADDRESS));
                longitude = cursor.getDouble(cursor.getColumnIndex(DC_CRIME_X_CORD));
                latitude = cursor.getDouble(cursor.getColumnIndex(DC_CRIME_Y_CORD));
                useData(latitude, longitude, address);
            }

            cursor.close();
            database.close();
        }

        public abstract void useData(double latitude, double longitude, String address);
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
     * @param context used to access the database.
     * @param novaCrimes JSONArray with fields for different attributes of the crime data.
     */
    public void insertNovaCrimeData(Context context, JSONArray novaCrimes) throws JSONException {
        Log.v(TAG, "insertNovaCrimeData");
        DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(context);
        SQLiteDatabase database = accessPoint.getWritableDatabase();

        JSONObject novaCrime;
        ContentValues contentValues;
        for (int i = 0; i < novaCrimes.length(); i++) {
            novaCrime = novaCrimes.getJSONObject(i);
            Iterator<String> jsonIt = novaCrime.keys();
            contentValues = new ContentValues();
            while (jsonIt.hasNext()) {
                String key = jsonIt.next();
                if (novaCrime.isNull(key)) {
                    continue;
                }
                switch(key) {
                    case "id":
                        contentValues.put(NOVA_CRIME_ID, novaCrime.getInt(key));
                        break;
                    case "x_cord":
                        contentValues.put(NOVA_CRIME_X_CORD, novaCrime.getDouble(key));
                        break;
                    case "y_cord":
                        contentValues.put(NOVA_CRIME_Y_CORD, novaCrime.getDouble(key));
                        break;
                    case "address":
                        contentValues.put(NOVA_CRIME_ADDRESS, novaCrime.getString(key));
                        break;
                    case "zip_code":
                        contentValues.put(NOVA_CRIME_ZIP_CODE, novaCrime.getString(key));
                        break;
                    case "city":
                        contentValues.put(NOVA_CRIME_CITY, novaCrime.getString(key));
                        break;
                    case "county":
                        contentValues.put(NOVA_CRIME_COUNTY, novaCrime.getString(key));
                        break;
                    case "report_date":
                        contentValues.put(NOVA_CRIME_REPORT_DATE, novaCrime.getString(key));
                        break;
                }
            }
            database.insert(NOVA_CRIME_TABLE_NAME, null, contentValues);
        }

        database.close();

        database = accessPoint.getReadableDatabase();
        Log.d(TAG, "database insertion sanity check: ");
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + NOVA_CRIME_TABLE_NAME + ";", null);
        cursor.moveToFirst();
        Log.d(TAG, "Count: ||" + cursor.getString(0) + "||");
        cursor.close();
        database.close();
    }

    /**
     * Counts the number of rows in the nova crimes table.
     *
     * @param context used for database access.
     * @return the number of rows in the nova crimes table.
     */
    public int novaCrimeRowsChecksum(Context context) {
        int novaCrimeRowsCheckSum;
        DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(context);
        SQLiteDatabase database = accessPoint.getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + NOVA_CRIME_TABLE_NAME + ";", null);
        novaCrimeRowsCheckSum = cursor.getCount();
        cursor.close();
        database.close();
        return novaCrimeRowsCheckSum;
    }

    /**
     * Removes all rows from the nova county outline table.
     * Cache invalidation.
     * @param context used for database access.
     */
    public void wipeNovaCrimeRows(Context context) {
        Log.w(TAG, "wipeNovaCrimeRows");
        DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(context);
        SQLiteDatabase database = accessPoint.getWritableDatabase();
        database.delete(NOVA_CRIME_TABLE_NAME, null, null);
        database.close();
    }

    /**
     * Insert DC crime data
     *
     * @param context used to access database.
     */
    public void insertDcCrimeData(Context context, JSONArray dcCrimes) throws JSONException {
        Log.v(TAG, "insertDcCrimeData");
        DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(context);
        SQLiteDatabase database = accessPoint.getWritableDatabase();

        JSONObject dcCrime;
        ContentValues contentValues;
        for (int i = 0; i < dcCrimes.length(); i++) {
            dcCrime = dcCrimes.getJSONObject(i);
            Iterator<String> jsonIt = dcCrime.keys();
            contentValues = new ContentValues();
            while (jsonIt.hasNext()) {
                String key = jsonIt.next();
                if (dcCrime.isNull(key)) {
                    continue;
                }
                switch(key) {
                    case "id":
                        contentValues.put(DC_CRIME_ID, dcCrime.getInt(key));
                        break;
                    case "x_cord":
                        contentValues.put(DC_CRIME_X_CORD, dcCrime.getDouble(key));
                        break;
                    case "y_cord":
                        contentValues.put(DC_CRIME_Y_CORD, dcCrime.getDouble(key));
                        break;
                    case "address":
                        contentValues.put(DC_CRIME_ADDRESS, dcCrime.getString(key));
                        break;
                    case "ward":
                        contentValues.put(DC_CRIME_WARD, dcCrime.getString(key));
                        break;
                    case "report_date":
                        contentValues.put(NOVA_CRIME_REPORT_DATE, dcCrime.getString(key));
                        break;
                }
            }
            database.insert(DC_CRIME_TABLE_NAME, null, contentValues);
        }
        database.close();

        database = accessPoint.getReadableDatabase();
        Log.d(TAG, "database insertion sanity check: ");
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + DC_CRIME_TABLE_NAME + ";", null);
        cursor.moveToFirst();
        Log.d(TAG, "Count: ||" + cursor.getString(0)  + "||");
        cursor.close();
        database.close();
    }

    public int dcCrimeRowsChecksum(Context context) {
        int dcCrimeRowsCheckSum;
        DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(context);
        SQLiteDatabase database = accessPoint.getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + DC_CRIME_TABLE_NAME + ";", null);
        dcCrimeRowsCheckSum = cursor.getCount();
        cursor.close();
        database.close();
        return dcCrimeRowsCheckSum;
    }

    public void wipeDcCrimeRows(Context context) {
        Log.w(TAG, "wipeDcCrimeRows");
        DatabaseAccessPoint accessPoint = new DatabaseAccessPoint(context);
        SQLiteDatabase database = accessPoint.getWritableDatabase();
        database.delete(DC_CRIME_TABLE_NAME, null, null);
        database.close();
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
        JSONObject areaStatisticsObject = response.getJSONObject("area_statistics");

        // ASSERTION
        if (BuildConfig.DEBUG && areaOutlinesObject.length() != areaOutlinesObject.length()) {
            throw new AssertionError("areaOutlinesObject.length() != areaOutlinesObject.length())");
        }

        Iterator<String> outlineKeyIt = areaOutlinesObject.keys();
        Iterator<String> statisticsKeyIt = areaStatisticsObject.keys();
        String outlineKey;
        String statisticsKey;
        JSONArray jsonAreaPoints;
        JSONObject jsonStatistics;
        ContentValues contentValues;
        while (outlineKeyIt.hasNext()) {
            outlineKey = outlineKeyIt.next();
            jsonAreaPoints = areaOutlinesObject.getJSONArray(outlineKey);
            statisticsKey = statisticsKeyIt.next();
            jsonStatistics = areaStatisticsObject.getJSONObject(statisticsKey);
            contentValues = new ContentValues();
            contentValues.put(NOVA_COUNTY_OUTLINE_COUNTY_NAME_COLUMN, outlineKey);
            contentValues.put(NOVA_COUNTY_OUTLINE_COUNTY_OUTLINE_COLUMN, jsonAreaPoints.toString());
            contentValues.put(NOVA_COUNTY_OUTLINE_STATISTICS_COLUMN, jsonStatistics.toString());
            database.insert(NOVA_COUNTY_OUTLINE_TABLE_NAME, null, contentValues);
        }
        database.close();

        database = accessPoint.getReadableDatabase();
        Log.d(TAG, "database insertion sanity check: ");
        Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM " + NOVA_COUNTY_OUTLINE_TABLE_NAME + ";", null);
        cursor.moveToFirst();
        Log.d(TAG, "Count: ||" + cursor.getString(0)  + "||");
        cursor.close();
        database.close();
    }

    /**
     * Performs a checksum on the number of rows in the nova county table.
     * @param context used for database access.
     * @return the number of rows in the nova county outlines table.
     */
    public int novaCountyRowsChecksum(Context context) {
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

    /**
     * Clear all rows out of the local SQLite database.
     *
     * @param context used for database access.
     */
    public void clearCache(Context context) {
        wipeCountyRows(context);
        wipeNovaCrimeRows(context);
        wipeDcCrimeRows(context);
    }

    //----------------------------------------------------------------------------------------------
    // General database fields
    private static final int DATABASE_VERSION = 17;
    private static final String DATABASE_NAME = "CrimeOnTheMoveSQLiteCache";
    // Nova County Outline Fields
    private static final String NOVA_COUNTY_OUTLINE_TABLE_NAME = "nova_county_outlines";
    private static final String NOVA_COUNTY_OUTLINE_COUNTY_NAME_COLUMN = "county_name";
    private static final String NOVA_COUNTY_OUTLINE_COUNTY_OUTLINE_COLUMN = "points_json";
    private static final String NOVA_COUNTY_OUTLINE_STATISTICS_COLUMN = "statistics";
    private static final String CREATE_NOVA_COUNTY_OUTLINE_TABLE_SQL
            = "CREATE TABLE " + NOVA_COUNTY_OUTLINE_TABLE_NAME
                + "("
                + NOVA_COUNTY_OUTLINE_COUNTY_NAME_COLUMN + " TEXT PRIMARY KEY, "
                + NOVA_COUNTY_OUTLINE_COUNTY_OUTLINE_COLUMN + " TEXT, "
                + NOVA_COUNTY_OUTLINE_STATISTICS_COLUMN + " TEXT"
                + ");";
    // Nova Crime Data Fields
    private static final String NOVA_CRIME_TABLE_NAME = "nova_crime_data";
    private static final String NOVA_CRIME_REPORT_DATE = "report_date";
    private static final String NOVA_CRIME_OFFENSE = "offense";
    private static final String NOVA_CRIME_ADDRESS = "address";
    private static final String NOVA_CRIME_CITY = "city";
    private static final String NOVA_CRIME_COUNTY = "county";
    private static final String NOVA_CRIME_ZIP_CODE = "zip_code";
    private static final String NOVA_CRIME_X_CORD = "x_cord";
    private static final String NOVA_CRIME_Y_CORD = "y_cord";
    private static final String NOVA_CRIME_ID = "id";
    private static final String CREATE_NOVA_CRIME_DATA_TABLE_SQL
            = "CREATE TABLE " + NOVA_CRIME_TABLE_NAME
                + "("
                + NOVA_CRIME_REPORT_DATE + " TEXT, "
                + NOVA_CRIME_OFFENSE + " TEXT, "
                + NOVA_CRIME_ADDRESS + " TEXT, "
                + NOVA_CRIME_CITY + " TEXT, "
                + NOVA_CRIME_COUNTY + " TEXT, "
                + NOVA_CRIME_ZIP_CODE + " TEXT, "
                + NOVA_CRIME_X_CORD + " REAL, "
                + NOVA_CRIME_Y_CORD + " REAL, "
                + NOVA_CRIME_ID + " INTEGER PRIMARY KEY"
                + ");";
    // DC Crime Data Fields
    private static final String DC_CRIME_TABLE_NAME = "dc_crime_data";
    private static final String DC_CRIME_REPORT_DATE = "report_date";
    private static final String DC_CRIME_OFFENSE = "offense";
    private static final String DC_CRIME_ADDRESS = "address";
    private static final String DC_CRIME_WARD = "ward";
    private static final String DC_CRIME_X_CORD = "x_cord";
    private static final String DC_CRIME_Y_CORD = "y_cord";
    private static final String DC_CRIME_ID = "id";
    private static final String CREATE_DC_CRIME_DATA_TABLE_SQL
            = "CREATE TABLE " + DC_CRIME_TABLE_NAME
                + "("
                + DC_CRIME_REPORT_DATE + " TEXT, "
                + DC_CRIME_OFFENSE + " TEXT, "
                + DC_CRIME_ADDRESS + " TEXT, "
                + DC_CRIME_WARD + " TEXT, "
                + DC_CRIME_X_CORD + " REAL, "
                + DC_CRIME_Y_CORD + " REAL, "
                + DC_CRIME_ID + " INTEGER PRIMARY KEY"
                + ")";

    /**
     * Class used for access to SQLite database.
     */
    private static class DatabaseAccessPoint extends SQLiteOpenHelper {

        private final String TAG = "DatabaseAccessPoint";

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
            Log.w(TAG, "onCreate Called!");
            db.execSQL(CREATE_NOVA_COUNTY_OUTLINE_TABLE_SQL);
            db.execSQL(CREATE_NOVA_CRIME_DATA_TABLE_SQL);
            db.execSQL(CREATE_DC_CRIME_DATA_TABLE_SQL);
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
            db.execSQL("DROP TABLE IF EXISTS " + NOVA_COUNTY_OUTLINE_TABLE_NAME + ";");
            db.execSQL("DROP TABLE IF EXISTS " + NOVA_CRIME_TABLE_NAME + ";");
            db.execSQL("DROP TABLE IF EXISTS " + DC_CRIME_TABLE_NAME + ";");

            db.execSQL(CREATE_NOVA_COUNTY_OUTLINE_TABLE_SQL);
            db.execSQL(CREATE_NOVA_CRIME_DATA_TABLE_SQL);
            db.execSQL(CREATE_DC_CRIME_DATA_TABLE_SQL);
        }
    }
}
