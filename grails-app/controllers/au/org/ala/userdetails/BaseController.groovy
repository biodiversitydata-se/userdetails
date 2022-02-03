package au.org.ala.userdetails

import grails.converters.JSON

import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR
import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_OK

class BaseController {
    public static final String CONTEXT_TYPE_JSON = "application/json"

    protected def notFound(String message = null) {
        sendError(SC_NOT_FOUND, message ?: "")
    }

    protected def badRequest(String message = null) {
        sendError(SC_BAD_REQUEST, message ?: "")
    }

    protected def success(resp) {
        response.status = SC_OK
        response.setContentType(CONTEXT_TYPE_JSON)
        render resp as JSON
    }

    protected def saveFailed() {
        sendError(SC_INTERNAL_SERVER_ERROR)
    }

    protected def sendError(int status, String msg = null) {
        response.status = status
        response.sendError(status, msg)
    }
}
