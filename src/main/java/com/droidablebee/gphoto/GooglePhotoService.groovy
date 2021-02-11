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
            <access token>          OAUTH2 Access Token obtained from https://developers.google.com/oauthplayground
        """
        )
    }

    @Override
    protected String getDefaultUri() {
        return 'https://photoslibrary.googleapis.com/v1'
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
                    System.err.println("Warning: item: ${item} from album ${album.title}: ${album.productUrl} not found")
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
        int pageSize = 50
        List albums = []
        String nextPageToken

        while (true) {
            print("Processing albums page: ${++page} ...")
            HttpResponseDecorator response = http.get(
                    requestContentType: ContentType.JSON,
                    path: getDefaultUri() + "/albums",
                    headers: [Authorization: "Bearer ${token}"],
                    query: [pageSize: pageSize, pageToken: nextPageToken]
            )

            assert response.status == 200: response.data['error']
            List list = response.data['albums'] ?: []
            println(" received: ${list.size()}")
            albums.addAll(list)
            nextPageToken = response.data['nextPageToken']

            if (!nextPageToken) {
                break
            }
        }

        return albums
    }

    def getItemsForAlbum(String albumId, String token) {

        int page = 0
        int pageSize = 100
        List items = []
        String nextPageToken

        while (true) {
            print("Processing album items page: ${++page} ...")
            HttpResponseDecorator response = http.post(
                    requestContentType: ContentType.JSON,
                    path: getDefaultUri() + "/mediaItems:search",
                    headers: [Authorization: "Bearer ${token}"],
                    body: [albumId: albumId, pageSize: pageSize, pageToken: nextPageToken]
            )

            assert response.status == 200: response.data['error']
            List list = response.data['mediaItems'] ?: []
            println(" received: ${list.size()}")
            items.addAll(list)
            nextPageToken = response.data['nextPageToken']

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
                    path: getDefaultUri() + "/mediaItems",
                    headers: [Authorization: "Bearer ${token}"],
                    query: [pageSize: pageSize, pageToken: nextPageToken]
            )

            assert response.status == 200: response.data['error']
            List list = response.data['mediaItems'] ?: []
            println(" received: ${list.size()}")
            items.addAll(list)
            nextPageToken = response.data['nextPageToken']

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
