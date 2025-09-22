package com.bitflaker.lucidsourcekit.data

class AudioBufferPosition private constructor(
    var index: Int,
    var bufferPosition: Int,
    var angleLeftSpeaker: Long,
    var angleRightSpeaker: Long,
    var isFinished: Boolean
) : Cloneable {
    constructor() : this(0, 0, 0, 0, false)

    public override fun clone(): AudioBufferPosition {
        return AudioBufferPosition(
            index,
            bufferPosition,
            angleLeftSpeaker,
            angleRightSpeaker,
            isFinished
        )
    }
}
