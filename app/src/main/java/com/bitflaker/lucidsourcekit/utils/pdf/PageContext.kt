package com.bitflaker.lucidsourcekit.utils.pdf

data class PageContext(
    /**
     * The current page number within the entire document
     */
    val pageNumber: Int,

    /**
     * The index of the content layout currently being drawn to the page
     */
    val contentLayoutIndex: Int,

    /**
     * The current page number within the current layout (not across the entire document)
     */
    val contentLayoutPageNumber: Int
)