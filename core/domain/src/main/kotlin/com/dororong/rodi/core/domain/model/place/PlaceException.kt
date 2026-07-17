package com.dororong.rodi.core.domain.model.place

sealed class PlaceException(message: String) : RuntimeException(message) {
    class AuthenticationRequired(message: String) : PlaceException(message)
    class NotFound(message: String) : PlaceException(message)
    class Network(message: String) : PlaceException(message)
    class Unexpected(message: String) : PlaceException(message)
}
