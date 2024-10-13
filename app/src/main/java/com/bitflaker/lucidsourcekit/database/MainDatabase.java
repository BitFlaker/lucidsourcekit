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

import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;
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
import com.bitflaker.lucidsourcekit.database.goals.daos.ShuffleTransactionDao;
import com.bitflaker.lucidsourcekit.database.goals.entities.Goal;
import com.bitflaker.lucidsourcekit.database.goals.entities.Shuffle;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleHasGoal;
import com.bitflaker.lucidsourcekit.database.goals.entities.ShuffleTransaction;
import com.bitflaker.lucidsourcekit.database.notifications.daos.NotificationCategoryDao;
import com.bitflaker.lucidsourcekit.database.notifications.daos.NotificationMessageDao;
import com.bitflaker.lucidsourcekit.database.notifications.daos.NotificationObfuscationDao;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationObfuscations;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.bitflaker.lucidsourcekit.utils.Zipper;

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
        NotificationMessage.class, NotificationCategory.class, ShuffleTransaction.class}, version = 15, exportSchema = false)
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
    public abstract ShuffleTransactionDao getShuffleTransactionDao();

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
                .addMigrations(MainDatabaseMigrations.MIGRATION_5_6)
                .addMigrations(MainDatabaseMigrations.MIGRATION_6_7)
                .addMigrations(MainDatabaseMigrations.MIGRATION_7_8)
                .addMigrations(MainDatabaseMigrations.MIGRATION_8_9)
                .addMigrations(MainDatabaseMigrations.MIGRATION_9_10)
                .addMigrations(MainDatabaseMigrations.MIGRATION_10_11)
                .addMigrations(MainDatabaseMigrations.MIGRATION_11_12)
                .addMigrations(MainDatabaseMigrations.MIGRATION_12_13)
                .addMigrations(MainDatabaseMigrations.MIGRATION_13_14)
                .addMigrations(MainDatabaseMigrations.MIGRATION_14_15)
                .build();
    }

    private static void populateStaticTables(MainDatabase instance) {
        SleepQuality[] sleepQualities = SleepQuality.defaultData;
        DreamMood[] dreamMoods = DreamMood.defaultData;
        DreamClarity[] dreamClarities = DreamClarity.defaultData;
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
        File dataStoreExport = new File(context.getFilesDir(), "datastore" + File.separator + DataStoreManager.DATA_STORE_FILE_NAME + ".preferences_pb");
        File recordingsExport = new File(context.getFilesDir(), "Recordings");

        backupFiles.add(dbFile.getAbsolutePath());
        if(dbWalFile.exists()) { backupFiles.add(dbWalFile.getAbsolutePath()); }
        if(dbShmFile.exists()) { backupFiles.add(dbShmFile.getAbsolutePath()); }
        if(preferenceExport.exists()) { backupFiles.add(preferenceExport.getAbsolutePath()); }
        if(dataStoreExport.exists()) { backupFiles.add(dataStoreExport.getAbsolutePath()); }
        if(recordingsExport.exists()) { backupFiles.add(recordingsExport.getAbsolutePath()); }

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

    public boolean restoreDatabase(Context context, Uri backupFileUri) {
        if (instance == null) return false;

        File origDbFile = context.getDatabasePath(MAIN_DATABASE_NAME);
        File origDbWalFile = new File(origDbFile.getPath() + "-wal");
        File origDbShmFile = new File(origDbFile.getPath() + "-shm");
        File origDataStoreExport = new File(context.getFilesDir(), "datastore" + File.separator + DataStoreManager.DATA_STORE_FILE_NAME + ".preferences_pb");
        File origRecordingsExport = new File(context.getFilesDir(), "Recordings");

        try {
            File baseTempBackupLocation = new File(context.getFilesDir(), "temp_backup_restore");
            if(Zipper.unzipFile(context.getContentResolver().openInputStream(backupFileUri), baseTempBackupLocation.getPath())) {
                File bckDbFile = new File(baseTempBackupLocation.getPath() + File.separator + MAIN_DATABASE_NAME);
                File bckDbWalFile = new File(bckDbFile.getPath() + "-wal");
                File bckDbShmFile = new File(bckDbFile.getPath() + "-shm");
                File bckDataStoreExport = new File(baseTempBackupLocation.getPath() + File.separator + DataStoreManager.DATA_STORE_FILE_NAME + ".preferences_pb");
                File bckRecordingsExport = new File(baseTempBackupLocation.getPath() + File.separator + "Recordings");

                try {
                    if(bckDbFile.exists()) { Tools.copyFile(bckDbFile, origDbFile); }
                    if(bckDbWalFile.exists()) { Tools.copyFile(bckDbWalFile, origDbWalFile); }
                    if(bckDbShmFile.exists()) { Tools.copyFile(bckDbShmFile, origDbShmFile); }
                    if(bckDataStoreExport.exists()) { Tools.copyFile(bckDataStoreExport, origDataStoreExport); }
                    if(bckRecordingsExport.exists()) { Tools.copyDir(bckRecordingsExport, origRecordingsExport); }
                }
                catch (Exception e) {
                    Log.e("MainDatabase_Backup_Restore", "Unable to restore at least one file from backup", e);
                    return false;
                }

                if(!Tools.deleteFile(baseTempBackupLocation)) {
                    Log.w("MainDatabase_Backup_Restore", "Unable to remove temp backup restore directory");
                }

                return true;
            }
        } catch (Exception e) {
            Log.e("MainDatabase_Backup_Restore", "Data restore from backup failed", e);
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
}
