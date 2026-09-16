package util

class ValidationException(message: String) : Exception(message)

object Validation {

    const val MAX_TITLE_LENGTH = 100
    const val MAX_DESCRIPTION_LENGTH = 500

    fun validateTitle(title: String) {
        if (title.isBlank()) throw ValidationException("Title cannot be empty.")
        if (title.length > MAX_TITLE_LENGTH) {
            throw ValidationException("Title exceeds $MAX_TITLE_LENGTH characters.")
        }
    }

    fun validateDescription(description: String) {
        if (description.length > MAX_DESCRIPTION_LENGTH) {
            throw ValidationException("Description exceeds $MAX_DESCRIPTION_LENGTH characters.")
        }
    }

    fun validateId(id: String): Int {
        val parsed = id.toIntOrNull()
            ?: throw ValidationException("Invalid ID: '$id' is not a number.")
        if (parsed <= 0) throw ValidationException("ID must be positive.")
        return parsed
    }
}