package com.droidablebee.gphoto

class GooglePhotoApplication {

    static final String OPT_ALBUMS = "--albums"
    static final String OPT_ALBUM_ITEMS = "--album-items"
    static final String OPT_ITEMS = "--items"
    static final String OPT_ITEMS_NO_ALBUM = "--items-no-album"
    static final List VALID_OPTIONS = [OPT_ALBUMS, OPT_ALBUM_ITEMS, OPT_ITEMS, OPT_ITEMS_NO_ALBUM]

    static final String OPT_TOKEN = "token"

    static final String ID = "id"
    static final String TITLE = "title"
    static final String PRODUCT_URL = "productUrl"
    static final String MEDIA_ITEMS_COUNT = "mediaItemsCount"
    static final String MEDIA_ITEMS = GooglePhotoService.MEDIA_ITEMS

    GooglePhotoService service = new GooglePhotoService()

    static void main(String[] args) {

        GooglePhotoApplication application = new GooglePhotoApplication()
        application.process(args.toList())
    }

    def usage() {

        System.err.println("""Usage: ${GooglePhotoApplication.name} [option] -Dtoken=<access token>
            ${OPT_ALBUMS}           list all albums, sorted by name
            ${OPT_ALBUM_ITEMS}      list all items per album (use with care for large number of media items)
            ${OPT_ITEMS}            list all items (use with care for large number of media items)
            ${OPT_ITEMS_NO_ALBUM}   list all items not included in any album
            <access token>          OAuth 2 Access Token can be obtained from https://developers.google.com/oauthplayground
                                    use 'https://www.googleapis.com/auth/photoslibrary.readonly' as the requested scope
        """
        )
        System.exit(1)
    }

    def "process"(List args) {

        if (!args) {
            usage()
            return
        }

        if (!VALID_OPTIONS.containsAll(args)) {
            System.err.println("Invalid options: ${args}")
            usage()
            return
        }

        String token = getToken()
        if (!token) {
            System.err.println("Access token is required")
            usage()
            return
        }

        process(args, token)
    }

    def "process"(List args, String token) {

        List albums

        if (args.contains(OPT_ALBUMS) || args.contains(OPT_ALBUM_ITEMS) || args.contains(OPT_ITEMS_NO_ALBUM)) {
            albums = processAlbums(
                    token,
                    args.contains(OPT_ALBUM_ITEMS) || args.contains(OPT_ITEMS_NO_ALBUM),
                    args.contains(OPT_ALBUM_ITEMS)
            )
        }

        List items
        if (args.contains(OPT_ITEMS) || args.contains(OPT_ITEMS_NO_ALBUM)) {
            items = service.getAllItems(token)
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

    List processAlbums(String token, boolean retrieveAlbumItems, boolean logAlbumItems) {

        List albums = service.getAllAlbums(token)
        int totalItems = albums.inject(0) { count, album ->
            count + (album[MEDIA_ITEMS_COUNT] ? Integer.valueOf(album[MEDIA_ITEMS_COUNT]) : 0)
        }
        List sorted = albums.sort({ Map album -> album[TITLE] })

        sorted.eachWithIndex { Map album, int index ->
            album[MEDIA_ITEMS] = []

            logAlbum(album)

            if (retrieveAlbumItems) {
                List items = service.getItemsForAlbum(album[ID], token)
                logAlbumItemsReceived(items)
                if (items.size() != Integer.valueOf(album[MEDIA_ITEMS_COUNT] ? Integer.valueOf(album[MEDIA_ITEMS_COUNT]) : 0)) {
                    logAlbumItemsCountMismatch()
                }

                album[MEDIA_ITEMS] = items

                if (logAlbumItems) {
                    logAlbumMediaItems(items)
                }
            }
        }
        println("Total albums: ${albums.size()} total album items: ${totalItems}")

        return albums
    }

    String getToken() {

        return System.getProperty(OPT_TOKEN)
    }

    def logAlbumMediaItems(List items) {

        items.each { Map item ->
            println(item)
        }
    }

    def logAlbumItemsCountMismatch() {

        System.err.println("\t\t Warning: total items received does not match album media count")
    }

    def logAlbumItemsReceived(List items) {

        println("\t total items received for album: ${items.size()}")
    }

    def logAlbum(Map album) {

        println("Album: ${album[TITLE]}, items: ${album[MEDIA_ITEMS_COUNT]} url: ${album[PRODUCT_URL]}")
    }

}
