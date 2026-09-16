import org.junit.jupiter.api.Assertions.assertFalse
import repository.TaskRepository
import service.TaskService
import util.ValidationException
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TaskServiceTest {

    private lateinit var tmpFile: File
    private lateinit var taskService: TaskService

    @BeforeTest
    fun setup() {
        tmpFile = File.createTempFile("tasks-test", ".json").also { it.delete() }
        taskService = TaskService(TaskRepository(tmpFile.absolutePath))
    }

    @AfterTest
    fun teardown() {
        tmpFile.delete()
    }

    @Test
    fun `add creates task with incrementing id`() {
        val task1 = taskService.add("Buy milk")
        val task2 = taskService.add("Walk dog")
        assertEquals(1, task1.id)
        assertEquals(2, task2.id)
        assertEquals(2, taskService.list().size)
    }

    @Test
    fun `add rejects blank title`() {
        assertFailsWith<ValidationException> { taskService.add("   ") }
    }

    @Test
    fun `add rejects too-long title`() {
        val long = "x".repeat(101)
        assertFailsWith<ValidationException> { taskService.add(long) }
    }

    @Test
    fun `update changes title and description`() {
        val task = taskService.add("Old", "old desc")
        taskService.update(task.id, "New", "new desc")
        val refreshedTask = taskService.list().first { it.id == task.id }
        assertEquals("New", refreshedTask.title)
        assertEquals("new desc", refreshedTask.description)
    }

    @Test
    fun `update with only title preserves description`() {
        val task = taskService.add("Old", "keep me")
        taskService.update(task.id, newTitle = "Changed")
        val refreshedTask = taskService.list().first { it.id == task.id }
        assertEquals("Changed", refreshedTask.title)
        assertEquals("keep me", refreshedTask.description)
    }

    @Test
    fun `update missing id throws`() {
        assertFailsWith<ValidationException> { taskService.update(999, "x") }
    }

    @Test
    fun `toggle flips completion`() {
        val task = taskService.add("Task")
        assertFalse(task.completed)
        taskService.toggle(task.id)
        assertTrue(taskService.list().first { it.id == task.id }.completed)
    }

    @Test
    fun `delete removes task`() {
        val task = taskService.add("To delete")
        taskService.delete(task.id)
        assertTrue(taskService.list().isEmpty())
    }

    @Test
    fun `clearCompleted removes only completed`() {
        val task = taskService.add("A")
        taskService.add("B")
        taskService.complete(task.id)
        val numRemovedTasks = taskService.clearCompleted()
        assertEquals(1, numRemovedTasks)
        assertEquals(1, taskService.list().size)
    }

    @Test
    fun `list pending filters completed`() {
        val task = taskService.add("A")
        taskService.add("B")
        taskService.complete(task.id)
        val pendingTasks = taskService.list(showAll = false)
        assertEquals(1, pendingTasks.size)
        assertEquals("B", pendingTasks[0].title)
    }

    @Test
    fun `persistence round-trips through file`() {
        taskService.add("Persist me", "with desc")
        val reloadedTaskService = TaskService(TaskRepository(tmpFile.absolutePath))
        val tasks = reloadedTaskService.list()
        assertEquals(1, tasks.size)
        assertEquals("Persist me", tasks[0].title)
        assertEquals("with desc", tasks[0].description)
    }

    @Test
    fun `next id continues after reload`() {
        taskService.add("A") // id 1
        taskService.add("B") // id 2
        val reloadedTaskService = TaskService(TaskRepository(tmpFile.absolutePath))
        val task = reloadedTaskService.add("C")
        assertEquals(3, task.id)
    }

    @Test
    fun `corrupt file is handled gracefully`() {
        tmpFile.writeText("{ not valid json ")
        val taskService = TaskService(TaskRepository(tmpFile.absolutePath))
        assertTrue(taskService.list().isEmpty())
    }
}