package xyz.olympusblog.utils

import am.ik.yavi.core.ConstraintViolation
import am.ik.yavi.core.ConstraintViolations
import java.util.function.Consumer

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

open class ValidationException(val params: Map<String, List<String>>) : RuntimeException()

class UserExists : RuntimeException()

class UserDoesNotExists : RuntimeException()

class ArticleDoesNotExist(val slug: String) : RuntimeException()

class CommentNotFound : RuntimeException()

data class Errors(val errors: List<FormError>) {
    data class FormError(val field: String, val message: String)
}

fun formatErrors(violations: ConstraintViolations): Errors {
    val errors = mutableListOf<Errors.FormError>()
    violations.forEach(Consumer { x: ConstraintViolation -> errors.add(Errors.FormError(x.name(), x.message())) })
    return Errors(errors)
}