# Google Photos Manager
Take advantage of [Google Photos REST API](https://developers.google.com/photos/library/reference/rest) to:
 * Get the total number of albums
 * Get the total media items in all of the albums
 * Get total number of media items
 * Get the media items not included in any albums (some issues with shared media items)

Used [OAuth 2.0 Playground](https://developers.google.com/oauthplayground)
to obtain Oauth JWT w/out setting up [Google API project](https://console.developers.google.com/apis/dashboard)
using "https://www.googleapis.com/auth/photoslibrary.readonly" as a requested scope.

Access token is passed to the GooglePhotoService using -Dtoken=<access token>

## Download
Download [latest version](https://github.com/pavelfomin/gphoto-manager/releases)

### Java 11 runtime
Use 0.1.x versions

### Java 17+ runtime
Use 1.x versions

## Usage
To list the available options:
```
java -jar gphoto-manager-<version>.jar
```
Examples:
```
java -Dtoken=<access token> -jar gphoto-manager-<version>.jar --albums
java -Dtoken=<access token> -jar gphoto-manager-<version>.jar --items-no-album
```

### Maven wrapper (for developers)
```
./mvnw clean compile exec:java -Dexec.mainClass="com.droidablebee.gphoto.GooglePhotoApplication"
./mvnw clean compile exec:java -Dexec.mainClass="com.droidablebee.gphoto.GooglePhotoApplication" -Dexec.args="--items-no-album" -Dtoken=<access token>
```

### Running tests
 - `./mvnw clean test`
 - `open target/spock-reports/index.html`

## Issues with shared media items
For a shared album owned by account A, if a media item is added from another account B then its id won't be found in the list of the media items of the account A.
If such a shared media item is saved in account A its id won't match the id of the original shared media item added to the shared album.
W/out an indicator of some sort to identify if a media item belongs to another account it's challenging to filter them out.
