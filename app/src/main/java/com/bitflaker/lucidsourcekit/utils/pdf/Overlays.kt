package com.bitflaker.lucidsourcekit.utils.pdf

import android.view.View

class Overlays : Iterable<Overlay> {
    private val overlays: MutableList<Overlay> = mutableListOf()

    fun add(view: View) {
        add(view) {
            true
        }
    }

    fun add(view: View, displayCondition: (PageContext) -> Boolean) {
        overlays.add(Overlay(view, displayCondition))
    }

    fun add(overlay: Overlay) {
        overlays.add(overlay)
    }

    override fun iterator(): Iterator<Overlay> {
        return overlays.iterator()
    }
}
