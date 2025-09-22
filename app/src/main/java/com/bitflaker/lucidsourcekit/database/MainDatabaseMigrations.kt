package com.bitflaker.lucidsourcekit.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MainDatabaseMigrations {
    @JvmField
    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE Goal (
            goalId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            description TEXT NOT NULL,
            difficulty REAL NOT NULL,
            difficultyLocked INTEGER NOT NULL DEFAULT 0
        );""".trimIndent())

            db.execSQL("""CREATE TABLE Shuffle (
            shuffleId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            dayStartTimestamp INTEGER NOT NULL,
            dayEndTimestamp INTEGER NOT NULL
        );""".trimIndent())

            db.execSQL("""CREATE TABLE ShuffleHasGoal (
            shuffleId INTEGER NOT NULL,
            goalId INTEGER NOT NULL,
            achieved INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(shuffleId, goalId),
            FOREIGN KEY (shuffleId) REFERENCES Shuffle (shuffleId) ON DELETE CASCADE ON UPDATE NO ACTION,
            FOREIGN KEY (goalId) REFERENCES Goal (goalId) ON DELETE CASCADE ON UPDATE NO ACTION
        );""".trimIndent())
        }
    }

    @JvmField
    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM Shuffle;")
            db.execSQL("DELETE FROM ShuffleHasGoal;")
            db.execSQL("CREATE INDEX index_ShuffleHasGoal_goalId ON ShuffleHasGoal (goalId);")
        }
    }

    @JvmField
    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE AudioLocation ADD recordingTimestamp INTEGER DEFAULT 0 NOT NULL")
        }
    }

    @JvmField
    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE Weekdays (weekdayId INTEGER PRIMARY KEY NOT NULL,description TEXT);")
            db.execSQL("CREATE TABLE AlarmToneTypes (alarmToneTypeId INTEGER PRIMARY KEY NOT NULL,description TEXT);")
            db.execSQL("""CREATE TABLE Alarm (
            alarmId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            bedtimeHour INTEGER NOT NULL,
            bedtimeMinute INTEGER NOT NULL,
            alarmHour INTEGER NOT NULL,
            alarmMinute INTEGER NOT NULL,
            alarmToneType INTEGER NOT NULL,
            alarmUri TEXT NOT NULL,
            alarmVolume INTEGER NOT NULL,
            alarmVolumeIncreaseMinutes INTEGER NOT NULL,
            alarmVolumeIncreaseSeconds INTEGER NOT NULL,
            vibrate INTEGER NOT NULL,
            useFlashlight INTEGER NOT NULL,
            isActive INTEGER NOT NULL,
            FOREIGN KEY (alarmToneType) REFERENCES AlarmToneTypes (alarmToneTypeId) ON DELETE CASCADE ON UPDATE NO ACTION
        );""".trimIndent())

            db.execSQL("""CREATE TABLE AlarmIsOnWeekday (
            alarmId INTEGER NOT NULL,
            weekdayId INTEGER NOT NULL,
            PRIMARY KEY (alarmId, weekdayId),
            FOREIGN KEY (alarmId) REFERENCES Alarm (alarmId) ON DELETE CASCADE ON UPDATE NO ACTION,
            FOREIGN KEY (weekdayId) REFERENCES Weekdays (weekdayId) ON DELETE CASCADE ON UPDATE NO ACTION
        );""".trimIndent())

            db.execSQL("CREATE INDEX index_Alarm_alarmToneType ON Alarm (alarmToneType);")
            db.execSQL("CREATE INDEX index_AlarmIsOnWeekday_weekdayId ON AlarmIsOnWeekday (weekdayId);")
        }
    }

    @JvmField
    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) { }
    }

    @JvmField
    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Alarm ADD title TEXT DEFAULT 'Unnamed Alarm' NOT NULL;")
        }
    }

    @JvmField
    val MIGRATION_11_12: Migration = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE ActiveAlarm (
            requestCode INTEGER NOT NULL,
            initialTime INTEGER NOT NULL,
            interval INTEGER NOT NULL,
            patternIndex INTEGER NOT NULL,
            pattern TEXT NOT NULL,
            PRIMARY KEY(requestCode)
        );""".trimIndent())
        }
    }

    @JvmField
    val MIGRATION_12_13: Migration = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE ActiveAlarm")
            db.execSQL("""CREATE TABLE ActiveAlarm (
            requestCode INTEGER NOT NULL,
            initialTime INTEGER NOT NULL,
            interval INTEGER NOT NULL,
            patternIndex INTEGER NOT NULL,
            PRIMARY KEY(requestCode)
        );""".trimIndent())

            db.execSQL("""CREATE TABLE StoredAlarm (
            alarmId INTEGER NOT NULL,
            title TEXT NOT NULL DEFAULT('Unnamed Alarm'),
            bedtimeTimestamp INTEGER NOT NULL,
            alarmTimestamp INTEGER NOT NULL,
            pattern TEXT NOT NULL,
            alarmToneTypeId INTEGER NOT NULL,
            alarmUri TEXT NOT NULL,
            alarmVolume REAL NOT NULL,
            alarmVolumeIncreaseTimestamp INTEGER NOT NULL,
            isVibrationActive INTEGER NOT NULL,
            isFlashlightActive INTEGER NOT NULL,
            isAlarmActive INTEGER NOT NULL,
            requestCodeActiveAlarm INTEGER NOT NULL DEFAULT(-1),
            PRIMARY KEY(alarmId),
            FOREIGN KEY (alarmToneTypeId) REFERENCES AlarmToneTypes (alarmToneTypeId) ON DELETE CASCADE ON UPDATE NO ACTION,
            FOREIGN KEY (requestCodeActiveAlarm) REFERENCES ActiveAlarm (requestCode) ON DELETE SET DEFAULT ON UPDATE NO ACTION
        );""".trimIndent())

            db.execSQL("CREATE INDEX index_StoredAlarm_alarmToneTypeId ON StoredAlarm (alarmToneTypeId);")
            db.execSQL("CREATE INDEX index_StoredAlarm_requestCodeActiveAlarm ON StoredAlarm (requestCodeActiveAlarm);")
        }
    }

    @JvmField
    val MIGRATION_13_14: Migration = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE NotificationObfuscations (
            obfuscationTypeId INTEGER PRIMARY KEY NOT NULL,
            description TEXT NOT NULL
        );""".trimIndent())

            db.execSQL("""CREATE TABLE NotificationMessage (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            notificationCategoryId TEXT NOT NULL,
            message TEXT NOT NULL,
            obfuscationTypeId INTEGER NOT NULL,
            weight INTEGER NOT NULL,
            FOREIGN KEY (obfuscationTypeId) REFERENCES NotificationObfuscations (obfuscationTypeId) ON DELETE CASCADE ON UPDATE NO ACTION,
            FOREIGN KEY (notificationCategoryId) REFERENCES NotificationCategory (id) ON DELETE CASCADE ON UPDATE NO ACTION);
        """.trimIndent())

            db.execSQL("""CREATE TABLE NotificationCategory (
            id TEXT PRIMARY KEY NOT NULL,
            description TEXT NOT NULL,
            timeFrom INTEGER NOT NULL,
            timeTo INTEGER NOT NULL,
            obfuscationTypeId INTEGER NOT NULL,
            dailyNotificationCount INTEGER NOT NULL,
            isPermanent INTEGER NOT NULL,
            isEnabled INTEGER NOT NULL,
            FOREIGN KEY (obfuscationTypeId) REFERENCES NotificationObfuscations (obfuscationTypeId) ON DELETE CASCADE ON UPDATE NO ACTION
        );""".trimIndent())

            db.execSQL("CREATE INDEX index_NotificationCategory_obfuscationTypeId ON NotificationCategory (obfuscationTypeId);")
            db.execSQL("CREATE INDEX index_NotificationMessage_notificationCategoryId ON NotificationMessage (notificationCategoryId);")
            db.execSQL("CREATE INDEX index_NotificationMessage_obfuscationTypeId ON NotificationMessage (obfuscationTypeId);")
        }
    }

    @JvmField
    val MIGRATION_14_15: Migration = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE ShuffleTransaction (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            shuffleId INTEGER NOT NULL,
            goalId INTEGER NOT NULL,
            achievedAt INTEGER NOT NULL,
            FOREIGN KEY (shuffleId) REFERENCES Shuffle (shuffleId) ON DELETE CASCADE ON UPDATE NO ACTION,
            FOREIGN KEY (goalId) REFERENCES Goal (goalId) ON DELETE CASCADE ON UPDATE NO ACTION
        );""".trimIndent())

            db.execSQL("CREATE INDEX index_ShuffleTransaction_shuffleId ON ShuffleTransaction (shuffleId);")
            db.execSQL("CREATE INDEX index_ShuffleTransaction_goalId ON ShuffleTransaction (goalId);")
            db.execSQL("""INSERT INTO ShuffleTransaction SELECT 
                null AS id,
                shg.shuffleId,
                shg.goalId,
                s.dayStartTimestamp as achievedAt
            FROM ShuffleHasGoal shg 
            LEFT JOIN Shuffle s ON shg.shuffleId = s.shuffleId WHERE shg.achieved = 1
        """.trimIndent())
        }
    }

    @JvmField
    val MIGRATION_15_16: Migration = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE Questionnaire (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            title TEXT NOT NULL,
            description TEXT,
            isHidden INTEGER NOT NULL,
            isCompact INTEGER NOT NULL
        );""".trimIndent())

            db.execSQL("""CREATE TABLE QuestionType (i
             INTEGER PRIMARY KEY NOT NULL,
             description TEXT NOT NULL
         );""".trimIndent())

            db.execSQL("""CREATE TABLE CompletedQuestionnaire (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            timestamp INTEGER NOT NULL,
            questionnaireId INTEGER NOT NULL,
            FOREIGN KEY (questionnaireId) REFERENCES Questionnaire (id) ON DELETE CASCADE ON UPDATE CASCADE
        );""".trimIndent())

            db.execSQL("""CREATE TABLE Question (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            question TEXT NOT NULL,
            questionTypeId INTEGER NOT NULL,
            questionnaireId INTEGER NOT NULL,
            orderNr INTEGER NOT NULL,
            valueFrom INTEGER,
            valueTo INTEGER,
            autoContinue INTEGER NOT NULL,
            isHidden INTEGER NOT NULL,
            FOREIGN KEY (questionTypeId) REFERENCES QuestionType (id) ON DELETE CASCADE ON UPDATE CASCADE,
            FOREIGN KEY (questionnaireId) REFERENCES Questionnaire (id) ON DELETE CASCADE ON UPDATE CASCADE);
        """.trimIndent())

            db.execSQL("""CREATE TABLE QuestionOptions (
            questionId INTEGER NOT NULL,
            id INTEGER NOT NULL,
            text TEXT NOT NULL,
            description TEXT,
            PRIMARY KEY (questionId, id),
            FOREIGN KEY (questionId) REFERENCES Question (id) ON DELETE CASCADE ON UPDATE CASCADE
        );""".trimIndent())

            db.execSQL("""CREATE TABLE QuestionnaireAnswer (
            completedQuestionnaireId INTEGER NOT NULL,
            questionId INTEGER NOT NULL,
            value TEXT,
            PRIMARY KEY (completedQuestionnaireId, questionId),
            FOREIGN KEY (completedQuestionnaireId) REFERENCES CompletedQuestionnaire (id) ON DELETE CASCADE ON UPDATE CASCADE,
            FOREIGN KEY (questionId) REFERENCES Question (id) ON DELETE CASCADE ON UPDATE CASCADE
        );""".trimIndent())

            db.execSQL("""CREATE TABLE SelectedOptions (
            completedQuestionnaireId INTEGER NOT NULL,
            questionId INTEGER NOT NULL,
            optionId INTEGER NOT NULL,
            PRIMARY KEY (completedQuestionnaireId, questionId, optionId),
            FOREIGN KEY (completedQuestionnaireId, questionId) REFERENCES QuestionnaireAnswer (completedQuestionnaireId, questionId) ON DELETE CASCADE ON UPDATE CASCADE,
            FOREIGN KEY (questionId, optionId) REFERENCES QuestionOptions (questionId, id) ON DELETE CASCADE ON UPDATE CASCADE
        );""".trimIndent())

            db.execSQL("CREATE INDEX index_CompletedQuestionnaire_questionnaireId ON CompletedQuestionnaire (questionnaireId);")
            db.execSQL("CREATE INDEX index_Question_questionTypeId ON Question (questionTypeId);")
            db.execSQL("CREATE INDEX index_Question_questionnaireId ON Question (questionnaireId);")
            db.execSQL("CREATE INDEX index_QuestionOptions_questionId ON QuestionOptions (questionId);")
            db.execSQL("CREATE INDEX index_QuestionnaireAnswer_completedQuestionnaireId ON QuestionnaireAnswer (completedQuestionnaireId);")
            db.execSQL("CREATE INDEX index_QuestionnaireAnswer_questionId ON QuestionnaireAnswer (questionId);")
            db.execSQL("CREATE INDEX index_SelectedOptions_completedQuestionnaireId_questionId ON SelectedOptions (completedQuestionnaireId, questionId);")
            db.execSQL("CREATE INDEX index_SelectedOptions_questionId_optionId ON SelectedOptions (questionId, optionId);")
        }
    }

    @JvmField
    val MIGRATION_16_17: Migration = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Questionnaire ADD orderNr INTEGER DEFAULT -1 NOT NULL;")
            db.execSQL("ALTER TABLE Questionnaire ADD colorCode TEXT DEFAULT NULL;")
            db.execSQL("ALTER TABLE CompletedQuestionnaire ADD answerDuration INTEGER DEFAULT 0 NOT NULL;")
        }
    }

    @JvmField
    val MIGRATION_17_18: Migration = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE QuestionOptions ADD orderNr INTEGER DEFAULT 0 NOT NULL;")
            db.execSQL("ALTER TABLE QuestionOptions ADD isHidden INTEGER DEFAULT 0 NOT NULL;")
        }
    }

    // Removed `obfuscationTypeId` columns
    @JvmField
    val MIGRATION_18_19: Migration = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create temporary table to back up data before migrating
            db.execSQL("""
                CREATE TEMPORARY TABLE NotificationMessage_backup(
                    id INTEGER PRIMARY KEY NOT NULL,
                    notificationCategoryId TEXT NOT NULL,
                    message TEXT NOT NULL,
                    weight INTEGER NOT NULL
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TEMPORARY TABLE NotificationCategory_backup(
                    id TEXT PRIMARY KEY NOT NULL,
                    description TEXT NOT NULL,
                    timeFrom INTEGER NOT NULL,
                    timeTo INTEGER NOT NULL,
                    dailyNotificationCount INTEGER NOT NULL,
                    isPermanent INTEGER NOT NULL,
                    isEnabled INTEGER NOT NULL
                );
            """.trimIndent())
            db.execSQL("INSERT INTO NotificationMessage_backup SELECT id, notificationCategoryId, message, weight FROM NotificationMessage;")
            db.execSQL("INSERT INTO NotificationCategory_backup SELECT id, description, timeFrom, timeTo, dailyNotificationCount, isPermanent, isEnabled FROM NotificationCategory;")
            db.execSQL("DROP TABLE NotificationMessage;")
            db.execSQL("DROP TABLE NotificationCategory;")

            // Create tables again with previous data
            db.execSQL("""
                CREATE TABLE NotificationCategory(
                    id TEXT PRIMARY KEY NOT NULL,
                    description TEXT NOT NULL,
                    timeFrom INTEGER NOT NULL,
                    timeTo INTEGER NOT NULL,
                    dailyNotificationCount INTEGER NOT NULL,
                    isPermanent INTEGER NOT NULL,
                    isEnabled INTEGER NOT NULL
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE NotificationMessage(
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    notificationCategoryId TEXT NOT NULL,
                    message TEXT NOT NULL,
                    weight INTEGER NOT NULL,
                    FOREIGN KEY (notificationCategoryId) REFERENCES NotificationCategory (id) ON DELETE CASCADE ON UPDATE NO ACTION
                );
            """.trimIndent())
            db.execSQL("INSERT INTO NotificationCategory SELECT id, description, timeFrom, timeTo, dailyNotificationCount, isPermanent, isEnabled FROM NotificationCategory_backup;")
            db.execSQL("INSERT INTO NotificationMessage SELECT id, notificationCategoryId, message, weight FROM NotificationMessage_backup;")
            db.execSQL("DROP TABLE NotificationCategory_backup;")
            db.execSQL("DROP TABLE NotificationMessage_backup;")

            db.execSQL("CREATE INDEX index_NotificationMessage_notificationCategoryId ON NotificationMessage (notificationCategoryId);")
        }
    }
}