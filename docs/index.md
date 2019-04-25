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
If you administrate an LDAP server and want to make the configuration easier for your users, you can provide configuration links and QR codes. The required format is described below. Code to generate are provided [in PHP and JS](https://github.com/d120/ldap-web/blob/master/ldapsync.php#L52), and as a [simplified version in JS only](https://github.com/max-weller/LDAP-Sync/blob/master/docs/config_example.html).

You can also use the [generator for config links](generator.html).

### Create configuration links and QR codes

The configuration link is of the following general format

    ldaps://hostname:port/?parameter1=value1&parameter2=value2& ... &parameterN=valueN

Instead of `ldaps://` you can also specify `ldap://` for an insecure connection to the LDAP server. I highly advise against using `ldap://`, especially when the phone is sometimes used in public networks, because the password will be transferred in clear text.

#### Parameters

| Parameter | Description |
|-
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


## Source

The original source is hosted at https://github.com/weisserd/LDAP-Sync. An updated fork is hosted at https://github.com/max-weller/LDAP-Sync

## License 

This project is licensed under the Apache License v2.0.
