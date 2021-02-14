package com.droidablebee.gphoto


import groovyx.net.http.HttpResponseDecorator

class GooglePhotoService extends BaseRestService {

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
    static final String BEARER = "Bearer"

    static final String BODY = "body"

    static final String QUERY = "query"
    static final String PAGE_SIZE = "pageSize"
    static final String PAGE_TOKEN = "pageToken"
    static final String ALBUM_ID = "albumId"

    @Override
    protected String getDefaultUri() {
        return 'https://photoslibrary.googleapis.com/v1/'
    }

    @Override
    protected String getApiClient() {
        return "gphoto-manager"
    }

    def getAllAlbums(String token) {

        int page = 0
        List albums = []
        String nextPageToken

        while (true) {
            print("Processing albums page: ${++page} ...")

            HttpResponseDecorator response = http.get(
                    (PATH): getDefaultUri() + ALBUMS,
                    (HEADERS): [(AUTHORIZATION): "${BEARER} ${token}"],
                    (QUERY): [(PAGE_SIZE): ALBUMS_MAX_PAGE_SIZE, (PAGE_TOKEN): nextPageToken]
            )

            if (response.status != 200) {
                throw new HttpException(response.data[ERROR])
            }

            List list = response.data[ALBUMS] ?: []
            println(" received: ${list.size()}")
            albums.addAll(list)
            nextPageToken = response.data[NEXT_PAGE_TOKEN]

            if (!nextPageToken) {
                break
            }
        }

        return albums
    }

    def getItemsForAlbum(String albumId, String token) {

        int page = 0
        List items = []
        String nextPageToken

        while (true) {
            print("Processing album items page: ${++page} ...")

            HttpResponseDecorator response = http.post(
                    (PATH): getDefaultUri() + MEDIA_ITEMS_SEARCH,
                    (HEADERS): [(AUTHORIZATION): "${BEARER} ${token}"],
                    (BODY): [(ALBUM_ID): albumId, (PAGE_SIZE): ITEMS_MAX_PAGE_SIZE, (PAGE_TOKEN): nextPageToken]
            )

            if (response.status != 200) {
                throw new HttpException(response.data[ERROR])
            }

            List list = response.data[MEDIA_ITEMS] ?: []
            println(" received: ${list.size()}")
            items.addAll(list)
            nextPageToken = response.data[NEXT_PAGE_TOKEN]

            if (!nextPageToken) {
                break
            }
        }

        return items
    }

    def getAllItems(String token) {

        int page = 0
        List items = []
        String nextPageToken

        while (true) {
            print("Processing media items page: ${++page} ...")

            HttpResponseDecorator response = http.get(
                    (PATH): getDefaultUri() + MEDIA_ITEMS,
                    (HEADERS): [(AUTHORIZATION): "${BEARER} ${token}"],
                    (QUERY): [(PAGE_SIZE): ITEMS_MAX_PAGE_SIZE, (PAGE_TOKEN): nextPageToken]
            )

            if (response.status != 200) {
                throw new HttpException(response.data[ERROR])
            }

            List list = response.data[MEDIA_ITEMS] ?: []
            println(" received: ${list.size()}")
            items.addAll(list)
            nextPageToken = response.data[NEXT_PAGE_TOKEN]

            if (!nextPageToken) {
                break
            }
        }

        return items
    }

}
