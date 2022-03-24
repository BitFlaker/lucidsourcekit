package com.bitflaker.lucidsourcekit.database.goals.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Shuffle {
    @PrimaryKey(autoGenerate = true)
    public int shuffleId;
    public long dayStartTimestamp;
    public long dayEndTimestamp;

    public Shuffle(int shuffleId, long dayStartTimestamp, long dayEndTimestamp) {
        this.shuffleId = shuffleId;
        this.dayStartTimestamp = dayStartTimestamp;
        this.dayEndTimestamp = dayEndTimestamp;
    }
}
