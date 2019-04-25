# A LDAP Sync adapter for Android


## Download

Downloads are available from [F-Droid](https://f-droid.org/de/packages/de.wikilab.android.ldapsync/) and [Play Store](https://play.google.com/store/apps/details?id=de.wikilab.android.ldapsync).

## Usage
There are multiple ways to configure a connection to a new LDAP server in the app.

* enter details manually
* click a configuration link
* scan a configuration QR code

### Manual configuration
Open the app and click the `+` button. Enter the details to the LDAP server as required, according to your server configuration.

### Configuration QR code
Open the app and click the camera button. Scan the provided QR code. 


## Configuration templates
If you administrate an LDAP server and want to make the configuration easier for your users, you can provide configuration links and QR codes. The required format is described below. Code in PHP and JS to generate can be found [here](https://github.com/d120/ldap-web/blob/master/ldapsync.php#L52), and a simplified version is provided below.

You can also use the provided [generator for config links](generator.html).

### Create configuration links and QR codes

The configuration link is of the following general format

    ldaps://hostname:port/?parameter1=value1&parameter2=value2& ... &parameterN=valueN

Instead of `ldaps://` you can also specify `ldap://` for an insecure connection to the LDAP server. I highly advise against using `ldap://`, especially when the phone is sometimes used in public networks, because the password will be transferred in clear text.

#### Parameters

| user | BindDN for login | 
| accountName | will be displayed in app and android settings (free form) |
| cfg_baseDN | BaseDN |
| cfg_searchFilter | LDAP Search filter, only entries matching this filter will be synced |
| skip | set to `1` to skip the detailed configuration screen |
| cfg_<mapping> | configure a mapping for non-standard attribute names (list below) |

All parameter values need to be URI component encoded (e.g. with `encodeURIComponent` in JavaScript). Please note that "&" and "=" must be encoded if they occur inside a value, but the delimiters between parameters must *not* be encoded.

#### QR code

To create a QR code, you need to build a configuration link and generate a QR code of that link with any QR code generator. 

#### Ldap attribute names
The following parameters can be added to a configuration link to map non-standard attribute names:

* cfg_givenName 
* cfg_sn
* cfg_telephonenumber 
* cfg_mobile
* cfg_homephone 
* cfg_mail
* cfg_jpegphoto 
* cfg_street
* cfg_postalCode 
* cfg_l
* cfg_st 
* cfg_co 


## Build from source

After an initial pull, open the project in Android Studio. To build an APK, use the `Build > Generate Signed APK ...` command.


## Source

The original source is hosted at https://github.com/weisserd/LDAP-Sync. An updated fork is hosted at https://github.com/max-weller/LDAP-Sync

## License 

This project is licensed under the Apache License v2.0.

## Configuration link example script
```html
<a href="javascript:" id="confUrl">Click here to configure LDAP-Sync</a>
<span id="confQr"></span>

<!-- QR code library - get it from here: https://raw.githubusercontent.com/davidshimjs/qrcodejs/master/qrcode.min.js -->

<script src="qrcode.min.js"></script>
<script>
// Define the parameters - change this according to your requirements
var host = "ldap.example.org";
var username = "jondoe";  // this might be read from an input field, from server side, or just ommitted, then the user has to enter it later on
var baseDN = "cn=example,cn=org";
var params = {
	user: "uid=" + username + "," + baseDN,
	accountName: "My little LDAP",
	cfg_baseDN: baseDN,
	cfg_searchFilter: "(objectClass=inetOrgPerson)",
	skip: "1",
	/* ... add more parameters here, if required */
}

// Build the URL
var url = "ldaps://"+host+"/?";
for (var k in params) url += k + "=" + encodeURIComponent(params[k]) + "&";

// Set URL in link tag
document.getElementById("confUrl").href = url;

// Generate QR code
new QRCode(document.getElementById("confQr"), {
	text: url,
	width: 400,
	height: 400,
	colorDark : "#000000",
	colorLight : "#ffffff",
	correctLevel : QRCode.CorrectLevel.H
});

</script>
```
