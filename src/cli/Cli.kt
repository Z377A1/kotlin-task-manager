package cli

import service.TaskService
import util.Validation
import util.ValidationException

class Cli(private val taskService: TaskService) {

    fun run() {
        printHelp()
        loop@ while (true) {
            print("> ")
            val line = readlnOrNull()?.trim() ?: break
            if (line.isEmpty()) continue

            val parts = tokenize(line)
            val cmd = parts[0].lowercase()
            val args = parts.drop(1)

            try {
                when (cmd) {
                    "add", "a" -> handleAdd(args)
                    "list", "ls", "l" -> handleList(args)
                    "show", "get" -> handleShow(args)
                    "update", "u" -> handleUpdate(args)
                    "toggle", "t" -> handleToggle(args)
                    "complete", "done", "c" -> handleComplete(args)
                    "delete", "rm", "del", "d" -> handleDelete(args)
                    "clear" -> handleClear()
                    "help", "h", "?" -> printHelp()
                    "quit", "exit", "q" -> break@loop
                    else -> println("Unknown command: $cmd. Type 'help'.")
                }
            } catch (e: ValidationException) {
                println("Error: ${e.message}")
            } catch (e: Exception) {
                println("Unexpected error: ${e.message}")
            }
        }
        println("Goodbye.")
    }

    private fun handleAdd(args: List<String>) {
        if (args.isEmpty()) throw ValidationException("Usage: add <title> [--desc <description>]")
        val (title, desc) = extractTitleAndDesc(args)
        if (title == null) throw ValidationException("Title is required.")
        val task = taskService.add(title, desc)
        println("Added: ${task.id}")
    }

    private fun handleList(args: List<String>) {
        val showAll = args.isEmpty() || args[0] != "--pending"
        val tasks = taskService.list(showAll)
        if (tasks.isEmpty()) {
            println(if (showAll) "No tasks found." else "No pending tasks.")
            return
        }
        tasks.forEach { println(it) }
    }

    private fun handleShow(args: List<String>) {
        require(args, "show <id>")
        val id = Validation.validateId(args[0])
        val task = taskService.list().firstOrNull { it.id == id }
            ?: throw ValidationException("Task with id=$id not found.")
        println(task)
        println("  created at: ${java.util.Date(task.createdAt)}")
    }

    private fun handleUpdate(args: List<String>) {
        if (args.isEmpty()) throw ValidationException("Usage: update <id> [--title <t>] [--desc <d>]")
        val id = Validation.validateId(args[0])
        val rest = args.drop(1)
        val (title, desc) = extractTitleAndDesc(rest, required = false)
        taskService.update(id, title, desc)
        println("Updated task #$id.")
    }

    private fun handleToggle(args: List<String>) {
        require(args, "toggle <id>")
        val id = Validation.validateId(args[0])
        val task = taskService.toggle(id)
        println("Task #$id now ${if (task.completed) "completed" else "pending"}.")
    }

    private fun handleComplete(args: List<String>) {
        require(args, "complete <id>")
        val id = Validation.validateId(args[0])
        taskService.complete(id)
        println("Task #$id completed.")
    }

    private fun handleDelete(args: List<String>) {
        require(args, "delete <id>")
        val id = Validation.validateId(args[0])
        taskService.delete(id)
        println("Deleted task #$id.")
    }

    private fun handleClear() {
        val num = taskService.clearCompleted()
        println("Cleared $num completed task(s).")
    }

    private fun printHelp() {
        println(
            """
            |Kotlin Task Manager
            |Commands:
            | add <title> [--desc <description>]        Add a new task
            | list [--pending]                          List tasks
            | show <id>                                 Show task details
            | update <id> [--title <t>] [--desc <d>]    Update a task
            | toggle <id>                               Toggle completed status
            | complete <id>                             Mark completed
            | delete <id>                               Delete a task
            | clear                                     Remove all completed tasks
            | help                                      Show this help
            | quit                                      Exit
            """.trimMargin()
        )
    }

    private fun require(args: List<String>, usage: String) {
        if (args.isEmpty()) throw ValidationException("Usage: $usage")
    }

    private fun extractTitleAndDesc(
        args: List<String>,
        required: Boolean = true
    ): Pair<String?, String> {
        val sb = StringBuilder()
        var desc = ""
        var i = 0
        while (i < args.size) {
            if (args[i] == "--desc" && i + 1 < args.size) {
                desc = args[i + 1]
                i += 2
            } else {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append(args[i])
                i++
            }
        }
        val title = sb.toString().ifBlank { null }
        if (required && title == null) throw ValidationException("Title is required.")
        return title to desc
    }

    private fun tokenize(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuote = false
        var quoteClear = ' '
        for (c in line) {
            when {
                inQuote && c == quoteClear -> inQuote = false
                !inQuote && (c == '"' || c == '\'') -> { inQuote = true; quoteClear = c}
                !inQuote && c.isWhitespace() -> {
                    if (sb.isNotEmpty()) { out.add(sb.toString()); sb.clear() }
                }
                else -> sb.append(c)
            }
        }
        if (sb.isNotEmpty()) out.add(sb.toString())
        return out
    }
}