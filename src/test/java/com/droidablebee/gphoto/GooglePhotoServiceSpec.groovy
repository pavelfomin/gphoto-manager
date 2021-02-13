package com.droidablebee.gphoto

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import spock.lang.Specification
import spock.lang.Unroll

import static com.droidablebee.gphoto.GooglePhotoService.ALBUMS
import static com.droidablebee.gphoto.GooglePhotoService.ERROR

class GooglePhotoServiceSpec extends Specification {

    HTTPBuilder http = Mock()
    GooglePhotoService service = new GooglePhotoService(http: http)

    HttpResponse base = Mock()
    StatusLine statusLine = Mock()

    void setup() {

        base.statusLine >> statusLine
    }

    @Unroll
    def "get all albums"() {

        HttpResponseDecorator response = new HttpResponseDecorator(base, data)

        when:
        List found = service.getAllAlbums()

        then:
        found == albums

        1 * http.get(_ as Map) >> response
        1 * statusLine.statusCode >> statusCode

        where:
        statusCode | albums                                                     | data
        200        | []                                                         | [(ALBUMS): albums]
        200        | [[id: "1", title: "title 1", productUrl: "http://mock/1"]] | [(ALBUMS): albums]
    }

    @Unroll
    def "get all albums - error"() {

        HttpResponseDecorator response = new HttpResponseDecorator(base, data)

        when:
        service.getAllAlbums()

        then:
        HttpException exception = thrown(HttpException)
        exception.message == error.message
        exception.code == error.code
        exception.status == error.status

        1 * http.get(_ as Map) >> response
        1 * statusLine.statusCode >> statusCode

        where:
        statusCode | error                                                                                                    | data
        401        | [code: statusCode, message: "Request had invalid authentication credentials", status: "UNAUTHENTICATED"] | [(ERROR): error]
    }

}
