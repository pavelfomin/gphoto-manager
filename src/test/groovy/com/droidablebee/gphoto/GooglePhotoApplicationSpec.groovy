package com.droidablebee.gphoto

import spock.lang.Specification

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
    GooglePhotoApplication application = Spy(new GooglePhotoApplication(service: service))

    
    def "process options"() {

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

    
    def "process with valid args and token specified"() {

        String token = "token"

        when:
        application.process(args, token)

        then:
        1 * application.suppressWarnings() >> suppressWarnings
        albumsCall * application.processAlbums(token, retrieveAlbumItems, logAlbumItems, suppressWarnings) >> albums
        itemsCall * service.getAllItems(token) >> items
        logMediaItemsSummary * application.logMediaItemsSummary(items)
        logMediaItems * application.logMediaItems(items)
        logAlbumItemNotFound * application.logAlbumItemNotFound(_, _)
        logItemsWithoutAlbumsSummary * application.logItemsWithoutAlbumsSummary(_)
        logItemsWithoutAlbums * application.logItemsWithoutAlbums(_)

        where:
        args                                             | albumsCall | albums                                                             | retrieveAlbumItems | logAlbumItems | itemsCall | items            | logMediaItemsSummary | logMediaItems | logAlbumItemNotFound | logItemsWithoutAlbumsSummary | logItemsWithoutAlbums | suppressWarnings
        []                                               | 0          | []                                                                 | false              | false         | 0         | []               | 0                    | 0             | 0                    | 0                            | 0                     | false
        [OPT_ALBUMS]                                     | 1          | []                                                                 | false              | false         | 0         | []               | 0                    | 0             | 0                    | 0                            | 0                     | false
        [OPT_ITEMS_NO_ALBUM]                             | 1          | [[(ID): "id1"]]                                                    | true               | false         | 1         | []               | 1                    | 0             | 0                    | 1                            | 0                     | false
        [OPT_ITEMS_NO_ALBUM, OPT_ALBUM_ITEMS]            | 1          | [[(ID): "id1"]]                                                    | true               | true          | 1         | []               | 1                    | 0             | 0                    | 1                            | 0                     | false
        [OPT_ITEMS]                                      | 0          | []                                                                 | false              | false         | 1         | []               | 1                    | 1             | 0                    | 0                            | 0                     | false
        [OPT_ITEMS_NO_ALBUM, OPT_ITEMS]                  | 1          | [[(ID): "id1"]]                                                    | true               | false         | 1         | []               | 1                    | 1             | 0                    | 1                            | 0                     | false
        [OPT_ITEMS_NO_ALBUM, OPT_ALBUM_ITEMS, OPT_ITEMS] | 1          | [[(ID): "id1"]]                                                    | true               | true          | 1         | []               | 1                    | 1             | 0                    | 1                            | 0                     | false
        [OPT_ITEMS_NO_ALBUM]                             | 1          | [[(ID): "id1", (MEDIA_ITEMS): [[(ID): "mine"]]]]                   | true               | false         | 1         | [[(ID): "mine"]] | 1                    | 0             | 0                    | 1                            | 0                     | false
        [OPT_ITEMS_NO_ALBUM]                             | 1          | [[(ID): "id1", (MEDIA_ITEMS): [[(ID): "shared"], [(ID): "mine"]]]] | true               | false         | 1         | [[(ID): "mine"]] | 1                    | 0             | 1                    | 1                            | 0                     | false
        [OPT_ITEMS_NO_ALBUM]                             | 1          | [[(ID): "id1", (MEDIA_ITEMS): [[(ID): "shared"]]]]                 | true               | false         | 1         | [[(ID): "mine"]] | 1                    | 0             | 1                    | 1                            | 1                     | false
        [OPT_ITEMS_NO_ALBUM]                             | 1          | [[(ID): "id1", (MEDIA_ITEMS): [[(ID): "shared"]]]]                 | true               | false         | 1         | [[(ID): "mine"]] | 1                    | 0             | 0                    | 1                            | 1                     | true
    }

    
    def "process albums"() {

        String token

        when:
        List found = application.processAlbums(token, retrieveAlbumItems, logAlbumItems, suppressWarnings)

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
        retrieveAlbumItems | logAlbumItems | albums                                  | albumItemCall | albumItems      | logItemsCountMismatch | logAlbumMediaItems | suppressWarnings
        false              | false         | []                                      | 0             | []              | 0                     | 0                  | false
        true               | false         | []                                      | 0             | []              | 0                     | 0                  | false
        true               | false         | [[(ID): "id1"]]                         | 1             | []              | 0                     | 0                  | false
        true               | false         | [[(ID): "id1", (MEDIA_ITEMS_COUNT): 1]] | 1             | []              | 1                     | 0                  | false
        true               | true          | [[(ID): "id1", (MEDIA_ITEMS_COUNT): 1]] | 1             | []              | 1                     | 1                  | false
        true               | true          | [[(ID): "id1", (MEDIA_ITEMS_COUNT): 1]] | 1             | [[(ID): "idx"]] | 0                     | 1                  | false
        true               | false         | [[(ID): "id1", (MEDIA_ITEMS_COUNT): 1]] | 1             | []              | 0                     | 0                  | true
    }

    
    def "get ignore media items map"() {

        when:
        Map map = application.getIgnoreMediaItemsMap()

        then:
        map == expected

        1 * application.getIgnoreMediaItemsFile() >> fileName
        call * application.getIgnoreMediaItemsMap(fileName) >> expected

        where:
        fileName    | call | expected
        null        | 0    | [:]
        "specified" | 1    | [:]
        "specified" | 1    | ["line1": "line1"]
    }


    def "get ignore media items map with from file"() {

        when:
        Map map = application.getIgnoreMediaItemsMap(fileName)

        then:
        map.keySet().containsAll(ignoreMediaItemsMap.keySet())

        1 * application.getFileContent(fileName) >> content

        where:
        fileName          | content           | ignoreMediaItemsMap
        "invalid"         | null              | [:]
        "valid empty"     | ""                | [:]
        "valid not empty" | "line1"           | ["line1": "line1"]
        "valid not empty" | " line1 \nline2"  | ["line1": "line1", "line2": "line2"]
        "valid not empty" | "# line1 \nline2" | ["line2": "line2"]
    }

    
    def "get file content"() {

        File file = Mock(constructorArgs: [fileName])

        when:
        String content = application.getFileContent(fileName)

        then:
        content == expected

        1 * application.getFile(fileName) >> file
        1 * file.exists() >> fileExists
        fileContentCall * application.getFileContent(file) >> expected

        where:
        fileName          | fileExists | expected       | fileContentCall
        "invalid"         | false      | null           | 0
        "valid empty"     | true       | ""             | 1
        "valid not empty" | true       | "some content" | 1
    }
}
