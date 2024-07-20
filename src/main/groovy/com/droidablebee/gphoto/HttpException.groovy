package com.droidablebee.gphoto

class HttpException extends Exception {

    String message
    Integer code
    String status

    @Override
    String toString() {
        return [code: code, message: message, status: status].toString()
    }
}
