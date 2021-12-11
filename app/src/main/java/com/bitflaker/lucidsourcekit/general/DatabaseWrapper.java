package com.bitflaker.lucidsourcekit.general;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseWrapper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + StoredSettings.TABLE_NAME + " (" +
                    StoredSettings.COLUMN_NAME_PROPERTY + " TEXT PRIMARY KEY," +
                    StoredSettings.COLUMN_NAME_VALUE + " TEXT)";
    private static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + StoredSettings.TABLE_NAME;
    private static SQLiteDatabase readableDB = null;
    private static SQLiteDatabase writableDB = null;

    public DatabaseWrapper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        if (readableDB==null){
            readableDB = getReadableDatabase();
        }
        if(writableDB==null){
            writableDB = getWritableDatabase();
        }
    }

    public StoredSettings GetProperty(String propertyName) {
        String[] projection = {
                StoredSettings.COLUMN_NAME_PROPERTY,
                StoredSettings.COLUMN_NAME_VALUE
        };
        String selection = StoredSettings.COLUMN_NAME_PROPERTY + " = ?";
        String[] selectionArgs = { propertyName };
        String sortOrder = StoredSettings.COLUMN_NAME_PROPERTY + " DESC";
        Cursor cursor = readableDB.query(StoredSettings.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

        StoredSettings property = null;
        while(cursor.moveToNext()) {
            String prop = cursor.getString(cursor.getColumnIndexOrThrow(StoredSettings.COLUMN_NAME_PROPERTY));
            String value = cursor.getString(cursor.getColumnIndexOrThrow(StoredSettings.COLUMN_NAME_VALUE));
            property = new StoredSettings(prop, value);
        }
        cursor.close();
        return property;
    }

    public void SetProperty(String propertyName, String propertyValue) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(StoredSettings.COLUMN_NAME_PROPERTY, propertyName);
        initialValues.put(StoredSettings.COLUMN_NAME_VALUE, propertyValue);

        int id = (int) writableDB.insertWithOnConflict(StoredSettings.TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            writableDB.update(StoredSettings.TABLE_NAME, initialValues, "property=?", new String[] { propertyName });
        }
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: convert data after database version upgrade and not just delete all data
        db.execSQL(SQL_DELETE_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
