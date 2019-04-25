# Configuration generator

<style>
	label {display:block;}
	form div {padding:10px;border-top:1px solid #eee;}
	div input[type=text], textarea {width:100%}
</style>
<form name="x">
	<div>
		<label>Protocol</label>
		<select name="scheme"><option>ldaps</option><option>ldap</option></select>
	</div>
	<div>
		<label>Host name</label>
		<input name="host">
	</div>
	<div>
		<label>Port</label>
		<input name="port" value="636">
	</div>
	
</form>

<p><input type="button" onclick="buildUrl()" value="Generate URL and QR code"></p>

<p>Output:</p>
<p><textarea id="confUrl"></textarea></p>
<p style="height:450px; padding:20px"><span id="confQr"></span></p>


<!-- QR code library - get it from here: https://raw.githubusercontent.com/davidshimjs/qrcodejs/master/qrcode.min.js -->

<script src="qrcode.min.js"></script>
<script>
// Define the possible parameters
var paramDefs = {
	"user": "BindDN for login ",
	"accountName": "will be displayed in app and android settings (free form)",
	"cfg_baseDN": "BaseDN",
	"cfg_searchFilter": "LDAP Search filter, only entries matching this filter will be synced",
	"skip": "set to `1` to skip the detailed configuration screen",
	"cfg_givenName" : "mapping for givenName ",
	"cfg_sn" : "mapping for sn",
	"cfg_telephonenumber" : "mapping for telephonenumber ",
	"cfg_mobile" : "mapping for mobile",
	"cfg_homephone" : "mapping for homephone ",
	"cfg_mail" : "mapping for mail",
	"cfg_jpegphoto" : "mapping for jpegphoto ",
	"cfg_street" : "mapping for street",
	"cfg_postalCode" : "mapping for postalCode ",
	"cfg_l" : "mapping for l",
	"cfg_st" : "mapping for st ",
	"cfg_co" : "mapping for co ",
}

var form = document.x;
for (var k in paramDefs) {
	var div = document.createElement("div");
	var lbl = document.createElement("label");
	lbl.setAttribute("for", "set_" + k);
	lbl.innerHTML = "<input type='checkbox' id='set_"+k+"'> <b><tt>" + k + "</tt></b> " + paramDefs[k];
	div.appendChild(lbl);
	var inp = document.createElement("input");
	inp.name = k; inp.type="text";
	div.appendChild(inp);
	form.appendChild(div);
}

function get(name) {
	return encodeURIComponent(form[name].value)
}

function buildUrl() {
	// Build the URL
	var url = get("scheme")+"://"+get("host")+":"+get("port")+"/?";
	for (var k in paramDefs) url += k + "=" + get(k) + "&";

	// Set URL in link tag
	document.getElementById("confUrl").value = url;

	// Generate QR code
	var qr = document.getElementById("confQr");
	qr.innerHTML = "";
	new QRCode(qr, {
		text: url,
		width: 400,
		height: 400,
		colorDark : "#000000",
		colorLight : "#ffffff",
		correctLevel : QRCode.CorrectLevel.H
	});
}
</script>

