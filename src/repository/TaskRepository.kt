package repository

import kotlinx.serialization.json.Json
import model.Task
import java.io.File

class TaskRepository(private val filePath: String) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val file = File(filePath)

    fun load(): MutableList<Task> {
        if (!file.exists()) return mutableListOf()
        return try {
            val content = file.readText()
            if (content.isBlank()) mutableListOf()
            else json.decodeFromString<MutableList<Task>>(content)
        } catch (e: Exception) {
            System.err.println("Warning: could not read tasks file (${e.message}). Starting fresh.")
            mutableListOf()
        }
    }

    fun save(tasks: List<Task>) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(tasks))
        } catch (e: Exception) {
            throw RuntimeException("Failed to save tasks to $filePath: ${e.message}", e)
        }
    }
}