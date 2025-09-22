package com.bitflaker.lucidsourcekit.database;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.bitflaker.lucidsourcekit.database.alarms.daos.AlarmToneTypesDao;
import com.bitflaker.lucidsourcekit.database.alarms.entities.Alarm;
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
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationCategory;
import com.bitflaker.lucidsourcekit.database.notifications.entities.NotificationMessage;
import com.bitflaker.lucidsourcekit.database.questionnaire.daos.CompletedQuestionnaireDao;
import com.bitflaker.lucidsourcekit.database.questionnaire.daos.QuestionDao;
import com.bitflaker.lucidsourcekit.database.questionnaire.daos.QuestionOptionsDao;
import com.bitflaker.lucidsourcekit.database.questionnaire.daos.QuestionTypeDao;
import com.bitflaker.lucidsourcekit.database.questionnaire.daos.QuestionnaireAnswerDao;
import com.bitflaker.lucidsourcekit.database.questionnaire.daos.QuestionnaireDao;
import com.bitflaker.lucidsourcekit.database.questionnaire.daos.SelectedOptionsDao;
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.CompletedQuestionnaire;
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Question;
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionOptions;
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionType;
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.Questionnaire;
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.QuestionnaireAnswer;
import com.bitflaker.lucidsourcekit.database.questionnaire.entities.SelectedOptions;

import org.json.JSONObject;

@Database(entities = {JournalEntryTag.class, DreamType.class, SleepQuality.class,
        DreamMood.class, DreamClarity.class, AudioLocation.class, JournalEntry.class,
        JournalEntryHasTag.class, JournalEntryHasType.class, Goal.class, Shuffle.class,
        ShuffleHasGoal.class, Alarm.class, AlarmToneTypes.class,
        Weekdays.class, ActiveAlarm.class, StoredAlarm.class,
        NotificationMessage.class, NotificationCategory.class, ShuffleTransaction.class,
        Questionnaire.class, Question.class, QuestionType.class, CompletedQuestionnaire.class,
        QuestionnaireAnswer.class, QuestionOptions.class, SelectedOptions.class},
        version = 19,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class MainDatabase extends RoomDatabase {

    public final static String MAIN_DATABASE_NAME = "journalDatabase.db";

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
    public abstract AlarmToneTypesDao getAlarmToneTypesDao();
    public abstract ActiveAlarmDao getActiveAlarmDao();
    public abstract StoredAlarmDao getStoredAlarmDao();

    // Notification tables
    public abstract NotificationMessageDao getNotificationMessageDao();
    public abstract NotificationCategoryDao getNotificationCategoryDao();

    // Questionnaire tables
    public abstract CompletedQuestionnaireDao getCompletedQuestionnaireDao();
    public abstract QuestionDao getQuestionDao();
    public abstract QuestionnaireAnswerDao getQuestionnaireAnswerDao();
    public abstract QuestionnaireDao getQuestionnaireDao();
    public abstract QuestionOptionsDao getQuestionOptionsDao();
    public abstract QuestionTypeDao getQuestionTypeDao();
    public abstract SelectedOptionsDao getSelectedOptionsDao();

    // Database
    private static volatile MainDatabase instance;

    public static synchronized MainDatabase getInstance(Context context) {
        if (instance == null) {
            instance = create(context);
        }
        return instance;
    }

    public MainDatabase() { }

    private static MainDatabase create(final Context context) {
        return Room.databaseBuilder(context, MainDatabase.class, MAIN_DATABASE_NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigrationFrom(false, 4)
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
                .addMigrations(MainDatabaseMigrations.MIGRATION_15_16)
                .addMigrations(MainDatabaseMigrations.MIGRATION_16_17)
                .addMigrations(MainDatabaseMigrations.MIGRATION_17_18)
                .addMigrations(MainDatabaseMigrations.MIGRATION_18_19)
                .build();
    }

    @NonNull
    public static byte[] exportSharedPreferences(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return new JSONObject(preferences.getAll()).toString().getBytes();
    }
}
