package org.fbme.lib.iec61131.converter

import org.fbme.lib.iec61131.model.OldStandardXml
import org.fbme.lib.iec61499.declarations.DeviceDeclaration
import org.fbme.lib.iec61499.declarations.Mapping
import org.fbme.lib.iec61499.declarations.ResourceDeclaration
import org.fbme.lib.iec61499.declarations.SystemDeclaration
import org.fbme.lib.iec61499.fbnetwork.EntryKind
import org.fbme.lib.iec61499.fbnetwork.FBNetwork
import org.fbme.lib.iec61499.fbnetwork.FBNetworkConnection
import org.fbme.lib.iec61499.parser.STConverter

class SystemConverter(
    private val xmlProject: OldStandardXml.Project,
    private val converterArguments: ConverterArguments
) : ConverterBase(converterArguments) {

    private val pouInstance = xmlProject
        .instances
        .configurations
        .configurationList.firstOrNull()
        ?.resourceList?.firstOrNull()
        ?.taskList?.firstOrNull()
        ?.pouInstanceList?.firstOrNull()

    private fun getPou(): OldStandardXml.Pou {
        return xmlProject
            .types
            .pous
            .pouList
            .first { it.name == pouInstance!!.typeName }
    }

    fun createSystem(): SystemDeclaration {
        val system = factory.createSystemDeclaration(null)
        system.name = "MainSystem"
        if (pouInstance == null) {
            return system;
        }

        val application = factory.createApplicationDeclaration(null)
        application.name = "App"
        system.applications.add(application)

        system.devices.add(createDevice())

        val segment = factory.createSegmentDeclaration(null)
        segment.name = "Ethernet"
        segment.typeReference.setTargetName("Ethernet")
        system.segments.add(segment)

        val link = factory.createLink()
        link.resourceReference.setFQName("Testee.MAIN_RES")
        link.segmentReference.setTargetName("Ethernet")
        system.links.add(link)

        return system
    }

    private fun createMapping(res: String, app: String): Mapping {
        val cycleMap = factory.createMapping()
        cycleMap.resourceFBReference.setFQName(res)
        cycleMap.applicationFBReference.setFQName(app)
        return cycleMap
    }

    private fun createDevice(): DeviceDeclaration {
        val device = factory.createDeviceDeclaration(null)
        device.name = "Testee"

        device.typeReference.setTargetName("FORTE_PC")
        val mgrId = factory.createParameterAssignment()
        mgrId.parameterReference.setTargetName("MGR_ID")
        mgrId.value = STConverter.parseLiteral(stFactory, "'localhost:61499'")
        device.parameters.add(mgrId)

        device.resources.add(createResource())

        return device
    }

    private fun createResource(): ResourceDeclaration {
        val resource = factory.createResourceDeclaration(null)
        resource.name = "MAIN_RES"
        resource.typeReference.setTargetName("EMB_RES")
        val resourceNetwork = resource.network
        fillNetwork(resourceNetwork)
        return resource
    }

    private fun fillNetwork(network: FBNetwork) {

        val cycleTimeout = factory.createParameterAssignment()
        cycleTimeout.parameterReference.setTargetName("DT")
        cycleTimeout.value = STConverter.parseLiteral(stFactory, "T#3000ms")!!

        val cycle = factory.createFunctionBlockDeclaration(null)
        cycle.name = "E_CYCLE"
        cycle.typeReference.setTargetName("E_CYCLE")
        cycle.parameters.add(cycleTimeout)
        cycle.x = 500
        network.functionBlocks.add(cycle)

        FbNetworkConverter(
            getPou().bodyList[0].fbd!!,
            getPou().pouInterface!!,
            converterArguments,
            startEvent = "E_CYCLE.EO",
            endEvent = null
        ).fillNetwork(network);
        network.eventConnections.add(createEvent("START.COLD", "E_CYCLE.START"))
        network.eventConnections.add(createEvent("START.WARM", "E_CYCLE.START"))
    }

    private fun createEvent(from: String, to: String): FBNetworkConnection {
        val connection = factory.createFBNetworkConnection(EntryKind.EVENT)
        connection.sourceReference.setFQName(from)
        connection.targetReference.setFQName(to)
        return connection
    }
}