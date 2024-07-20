package com.droidablebee.gphoto

import groovy.json.JsonOutput
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import static com.droidablebee.gphoto.GooglePhotoService.ALBUMS
import static com.droidablebee.gphoto.GooglePhotoService.ALBUMS_MAX_PAGE_SIZE
import static com.droidablebee.gphoto.GooglePhotoService.AUTHORIZATION
import static com.droidablebee.gphoto.GooglePhotoService.BEARER
import static com.droidablebee.gphoto.GooglePhotoService.NEXT_PAGE_TOKEN
import static com.droidablebee.gphoto.GooglePhotoService.PAGE_SIZE
import static com.droidablebee.gphoto.GooglePhotoService.PAGE_TOKEN

class GooglePhotoServiceSpec extends Specification {

    HttpClient httpClient = Mock()
    GooglePhotoService service = new GooglePhotoService(httpClient: httpClient)

    HttpResponse<String> response1 = Mock()
    HttpResponse<String> response2 = Mock()

    def "get all albums"() {

        String token = "token"

        when:
        List found = service.getAllAlbums(token)

        then:
        found == albums1 + albums2

        call1 * httpClient.send({ HttpRequest request ->
            request.uri().toString() == service.getDefaultUri() + "${ALBUMS}?${PAGE_SIZE}=${ALBUMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=" &&
                    request.headers().firstValue(AUTHORIZATION).get() == "${BEARER} ${token}"
        }, HttpResponse.BodyHandlers.ofString()) >> response1

        call2 * httpClient.send({ HttpRequest request ->
            request.uri().toString() == service.getDefaultUri() + "${ALBUMS}?${PAGE_SIZE}=${ALBUMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=${data1[NEXT_PAGE_TOKEN]}" &&
                    request.headers().firstValue(AUTHORIZATION).get() == "${BEARER} ${token}"
        }, HttpResponse.BodyHandlers.ofString()) >> response2

        call1 * response1.body() >> JsonOutput.toJson(data1)
        call1 * response1.statusCode() >> statusCode
        call2 * response2.body() >> JsonOutput.toJson(data2)
        call2 * response2.statusCode() >> statusCode

        where:
        statusCode | albums1                                                    | albums2                                                    | call1 | call2 | data1                                                   | data2
        200        | []                                                         | []                                                         | 1     | 0     | [(ALBUMS): albums1]                                     | null
        200        | [[id: "1", title: "title 1", productUrl: "http://mock/1"]] | []                                                         | 1     | 0     | [(ALBUMS): albums1]                                     | null
        200        | [[id: "1", title: "title 1", productUrl: "http://mock/1"]] | [[id: "2", title: "title 2", productUrl: "http://mock/2"]] | 1     | 1     | [(ALBUMS): albums1, (NEXT_PAGE_TOKEN): "nextPageToken"] | [(ALBUMS): albums2]
    }

/*
    def "get all albums - error"() {

        String token = "token"
        HttpResponseDecorator response = new HttpResponseDecorator(base, data)

        when:
        service.getAllAlbums(token)

        then:
        HttpException exception = thrown(HttpException)
        exception.message == error.message
        exception.code == error.code
        exception.status == error.status

        1 * httpClient.get(_ as Map) >> response
        1 * statusLine.statusCode >> statusCode

        where:
        statusCode | error                                                                                                    | data
        401        | [code: statusCode, message: "Request had invalid authentication credentials", status: "UNAUTHENTICATED"] | [(ERROR): error]
    }


    def "get items for album"() {

        String token = "token"
        String albumId = "albumId"

        when:
        List found = service.getItemsForAlbum(albumId, token)

        then:
        found == items1 + items2

//        calls * http.post(_ as Map) >>> new HttpResponseDecorator(base, data1) >> new HttpResponseDecorator(base, data2)

        call1 * httpClient.post({ Map params ->
            params[PATH] == service.getDefaultUri() + MEDIA_ITEMS_SEARCH &&
                    params[HEADERS][AUTHORIZATION] == "${BEARER} ${token}" &&
                    params[BODY][ALBUM_ID] == albumId &&
                    params[BODY][PAGE_SIZE] == ITEMS_MAX_PAGE_SIZE &&
                    params[BODY][PAGE_TOKEN] == null
        }) >> new HttpResponseDecorator(base, data1)

        call2 * httpClient.post({ Map params ->
            params[PATH] == service.getDefaultUri() + MEDIA_ITEMS_SEARCH &&
                    params[HEADERS][AUTHORIZATION] == "${BEARER} ${token}" &&
                    params[BODY][ALBUM_ID] == albumId &&
                    params[BODY][PAGE_SIZE] == ITEMS_MAX_PAGE_SIZE &&
                    params[BODY][PAGE_TOKEN] == data1[NEXT_PAGE_TOKEN]
        }) >> new HttpResponseDecorator(base, data2)

        (call1 + call2) * statusLine.statusCode >> statusCode

        where:
        statusCode | items1                                                                 | items2                                                                 | call1 | call2 | data1                                                       | data2
        200        | []                                                                     | []                                                                     | 1     | 0     | [(MEDIA_ITEMS): items1]                                     | null
        200        | [[id: "1", description: "description 1", productUrl: "http://mock/1"]] | []                                                                     | 1     | 0     | [(MEDIA_ITEMS): items1]                                     | null
        200        | [[id: "1", description: "description 1", productUrl: "http://mock/1"]] | [[id: "2", description: "description 2", productUrl: "http://mock/2"]] | 1     | 1     | [(MEDIA_ITEMS): items1, (NEXT_PAGE_TOKEN): "nextPageToken"] | [(MEDIA_ITEMS): items2]
    }


    def "get items for album - error"() {

        String token = "token"
        String albumId = "albumId"
        HttpResponseDecorator response = new HttpResponseDecorator(base, data)

        when:
        service.getItemsForAlbum(albumId, token)

        then:
        HttpException exception = thrown(HttpException)
        exception.message == error.message
        exception.code == error.code
        exception.status == error.status

        1 * httpClient.post(_ as Map) >> response
        1 * statusLine.statusCode >> statusCode

        where:
        statusCode | error                                                                                                    | data
        401        | [code: statusCode, message: "Request had invalid authentication credentials", status: "UNAUTHENTICATED"] | [(ERROR): error]
    }


    def "get all items"() {

        String token = "token"

        when:
        List found = service.getAllItems(token)

        then:
        found == items1 + items2

//        calls * http.get(_ as Map) >>> new HttpResponseDecorator(base, data1) >> new HttpResponseDecorator(base, data2)

        call1 * httpClient.get({ Map params ->
            params[PATH] == service.getDefaultUri() + MEDIA_ITEMS &&
                    params[HEADERS][AUTHORIZATION] == "${BEARER} ${token}" &&
                    params[QUERY][PAGE_SIZE] == ITEMS_MAX_PAGE_SIZE &&
                    params[QUERY][PAGE_TOKEN] == null
        }) >> new HttpResponseDecorator(base, data1)

        call2 * httpClient.get({ Map params ->
            params[PATH] == service.getDefaultUri() + MEDIA_ITEMS &&
                    params[HEADERS][AUTHORIZATION] == "${BEARER} ${token}" &&
                    params[QUERY][PAGE_SIZE] == ITEMS_MAX_PAGE_SIZE &&
                    params[QUERY][PAGE_TOKEN] == data1[NEXT_PAGE_TOKEN]
        }) >> new HttpResponseDecorator(base, data2)

        (call1 + call2) * statusLine.statusCode >> statusCode

        where:
        statusCode | items1                                                                 | items2                                                                 | call1 | call2 | data1                                                       | data2
        200        | []                                                                     | []                                                                     | 1     | 0     | [(MEDIA_ITEMS): items1]                                     | null
        200        | [[id: "1", description: "description 1", productUrl: "http://mock/1"]] | []                                                                     | 1     | 0     | [(MEDIA_ITEMS): items1]                                     | null
        200        | [[id: "1", description: "description 1", productUrl: "http://mock/1"]] | [[id: "2", description: "description 2", productUrl: "http://mock/2"]] | 1     | 1     | [(MEDIA_ITEMS): items1, (NEXT_PAGE_TOKEN): "nextPageToken"] | [(MEDIA_ITEMS): items2]
    }


    def "get all items - error"() {

        String token = "token"
        HttpResponseDecorator response = new HttpResponseDecorator(base, data)

        when:
        service.getAllItems(token)

        then:
        HttpException exception = thrown(HttpException)
        exception.message == error.message
        exception.code == error.code
        exception.status == error.status

        1 * httpClient.get(_ as Map) >> response
        1 * statusLine.statusCode >> statusCode

        where:
        statusCode | error                                                                                                    | data
        401        | [code: statusCode, message: "Request had invalid authentication credentials", status: "UNAUTHENTICATED"] | [(ERROR): error]
    }
    */
}
