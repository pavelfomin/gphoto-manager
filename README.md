# Google Photos Manager
Take advantage of [Google Photos REST API](https://developers.google.com/photos/library/reference/rest) to:
 * Get the total number of albums
 * Get the total media items in all of the albums
 * Get total number of media items
 * Get the media items not included in any albums (some issues with shared media items)

Used [OAuth 2.0 Playground](https://developers.google.com/oauthplayground/?code=4/0AY0e-g7bssxEcyfWUnwKCC_a0e6RS0YWFJ921PEHiY1y1TOLZuwlIoSVXEiodSJtMMpkSA&scope=https://www.googleapis.com/auth/photoslibrary.readonly) 
to obtain Oauth JWT w/out setting up [Google API project](https://console.developers.google.com/apis/dashboard).

JWT is passed in the GooglePhotoService using -Dtoken=<access token>

## Usage
 - `./mvnw clean test`
 - `open target/spock-reports/index.html`
