package service

import model.Task
import repository.TaskRepository
import util.Validation
import util.ValidationException

class TaskService(private val taskRepository: TaskRepository) {

    private val tasks: MutableList<Task> = taskRepository.load()
    private var nextId: Int = (tasks.maxOfOrNull { it.id } ?: 0) + 1

    fun list(showAll: Boolean = true): List<Task> =
        if (showAll) tasks.toList() else tasks.filter { !it.completed }

    fun add(title: String, description: String = ""): Task {
        Validation.validateTitle(title)
        Validation.validateDescription(description)
        val task = Task(id = nextId++, title = title.trim(), description = description.trim())
        tasks.add(task)
        persist()
        return task
    }

    fun update(id: Int, newTitle: String? = null, newDescription: String? = null): Task {
        val task = findById(id)
        newTitle?.let {
            Validation.validateTitle(it)
            task.title = it.trim()
        }
        newDescription?.let {
            Validation.validateDescription(it)
            task.description = it.trim()
        }
        persist()
        return task
    }

    fun toggle(id: Int): Task {
        val task = findById(id)
        task.completed = !task.completed
        persist()
        return task
    }

    fun complete(id: Int): Task {
        val task = findById(id)
        task.completed = true
        persist()
        return task
    }

    fun delete(id: Int): Task {
        val task = findById(id)
        tasks.remove(task)
        persist()
        return task
    }

    fun clearCompleted(): Int {
        val before = tasks.size
        tasks.removeAll { it.completed }
        persist()
        return before - tasks.size
    }

    private fun findById(id: Int): Task =
        tasks.firstOrNull { it.id == id }
            ?: throw ValidationException("Task with id=$id not found.")

    private fun persist() = taskRepository.save(tasks)
}