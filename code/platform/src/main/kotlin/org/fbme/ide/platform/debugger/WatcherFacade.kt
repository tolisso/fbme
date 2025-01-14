package org.fbme.ide.platform.debugger

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import jetbrains.mps.project.MPSProject
import jetbrains.mps.util.JDOMUtil
import org.fbme.ide.iec61499.repository.PlatformRepositoryProvider
import org.fbme.lib.common.Declaration
import org.fbme.lib.common.Identifier
import org.fbme.lib.iec61499.declarations.*
import org.fbme.lib.iec61499.descriptors.FBPortDescriptor
import org.fbme.lib.iec61499.fbnetwork.FunctionBlockDeclaration
import org.fbme.lib.iec61499.fbnetwork.PortPath
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.function.Function

class WatcherFacade(project: Project) : Disposable {
    private val devices: MutableMap<Identifier, MutableSet<WatchableData>> = HashMap()
    private val readWatchesListeners: MutableSet<ReadWatchesListener> = mutableSetOf()
    private val watchedValueListeners: MutableMap<WatchableData, MutableSet<WatchedValueListener>> = HashMap()
    private val repository = PlatformRepositoryProvider.getInstance(project.getComponent(MPSProject::class.java))

    private val devicesFacade = project.service<DevicesFacade>()

    private val thread: Thread = Thread {
        try {
            while (!Thread.currentThread().isInterrupted) {
                repository.mpsRepository.modelAccess.runReadInEDT {
                    for (deviceIdentifier in devices.keys) {
                        try {
                            val device = repository.declarationsScope.findDeviceDeclaration(deviceIdentifier)
                            requireNotNull(device)
                            val connection = devicesFacade.attach(device)
                            if (connection.isAlive) {
                                val resolvedWatches = resolveWatches(device, connection.readWatches())

                                for (listener in readWatchesListeners) {
                                    listener.onReadWatches(resolvedWatches)
                                }

                                for ((key, value) in resolvedWatches) {
                                    val listeners = watchedValueListeners[key] ?: continue
                                    for (listener in listeners) {
                                        listener.onValueChanged(value)
                                    }
                                }
                            }
                        } catch (e: Throwable) {

                        }
                    }
                }
                Thread.sleep(100)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    init {
        thread.start()
    }

    fun watchResourceNetwork(resourceDeclaration: ResourceDeclaration) {
        watchFBNetwork(resourceDeclaration, resourceDeclaration.allFunctionBlocks())
    }

    fun addWatchedValueListenersResourceNetwork(
        resourceDeclaration: ResourceDeclaration,
        createListener: Function<PortPath<out Declaration>, WatchedValueListener>
    ) {
        val children = resourceDeclaration.allFunctionBlocks()
        for (functionBlock in children) {
            val path = listOf(functionBlock)

            val fbTypeDeclaration = functionBlock.type.declaration as FBTypeDeclaration

            for (event in fbTypeDeclaration.inputEvents + fbTypeDeclaration.outputEvents) {
                val watchable = Watchable(WatchablePath(resourceDeclaration, path), event.name)
                val portPath = PortPath.createEventPortPath(functionBlock, event)
                val listener = createListener.apply(portPath)
                addWatchedValueListener(watchable.serialize(), listener)
            }
            for (variable in fbTypeDeclaration.inputParameters + fbTypeDeclaration.outputParameters) {
                val watchable = Watchable(WatchablePath(resourceDeclaration, path), variable.name)
                val portPath = PortPath.createDataPortPath(functionBlock, variable)
                val listener = createListener.apply(portPath)
                addWatchedValueListener(watchable.serialize(), listener)
            }
        }
    }

    private fun watchFBNetwork(
        resource: ResourceDeclaration,
        children: List<FunctionBlockDeclaration>,
        path: List<FunctionBlockDeclaration> = listOf()
    ) {
        for (functionBlock in children) {
            val newPath = path.toList().plus(functionBlock)

            val fbTypeDeclaration = functionBlock.type.declaration as FBTypeDeclaration

            for (event in fbTypeDeclaration.inputEvents) {
                watch(Watchable(WatchablePath(resource, newPath), event.name))
            }
            for (event in fbTypeDeclaration.outputEvents) {
                watch(Watchable(WatchablePath(resource, newPath), event.name))
            }
            for (variable in fbTypeDeclaration.inputParameters) {
                watch(Watchable(WatchablePath(resource, newPath), variable.name))
            }
            for (variable in fbTypeDeclaration.outputParameters) {
                watch(Watchable(WatchablePath(resource, newPath), variable.name))
            }

            when (fbTypeDeclaration) {
                is BasicFBTypeDeclaration -> {
                    watch(Watchable(WatchablePath(resource, newPath), "\$ECC"))
                    for (internalVariable in fbTypeDeclaration.internalVariables) {
                        watch(Watchable(WatchablePath(resource, newPath), internalVariable.name))
                    }
                }

                is CompositeFBTypeDeclaration -> {
                    watchFBNetwork(resource, fbTypeDeclaration.network.functionBlocks, newPath)
                }

                else -> {}
            }
        }
    }

    fun watch(watchable: Watchable) {
        val device = watchable.path.root.container as DeviceDeclaration
        val identifier = device.identifier
        try {
            devicesFacade.attach(device).addWatch(watchable)
        } catch (e: IOException) {
            LOG.error("on start watching", e)
        }
        val watchables = devices.computeIfAbsent(identifier) { k: Identifier? -> HashSet() }
        watchables.add(watchable.serialize())
    }

    fun unwatch(watchable: Watchable) {
        val device = watchable.path.root.container as DeviceDeclaration
        val identifier = device.identifier
        try {
            devicesFacade.attach(device).removeWatch(watchable)
        } catch (e: IOException) {
            LOG.error("on remove watching", e)
        }
        val watchables = devices[identifier] ?: return
        watchables.remove(watchable.serialize())
        if (watchables.isEmpty()) {
            devices.remove(identifier)
        }
    }

    fun isWatched(watchable: Watchable): Boolean {
        val device = watchable.path.root.container as DeviceDeclaration
        val identifier = device.identifier
        return devices[identifier]?.contains(watchable.serialize()) == true
    }

    fun addReadWatchesListener(listener: ReadWatchesListener) {
        readWatchesListeners.add(listener)
    }

    fun removeReadWatchesListener(listener: ReadWatchesListener) {
        readWatchesListeners.remove(listener)
    }

    fun addWatchedValueListener(watchable: WatchableData, listener: WatchedValueListener) {
        val listeners = watchedValueListeners.computeIfAbsent(watchable) { HashSet() }
        listeners.add(listener)
    }

    fun removeWatchedValueListener(watchable: WatchableData, listener: WatchedValueListener) {
        val listeners = watchedValueListeners[watchable] ?: return
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            watchedValueListeners.remove(watchable)
        }
    }

    private fun findPort(
        parent: FunctionBlockDeclaration,
        port: String
    ): FBPortDescriptor? {
        return parent.type.eventInputPorts.find { it.name == port }
            ?: parent.type.eventOutputPorts.find { it.name == port }
            ?: parent.type.dataInputPorts.find { it.name == port }
            ?: parent.type.dataOutputPorts.find { it.name == port }
    }

    override fun dispose() {
        thread.interrupt()
        thread.join()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WatcherFacade::class.java)

        fun resolveWatches(device: DeviceDeclaration, watch: String): Map<WatchableData, String> {
            return try {
                val doc = JDOMUtil.loadDocument(ByteArrayInputStream(watch.toByteArray()))
                val watches: MutableMap<WatchableData, String> = HashMap()
                val watchesElement = doc.rootElement.getChild("Watches")
                for (resourceElement in watchesElement.getChildren("Resource")) {
                    val resourceName = resourceElement.getAttributeValue("name")
                    val resource = device.resources.firstOrNull { it.name == resourceName }
                        ?: error("resource not found")
                    for (fbElement in resourceElement.getChildren("FB")) {
                        val fbPath = fbElement.getAttributeValue("name").split(".")

                        val firstFB = resource.allFunctionBlocks().firstOrNull { it.name == fbPath.first() }
                            ?: error("fb not found")

                        val fbDeclarationsPath = mutableListOf(firstFB)

                        var currentFB = firstFB

                        for (fbName in fbPath.drop(1)) {
                            val currentFBDeclaration = currentFB.type.declaration as FBTypeDeclaration

                            if (currentFBDeclaration is DeclarationWithNetwork) {
                                currentFB = currentFBDeclaration.network.functionBlocks
                                    .firstOrNull { it.name == fbName }
                                    ?: error("fb not found")
                            }

                            fbDeclarationsPath.add(currentFB)
                        }

                        for (portElement in fbElement.getChildren("Port")) {
                            val portName = portElement.getAttributeValue("name")
                            val dataElement = portElement.getChild("Data")
                            if (dataElement != null) {
                                watches[Watchable(WatchablePath(resource, fbDeclarationsPath), portName).serialize()] =
                                    dataElement.getAttributeValue("value")
                            }
                        }
                    }
                }
                watches
            } catch (e: Throwable) {
                error(e)
            }
        }
    }
}