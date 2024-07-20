package com.droidablebee.gphoto

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GooglePhotoService {

    static final int ALBUMS_MAX_PAGE_SIZE = 50
    static final int ITEMS_MAX_PAGE_SIZE = 100

    static final String ALBUMS = "albums"
    static final String MEDIA_ITEMS = "mediaItems"
    static final String MEDIA_ITEMS_SEARCH = "${MEDIA_ITEMS}:search"

    static final String ERROR = "error"
    static final String NEXT_PAGE_TOKEN = "nextPageToken"

    static final String HEADERS = "headers"
    static final String PATH = "path"
    static final String AUTHORIZATION = "Authorization"
    static final String CONTENT_TYPE_JSON = "Content-Type"
    static final String BEARER = "Bearer"

    static final String BODY = "body"

    static final String QUERY = "query"
    static final String PAGE_SIZE = "pageSize"
    static final String PAGE_TOKEN = "pageToken"
    static final String ALBUM_ID = "albumId"

    protected HttpClient httpClient = HttpClient.newBuilder().build()


    protected String getDefaultUri() {
        return 'https://photoslibrary.googleapis.com/v1/'
    }

    protected String getApiClient() {
        return "gphoto-manager"
    }

    protected HttpClient getHttpClient() {

        return httpClient
    }

    def getAllAlbums(String token) {

        int page = 0
        List albums = []
        String nextPageToken = ""

        HttpClient client = getHttpClient()

        while (true) {
            print("Processing albums page: ${++page} ...")

            URI uri = URI.create(getDefaultUri() + "${ALBUMS}?${PAGE_SIZE}=${ALBUMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=${nextPageToken}")
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .header(AUTHORIZATION, "${BEARER} $token")
                    .build()

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
            Map content = new JsonSlurper().parseText(response.body())

            if (response.statusCode() != 200) {
                throw new HttpException(content[ERROR])
            }

            List list = content[ALBUMS] ?: []
            println(" received: ${list.size()}")
            albums.addAll(list)
            nextPageToken = content[NEXT_PAGE_TOKEN]

            if (!nextPageToken) {
                break
            }
        }

        return albums
    }

    def getItemsForAlbum(String albumId, String token) {

        int page = 0
        List items = []
        String nextPageToken = ""

        HttpClient client = getHttpClient()

        while (true) {
            print("Processing album items page: ${++page} ...")

            String json = JsonOutput.toJson([(ALBUM_ID): albumId, (PAGE_SIZE): ITEMS_MAX_PAGE_SIZE, (PAGE_TOKEN): nextPageToken])

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getDefaultUri() + MEDIA_ITEMS_SEARCH))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header(AUTHORIZATION, "${BEARER} $token")
                    .header(CONTENT_TYPE_JSON, "application/json")
                    .build()

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
            Map content = new JsonSlurper().parseText(response.body())

            if (response.statusCode() != 200) {
                throw new HttpException(content[ERROR])
            }

            List list = content[MEDIA_ITEMS] ?: []
            println(" received: ${list.size()}")
            items.addAll(list)
            nextPageToken = content[NEXT_PAGE_TOKEN]

            if (!nextPageToken) {
                break
            }
        }

        return items
    }

    def getAllItems(String token) {

        int page = 0
        List items = []
        String nextPageToken = ""

        HttpClient client = getHttpClient()

        while (true) {
            print("Processing media items page: ${++page} ...")

            URI uri = URI.create(getDefaultUri() + "${MEDIA_ITEMS}?${PAGE_SIZE}=${ITEMS_MAX_PAGE_SIZE}&${PAGE_TOKEN}=${nextPageToken}")
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .header(AUTHORIZATION, "${BEARER} $token")
                    .build()

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
            Map content = new JsonSlurper().parseText(response.body())

            if (response.statusCode() != 200) {
                throw new HttpException(content[ERROR])
            }

            List list = content[MEDIA_ITEMS] ?: []
            println(" received: ${list.size()}")
            items.addAll(list)
            nextPageToken = content[NEXT_PAGE_TOKEN]

            if (!nextPageToken) {
                break
            }
        }

        return items
    }

}