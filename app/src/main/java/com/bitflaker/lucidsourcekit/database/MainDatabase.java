package com.bitflaker.lucidsourcekit.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.bitflaker.lucidsourcekit.database.alarms.daos.AlarmDao;
import com.bitflaker.lucidsourcekit.database.alarms.daos.AlarmIsOnWeekdayDao;
import com.bitflaker.lucidsourcekit.database.alarms.daos.AlarmToneTypesDao;
import com.bitflaker.lucidsourcekit.database.alarms.daos.WeekdaysDao;
import com.bitflaker.lucidsourcekit.database.alarms.entities.Alarm;
import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmIsOnWeekday;
import com.bitflaker.lucidsourcekit.database.alarms.entities.AlarmToneTypes;
import com.bitflaker.lucidsourcekit.database.alarms.entities.Weekdays;
import com.bitflaker.lucidsourcekit.database.alarms.updated.daos.ActiveAlarmDao;
import com.bitflaker.lucidsourcekit.database.alarms.updated.daos.StoredAlarmDao;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.ActiveAlarm;
import com.bitflaker.lucidsourcekit.database.alarms.updated.entities.StoredAlarm;
import com.bitflaker.lucidsourcekit.database.dreamjournal.daos.AudioLocationDao;
import com.bitflaker.lucidsourcekit.database.dreamjournal.daos.DreamClarityDao;
import com.bitflaker.lucidsourcekit.database.dreamjournal.daos.DreamMoodDao;
import com.bitflaker.lucidsourcekit.database.dreamjournal.daos.DreamTypeDao;
import com.bitflaker.lucidsourcekit.database.dreamjournal.daos.JournalEntryDao;
import com.bitflaker.lucidsourcekit.database.dreamjournal.daos.JournalEntryHasTagDao;
import com.bitflaker.lucidsourcekit.database.dreamjournal.daos.JournalEntryIsTypeDao;
import com.bitflaker.lucidsourcekit.database.dreamjournal.daos.JournalEntryTagDao;
import com.bitflaker.lucidsourcekit.database.dreamjournal.daos.SleepQualityDao;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamClarity;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.DreamType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.JournalEntryTag;
import com.bitflaker.lucidsourcekit.database.dreamjournal.entities.SleepQuality;
import com.bitflaker.lucidsourcekit.database.goals.daos.GoalDao;
import com.bitflaker.lucidsourcekit.database.goals.daos.ShuffleDao;
import com.bitflaker.lucidsourcekit.database.goals.daos.ShuffleHasGoalDao;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;
import com.bitflaker.lucidsourcekit.database.notifications.daos.NotificationCategoryDao;
import com.bitflaker.lucidsourcekit.database.notifications.daos.NotificationMessageDao;
import com.bitflaker.lucidsourcekit.database.notifications.daos.NotificationObfuscationDao;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationObfuscations;
import com.bitflaker.lucidsourcekit.general.Zipper;
import com.bitflaker.lucidsourcekit.general.datastore.DataStoreManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Database(entities = {JournalEntryTag.class, DreamType.class, SleepQuality.class,
        DreamMood.class, DreamClarity.class, AudioLocation.class, JournalEntry.class,
        JournalEntryHasTag.class, JournalEntryHasType.class, Goal.class, Shuffle.class,
        ShuffleHasGoal.class, Alarm.class, AlarmIsOnWeekday.class, AlarmToneTypes.class,
        Weekdays.class, ActiveAlarm.class, StoredAlarm.class, NotificationObfuscations.class,
        NotificationMessage.class, NotificationCategory.class}, version = 14, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class MainDatabase extends RoomDatabase {

    private final static String MAIN_DATABASE_NAME = "journalDatabase.db";

    // Dream Journal tables
    public abstract JournalEntryTagDao getJournalEntryTagDao();
    public abstract DreamTypeDao getDreamTypeDao();
    public abstract SleepQualityDao getSleepQualityDao();
    public abstract DreamMoodDao getDreamMoodDao();
    public abstract DreamClarityDao getDreamClarityDao();
    public abstract AudioLocationDao getAudioLocationDao();
    public abstract JournalEntryDao getJournalEntryDao();
    public abstract JournalEntryHasTagDao getJournalEntryHasTagDao();
    public abstract JournalEntryIsTypeDao getJournalEntryIsTypeDao();

    // Goal tables
    public abstract GoalDao getGoalDao();
    public abstract ShuffleDao getShuffleDao();
    public abstract ShuffleHasGoalDao getShuffleHasGoalDao();

    // Alarm tables
    public abstract AlarmDao getAlarmDao();
    public abstract AlarmIsOnWeekdayDao getAlarmIsOnWeekdayDao();
    public abstract AlarmToneTypesDao getAlarmToneTypesDao();
    public abstract WeekdaysDao getWeekdaysDao();

    // New Alarm tables
    public abstract ActiveAlarmDao getActiveAlarmDao();
    public abstract StoredAlarmDao getStoredAlarmDao();

    // Notification tables
    public abstract NotificationObfuscationDao getNotificationObfuscationDao();
    public abstract NotificationMessageDao getNotificationMessageDao();
    public abstract NotificationCategoryDao getNotificationCategoryDao();

    // Database
    private static volatile MainDatabase instance;

    public static synchronized MainDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
            populateStaticTables(instance);
            instance.getNotificationObfuscationDao().insertAll(NotificationObfuscations.populateData());
            instance.getNotificationCategoryDao().insertAll(NotificationCategory.populateData());
        }
        return instance;
    }

    public MainDatabase() {

    }

    private static MainDatabase create(final Context context) {
        return Room.databaseBuilder(context, MainDatabase.class, MAIN_DATABASE_NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigrationFrom(4)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .addMigrations(MIGRATION_8_9)
                .addMigrations(MIGRATION_9_10)
                .addMigrations(MIGRATION_10_11)
                .addMigrations(MIGRATION_11_12)
                .addMigrations(MIGRATION_12_13)
                .addMigrations(MIGRATION_13_14)
                .build();
    }

    private static void populateStaticTables(MainDatabase instance) {
        SleepQuality[] sleepQualities = SleepQuality.populateData();
        DreamMood[] dreamMoods = DreamMood.populateData();
        DreamClarity[] dreamClarities = DreamClarity.populateData();
        DreamType[] dreamTypes = DreamType.populateData();

        instance.getSleepQualityDao().insertAll(sleepQualities);
        instance.getDreamMoodDao().insertAll(dreamMoods);
        instance.getDreamClarityDao().insertAll(dreamClarities);
        instance.getDreamTypeDao().insertAll(dreamTypes);
    }

    public boolean backupDatabase(Context context, Uri fileUri) {
        if (instance == null) return false;

        List<String> backupFiles = new ArrayList<>();

        File dbFile = context.getDatabasePath(MAIN_DATABASE_NAME);
        File dbWalFile = new File(dbFile.getPath() + "-wal");
        File dbShmFile = new File(dbFile.getPath() + "-shm");
        File preferenceExport = exportSharedPreferences(context);
        File dataStoreExport = new File(context.getFilesDir(), "datastore/" + DataStoreManager.DATA_STORE_FILE_NAME + ".preferences_pb");

        backupFiles.add(dbFile.getAbsolutePath());
        if(dbWalFile.exists()) { backupFiles.add(dbWalFile.getAbsolutePath()); }
        if(dbShmFile.exists()) { backupFiles.add(dbShmFile.getAbsolutePath()); }
        if(preferenceExport.exists()) { backupFiles.add(preferenceExport.getAbsolutePath()); }
        if(dataStoreExport.exists()) { backupFiles.add(dataStoreExport.getAbsolutePath()); }

        try {
            Zipper.createZipFile(backupFiles.toArray(new String[0]), context.getContentResolver().openOutputStream(fileUri));
            if(preferenceExport.exists() && !preferenceExport.delete()) {
                Log.e("MainDatabase_Backup", "Unable to delete preferenceExport file after backup!");
            }
            return true;
        } catch (IOException e) {
            Log.e("MainDatabase_Backup", "Data backup failed", e);
        }
        return false;
    }

    @NonNull
    private static File exportSharedPreferences(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> allPreferences = preferences.getAll();
        JSONObject preferencesJson = new JSONObject(allPreferences);
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("preferences.json", Context.MODE_PRIVATE))) {
            outputStreamWriter.write(preferencesJson.toString());
        } catch (Exception e) {
            Log.e("MAIN_DATABASE_BACKUP", "Preferences export failed", e);
        }
        String preferenceExportLocation = context.getFilesDir().getAbsolutePath() + File.separator + "preferences.json";
        return new File(preferenceExportLocation);
    }

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE Goal (" +
                    "goalId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "description TEXT NOT NULL," +
                    "difficulty REAL NOT NULL," +
                    "difficultyLocked INTEGER NOT NULL DEFAULT 0);");
            database.execSQL("CREATE TABLE Shuffle (" +
                    "shuffleId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "dayStartTimestamp INTEGER NOT NULL," +
                    "dayEndTimestamp INTEGER NOT NULL);");
            database.execSQL("CREATE TABLE ShuffleHasGoal (" +
                    "shuffleId INTEGER NOT NULL," +
                    "goalId INTEGER NOT NULL," +
                    "achieved INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY(shuffleId, goalId)," +
                    "FOREIGN KEY (shuffleId)" +
                        "REFERENCES Shuffle (shuffleId)" +
                            "ON DELETE CASCADE " +
                            "ON UPDATE NO ACTION," +
                    "FOREIGN KEY (goalId)" +
                        "REFERENCES Goal (goalId)" +
                            "ON DELETE CASCADE " +
                            "ON UPDATE NO ACTION" +
                    ");");
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM Shuffle;");
            database.execSQL("DELETE FROM ShuffleHasGoal;");
            database.execSQL("CREATE INDEX index_ShuffleHasGoal_goalId ON ShuffleHasGoal (goalId);");
        }
    };

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE AudioLocation ADD recordingTimestamp INTEGER DEFAULT 0 NOT NULL");
        }
    };

    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

            database.execSQL("CREATE TABLE Weekdays (" +
                    "weekdayId INTEGER PRIMARY KEY NOT NULL," +
                    "description TEXT);");
            database.execSQL("CREATE TABLE AlarmToneTypes (" +
                    "alarmToneTypeId INTEGER PRIMARY KEY NOT NULL," +
                    "description TEXT);");
            database.execSQL("CREATE TABLE Alarm (" +
                    "alarmId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "bedtimeHour INTEGER NOT NULL," +
                    "bedtimeMinute INTEGER NOT NULL," +
                    "alarmHour INTEGER NOT NULL," +
                    "alarmMinute INTEGER NOT NULL," +
                    "alarmToneType INTEGER NOT NULL," +
                    "alarmUri TEXT NOT NULL," +
                    "alarmVolume INTEGER NOT NULL," +
                    "alarmVolumeIncreaseMinutes INTEGER NOT NULL," +
                    "alarmVolumeIncreaseSeconds INTEGER NOT NULL," +
                    "vibrate INTEGER NOT NULL," +
                    "useFlashlight INTEGER NOT NULL," +
                    "isActive INTEGER NOT NULL," +
                    "FOREIGN KEY (alarmToneType)" +
                    "REFERENCES AlarmToneTypes (alarmToneTypeId)" +
                    "ON DELETE CASCADE " +
                    "ON UPDATE NO ACTION);");
            database.execSQL("CREATE TABLE AlarmIsOnWeekday (" +
                    "alarmId INTEGER NOT NULL," +
                    "weekdayId INTEGER NOT NULL," +
                    "PRIMARY KEY (alarmId, weekdayId)," +
                    "FOREIGN KEY (alarmId) " +
                    "REFERENCES Alarm (alarmId) " +
                    "ON DELETE CASCADE " +
                    "ON UPDATE NO ACTION," +
                    "FOREIGN KEY (weekdayId) " +
                    "REFERENCES Weekdays (weekdayId) " +
                    "ON DELETE CASCADE " +
                    "ON UPDATE NO ACTION);");

            database.execSQL("CREATE INDEX index_Alarm_alarmToneType ON Alarm (alarmToneType);");
            database.execSQL("CREATE INDEX index_AlarmIsOnWeekday_weekdayId ON AlarmIsOnWeekday (weekdayId);");
        }
    };

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) { }
    };

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Alarm ADD title TEXT DEFAULT 'Unnamed Alarm' NOT NULL;");
        }
    };

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE ActiveAlarm (" +
                    "requestCode INTEGER NOT NULL, " +
                    "initialTime INTEGER NOT NULL, " +
                    "interval INTEGER NOT NULL, " +
                    "patternIndex INTEGER NOT NULL, " +
                    "pattern TEXT NOT NULL, " +
                    "PRIMARY KEY(requestCode));");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE ActiveAlarm");
            database.execSQL("CREATE TABLE ActiveAlarm (" +
                    "requestCode INTEGER NOT NULL, " +
                    "initialTime INTEGER NOT NULL, " +
                    "interval INTEGER NOT NULL, " +
                    "patternIndex INTEGER NOT NULL, " +
                    "PRIMARY KEY(requestCode));");
            database.execSQL("CREATE TABLE StoredAlarm (" +
                    "alarmId INTEGER NOT NULL, " +
                    "title TEXT NOT NULL DEFAULT('Unnamed Alarm'), " +
                    "bedtimeTimestamp INTEGER NOT NULL, " +
                    "alarmTimestamp INTEGER NOT NULL, " +
                    "pattern TEXT NOT NULL, " +
                    "alarmToneTypeId INTEGER NOT NULL, " +
                    "alarmUri TEXT NOT NULL, " +
                    "alarmVolume REAL NOT NULL, " +
                    "alarmVolumeIncreaseTimestamp INTEGER NOT NULL, " +
                    "isVibrationActive INTEGER NOT NULL, " +
                    "isFlashlightActive INTEGER NOT NULL, " +
                    "isAlarmActive INTEGER NOT NULL, " +
                    "requestCodeActiveAlarm INTEGER NOT NULL DEFAULT(-1), " +
                    "PRIMARY KEY(alarmId)," +
                    "FOREIGN KEY (alarmToneTypeId) REFERENCES AlarmToneTypes (alarmToneTypeId) ON DELETE CASCADE ON UPDATE NO ACTION, " +
                    "FOREIGN KEY (requestCodeActiveAlarm) REFERENCES ActiveAlarm (requestCode) ON DELETE SET DEFAULT ON UPDATE NO ACTION)");
            database.execSQL("CREATE INDEX index_StoredAlarm_alarmToneTypeId ON StoredAlarm (alarmToneTypeId);");
            database.execSQL("CREATE INDEX index_StoredAlarm_requestCodeActiveAlarm ON StoredAlarm (requestCodeActiveAlarm);");
        }
    };

    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE NotificationObfuscations (" +
                    "obfuscationTypeId INTEGER PRIMARY KEY NOT NULL," +
                    "description TEXT NOT NULL);");
            database.execSQL("CREATE TABLE NotificationMessage (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "notificationCategoryId TEXT NOT NULL," +
                    "message TEXT NOT NULL," +
                    "obfuscationTypeId INTEGER NOT NULL," +
                    "weight INTEGER NOT NULL," +
                    "FOREIGN KEY (obfuscationTypeId)" +
                    "REFERENCES NotificationObfuscations (obfuscationTypeId)" +
                    "ON DELETE CASCADE " +
                    "ON UPDATE NO ACTION," +
                    "FOREIGN KEY (notificationCategoryId)" +
                    "REFERENCES NotificationCategory (id)" +
                    "ON DELETE CASCADE " +
                    "ON UPDATE NO ACTION);");
            database.execSQL("CREATE TABLE NotificationCategory (" +
                    "id TEXT PRIMARY KEY NOT NULL," +
                    "description TEXT NOT NULL," +
                    "timeFrom INTEGER NOT NULL," +
                    "timeTo INTEGER NOT NULL," +
                    "obfuscationTypeId INTEGER NOT NULL," +
                    "dailyNotificationCount INTEGER NOT NULL," +
                    "isPermanent INTEGER NOT NULL," +
                    "isEnabled INTEGER NOT NULL," +
                    "FOREIGN KEY (obfuscationTypeId)" +
                    "REFERENCES NotificationObfuscations (obfuscationTypeId)" +
                    "ON DELETE CASCADE " +
                    "ON UPDATE NO ACTION);");

            database.execSQL("CREATE INDEX index_NotificationCategory_obfuscationTypeId ON NotificationCategory (obfuscationTypeId);");
            database.execSQL("CREATE INDEX index_NotificationMessage_notificationCategoryId ON NotificationMessage (notificationCategoryId);");
            database.execSQL("CREATE INDEX index_NotificationMessage_obfuscationTypeId ON NotificationMessage (obfuscationTypeId);");
        }
    };
}
