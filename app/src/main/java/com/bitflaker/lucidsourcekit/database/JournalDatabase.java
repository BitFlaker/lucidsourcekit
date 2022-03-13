package com.bitflaker.lucidsourcekit.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.bitflaker.lucidsourcekit.database.daos.AudioLocationDao;
import com.bitflaker.lucidsourcekit.database.daos.DreamClarityDao;
import com.bitflaker.lucidsourcekit.database.daos.DreamMoodDao;
import com.bitflaker.lucidsourcekit.database.daos.JournalEntryDao;
import com.bitflaker.lucidsourcekit.database.daos.JournalEntryHasTagDao;
import com.bitflaker.lucidsourcekit.database.daos.JournalEntryIsTypeDao;
import com.bitflaker.lucidsourcekit.database.daos.JournalEntryTagDao;
import com.bitflaker.lucidsourcekit.database.daos.DreamTypeDao;
import com.bitflaker.lucidsourcekit.database.daos.SleepQualityDao;
import com.bitflaker.lucidsourcekit.database.entities.AudioLocation;
import com.bitflaker.lucidsourcekit.database.entities.DreamClarity;
import com.bitflaker.lucidsourcekit.database.entities.DreamMood;
import com.bitflaker.lucidsourcekit.database.entities.DreamType;
import com.bitflaker.lucidsourcekit.database.entities.JournalEntry;
import com.bitflaker.lucidsourcekit.database.entities.JournalEntryHasTag;
import com.bitflaker.lucidsourcekit.database.entities.JournalEntryHasType;
import com.bitflaker.lucidsourcekit.database.entities.JournalEntryTag;
import com.bitflaker.lucidsourcekit.database.entities.SleepQuality;

@Database(entities = {JournalEntryTag.class, DreamType.class, SleepQuality.class,
        DreamMood.class, DreamClarity.class, AudioLocation.class, JournalEntry.class,
        JournalEntryHasTag.class, JournalEntryHasType.class}, version = 2, exportSchema = false)
public abstract class JournalDatabase extends RoomDatabase {
    public abstract JournalEntryTagDao journalEntryTagDao();
    public abstract DreamTypeDao dreamTypeDao();
    public abstract SleepQualityDao sleepQualityDao();
    public abstract DreamMoodDao dreamMoodDao();
    public abstract DreamClarityDao dreamClarityDao();
    public abstract AudioLocationDao audioLocationDao();
    public abstract JournalEntryDao journalEntryDao();
    public abstract JournalEntryHasTagDao hasTagDao();
    public abstract JournalEntryIsTypeDao isTypeDao();

    private static volatile JournalDatabase instance;

    public static synchronized JournalDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
            populateStaticTables(instance);
        }
        return instance;
    }

    public JournalDatabase() {

    }

    private static JournalDatabase create(final Context context) {
        // .allowMainThreadQueries()
        return Room.databaseBuilder(context, JournalDatabase.class, "journalDatabase.db").fallbackToDestructiveMigrationFrom(1).build();
    }

    private static void populateStaticTables(JournalDatabase instance) {
        SleepQuality[] sleepQualities = SleepQuality.populateData();
        DreamMood[] dreamMoods = DreamMood.populateData();
        DreamClarity[] dreamClarities = DreamClarity.populateData();
        DreamType[] dreamTypes = DreamType.populateData();

        instance.sleepQualityDao().insertAll(sleepQualities);
        instance.dreamMoodDao().insertAll(dreamMoods);
        instance.dreamClarityDao().insertAll(dreamClarities);
        instance.dreamTypeDao().insertAll(dreamTypes);
    }
}
