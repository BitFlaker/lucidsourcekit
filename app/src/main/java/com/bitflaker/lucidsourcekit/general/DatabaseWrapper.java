package com.bitflaker.lucidsourcekit.general;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bitflaker.lucidsourcekit.general.database.StoredJournalAudioLocations;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalDreamClarities;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalDreamMoods;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalEntries;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalHasTag;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalIsType;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalSleepQualities;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalTags;
import com.bitflaker.lucidsourcekit.general.database.StoredJournalTypes;
import com.bitflaker.lucidsourcekit.general.database.StoredSettings;
import com.bitflaker.lucidsourcekit.general.database.values.DreamJournalEntriesList;

public class DatabaseWrapper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 8;
    public static final String DATABASE_NAME = "FeedReader.db";
    private static final String SQL_CREATE_SETTINGS_TABLE = "CREATE TABLE " + StoredSettings.TABLE_NAME + " (" + StoredSettings.PROPERTY + " TEXT PRIMARY KEY," + StoredSettings.VALUE + " TEXT)";
    private static final String SQL_DELETE_SETTINGS_TABLE = "DROP TABLE IF EXISTS " + StoredSettings.TABLE_NAME;
    private static final String SQL_CREATE_JOURNAL_TAGS_TABLE = "CREATE TABLE " + StoredJournalTags.TABLE_NAME + " (" + StoredJournalTags.TAG_ID + " INTEGER PRIMARY KEY," + StoredJournalTags.DESCRIPTION + " TEXT)";
    private static final String SQL_DELETE_JOURNAL_TAGS_TABLE = "DROP TABLE IF EXISTS " + StoredJournalTags.TABLE_NAME;
    private static final String SQL_CREATE_JOURNAL_TYPES_TABLE = "CREATE TABLE " + StoredJournalTypes.TABLE_NAME + " (" + StoredJournalTypes.TYPE_ID + " TEXT PRIMARY KEY," + StoredJournalTypes.DESCRIPTION + " TEXT)";
    private static final String SQL_DELETE_JOURNAL_TYPES_TABLE = "DROP TABLE IF EXISTS " + StoredJournalTypes.TABLE_NAME;
    private static final String SQL_CREATE_JOURNAL_AUDIO_LOCATIONS_TABLE = "CREATE TABLE " + StoredJournalAudioLocations.TABLE_NAME + " (" + StoredJournalAudioLocations.AUDIO_ID + " INTEGER PRIMARY KEY," + StoredJournalAudioLocations.AUDIO_PATH + " TEXT, " + StoredJournalAudioLocations.ENTRY_ID + " INTEGER, FOREIGN KEY (" + StoredJournalAudioLocations.ENTRY_ID + ") REFERENCES " + StoredJournalEntries.TABLE_NAME + "(" + StoredJournalEntries.ENTRY_ID + "))";
    private static final String SQL_DELETE_JOURNAL_AUDIO_LOCATIONS_TABLE = "DROP TABLE IF EXISTS " + StoredJournalAudioLocations.TABLE_NAME;
    private static final String SQL_CREATE_JOURNAL_SLEEP_QUALITIES_TABLE = "CREATE TABLE " + StoredJournalSleepQualities.TABLE_NAME + " (" + StoredJournalSleepQualities.QUALITY_ID + " TEXT PRIMARY KEY," + StoredJournalSleepQualities.DESCRIPTION + " TEXT)";
    private static final String SQL_DELETE_JOURNAL_SLEEP_QUALITIES_TABLE = "DROP TABLE IF EXISTS " + StoredJournalSleepQualities.TABLE_NAME;
    private static final String SQL_CREATE_JOURNAL_DREAM_MOODS_TABLE = "CREATE TABLE " + StoredJournalDreamMoods.TABLE_NAME + " (" + StoredJournalDreamMoods.MOOD_ID + " TEXT PRIMARY KEY," + StoredJournalDreamMoods.DESCRIPTION + " TEXT)";
    private static final String SQL_DELETE_JOURNAL_DREAM_MOODS_TABLE = "DROP TABLE IF EXISTS " + StoredJournalDreamMoods.TABLE_NAME;
    private static final String SQL_CREATE_JOURNAL_DREAM_CLARITIES_TABLE = "CREATE TABLE " + StoredJournalDreamClarities.TABLE_NAME + " (" + StoredJournalDreamClarities.CLARITY_ID + " TEXT PRIMARY KEY," + StoredJournalDreamClarities.DESCRIPTION + " TEXT)";
    private static final String SQL_DELETE_JOURNAL_DREAM_CLARITIES_TABLE = "DROP TABLE IF EXISTS " + StoredJournalDreamClarities.TABLE_NAME;
    private static final String SQL_CREATE_JOURNAL_HAS_TAG_TABLE = "CREATE TABLE " + StoredJournalHasTag.TABLE_NAME + " (" + StoredJournalHasTag.ENTRY_ID + " INTEGER," + StoredJournalHasTag.TAG_ID + " INTEGER, PRIMARY KEY (" + StoredJournalHasTag.ENTRY_ID + ", " + StoredJournalHasTag.TAG_ID + "), FOREIGN KEY (" + StoredJournalHasTag.ENTRY_ID + ") REFERENCES " + StoredJournalEntries.TABLE_NAME + "(" + StoredJournalEntries.ENTRY_ID + "), FOREIGN KEY (" + StoredJournalHasTag.TAG_ID + ") REFERENCES " + StoredJournalTags.TABLE_NAME + "(" + StoredJournalTags.TAG_ID + "))";
    private static final String SQL_DELETE_JOURNAL_HAS_TAG_TABLE = "DROP TABLE IF EXISTS " + StoredJournalHasTag.TABLE_NAME;
    private static final String SQL_CREATE_JOURNAL_IS_TYPE_TABLE = "CREATE TABLE " + StoredJournalIsType.TABLE_NAME + " (" + StoredJournalIsType.ENTRY_ID + " INTEGER," + StoredJournalIsType.TYPE_ID + " TEXT, PRIMARY KEY (" + StoredJournalIsType.ENTRY_ID + ", " + StoredJournalIsType.TYPE_ID + "), FOREIGN KEY (" + StoredJournalIsType.ENTRY_ID + ") REFERENCES " + StoredJournalEntries.TABLE_NAME + "(" + StoredJournalEntries.ENTRY_ID + "), FOREIGN KEY (" + StoredJournalIsType.TYPE_ID + ") REFERENCES " + StoredJournalTypes.TABLE_NAME + "(" + StoredJournalTypes.TYPE_ID + "))";
    private static final String SQL_DELETE_JOURNAL_IS_TYPE_TABLE = "DROP TABLE IF EXISTS " + StoredJournalIsType.TABLE_NAME;
    private static final String SQL_CREATE_JOURNAL_ENTRIES_TABLE = "CREATE TABLE " + StoredJournalEntries.TABLE_NAME + " (" + StoredJournalEntries.ENTRY_ID + " INTEGER PRIMARY KEY," + StoredJournalEntries.DATE + " TEXT," + StoredJournalEntries.TIME + " TEXT," + StoredJournalEntries.TITLE + " TEXT," + StoredJournalEntries.DESCRIPTION + " TEXT," + StoredJournalEntries.QUALITY_ID + " TEXT," + StoredJournalEntries.CLARITY_ID + " TEXT," + StoredJournalEntries.MOOD_ID + " TEXT, FOREIGN KEY (" + StoredJournalEntries.QUALITY_ID + ") REFERENCES " + StoredJournalSleepQualities.TABLE_NAME + "(" + StoredJournalSleepQualities.QUALITY_ID + "), FOREIGN KEY (" + StoredJournalEntries.CLARITY_ID + ") REFERENCES " + StoredJournalDreamClarities.TABLE_NAME + "(" + StoredJournalDreamClarities.CLARITY_ID + "), FOREIGN KEY (" + StoredJournalEntries.MOOD_ID + ") REFERENCES " + StoredJournalDreamMoods.TABLE_NAME + "(" + StoredJournalDreamMoods.MOOD_ID + "))";
    private static final String SQL_DELETE_JOURNAL_ENTRIES_TABLE = "DROP TABLE IF EXISTS " + StoredJournalEntries.TABLE_NAME;
    private static final String SQL_QUERY_ASSIGNED_TAGS = "SELECT b." + StoredJournalTags.DESCRIPTION + " FROM " + StoredJournalHasTag.TABLE_NAME + " a INNER JOIN " + StoredJournalTags.TABLE_NAME + " b ON a." + StoredJournalHasTag.TAG_ID + "=b." + StoredJournalTags.TAG_ID + " WHERE a." + StoredJournalHasTag.ENTRY_ID + "=?";
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

    public StoredSettings getProperty(String propertyName) {
        String[] projection = {
                StoredSettings.PROPERTY,
                StoredSettings.VALUE
        };
        String selection = StoredSettings.PROPERTY + " = ?";
        String[] selectionArgs = { propertyName };
        String sortOrder = StoredSettings.PROPERTY + " DESC";
        Cursor cursor = readableDB.query(StoredSettings.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

        StoredSettings property = null;
        while(cursor.moveToNext()) {
            String prop = cursor.getString(cursor.getColumnIndexOrThrow(StoredSettings.PROPERTY));
            String value = cursor.getString(cursor.getColumnIndexOrThrow(StoredSettings.VALUE));
            property = new StoredSettings(prop, value);
        }
        cursor.close();
        return property;
    }

    public DreamJournalEntriesList getJournalEntries() {
        DreamJournalEntriesList entries = new DreamJournalEntriesList();
        String[] projection = {
                StoredJournalEntries.ENTRY_ID,
                StoredJournalEntries.DATE,
                StoredJournalEntries.TIME,
                StoredJournalEntries.TITLE,
                StoredJournalEntries.DESCRIPTION,
                StoredJournalEntries.QUALITY_ID,
                StoredJournalEntries.CLARITY_ID,
                StoredJournalEntries.MOOD_ID
        };
        String selection = "";
        String[] selectionArgs = { };
        String sortOrder = StoredJournalEntries.ENTRY_ID + " DESC";
        Cursor cursor = readableDB.query(StoredJournalEntries.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

        while(cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(StoredJournalEntries.ENTRY_ID));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalEntries.DATE));
            String time = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalEntries.TIME));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalEntries.TITLE));
            String desc = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalEntries.DESCRIPTION));
            String qual = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalEntries.QUALITY_ID));
            String clar = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalEntries.CLARITY_ID));
            String mood = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalEntries.MOOD_ID));
            entries.add(new StoredJournalEntries(id, date, time, title, desc, qual, clar, mood), getAssignedTags(id), getAssignedAudioLocations(id), getAssignedTypes(id));
        }
        cursor.close();
        return entries;
    }

    private String[] getAssignedTypes(int entry_id) {
        String[] projection = {
                StoredJournalIsType.TYPE_ID
        };
        String selection = StoredJournalIsType.ENTRY_ID + " = ?";
        String[] selectionArgs = { Integer.toString(entry_id) };
        String sortOrder = StoredJournalIsType.TYPE_ID + " DESC";
        Cursor cursor = readableDB.query(StoredJournalIsType.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

        String[] types = new String[cursor.getCount()];
        int i = 0;
        while(cursor.moveToNext()) {
            types[i] = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalIsType.TYPE_ID));
            i++;
        }
        cursor.close();
        return types;
    }

    private String[] getAssignedAudioLocations(int entry_id) {
        String[] projection = {
                StoredJournalAudioLocations.AUDIO_PATH
        };
        String selection = StoredJournalAudioLocations.ENTRY_ID + " = ?";
        String[] selectionArgs = { Integer.toString(entry_id) };
        String sortOrder = StoredJournalAudioLocations.AUDIO_PATH + " DESC";
        Cursor cursor = readableDB.query(StoredJournalAudioLocations.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

        String[] audioPaths = new String[cursor.getCount()];
        int i = 0;
        while(cursor.moveToNext()) {
            audioPaths[i] = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalAudioLocations.AUDIO_PATH));
            i++;
        }
        cursor.close();
        return audioPaths;
    }

    private String[] getAssignedTags(int entry_id) {
        Cursor cursor = readableDB.rawQuery(SQL_QUERY_ASSIGNED_TAGS, new String[] { Integer.toString(entry_id) });

        String[] tags = new String[cursor.getCount()];
        int i = 0;
        while(cursor.moveToNext()) {
            tags[i] = cursor.getString(cursor.getColumnIndexOrThrow(StoredJournalTags.DESCRIPTION));
            i++;
        }
        cursor.close();
        return tags;
    }

    public void setProperty(String propertyName, String propertyValue) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(StoredSettings.PROPERTY, propertyName);
        initialValues.put(StoredSettings.VALUE, propertyValue);

        int id = (int) writableDB.insertWithOnConflict(StoredSettings.TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            writableDB.update(StoredSettings.TABLE_NAME, initialValues, StoredSettings.PROPERTY + "=?", new String[] { propertyName });
        }
    }

    public int addJournalEntry(int entry_id, String date, String time, String title, String description, String quality, String clarity, String mood) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(StoredJournalEntries.DATE, date);
        initialValues.put(StoredJournalEntries.TIME, time);
        initialValues.put(StoredJournalEntries.TITLE, title);
        initialValues.put(StoredJournalEntries.DESCRIPTION, description);
        initialValues.put(StoredJournalEntries.QUALITY_ID, quality);
        initialValues.put(StoredJournalEntries.CLARITY_ID, clarity);
        initialValues.put(StoredJournalEntries.MOOD_ID, mood);

        int id = entry_id;
        if(entry_id == -1) {
            id = (int) writableDB.insertWithOnConflict(StoredJournalEntries.TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);
        }
        else {
            writableDB.update(StoredJournalEntries.TABLE_NAME, initialValues, StoredJournalEntries.ENTRY_ID + "=?", new String[] { Integer.toString(entry_id) });
        }
        return id;
    }

    public void clearRelationsForEntry(int entry_id) {
        writableDB.delete(StoredJournalHasTag.TABLE_NAME, StoredJournalHasTag.ENTRY_ID + "=?", new String[] { Integer.toString(entry_id) });
        writableDB.delete(StoredJournalIsType.TABLE_NAME, StoredJournalIsType.ENTRY_ID + "=?", new String[] { Integer.toString(entry_id) });
        writableDB.delete(StoredJournalAudioLocations.TABLE_NAME, StoredJournalAudioLocations.ENTRY_ID + "=?", new String[] { Integer.toString(entry_id) });
    }

    public void deleteEntry(int entry_id) {
        clearRelationsForEntry(entry_id);
        writableDB.delete(StoredJournalEntries.TABLE_NAME, StoredJournalEntries.ENTRY_ID + "=?", new String[] { Integer.toString(entry_id) });
    }

    public void addDreamTypeToEntry(int entry_id, String dreamType_id) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(StoredJournalIsType.ENTRY_ID, entry_id);
        initialValues.put(StoredJournalIsType.TYPE_ID, dreamType_id);
        writableDB.insertWithOnConflict(StoredJournalIsType.TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void addDreamAudioRecording(int entry_id, String recording_path) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(StoredJournalAudioLocations.ENTRY_ID, entry_id);
        initialValues.put(StoredJournalAudioLocations.AUDIO_PATH, recording_path);
        writableDB.insertWithOnConflict(StoredJournalAudioLocations.TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void addDreamTagToEntry(int entry_id, String tag) {
        String[] projection = {
                StoredJournalTags.TAG_ID,
                StoredJournalTags.DESCRIPTION
        };
        String selection = StoredJournalTags.DESCRIPTION + " = ?";
        String[] selectionArgs = { tag };
        String sortOrder = StoredJournalTags.TAG_ID + " ASC";
        Cursor cursor = readableDB.query(StoredJournalTags.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

        int tag_id = -1;
        while(cursor.moveToNext()) {
            tag_id = cursor.getInt(cursor.getColumnIndexOrThrow(StoredJournalTags.TAG_ID));
        }
        cursor.close();

        if(tag_id == -1){
            ContentValues initialValues = new ContentValues();
            initialValues.put(StoredJournalTags.DESCRIPTION, tag);
            tag_id = (int)writableDB.insertWithOnConflict(StoredJournalTags.TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);
        }

        ContentValues initialValues = new ContentValues();
        initialValues.put(StoredJournalHasTag.ENTRY_ID, entry_id);
        initialValues.put(StoredJournalHasTag.TAG_ID, tag_id);
        writableDB.insertWithOnConflict(StoredJournalHasTag.TABLE_NAME, null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SETTINGS_TABLE);
        db.execSQL(SQL_CREATE_JOURNAL_TAGS_TABLE);
        db.execSQL(SQL_CREATE_JOURNAL_TYPES_TABLE);
        db.execSQL(SQL_CREATE_JOURNAL_AUDIO_LOCATIONS_TABLE);
        db.execSQL(SQL_CREATE_JOURNAL_SLEEP_QUALITIES_TABLE);
        db.execSQL(SQL_CREATE_JOURNAL_DREAM_MOODS_TABLE);
        db.execSQL(SQL_CREATE_JOURNAL_DREAM_CLARITIES_TABLE);
        db.execSQL(SQL_CREATE_JOURNAL_ENTRIES_TABLE);
        db.execSQL(SQL_CREATE_JOURNAL_HAS_TAG_TABLE);
        db.execSQL(SQL_CREATE_JOURNAL_IS_TYPE_TABLE);

        db.execSQL("INSERT INTO " + StoredJournalTypes.TABLE_NAME + "(" + StoredJournalTypes.TYPE_ID + ", " + StoredJournalTypes.DESCRIPTION + ") VALUES ('" + StoredJournalTypes.DATA_ROW1[0] + "','" + StoredJournalTypes.DATA_ROW1[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalTypes.TABLE_NAME + "(" + StoredJournalTypes.TYPE_ID + ", " + StoredJournalTypes.DESCRIPTION + ") VALUES ('" + StoredJournalTypes.DATA_ROW2[0] + "','" + StoredJournalTypes.DATA_ROW2[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalTypes.TABLE_NAME + "(" + StoredJournalTypes.TYPE_ID + ", " + StoredJournalTypes.DESCRIPTION + ") VALUES ('" + StoredJournalTypes.DATA_ROW3[0] + "','" + StoredJournalTypes.DATA_ROW3[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalTypes.TABLE_NAME + "(" + StoredJournalTypes.TYPE_ID + ", " + StoredJournalTypes.DESCRIPTION + ") VALUES ('" + StoredJournalTypes.DATA_ROW4[0] + "','" + StoredJournalTypes.DATA_ROW4[1] + "')");

        db.execSQL("INSERT INTO " + StoredJournalDreamMoods.TABLE_NAME + "(" + StoredJournalDreamMoods.MOOD_ID + ", " + StoredJournalDreamMoods.DESCRIPTION + ") VALUES ('" + StoredJournalDreamMoods.DATA_ROW1[0] + "','" + StoredJournalDreamMoods.DATA_ROW1[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalDreamMoods.TABLE_NAME + "(" + StoredJournalDreamMoods.MOOD_ID + ", " + StoredJournalDreamMoods.DESCRIPTION + ") VALUES ('" + StoredJournalDreamMoods.DATA_ROW2[0] + "','" + StoredJournalDreamMoods.DATA_ROW2[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalDreamMoods.TABLE_NAME + "(" + StoredJournalDreamMoods.MOOD_ID + ", " + StoredJournalDreamMoods.DESCRIPTION + ") VALUES ('" + StoredJournalDreamMoods.DATA_ROW3[0] + "','" + StoredJournalDreamMoods.DATA_ROW3[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalDreamMoods.TABLE_NAME + "(" + StoredJournalDreamMoods.MOOD_ID + ", " + StoredJournalDreamMoods.DESCRIPTION + ") VALUES ('" + StoredJournalDreamMoods.DATA_ROW4[0] + "','" + StoredJournalDreamMoods.DATA_ROW4[1] + "')");

        db.execSQL("INSERT INTO " + StoredJournalDreamClarities.TABLE_NAME + "(" + StoredJournalDreamClarities.CLARITY_ID + ", " + StoredJournalDreamClarities.DESCRIPTION + ") VALUES ('" + StoredJournalDreamClarities.DATA_ROW1[0] + "','" + StoredJournalDreamClarities.DATA_ROW1[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalDreamClarities.TABLE_NAME + "(" + StoredJournalDreamClarities.CLARITY_ID + ", " + StoredJournalDreamClarities.DESCRIPTION + ") VALUES ('" + StoredJournalDreamClarities.DATA_ROW2[0] + "','" + StoredJournalDreamClarities.DATA_ROW2[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalDreamClarities.TABLE_NAME + "(" + StoredJournalDreamClarities.CLARITY_ID + ", " + StoredJournalDreamClarities.DESCRIPTION + ") VALUES ('" + StoredJournalDreamClarities.DATA_ROW3[0] + "','" + StoredJournalDreamClarities.DATA_ROW3[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalDreamClarities.TABLE_NAME + "(" + StoredJournalDreamClarities.CLARITY_ID + ", " + StoredJournalDreamClarities.DESCRIPTION + ") VALUES ('" + StoredJournalDreamClarities.DATA_ROW4[0] + "','" + StoredJournalDreamClarities.DATA_ROW4[1] + "')");

        db.execSQL("INSERT INTO " + StoredJournalSleepQualities.TABLE_NAME + "(" + StoredJournalSleepQualities.QUALITY_ID + ", " + StoredJournalSleepQualities.DESCRIPTION + ") VALUES ('" + StoredJournalSleepQualities.DATA_ROW1[0] + "','" + StoredJournalSleepQualities.DATA_ROW1[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalSleepQualities.TABLE_NAME + "(" + StoredJournalSleepQualities.QUALITY_ID + ", " + StoredJournalSleepQualities.DESCRIPTION + ") VALUES ('" + StoredJournalSleepQualities.DATA_ROW2[0] + "','" + StoredJournalSleepQualities.DATA_ROW2[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalSleepQualities.TABLE_NAME + "(" + StoredJournalSleepQualities.QUALITY_ID + ", " + StoredJournalSleepQualities.DESCRIPTION + ") VALUES ('" + StoredJournalSleepQualities.DATA_ROW3[0] + "','" + StoredJournalSleepQualities.DATA_ROW3[1] + "')");
        db.execSQL("INSERT INTO " + StoredJournalSleepQualities.TABLE_NAME + "(" + StoredJournalSleepQualities.QUALITY_ID + ", " + StoredJournalSleepQualities.DESCRIPTION + ") VALUES ('" + StoredJournalSleepQualities.DATA_ROW4[0] + "','" + StoredJournalSleepQualities.DATA_ROW4[1] + "')");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: convert data after database version upgrade and not just delete all data
        // TODO store user data as well!
        db.execSQL(SQL_DELETE_SETTINGS_TABLE);
        db.execSQL(SQL_DELETE_JOURNAL_HAS_TAG_TABLE);
        db.execSQL(SQL_DELETE_JOURNAL_IS_TYPE_TABLE);
        db.execSQL(SQL_DELETE_JOURNAL_ENTRIES_TABLE);
        db.execSQL(SQL_DELETE_JOURNAL_TAGS_TABLE);
        db.execSQL(SQL_DELETE_JOURNAL_TYPES_TABLE);
        db.execSQL(SQL_DELETE_JOURNAL_AUDIO_LOCATIONS_TABLE);
        db.execSQL(SQL_DELETE_JOURNAL_SLEEP_QUALITIES_TABLE);
        db.execSQL(SQL_DELETE_JOURNAL_DREAM_MOODS_TABLE);
        db.execSQL(SQL_DELETE_JOURNAL_DREAM_CLARITIES_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
