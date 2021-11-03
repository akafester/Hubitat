/**
 *  Ikea Styrbar
 *
 *
 * Modified from code written by motley74 and sticks18 by AnotherUser.
 * Original source: https://github.com/motley74/SmartThingsPublic/blob/master/devicetypes/motley74/osram-lightify-dimming-switch.src/osram-lightify-dimming-switch.groovy
 * This is modified on the bases made by AnotherUser
 * https://github.com/AnotherUser17/SmartThingsPublic-1/blob/master/devicetypes/AnotherUser/osram-lightify-4x-switch.src/osram-lightify-4x-switch.groovy
 *
 * v03112021 - Uploaded to Github. Initial release
 */




metadata {
	definition (name: "IKEA Styrbar", namespace: "Hubitat", author: "akafester") {
	
    capability "Actuator"
    capability "Battery"
    capability "PushableButton"
    capability "ReleasableButton"
    capability "HoldableButton"
    capability "Configuration"
    capability "Refresh"
        
    attribute "pushed", "Number"
    attribute "released", "Number"
    attribute "held", "Number"
       
		fingerprint inClusters: "0000,0001,0003,0020,1000,FC57", outClusters: "0003,0006,0008,0019,1000", manufacturer: "IKEA of Sweden", model: "Remote Control N2"
	    
    }
    preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "txtEnable", type: "bool", title: "Enable description logging", defaultValue: true
    input name: "betaFeature", type: "bool", title: "Enable hold and release", defaultValue: false, description: "Hold and release isn't working as expected with left and right."
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def installed() {
    configure()
    atomicState.releaseButton = 0
    updated()
}

def configure() {
    log.warn "Configuring Reporting and Bindings."
    sendEvent(name:"numberOfButtons", value: 4, isStateChange: true)
/*    def configCmds = [
//-------------------------Binding battery reporting------------------------//     
        "zdo bind 0x${device.deviceNetworkId} 1 1 1 {${device.zigbeeId}} {}",
    ]
    return configCmds */
    zigbee.configureReporting( 0x0001, 0x0021, DataType.UINT8, 1,86400, 1)
    zigbee.configureReporting( 0x0001, 0x0021, DataType.UINT8, 1,86400, 1)
    refresh()
}

def refresh() {
    //when refresh button is pushed, read updated status
    if (txtEnable) log.info "Refresh pushed... Attempting to read battery"
    def refreshCmds = [
        zigbee.readAttribute(0x0001, 0x0020),
        zigbee.readAttribute(0x0001, 0x0021)
    ]
    if (logEnable) log.debug "$refreshCmds"
    return refreshCmds
}

def parse(String description) {
    if (logEnable) log.debug "Parse description $description"
    def map = [:]
    
    if (logEnable) log.debug "Parse returned $map"
    
/*    if (description?.startsWith("read attr -")) {
        def descMap = zigbee.parseDescriptionAsMap(description)
        if (logEnable) log.debug "Read attr: $msg.clusterId, $msg.command, $descmap.attrId"
        // -------------------------Endpoint 21 - Cluster 1----------------------------//        
        if (descMap.cluster == "0001" && descMap.attrId == "0021") {
	        if (txtEnable) log.info "Battery percent"
	        map.name = "batteryPercent"
	        map.value = getBatteryPercent(descMap.value)
        } else {
            log.warn "Can't parse $descMap"
        }                                                                                    */
    
    if (description?.startsWith("read attr -")) {
        def descMap = zigbee.parse(description)
        if (logEnable) log.debug "Read attr: $descMap.clusterId, $descMap.command"
        // -------------------------Endpoint 21 - Cluster 1----------------------------//        
        if (descMap.clusterId == 1 && descMap.command == 21) {
	        if (txtEnable) log.info "Battery percent"
	        map.name = "batteryPercent"
	        map.value = getBatteryPercent(descMap.value)
        } else {
            log.warn "Can't parse $descMap"
        }
    }
    
    // Create a map from the raw zigbee message to make parsing more intuitive
    if (description?.startsWith("catchall")) {
        if (logEnable) log.debug "$description"
        def msg = zigbee.parse(description)
        if (logEnable) log.debug "Catchall: $msg.sourceEndpoint, $msg.clusterId, $msg.command, $msg.data"

        //-------------------------------Push commands--------------------------------//
        if (msg.clusterId == 6 && msg.command == 01) {
            def button = 1
			if (txtEnable) log.info "Button $button pushed - Top button"
            map.name = "Button $button pushed"
            push(button)
        } else if (msg.clusterId == 5 && msg.command == 07 && msg.data == [1, 1, 13, 0]) {
            def button = 2
			if (txtEnable) log.info "Button $button pushed - Left button"
            map.name = "Button $button pushed"
            push(button)
        } else if (msg.clusterId == 5 && msg.command == 07 && msg.data == [0, 1, 13, 0]) {
            def button = 3
			if (txtEnable) log.info "Button $button pushed - Right button"
            map.name = "Button $button pushed"
            push(button)
        } else if (msg.clusterId == 6 && msg.command == 00) {
            def button = 4
			if (txtEnable) log.info "Button $button pushed - Bottom button"
            map.name = "Button $button pushed"
            push(button)

		//-------------------------------Hold commands--------------------------------//
        } else if (msg.clusterId == 8 && msg.command == 05) {
            def button = 1
            atomicState.releaseButton = 1
			if (txtEnable) log.info "Button $button held - Top button"
            map.name = "Button $button held"
            hold(button)
	    } else if (msg.clusterId == 5 && msg.command == 8 && msg.data == [1, 13, 0] && betaFeature == true) {
            def button = 2
            atomicState.releaseButton = 2
			if (txtEnable) log.info "Button $button held - Left button"
            map.name = "Button $button held"
            hold(button)
        } else if (msg.clusterId == 8 && msg.command == 8 && msg.data == [0, 13, 0] && betaFeature == true) {
            def button = 3
            atomicState.releaseButton = 3
			if (txtEnable) log.info "Button $button held - Right button"
            map.name = "Button $button held"
            hold(button)
	    } else if (msg.clusterId == 8 && msg.command == 01) {
            def button = 4
            atomicState.releaseButton = 4
			if (txtEnable) log.info "Button $button held - Bottom button"
            map.name = "Button $button held"
            hold(button)
		//------------------------------Release commands-------------------------------//
        } else if (msg.clusterId == 8 && msg.command == 7) {                           //
            def button = atomicState.releaseButton                                     //
			if (txtEnable) log.info "Button $button released"                          //    The release command is the 
            if (atomicState.releaseButton == 1) {                                      //    same for both the top and bottom
                map.name = "Button $button released - Top button"                      //    buttons. 
            } else if (atomicState.releaseButton == 4) {                               //
                map.name = "Button $button released - Bottom button"                   //
            }                                                                          //
            release(button)                                                            //
        } else if (msg.clusterId == 5 && msg.command == 9 && betaFeature == true) {
            def button = atomicState.releaseButton
			if (txtEnable) log.info "Button $button released"
            map.name = "Button $button released - Left button"
            release(button) 
        } else if (msg.clusterId == 5 && msg.command == 9 && betaFeature == true) {
            def button = atomicState.releaseButton
			if (txtEnable) log.info "Button $button released"
            map.name = "Button $button released - Right button"
            release(button) 
        } 
	}
}

def push(button) {
    sendEvent(name:"pushed", value:button, isStateChange: true)
}

def hold(button) {
    sendEvent(name:"held", value:button, isStateChange: true)
}

def release(button) {
    sendEvent(name:"released", value:button, isStateChange: true)
}

// -------------------------Battery stuff----------------------------//

private getBatteryLevel(rawValue) {
    def intValue = Integer.parseInt(rawValue,16)
    def min = 2.1
    def max = 3.0
    def vBatt = intValue / 10
    return ((vBatt - min) / (max - min) * 100) as int
}

private getBatteryPercent(rawValue) {
    def intValue = Integer.parseInt(rawValue,8)
    def min = 0
    def max = 100
	def vBattPercent = intValue
    return ((intvalue / max) * 100) as int
}
