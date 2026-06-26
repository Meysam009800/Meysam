package javanepoya.ir.cloudpanel.api

import java.io.IOException

open class ApiException(message: String, val code: Int) : IOException(message)

class UnauthorizedException(message: String) : ApiException(message, 401)

class ForbiddenException(message: String) : ApiException(message, 403)

class RateLimitException(message: String) : ApiException(message, 429)

class NetworkException(message: String, cause: Throwable) : IOException(message, cause)
