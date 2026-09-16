import cli.Cli
import repository.TaskRepository
import service.TaskService
import java.nio.file.Paths

fun main(args: Array<String>) {
    val dataFile = System.getenv("TASKS_FILE")
        ?: Paths.get("${System.getProperty("user.home")}")
            .resolve(".kotlin-task-manager")
            .resolve("tasks.json")
            .toString()

    println("Using data file: $dataFile")

    val taskRepository = TaskRepository(dataFile)
    val taskService = TaskService(taskRepository)

    // Non-interactive mode: run a single command from args, then exit
    if (args.isNotEmpty()) {
        runSingleCommand(args.toList(), taskService)
        return
    }

    Cli(taskService).run()
}

private fun runSingleCommand(args: List<String>, taskService: TaskService) {
    val cli = Cli(taskService)
    // feed commands via reflection-free approach: simplest is to just exec a command string
    // We'll implement a thin dispatcher:
    val line = args.joinToString(" ")
    try {
        val method = Cli::class.java.getDeclaredMethod("tokenize", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val parts = method.invoke(cli, line) as List<String>
        val cmd = parts[0].lowercase()
        val rest = parts.drop(1)
        // delegate to interactive logic by feeding a synthetic BufferedReader? Simpler: direct when:
        when (cmd) {
            "add", "a" -> {
                val (t, d) = parseTD(rest)
                println("Added: ${taskService.add(t!!, d)}")
            }
            "list", "ls", "l" -> {
                val showAll = rest.isEmpty() || rest[0] != "--pending"
                taskService.list(showAll).forEach { println(it) }
            }
            "toggle", "t" -> println("Toggled: ${taskService.toggle(rest[0].toInt())}")
            "complete", "done", "c" -> println("Completed: ${taskService.complete(rest[0].toInt())}")
            "delete", "rm", "d" -> println("Deleted: ${taskService.delete(rest[0].toInt())}")
            "clear" -> println("Cleared ${taskService.clearCompleted()} task(s).")
            else -> print("Unknown or unsupported single command: $cmd")
        }
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        kotlin.system.exitProcess(1)
    }
}

private fun parseTD(args: List<String>): Pair<String?, String> {
    val sb = StringBuilder(); var desc = ""; var i = 0
    while (i < args.size) {
        if (args[i] == "--desc" && i + 1 < args.size) { desc = args[i + 1]; i +=2 }
        else { if (sb.isNotEmpty()) sb.append(' '); sb.append(args[i]); i++ }
    }
    return sb.toString().ifBlank { null } to desc
}
