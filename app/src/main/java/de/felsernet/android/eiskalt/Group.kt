package de.felsernet.android.eiskalt

import java.io.Serializable

data class Group(
    var name: String,
    var id: Long = nextId()
) : Serializable {

    constructor() : this("")

    companion object {
        private var counter: Long = 1
        private fun nextId(): Long = counter++

        fun initializeCounter(groups: List<Group>) {
            val maxId = groups.maxOfOrNull { it.id } ?: 0
            counter = maxId + 1
        }
    }
}
