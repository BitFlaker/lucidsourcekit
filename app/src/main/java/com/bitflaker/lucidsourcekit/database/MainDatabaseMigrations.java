package com.bitflaker.lucidsourcekit.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class MainDatabaseMigrations {
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
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

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM Shuffle;");
            database.execSQL("DELETE FROM ShuffleHasGoal;");
            database.execSQL("CREATE INDEX index_ShuffleHasGoal_goalId ON ShuffleHasGoal (goalId);");
        }
    };

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE AudioLocation ADD recordingTimestamp INTEGER DEFAULT 0 NOT NULL");
        }
    };

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
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

    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) { }
    };

    public static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Alarm ADD title TEXT DEFAULT 'Unnamed Alarm' NOT NULL;");
        }
    };

    public static final Migration MIGRATION_11_12 = new Migration(11, 12) {
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

    public static final Migration MIGRATION_12_13 = new Migration(12, 13) {
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

    public static final Migration MIGRATION_13_14 = new Migration(13, 14) {
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

    public static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE ShuffleTransaction (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "shuffleId INTEGER NOT NULL," +
                    "goalId INTEGER NOT NULL," +
                    "achievedAt INTEGER NOT NULL, " +
                    "FOREIGN KEY (shuffleId)" +
                    "REFERENCES Shuffle (shuffleId)" +
                    "ON DELETE CASCADE " +
                    "ON UPDATE NO ACTION," +
                    "FOREIGN KEY (goalId)" +
                    "REFERENCES Goal (goalId)" +
                    "ON DELETE CASCADE " +
                    "ON UPDATE NO ACTION);");

            database.execSQL("CREATE INDEX index_ShuffleTransaction_shuffleId ON ShuffleTransaction (shuffleId);");
            database.execSQL("CREATE INDEX index_ShuffleTransaction_goalId ON ShuffleTransaction (goalId);");
            database.execSQL("INSERT INTO ShuffleTransaction " +
                    "SELECT null AS id, shg.shuffleId, shg.goalId, s.dayStartTimestamp as achievedAt FROM ShuffleHasGoal shg " +
                    "LEFT JOIN Shuffle s ON shg.shuffleId = s.shuffleId " +
                    "WHERE shg.achieved = 1");
        }
    };

    public static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE Questionnaire (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "title TEXT NOT NULL," +
                    "description TEXT," +
                    "isHidden INTEGER NOT NULL, " +
                    "isCompact INTEGER NOT NULL);"
            );
            database.execSQL("CREATE TABLE QuestionType (" +
                    "id INTEGER PRIMARY KEY NOT NULL," +
                    "description TEXT NOT NULL);"
            );
            database.execSQL("CREATE TABLE CompletedQuestionnaire (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "timestamp INTEGER NOT NULL," +
                    "questionnaireId INTEGER NOT NULL," +
                    "FOREIGN KEY (questionnaireId) REFERENCES Questionnaire (id) ON DELETE CASCADE ON UPDATE CASCADE);"
            );
            database.execSQL("CREATE TABLE Question (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "question TEXT NOT NULL," +
                    "questionTypeId INTEGER NOT NULL," +
                    "questionnaireId INTEGER NOT NULL, " +
                    "orderNr INTEGER NOT NULL, " +
                    "valueFrom INTEGER, " +
                    "valueTo INTEGER, " +
                    "autoContinue INTEGER NOT NULL, " +
                    "isHidden INTEGER NOT NULL, " +
                    "FOREIGN KEY (questionTypeId) REFERENCES QuestionType (id) ON DELETE CASCADE ON UPDATE CASCADE," +
                    "FOREIGN KEY (questionnaireId) REFERENCES Questionnaire (id) ON DELETE CASCADE ON UPDATE CASCADE);"
            );
            database.execSQL("CREATE TABLE QuestionOptions (" +
                    "questionId INTEGER NOT NULL," +
                    "id INTEGER NOT NULL," +
                    "text TEXT NOT NULL," +
                    "description TEXT, " +
                    "PRIMARY KEY (questionId, id)," +
                    "FOREIGN KEY (questionId) REFERENCES Question (id) ON DELETE CASCADE ON UPDATE CASCADE);"
            );
            database.execSQL("CREATE TABLE QuestionnaireAnswer (" +
                    "completedQuestionnaireId INTEGER NOT NULL," +
                    "questionId INTEGER NOT NULL," +
                    "value TEXT," +
                    "PRIMARY KEY (completedQuestionnaireId, questionId)," +
                    "FOREIGN KEY (completedQuestionnaireId) REFERENCES CompletedQuestionnaire (id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "FOREIGN KEY (questionId) REFERENCES Question (id) ON DELETE CASCADE ON UPDATE CASCADE);"
            );
            database.execSQL("CREATE TABLE SelectedOptions (" +
                    "completedQuestionnaireId INTEGER NOT NULL," +
                    "questionId INTEGER NOT NULL," +
                    "optionId INTEGER NOT NULL," +
                    "PRIMARY KEY (completedQuestionnaireId, questionId, optionId)," +
                    "FOREIGN KEY (completedQuestionnaireId, questionId) REFERENCES QuestionnaireAnswer (completedQuestionnaireId, questionId) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "FOREIGN KEY (questionId, optionId) REFERENCES QuestionOptions (questionId, id) ON DELETE CASCADE ON UPDATE CASCADE);"
            );

            database.execSQL("CREATE INDEX index_CompletedQuestionnaire_questionnaireId ON CompletedQuestionnaire (questionnaireId);");
            database.execSQL("CREATE INDEX index_Question_questionTypeId ON Question (questionTypeId);");
            database.execSQL("CREATE INDEX index_Question_questionnaireId ON Question (questionnaireId);");
            database.execSQL("CREATE INDEX index_QuestionOptions_questionId ON QuestionOptions (questionId);");
            database.execSQL("CREATE INDEX index_QuestionnaireAnswer_completedQuestionnaireId ON QuestionnaireAnswer (completedQuestionnaireId);");
            database.execSQL("CREATE INDEX index_QuestionnaireAnswer_questionId ON QuestionnaireAnswer (questionId);");
            database.execSQL("CREATE INDEX index_SelectedOptions_completedQuestionnaireId_questionId ON SelectedOptions (completedQuestionnaireId, questionId);");
            database.execSQL("CREATE INDEX index_SelectedOptions_questionId_optionId ON SelectedOptions (questionId, optionId);");
        }
    };

    public static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Questionnaire ADD orderNr INTEGER DEFAULT -1 NOT NULL;");
            database.execSQL("ALTER TABLE Questionnaire ADD colorCode TEXT DEFAULT NULL;");
            database.execSQL("ALTER TABLE CompletedQuestionnaire ADD answerDuration INTEGER DEFAULT 0 NOT NULL;");
        }
    };

    public static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE QuestionOptions ADD orderNr INTEGER DEFAULT 0 NOT NULL;");
            database.execSQL("ALTER TABLE QuestionOptions ADD isHidden INTEGER DEFAULT 0 NOT NULL;");
        }
    };
}
