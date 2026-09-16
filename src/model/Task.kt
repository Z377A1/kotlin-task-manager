package model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Int,
    var title: String,
    var description: String = "",
    var completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {

    override fun toString(): String {
        val status = if (completed) "[✔]" else "[ ]"
        return "$status #$id $title" + if (description.isNotBlank()) " – $description" else ""
    }
}