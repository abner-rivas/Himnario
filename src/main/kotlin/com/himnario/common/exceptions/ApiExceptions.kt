package com.himnario.common.exceptions

sealed class ApplicationException(
    val code: String,
    override val message: String,
) : RuntimeException(message)

class BadRequestException(code: String, message: String) : ApplicationException(code, message)

class ResourceNotFoundException(code: String, message: String) : ApplicationException(code, message)

class ResourceConflictException(code: String, message: String) : ApplicationException(code, message)

class ValidationException(code: String, message: String) : ApplicationException(code, message)

