package com.bitflaker.lucidsourcekit.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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

@Database(entities = {JournalEntryTag.class, DreamType.class, SleepQuality.class,
        DreamMood.class, DreamClarity.class, AudioLocation.class, JournalEntry.class,
        JournalEntryHasTag.class, JournalEntryHasType.class, Goal.class, Shuffle.class,
        ShuffleHasGoal.class}, version = 7, exportSchema = false)
public abstract class MainDatabase extends RoomDatabase {
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

    // Database
    private static volatile MainDatabase instance;

    public static synchronized MainDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
            populateStaticTables(instance);
        }
        return instance;
    }

    public MainDatabase() {

    }

    private static MainDatabase create(final Context context) {
        // .allowMainThreadQueries()
        return Room.databaseBuilder(context, MainDatabase.class, "journalDatabase.db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigrationFrom(4)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
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
}
