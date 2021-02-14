package com.droidablebee.gphoto

import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator

class GooglePhotoService extends BaseRestService {

    static final String OPT_ALBUMS = "--albums"
    static final String OPT_ALBUM_ITEMS = "--album-items"
    static final String OPT_ITEMS = "--items"
    static final String OPT_ITEMS_NO_ALBUM = "--items-no-album"
    static final List VALID_OPTIONS = [OPT_ALBUMS, OPT_ALBUM_ITEMS, OPT_ITEMS, OPT_ITEMS_NO_ALBUM]

    static final String OPT_TOKEN = "token"

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

    static void main(String[] args) {

        if (!args) {
            usage()
            System.exit(1)
        }

        if (!VALID_OPTIONS.containsAll(args)) {
            System.err.println("Invalid options: ${args}")
            usage()
            System.exit(1)
        }

        String token = getToken()
        if (!token) {
            System.err.println("Access token is required")
            usage()
            System.exit(1)
        }

        GooglePhotoService service = new GooglePhotoService()
        service.process(args.toList(), token)
    }

    static void usage() {

        System.err.println("""Usage: ${GooglePhotoService.name} [option] -Dtoken=<access token>
            ${OPT_ALBUMS}           list all albums, sorted by name
            ${OPT_ALBUM_ITEMS}      list all items per album (use with care for large number of media items)
            ${OPT_ITEMS}            list all items (use with care for large number of media items)
            ${OPT_ITEMS_NO_ALBUM}   list all items not included in any album
            <access token>          OAUTH2 Access Token is obtained from https://developers.google.com/oauthplayground
                                    use 'https://www.googleapis.com/auth/photoslibrary.readonly' as the requested scope
        """
        )
    }

    @Override
    protected String getDefaultUri() {
        return 'https://photoslibrary.googleapis.com/v1/'
    }

    @Override
    protected String getApiClient() {
        return "gphoto-manager"
    }

    def "process"(List args, String token) {

        List albums

        if (args.contains(OPT_ALBUMS) || args.contains(OPT_ALBUM_ITEMS) || args.contains(OPT_ITEMS_NO_ALBUM)) {
            albums = getAllAlbums(token)
            int totalItems = albums.inject(0) { count, item -> count + Integer.valueOf(item['mediaItemsCount']) }
            List sorted = albums.sort({ Map album -> album.title })

            sorted.eachWithIndex { Map album, int index ->
                album.mediaItems = []

                println("Album: ${album.title}, items: ${album.mediaItemsCount} url: ${album.productUrl}")

                if (args.contains(OPT_ALBUM_ITEMS) || args.contains(OPT_ITEMS_NO_ALBUM)) {
                    List items = getItemsForAlbum(album.id, token)
                    println("\t total items received for album: ${items.size()}")
                    if (items.size() != Integer.valueOf(album.mediaItemsCount)) {
                        System.err.println("\t\t Warning: total items received does not match album media count")
                    }

                    album.mediaItems = items

                    if (args.contains(OPT_ALBUM_ITEMS)) {
                        items.each { Map item ->
                            println(item)
                        }
                    }
                }
            }
            println("Total albums: ${albums.size()} total album items: ${totalItems}")
        }

        List items
        if (args.contains(OPT_ITEMS) || args.contains(OPT_ITEMS_NO_ALBUM)) {
            items = getAllItems(token)
            println("Total media items: ${items.size()}")
            if (args.contains(OPT_ITEMS)) {
                items.each { Map item ->
                    println(item)
                }
            }
        }

        if (!args.contains(OPT_ITEMS_NO_ALBUM)) {
            return
        }

        //map items
        Map itemsMap = items.collectEntries { Map item ->
            item.albums = []
            [item.id, item]
        }

        //add corresponding albums found for each media item
        albums.each { Map album ->
            album.mediaItems.each { Map item ->
                Map foundItem = itemsMap[item.id]
                if (foundItem) {
                    itemsMap[item.id].albums << album
                } else {
                    System.err.println("Warning: item: ${item} from album ${album.title}: ${album.productUrl} not found in the list of media items. Probably shared item from another account.")
                }
            }
        }

        Map itemsWithoutAlbums = itemsMap.findAll { String key, Map item ->
            !item.albums
        }

        println("Media items w/out albums: ${itemsWithoutAlbums.size()}")
        itemsWithoutAlbums.each { String key, Map item ->
            println(item.productUrl)
        }
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
        int pageSize = 100
        List items = []
        String nextPageToken

        while (true) {
            print("Processing media items page: ${++page} ...")
            HttpResponseDecorator response = http.get(
                    requestContentType: ContentType.JSON,
                    path: getDefaultUri() + "mediaItems",
                    headers: [Authorization: "Bearer ${token}"],
                    query: [pageSize: pageSize, pageToken: nextPageToken]
            )

            assert response.status == 200: response.data[ERROR]
            List list = response.data['mediaItems'] ?: []
            println(" received: ${list.size()}")
            items.addAll(list)
            nextPageToken = response.data[NEXT_PAGE_TOKEN]

            if (!nextPageToken) {
                break
            }
        }

        return items
    }

    static def getToken() {

        return System.getProperty(OPT_TOKEN)
    }
}
