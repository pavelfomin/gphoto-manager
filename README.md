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

## Usage
To list the available options:
```
./mvnw clean compile exec:java -Dexec.mainClass="com.droidablebee.gphoto.GooglePhotoService"
```
Example:
```
./mvnw clean compile exec:java -Dexec.mainClass="com.droidablebee.gphoto.GooglePhotoService" -Dexec.args="--items-no-album" -Dtoken=<access token>
```

## Running tests
 - `./mvnw clean test`
 - `open target/spock-reports/index.html`
