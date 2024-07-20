package com.droidablebee.gphoto

import groovy.json.JsonOutput
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import static com.droidablebee.gphoto.GooglePhotoService.AUTHORIZATION
import static com.droidablebee.gphoto.GooglePhotoService.BEARER
import static com.droidablebee.gphoto.GooglePhotoService.CONTENT_TYPE_JSON
import static com.droidablebee.gphoto.GooglePhotoService.ERROR
import static com.droidablebee.gphoto.GooglePhotoService.MEDIA_ITEMS
import static com.droidablebee.gphoto.GooglePhotoService.MEDIA_ITEMS_SEARCH
import static com.droidablebee.gphoto.GooglePhotoService.NEXT_PAGE_TOKEN
import static com.droidablebee.gphoto.GooglePhotoService.PAGE_SIZE
import static com.droidablebee.gphoto.GooglePhotoService.PAGE_TOKEN
import static com.droidablebee.gphoto.GooglePhotoService.ALBUMS
import static com.droidablebee.gphoto.GooglePhotoService.ALBUMS_MAX_PAGE_SIZE
import static com.droidablebee.gphoto.GooglePhotoService.CONTENT_TYPE
import static com.droidablebee.gphoto.GooglePhotoService.ITEMS_MAX_PAGE_SIZE

class GooglePhotoServiceSpec extends Specification {

    HttpClient httpClient = Mock()
    GooglePhotoService service = new GooglePhotoService(httpClient: httpClient)
    GooglePhotoService serviceSpy = Spy(service)

    HttpResponse<String> response1 = Mock()
    HttpResponse<String> response2 = Mock()

    def "get all albums"() {

        String token = "token"

        when:
        List found = service.getAllAlbums(token)

        then:
        call1 * httpClient.send({ HttpRequest request ->
            request.uri().toString() == service.getBaseUri() + "${ALBUMS}?${PAGE_SIZE}=${ALBUMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=" &&
                    request.headers().firstValue(AUTHORIZATION).get() == "${BEARER} ${token}"
        }, HttpResponse.BodyHandlers.ofString()) >> response1

        call2 * httpClient.send({ HttpRequest request ->
            request.uri().toString() == service.getBaseUri() + "${ALBUMS}?${PAGE_SIZE}=${ALBUMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=${data1[NEXT_PAGE_TOKEN]}" &&
                    request.headers().firstValue(AUTHORIZATION).get() == "${BEARER} ${token}"
        }, HttpResponse.BodyHandlers.ofString()) >> response2

        call1 * response1.body() >> { JsonOutput.toJson(data1) }
        call1 * response1.statusCode() >> statusCode
        call2 * response2.body() >> { JsonOutput.toJson(data2) }
        call2 * response2.statusCode() >> statusCode

        found == albums1 + albums2

        where:
        statusCode | albums1                                                    | albums2                                                    | call1 | call2 | data1                                                   | data2
        200        | []                                                         | []                                                         | 1     | 0     | [(ALBUMS): albums1]                                     | null
        200        | [[id: "1", title: "title 1", productUrl: "http://mock/1"]] | []                                                         | 1     | 0     | [(ALBUMS): albums1]                                     | null
        200        | [[id: "1", title: "title 1", productUrl: "http://mock/1"]] | [[id: "2", title: "title 2", productUrl: "http://mock/2"]] | 1     | 1     | [(ALBUMS): albums1, (NEXT_PAGE_TOKEN): "nextPageToken"] | [(ALBUMS): albums2]
    }

    def "get all albums - error"() {

        String token = "token"

        when:
        service.getAllAlbums(token)

        then:
        1 * httpClient.send(_ as HttpRequest, _ as HttpResponse.BodyHandler) >> response1
        1 * response1.body() >> JsonOutput.toJson(data)
        1 * response1.statusCode() >> statusCode

        HttpException exception = thrown(HttpException)
        exception.message == error.message
        exception.code == error.code
        exception.status == error.status

        where:
        statusCode | error                                                                                                    | data
        401        | [code: statusCode, message: "Request had invalid authentication credentials", status: "UNAUTHENTICATED"] | [(ERROR): error]
    }

    def "get items for album"() {

        String token = "token"
        String albumId = "albumId"
        String searchPayload1 = """{"mock": 1, "${PAGE_TOKEN}": ""}"""
        String searchPayload2 = """{"mock": 2, "${PAGE_TOKEN}": "nextPageToken"}"""

        when:
        List found = serviceSpy.getItemsForAlbum(albumId, token)

        then:
        call1 * serviceSpy.createItemsSearchPayload(albumId, "") >> searchPayload1
        call1 * httpClient.send({ HttpRequest request ->
            request.uri().toString() == service.getBaseUri() + MEDIA_ITEMS_SEARCH &&
                    request.headers().firstValue(AUTHORIZATION).get() == "${BEARER} ${token}" &&
                    request.headers().firstValue(CONTENT_TYPE).get() == CONTENT_TYPE_JSON &&
                    request.method() == "POST" &&
                    request.bodyPublisher().get().contentLength() == searchPayload1.length()
            // todo: assert json body somehow (instead of using content length): ${ALBUM_ID}=${albumId}&${PAGE_SIZE}=${ITEMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=
        }, HttpResponse.BodyHandlers.ofString()) >> response1

        call2 * serviceSpy.createItemsSearchPayload(albumId, data1[NEXT_PAGE_TOKEN]) >> searchPayload2
        call2 * httpClient.send({ HttpRequest request ->
            request.uri().toString() == service.getBaseUri() + MEDIA_ITEMS_SEARCH &&
                    request.headers().firstValue(AUTHORIZATION).get() == "${BEARER} ${token}" &&
                    request.headers().firstValue(CONTENT_TYPE).get() == CONTENT_TYPE_JSON &&
                    request.method() == "POST" &&
                    request.bodyPublisher().get().contentLength() == searchPayload2.length()
            // todo: assert json body somehow (instead of using content length): ${ALBUM_ID}=${albumId}&${PAGE_SIZE}=${ITEMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=${data1[NEXT_PAGE_TOKEN]}
        }, HttpResponse.BodyHandlers.ofString()) >> response2

        call1 * response1.body() >> { JsonOutput.toJson(data1) }
        call1 * response1.statusCode() >> statusCode
        call2 * response2.body() >> { JsonOutput.toJson(data2) }
        call2 * response2.statusCode() >> statusCode

        found == items1 + items2

        where:
        statusCode | items1                                                                 | items2                                                                 | call1 | call2 | data1                                                       | data2
        200        | []                                                                     | []                                                                     | 1     | 0     | [(MEDIA_ITEMS): items1]                                     | null
        200        | [[id: "1", description: "description 1", productUrl: "http://mock/1"]] | []                                                                     | 1     | 0     | [(MEDIA_ITEMS): items1]                                     | null
        200        | [[id: "1", description: "description 1", productUrl: "http://mock/1"]] | [[id: "2", description: "description 2", productUrl: "http://mock/2"]] | 1     | 1     | [(MEDIA_ITEMS): items1, (NEXT_PAGE_TOKEN): "nextPageToken"] | [(MEDIA_ITEMS): items2]
    }

    def "get items for album - error"() {

        String token = "token"
        String albumId = "albumId"

        when:
        service.getItemsForAlbum(albumId, token)

        then:
        1 * httpClient.send(_ as HttpRequest, _ as HttpResponse.BodyHandler) >> response1
        1 * response1.body() >> JsonOutput.toJson(data)
        1 * response1.statusCode() >> statusCode

        HttpException exception = thrown(HttpException)
        exception.message == error.message
        exception.code == error.code
        exception.status == error.status

        where:
        statusCode | error                                                                                                    | data
        401        | [code: statusCode, message: "Request had invalid authentication credentials", status: "UNAUTHENTICATED"] | [(ERROR): error]
    }

    def "get all items"() {

        String token = "token"

        when:
        List found = service.getAllItems(token)

        then:
        call1 * httpClient.send({ HttpRequest request ->
            request.uri().toString() == service.getBaseUri() + "${MEDIA_ITEMS}?${PAGE_SIZE}=${ITEMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=" &&
                    request.headers().firstValue(AUTHORIZATION).get() == "${BEARER} ${token}"
        }, HttpResponse.BodyHandlers.ofString()) >> response1

        call2 * httpClient.send({ HttpRequest request ->
            request.uri().toString() == service.getBaseUri() + "${MEDIA_ITEMS}?${PAGE_SIZE}=${ITEMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=${data1[NEXT_PAGE_TOKEN]}" &&
                    request.headers().firstValue(AUTHORIZATION).get() == "${BEARER} ${token}"
        }, HttpResponse.BodyHandlers.ofString()) >> response2

        call1 * response1.body() >> { JsonOutput.toJson(data1) }
        call1 * response1.statusCode() >> statusCode
        call2 * response2.body() >> { JsonOutput.toJson(data2) }
        call2 * response2.statusCode() >> statusCode

        found == items1 + items2

        where:
        statusCode | items1                                                                 | items2                                                                 | call1 | call2 | data1                                                       | data2
        200        | []                                                                     | []                                                                     | 1     | 0     | [(MEDIA_ITEMS): items1]                                     | null
        200        | [[id: "1", description: "description 1", productUrl: "http://mock/1"]] | []                                                                     | 1     | 0     | [(MEDIA_ITEMS): items1]                                     | null
        200        | [[id: "1", description: "description 1", productUrl: "http://mock/1"]] | [[id: "2", description: "description 2", productUrl: "http://mock/2"]] | 1     | 1     | [(MEDIA_ITEMS): items1, (NEXT_PAGE_TOKEN): "nextPageToken"] | [(MEDIA_ITEMS): items2]
    }

    def "get all items - error"() {

        String token = "token"

        when:
        service.getAllItems(token)

        then:
        1 * httpClient.send(_ as HttpRequest, _ as HttpResponse.BodyHandler) >> response1
        1 * response1.body() >> JsonOutput.toJson(data)
        1 * response1.statusCode() >> statusCode

        HttpException exception = thrown(HttpException)
        exception.message == error.message
        exception.code == error.code
        exception.status == error.status

        where:
        statusCode | error                                                                                                    | data
        401        | [code: statusCode, message: "Request had invalid authentication credentials", status: "UNAUTHENTICATED"] | [(ERROR): error]
    }
}
