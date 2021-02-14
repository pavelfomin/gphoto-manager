package com.droidablebee.gphoto

import spock.lang.Specification
import spock.lang.Unroll

import static com.droidablebee.gphoto.GooglePhotoApplication.ID
import static com.droidablebee.gphoto.GooglePhotoApplication.MEDIA_ITEMS
import static com.droidablebee.gphoto.GooglePhotoApplication.MEDIA_ITEMS_COUNT
import static com.droidablebee.gphoto.GooglePhotoApplication.OPT_ALBUMS
import static com.droidablebee.gphoto.GooglePhotoApplication.OPT_ALBUM_ITEMS
import static com.droidablebee.gphoto.GooglePhotoApplication.OPT_ITEMS
import static com.droidablebee.gphoto.GooglePhotoApplication.OPT_ITEMS_NO_ALBUM
import static com.droidablebee.gphoto.GooglePhotoApplication.VALID_OPTIONS

class GooglePhotoApplicationSpec extends Specification {

    GooglePhotoService service = Mock()
    GooglePhotoApplication application = new GooglePhotoApplication(service: service)

    @Unroll
    def "process options"() {

        GooglePhotoApplication application = Spy(application)

        when:
        application.process(args)

        then:
        usageCall * application.usage() >> {}
        tokenCall * application.getToken() >> token
        processCall * application.process(args, token) >> {}

        where:
        args          | token   | usageCall | tokenCall | processCall
        []            | null    | 1         | 0         | 0
        ["invalid"]   | null    | 1         | 0         | 0
        [OPT_ALBUMS]  | null    | 1         | 1         | 0
        [OPT_ALBUMS]  | "token" | 0         | 1         | 1
        VALID_OPTIONS | "token" | 0         | 1         | 1
    }

    @Unroll
    def "process with valid args and token specified"() {

        GooglePhotoApplication application = Spy(application)
        String token = "token"

        when:
        application.process(args, token)

        then:
        albumsCall * application.processAlbums(token, retrieveAlbumItems, logAlbumItems) >> albums
        itemsCall * service.getAllItems(token) >> items
        logMediaItemsSummary * application.logMediaItemsSummary(items)
        logMediaItems * application.logMediaItems(items)
        logAlbumItemNotFound * application.logAlbumItemNotFound(_, _)
        logItemsWithoutAlbumsSummary * application.logItemsWithoutAlbumsSummary(_)
        logItemsWithoutAlbums * application.logItemsWithoutAlbums(_)

        where:
        args                                             | albumsCall | albums                                                             | retrieveAlbumItems | logAlbumItems | itemsCall | items            | logMediaItemsSummary | logMediaItems | logAlbumItemNotFound | logItemsWithoutAlbumsSummary | logItemsWithoutAlbums
        []                                               | 0          | []                                                                 | false              | false         | 0         | []               | 0                    | 0             | 0                    | 0                            | 0
        [OPT_ALBUMS]                                     | 1          | []                                                                 | false              | false         | 0         | []               | 0                    | 0             | 0                    | 0                            | 0
        [OPT_ITEMS_NO_ALBUM]                             | 1          | [[(ID): "id1"]]                                                    | true               | false         | 1         | []               | 1                    | 0             | 0                    | 1                            | 0
        [OPT_ITEMS_NO_ALBUM, OPT_ALBUM_ITEMS]            | 1          | [[(ID): "id1"]]                                                    | true               | true          | 1         | []               | 1                    | 0             | 0                    | 1                            | 0
        [OPT_ITEMS]                                      | 0          | []                                                                 | false              | false         | 1         | []               | 1                    | 1             | 0                    | 0                            | 0
        [OPT_ITEMS_NO_ALBUM, OPT_ITEMS]                  | 1          | [[(ID): "id1"]]                                                    | true               | false         | 1         | []               | 1                    | 1             | 0                    | 1                            | 0
        [OPT_ITEMS_NO_ALBUM, OPT_ALBUM_ITEMS, OPT_ITEMS] | 1          | [[(ID): "id1"]]                                                    | true               | true          | 1         | []               | 1                    | 1             | 0                    | 1                            | 0
        [OPT_ITEMS_NO_ALBUM]                             | 1          | [[(ID): "id1", (MEDIA_ITEMS): [[(ID): "mine"]]]]                   | true               | false         | 1         | [[(ID): "mine"]] | 1                    | 0             | 0                    | 1                            | 0
        [OPT_ITEMS_NO_ALBUM]                             | 1          | [[(ID): "id1", (MEDIA_ITEMS): [[(ID): "shared"], [(ID): "mine"]]]] | true               | false         | 1         | [[(ID): "mine"]] | 1                    | 0             | 1                    | 1                            | 0
        [OPT_ITEMS_NO_ALBUM]                             | 1          | [[(ID): "id1", (MEDIA_ITEMS): [[(ID): "shared"]]]]                 | true               | false         | 1         | [[(ID): "mine"]] | 1                    | 0             | 1                    | 1                            | 1
    }

    @Unroll
    def "process albums"() {

        GooglePhotoApplication application = Spy(application)
        String token

        when:
        List found = application.processAlbums(token, retrieveAlbumItems, logAlbumItems)

        then:
        found == albums
        if (found) {
            found.each { Map album ->
                assert album[MEDIA_ITEMS] == albumItems
            }
        }
        1 * service.getAllAlbums(token) >> albums
        (albums.size()) * application.logAlbum(_)
        albumItemCall * service.getItemsForAlbum(_, token) >> albumItems
        albumItemCall * application.logAlbumItemsReceived(albumItems)
        logItemsCountMismatch * application.logAlbumItemsCountMismatch()
        logAlbumMediaItems * application.logMediaItems(albumItems)
        1 * application.logAlbumsSummary(albums, _)
//        albumItemCall * service.getItemsForAlbum({ String albumId ->
//                albums[0][ID] == albumId
//        }, token) >> albumItems
//        albumItemCall * service.getItemsForAlbum({ String albumId ->
//            albums.find { Map album ->
//                album[ID] == albumId
//            }
//        }, token) >> albumItems

        where:
        retrieveAlbumItems | logAlbumItems | albums                                  | albumItemCall | albumItems      | logItemsCountMismatch | logAlbumMediaItems
        false              | false         | []                                      | 0             | []              | 0                     | 0
        true               | false         | []                                      | 0             | []              | 0                     | 0
        true               | false         | [[(ID): "id1"]]                         | 1             | []              | 0                     | 0
        true               | false         | [[(ID): "id1", (MEDIA_ITEMS_COUNT): 1]] | 1             | []              | 1                     | 0
        true               | true          | [[(ID): "id1", (MEDIA_ITEMS_COUNT): 1]] | 1             | []              | 1                     | 1
        true               | true          | [[(ID): "id1", (MEDIA_ITEMS_COUNT): 1]] | 1             | [[(ID): "idx"]] | 0                     | 1
    }

}
