/*
 * Device handler for Sonoff-Tasmota firmware, modified to add additional functionality
 * Original single-switch version by Brett Sheleski
 * Modifications by Ben Johnson
 *
 * No license specified for Brett's original code.  Ben's modifications are released
 * under the Apache License, version 2.
 *
 */


metadata {
    definition(name: "Sonoff-Tasmota-Dual", namespace: "bdjohnson79", author: "Brett Sheleski, Ben Johnson") {
		capability "Switch"
		capability "Momentary"
		capability "Polling"
		capability "Refresh"
    }

	// UI tile definitions
    tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}' + ' 1', action: "momentary.push1", icon: "st.switches.switch.on", backgroundColor: "#79b821"
				attributeState "off", label: '${name}'+ ' 1', action: "momentary.push1", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
		}

    multiAttributeTile(name:"switch2", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch2", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}' + ' 2', action: "momentary.push2", icon: "st.switches.switch.on", backgroundColor: "#79b821"
				attributeState "off", label: '${name}' + ' 2', action: "momentary.push2", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
		}

		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "switch"
		details(["switch","switch2","refresh"])
	}

    preferences {
		input(name: "ipAddress", type: "string", title: "IP Address", description: "IP Address of Sonoff", displayDuringSetup: true, required: true)
		input(name: "port", type: "number", title: "Port", description: "Port", displayDuringSetup: true, required: true, defaultValue: 80)

		section("Sonoff Host") {

		}

		section("Authentication") {
			input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
			input(name: "password", type: "password", title: "Password", description: "Password", displayDuringSetup: false, required: false)
		}
    }
}

def parse(String description) {
    log.debug "parse()"

	def STATUS_PREFIX = "STATUS = ";
	def RESULT_PREFIX = "RESULT = ";

	def message = parseLanMessage(description);

	if (message?.body?.startsWith(STATUS_PREFIX)) {
		def statusJson = message.body.substring(STATUS_PREFIX.length())

		parseStatus(statusJson);
	}
	else if (message?.body?.startsWith(RESULT_PREFIX)) {
		def resultJson = message.body.substring(RESULT_PREFIX.length())

		parseResult(resultJson);
	}
}

def parseStatus(String json){
	log.debug "status: $json"

	def status = new groovy.json.JsonSlurper().parseText(json);

	def isOn1 = status.Status.Power1 == 1;
  def isOn2 = status.Status.Power2 == 1;

	setSwitchState(isOn1,isOn2);
}

def parseResult(String json){
	log.debug "result: $json"

	def result = new groovy.json.JsonSlurper().parseText(json);

	def isOn1 = result.POWER1 == "ON";
  def isOn2 = result.POWER2 == "ON";

	setSwitchState(isOn1, isOn2);
}

def setSwitchState(Boolean on1, Boolean on2){
	log.debug "Switch 1 is " + (on1 ? "ON" : "OFF")
  log.debug "Switch 2 is " + (on2 ? "ON" : "OFF")

	sendEvent(name: "switch", value: on1 ? "on" : "off");
  sendEvent(name: "switch2", value: on2 ? "on" : "off");
}

def push1(){
	log.debug "PUSH"
	toggle1(); // push is just an alias for toggle
}

def push2(){
	log.debug "PUSH"
	toggle2(); // push is just an alias for toggle
}

def toggle1(){
	log.debug "TOGGLE1"
    sendCommand("Power1", "Toggle");
}

def toggle2(){
	log.debug "TOGGLE"
    sendCommand("Power2", "Toggle");
}

def on1(){
	log.debug "ON1"
    sendCommand("Power1", "On");
}

def off1(){
	log.debug "OFF1"
    sendCommand("Power1", "Off");
}

def on2(){
	log.debug "ON2"
    sendCommand("Power2", "On");
}

def off2(){
	log.debug "OFF2"
    sendCommand("Power2", "Off");
}

def poll(){
	log.debug "POLL"

	requestStatus()

}

def refresh(){
	log.debug "REFRESH"

	requestStatus();
}

def requestStatus(){
	log.debug "getStatus()"

	def result = sendCommand("Status", null);

	return result;
}

private def sendCommand(String command, String payload){

    log.debug "sendCommand(${command}:${payload})"

    def hosthex = convertIPtoHex(ipAddress);
    def porthex = convertPortToHex(port);

    device.deviceNetworkId = "$hosthex:$porthex";

	def path = "/cm"

	if (payload){
		path += "?cmnd=${command}%20${payload}"
	}
	else{
		path += "?cmnd=${command}"
	}

	if (username){
		path += "&user=${username}"

		if (password){
			path += "&password=${password}"
		}
	}

    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: path,
        headers: [
            HOST: "${ipAddress}:${port}"
        ]
    )

    return result
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format('%04x', port.toInteger())
    log.debug hexport
    return hexport
}
